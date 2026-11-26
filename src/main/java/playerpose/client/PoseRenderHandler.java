package playerpose.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import playerpose.common.ClientPoseCache;
import playerpose.common.MpmBridge;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 三人称描画の開始・終了を見張る。
 * ・「三人称描画中」の印を立てる（一人称の腕にポーズが当たらないように）
 * ・乗車中かつポーズ中なら見た目を上へずらす（RTM車両で脚が床を貫通しないように）
 *
 * MPMの処理（通常優先度）の後に動くよう LOWEST にしている。
 */
public class PoseRenderHandler {

    private final Deque<Boolean> pushed = new ArrayDeque<Boolean>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderPre(RenderPlayerEvent.Pre event) {
        PoseRenderState.thirdPersonDepth++;
        boolean push = shouldRaise(event.entityPlayer);
        if (push) {
            GL11.glPushMatrix();
            GL11.glTranslated(0.0D, PoseConfig.ridingYOffset, 0.0D);
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
     * 保険：他modの描画中の例外などで Post が来なかった場合に印が残らないよう、毎フレームの最初に戻す。
     * （FMLのイベントバスに登録する）
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            pushed.clear();
            PoseRenderState.thirdPersonDepth = 0;
        }
    }

    private static boolean shouldRaise(EntityPlayer player) {
        if (PoseRenderState.previewing || player.ridingEntity == null) {
            return false;
        }
        if (ClientPoseCache.getPose(player.getEntityId()) <= 0 || MpmBridge.isAnimating(player)) {
            return false;
        }
        // 一人称視点で自分が描かれるのはインベントリ画面などのGUIだけなので、そこではずらさない
        Minecraft mc = Minecraft.getMinecraft();
        return !(player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0);
    }
}
