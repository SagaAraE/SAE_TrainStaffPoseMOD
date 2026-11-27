package playerpose.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import playerpose.common.ClientPoseCache;
import playerpose.common.MpmBridge;
import playerpose.common.Poses;
import playerpose.pose.PoseDefinition;
import playerpose.pose.PoseMath;
import playerpose.pose.PoseRegistry;

/**
 * 側面監視の仲間（3・4・8・9）の間、自分の視点（カメラ）を横（傾く方向）へずらす（一人称・三人称とも）。
 * ・横：傾いた頭の目の位置（乗車中は横移動の調整分も含む）＋ 調整画面の「視点の横移動（追加）」
 * ・上下：調整画面の「視点の高さ（追加）」だけ（頭の傾きや乗車中の高さでは動かさない）
 * ・視界は傾けない
 *
 * 1.7.10 の Forge にはカメラを直接動かす仕組みが無いので、画面を描いている間（RenderTick の START〜END）だけ
 * 自分の座標をずらし、自分の体はその分だけ逆に戻して描く（ModelPoseMPM が bodyCompensation を使う）。
 * END で必ず元に戻すので、移動処理やサーバーとの通信には影響しない。
 * ポーズの開始・解除時は約0.2秒かけて滑らかに動かす。調整画面のオン・オフ（PoseConfig.cameraFollowHead）で切り替える。
 */
public class CameraFollowHandler {

    /** 滑らかさの時定数（秒）。0.2秒でほぼ（95%）追いつく。 */
    private static final float TIME_CONSTANT = 0.067F;

    /** 今のずらし量 {横（モデル座標のピクセル、+X=左）, 上（ピクセル、上がプラス）} */
    private final float[] current = new float[2];
    private long lastNanos = -1L;

    /** 描画中に自分の座標へ足しているワールドのずらし量 {x, y, z}（足していなければ null） */
    private double[] applied;
    /** 描画中に自分の体を戻す量（モデル座標のピクセル {X, Y}）。無ければ null */
    private static float[] compensation;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            updateSmoothing();
            apply(event.renderTickTime);
        } else {
            restore();
        }
    }

    /** ModelPoseMPM から呼ばれる。自分の体を描くときだけ、ずらした分を戻す量を返す。 */
    public static float[] bodyCompensation(Entity entity) {
        float[] c = compensation;
        if (c == null || PoseRenderState.previewing || entity != Minecraft.getMinecraft().thePlayer) {
            return null;
        }
        return c;
    }

    private void updateSmoothing() {
        long now = System.nanoTime();
        float dt = lastNanos < 0L ? 0F : Math.min(0.5F, (now - lastNanos) / 1.0e9F);
        lastNanos = now;
        float[] target = target();
        float k = 1.0F - (float) Math.exp(-dt / TIME_CONSTANT);
        for (int i = 0; i < 2; i++) {
            current[i] += (target[i] - current[i]) * k;
            if (Math.abs(target[i] - current[i]) < 0.001F) {
                current[i] = target[i];
            }
        }
    }

    /** 今あるべきずらし量 {横, 上}（対象のポーズ中でなければ0）。 */
    private static float[] target() {
        float[] zero = {0F, 0F};
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || !PoseConfig.cameraFollowHead) {
            return zero;
        }
        int pose = ClientPoseCache.getPose(player.getEntityId());
        if (!Poses.isSideWatch(pose) || MpmBridge.isAnimating(player)) {
            return zero;
        }
        PoseDefinition def = PoseRegistry.get(pose);
        if (def == null || def.bodyLean == 0F) {
            return zero;
        }
        float dir = def.bodyLean > 0F ? 1F : -1F; // 傾く方向（+X=左）
        float headDeg = def.bodyLean + def.parts[PoseDefinition.HEAD][2];
        float side = PoseMath.eyeShift(def.bodyLean, headDeg)[0];
        if (player.ridingEntity != null) {
            ClientPoseCache.Entry e = ClientPoseCache.get(player.getEntityId());
            if (e != null) {
                side += dir * e.sideShift;
            }
        }
        side += dir * PoseConfig.cameraExtraSide / PoseMath.PIXEL_TO_BLOCK;
        float up = PoseConfig.cameraExtraHeight / PoseMath.PIXEL_TO_BLOCK;
        return new float[] {side, up};
    }

    private void apply(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.thePlayer;
        if (p == null || mc.theWorld == null || mc.renderViewEntity != p || (current[0] == 0F && current[1] == 0F)) {
            return;
        }
        double bodyYaw = Math.toRadians(p.prevRenderYawOffset + (p.renderYawOffset - p.prevRenderYawOffset) * partialTicks);
        double s = PoseMath.PIXEL_TO_BLOCK;
        double dx = Math.cos(bodyYaw) * current[0] * s; // 体の左方向 = (cos, 0, sin)
        double dy = current[1] * s;
        double dz = Math.sin(bodyYaw) * current[0] * s;

        shift(p, dx, dy, dz);
        applied = new double[] {dx, dy, dz};
        // 体を戻す量（モデル座標は +Y が下なので、上へずらした分は +Y で戻す）
        compensation = new float[] {-current[0], current[1]};
    }

    private void restore() {
        compensation = null;
        if (applied != null) {
            EntityPlayer p = Minecraft.getMinecraft().thePlayer;
            if (p != null) {
                shift(p, -applied[0], -applied[1], -applied[2]);
            }
            applied = null;
        }
    }

    private static void shift(EntityPlayer p, double dx, double dy, double dz) {
        p.posX += dx;
        p.posY += dy;
        p.posZ += dz;
        p.prevPosX += dx;
        p.prevPosY += dy;
        p.prevPosZ += dz;
        p.lastTickPosX += dx;
        p.lastTickPosY += dy;
        p.lastTickPosZ += dz;
    }
}
