package playerpose.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import playerpose.common.MpmBridge;
import playerpose.pose.PoseDefinition;
import playerpose.pose.PoseRegistry;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 三人称描画の開始・終了を見張る。
 * ・「三人称描画中」の印を立てる（一人称の腕にポーズが当たらないように）
 * ・横ずらし（sideShift）のあるポーズなら、体全体の見た目を傾く方へずらす（側面監視）
 * ・手を上げるポーズなら、手持ちアイテムの描画の間だけ左手側に描くよう切り替える
 *
 * MPMの処理（通常優先度）の後に動くよう LOWEST にしている。
 */
public class PoseRenderHandler {

    private final Deque<Boolean> pushed = new ArrayDeque<Boolean>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderPre(RenderPlayerEvent.Pre event) {
        PoseRenderState.thirdPersonDepth++;
        EntityPlayer player = event.entityPlayer;
        float shift = sideShift(player);
        boolean push = shift != 0F;
        if (push) {
            // この時点の座標軸はワールドの向き。体の向きから「キャラの左」の方向を求めてずらす
            float yaw = player.prevRenderYawOffset
                    + MathHelper.wrapAngleTo180_float(player.renderYawOffset - player.prevRenderYawOffset)
                    * event.partialRenderTick;
            double rad = Math.toRadians(yaw);
            GL11.glPushMatrix();
            GL11.glTranslated(Math.cos(rad) * shift, 0.0D, Math.sin(rad) * shift);
        }
        pushed.push(push);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderPost(RenderPlayerEvent.Post event) {
        if (pushed.isEmpty()) {
            return;
        }
        if (pushed.pop()) {
            GL11.glPopMatrix();
        }
        PoseRenderState.thirdPersonDepth = Math.max(0, PoseRenderState.thirdPersonDepth - 1);
    }

    /**
     * 手持ちアイテム・帽子などの描画の直前。MPMはこのイベント（通常優先度）の中でアイテムを描くので、
     * それより前（HIGHEST）に右腕の欄を左手用の代役へ差し替える。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpecialsPre(RenderPlayerEvent.Specials.Pre event) {
        ModelPoseMPM model = MpmModelInstaller.mainModel;
        if (model != null) {
            model.beginItemSwap(event.entityPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSpecialsPost(RenderPlayerEvent.Specials.Post event) {
        ModelPoseMPM model = MpmModelInstaller.mainModel;
        if (model != null) {
            model.endItemSwap();
        }
    }

    /**
     * 保険：他modの描画中の例外などで Post が来なかった場合に印が残らないよう、毎フレームの最初に戻す。
     * （FMLのイベントバスに登録する）
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            pushed.clear();
            PoseRenderState.thirdPersonDepth = 0;
            ModelPoseMPM model = MpmModelInstaller.mainModel;
            if (model != null) {
                model.endItemSwap();
            }
        }
    }

    /** 今このプレイヤーを横へずらす量（ブロック、プラスでキャラの左）。ずらさないなら 0。 */
    private static float sideShift(EntityPlayer player) {
        int pose = PoseRenderState.resolvePose(player); // 選択画面のプレビューもここで判定される
        if (pose <= 0 || MpmBridge.isAnimating(player)) {
            return 0F;
        }
        PoseDefinition def = PoseRegistry.get(pose);
        return def != null ? def.sideShift : 0F;
    }
}
