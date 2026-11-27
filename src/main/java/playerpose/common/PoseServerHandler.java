package playerpose.common;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import playerpose.network.MsgPoseSync;
import playerpose.network.PoseNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側のポーズ管理。
 * ・ポーズ要求の受付（通信スレッドから来るので、いったん pending に積んで tick で処理する）
 *   同じポーズのまま調整値だけ変わった要求も、ここで受けて周囲へ配り直す
 * ・解除判定：スニークを始めた瞬間のみ（移動・乗車では解除しない）
 *   ※死亡・リスポーン・ディメンション移動・ログアウト時は状態の後片付けとして解除する
 * ・周囲への同期（変化時に即送信 + ポーズ中は2秒ごとに送り直し）
 * ・使用権限（PosePermission）：要求のたびに確認。ログイン時と、権限が変わった時（2秒ごとに確認、
 *   /posemode での切り替え時は即時）に本人へ知らせ、使えなくなった人のポーズはその場で解除する
 */
public class PoseServerHandler {

    /** ポーズ中の送り直し間隔（tick）。 */
    private static final int HEARTBEAT_TICKS = 40;
    /** 同期を送る範囲（ブロック）。 */
    private static final double SYNC_RANGE = 160.0D;

    private static final Map<UUID, float[]> PENDING = new ConcurrentHashMap<UUID, float[]>();

    private final Map<UUID, State> states = new HashMap<UUID, State>();
    /** 最後に本人へ知らせた「使えるかどうか」 */
    private final Map<UUID, Boolean> lastAllowed = new HashMap<UUID, Boolean>();
    /** 権限の確認間隔（tick） */
    private static final int PERMISSION_CHECK_TICKS = 40;

    private static PoseServerHandler instance;

    public PoseServerHandler() {
        instance = this;
    }

    /** /posemode で切り替えた直後に呼ぶ。全員の権限をすぐに確認し直す。 */
    public static void recheckAllPermissions() {
        if (instance == null) {
            return;
        }
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (server == null) {
            return;
        }
        for (Object o : server.getConfigurationManager().playerEntityList) {
            instance.checkPermission((EntityPlayer) o);
        }
    }

    /** 使えるかどうかが変わっていれば本人に知らせ、使えなくなったらポーズを解除する。 */
    private void checkPermission(EntityPlayer player) {
        boolean allowed = PosePermission.canUse(player);
        Boolean last = lastAllowed.get(player.getUniqueID());
        if (last == null || last != allowed) {
            lastAllowed.put(player.getUniqueID(), allowed);
            if (player instanceof EntityPlayerMP) {
                PoseNetwork.CHANNEL.sendTo(new playerpose.network.MsgPermission(allowed), (EntityPlayerMP) player);
            }
        }
        State st = states.get(player.getUniqueID());
        if (!allowed && st != null && !PosePermission.isOpenToAll(st.pose)) { // 起立はそのまま
            release(player);
            player.addChatMessage(new ChatComponentTranslation("playerpose.msg.noPermission"));
        }
    }

    private static final class State {
        int pose;
        float sideShift;
        float height;
        boolean sneaking;
        int ticks;
    }

    /** 通信スレッドから呼ばれる。実際の処理は次の PlayerTickEvent で行う（同じtick内の要求は最後のものだけ使う）。 */
    public static void request(EntityPlayerMP player, int pose, float sideShift, float height) {
        PENDING.put(player.getUniqueID(), new float[] {pose, sideShift, height});
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        UUID id = player.getUniqueID();

        if (player.ticksExisted % PERMISSION_CHECK_TICKS == 0) { // /op・/deop の反映（2秒ごと）
            checkPermission(player);
        }

        float[] requested = PENDING.remove(id);
        if (requested != null) {
            handleRequest(player, (int) requested[0], requested[1], requested[2]);
            return; // 要求を処理したtickは解除判定をしない
        }

        State st = states.get(id);
        if (st == null) {
            return;
        }

        boolean sneaking = player.isSneaking();
        if (sneaking && !st.sneaking) { // スニークを始めた瞬間
            release(player);
            return;
        }
        st.sneaking = sneaking;

        if (++st.ticks % HEARTBEAT_TICKS == 0) {
            broadcast(player, st);
        }
    }

    private void handleRequest(EntityPlayer player, int pose, float sideShift, float height) {
        UUID id = player.getUniqueID();
        if (pose == Poses.NONE) {
            if (states.containsKey(id)) {
                release(player);
            }
            return;
        }
        if (!Poses.isValid(pose)) {
            return;
        }
        if (!PosePermission.canUsePose(player, pose)) {
            player.addChatMessage(new ChatComponentTranslation("playerpose.msg.noPermission"));
            lastAllowed.put(id, false);
            if (player instanceof EntityPlayerMP) { // クライアント側の表示もそろえる（必ず送り直す）
                PoseNetwork.CHANNEL.sendTo(new playerpose.network.MsgPermission(false), (EntityPlayerMP) player);
            }
            return;
        }
        State st = states.get(id);
        boolean alreadyPosing = st != null;
        // 新しくポーズを取るときだけ制限する（ポーズ中の調整値の変更は通す）
        // ※MPMのモーションはサーバー側の状態が当てにならない（歩いて解除しても残る）ので見ない。
        //   MPMのモーション中なら、クライアント側で自動解除してから要求が来る。
        if (!alreadyPosing && (player.isSneaking() || player.isPlayerSleeping())) {
            player.addChatMessage(new ChatComponentTranslation("playerpose.msg.cannotPose"));
            return;
        }
        if (st == null) {
            st = new State();
            st.sneaking = player.isSneaking();
            states.put(id, st);
        }
        st.pose = pose;
        st.sideShift = Poses.clamp(sideShift, Poses.SIDE_SHIFT_MIN, Poses.SIDE_SHIFT_MAX);
        st.height = Poses.clamp(height, Poses.HEIGHT_MIN, Poses.HEIGHT_MAX);
        broadcast(player, st);
    }

    private void release(EntityPlayer player) {
        states.remove(player.getUniqueID());
        send(player, new MsgPoseSync(player.getEntityId(), Poses.NONE, 0F, 0F));
    }

    private static void broadcast(EntityPlayer player, State st) {
        send(player, new MsgPoseSync(player.getEntityId(), st.pose, st.sideShift, st.height));
    }

    private static void send(EntityPlayer player, MsgPoseSync msg) {
        PoseNetwork.CHANNEL.sendToAllAround(msg,
                new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, SYNC_RANGE));
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        lastAllowed.remove(event.player.getUniqueID());
        checkPermission(event.player); // ログインしたら、使えるかどうかを知らせる
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        lastAllowed.remove(id);
        PENDING.remove(id);
        if (states.containsKey(id)) {
            release(event.player);
        }
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (states.containsKey(event.player.getUniqueID())) {
            release(event.player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (states.containsKey(event.player.getUniqueID())) {
            release(event.player);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP) {
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            if (states.containsKey(player.getUniqueID())) {
                release(player);
            }
        }
    }
}
