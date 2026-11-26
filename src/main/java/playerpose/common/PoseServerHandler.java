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
 * ・解除判定（移動 / スニーク開始 / 乗車・降車・乗り換え / MPMモーション開始 / 就寝）
 * ・周囲への同期（変化時に即送信 + ポーズ中は2秒ごとに送り直し）
 */
public class PoseServerHandler {

    /** これを超えて1tickで動いたら「移動」とみなす（ブロック）。 */
    private static final double MOVE_THRESHOLD = 0.03D;
    /** ポーズ中の送り直し間隔（tick）。 */
    private static final int HEARTBEAT_TICKS = 40;
    /** 同期を送る範囲（ブロック）。 */
    private static final double SYNC_RANGE = 160.0D;

    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<UUID, Integer>();

    private final Map<UUID, State> states = new HashMap<UUID, State>();

    private static final class State {
        int pose;
        boolean sneaking;
        int ridingId;
        boolean mpmAnimating;
        double x;
        double y;
        double z;
        int ticks;
    }

    /** 通信スレッドから呼ばれる。実際の処理は次の PlayerTickEvent で行う。 */
    public static void request(EntityPlayerMP player, int pose) {
        PENDING.put(player.getUniqueID(), pose);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        UUID id = player.getUniqueID();

        Integer requested = PENDING.remove(id);
        if (requested != null) {
            handleRequest(player, requested);
            return; // 開始直後のtickは解除判定をしない
        }

        State st = states.get(id);
        if (st == null) {
            return;
        }

        boolean sneaking = player.isSneaking();
        int ridingId = ridingId(player);
        boolean mpm = MpmBridge.isAnimating(player);

        boolean cancel = (sneaking && !st.sneaking)   // スニークを始めた瞬間
                || ridingId != st.ridingId            // 乗った / 降りた / 乗り換えた瞬間
                || (mpm && !st.mpmAnimating)          // MPMのモーションを始めた瞬間
                || player.isPlayerSleeping();

        if (!cancel && ridingId == -1) {              // 乗車中は車両の移動なので判定しない
            double dx = player.posX - st.x;
            double dy = player.posY - st.y;
            double dz = player.posZ - st.z;
            if (dx * dx + dy * dy + dz * dz > MOVE_THRESHOLD * MOVE_THRESHOLD) {
                cancel = true;
            }
        }

        if (cancel) {
            release(player);
            return;
        }

        snapshot(st, player);
        if (++st.ticks % HEARTBEAT_TICKS == 0) {
            broadcast(player, st.pose);
        }
    }

    private void handleRequest(EntityPlayer player, int pose) {
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
        if (player.isSneaking() || player.isPlayerSleeping() || MpmBridge.isAnimating(player)) {
            player.addChatMessage(new ChatComponentTranslation("playerpose.msg.cannotPose"));
            return;
        }
        State st = new State();
        st.pose = pose;
        snapshot(st, player);
        states.put(id, st);
        broadcast(player, pose);
    }

    private void release(EntityPlayer player) {
        states.remove(player.getUniqueID());
        broadcast(player, Poses.NONE);
    }

    private static void snapshot(State st, EntityPlayer player) {
        st.sneaking = player.isSneaking();
        st.ridingId = ridingId(player);
        st.mpmAnimating = MpmBridge.isAnimating(player);
        st.x = player.posX;
        st.y = player.posY;
        st.z = player.posZ;
    }

    private static int ridingId(EntityPlayer player) {
        return player.ridingEntity != null ? player.ridingEntity.getEntityId() : -1;
    }

    private static void broadcast(EntityPlayer player, int pose) {
        PoseNetwork.CHANNEL.sendToAllAround(new MsgPoseSync(player.getEntityId(), pose),
                new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, SYNC_RANGE));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        PENDING.remove(id);
        if (states.remove(id) != null) {
            broadcast(event.player, Poses.NONE);
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
