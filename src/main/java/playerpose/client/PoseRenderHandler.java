package playerpose.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.event.RenderPlayerEvent;
import playerpose.pose.PoseDefinition;
import playerpose.pose.PoseRegistry;

/**
 * プレイヤー描画の開始・終了などを見張る。
 * ・「三人称描画中」の印を立てる（一人称の腕にポーズが当たらないように）
 * ・設定で指定したポーズ（初期値：5・6 手上げ）では、手持ちアイテムを
 *   上げている腕に持たせ、絵を左右反転して描く（持ち手は手の位置のまま）
 */
public class PoseRenderHandler {

    private FlippedItemRenderer flippedRenderer;
    /** 差し替え中に退避した元の描画部品（差し替えていなければ null） */
    private ItemRenderer savedRenderer;
    private boolean movedToLeftArm;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderPre(RenderPlayerEvent.Pre event) {
        PoseRenderState.thirdPersonDepth++;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderPost(RenderPlayerEvent.Post event) {
        PoseRenderState.thirdPersonDepth = Math.max(0, PoseRenderState.thirdPersonDepth - 1);
    }

    /**
     * 手持ちアイテム等の描画（Specials）の直前。MPMはここ（通常優先度）でアイテムを描く。
     * その前（HIGHEST）に、上げている腕へアイテムを移し、アイテムの描画部品を反転版に差し替える。
     * 後（LOWEST）で元に戻す。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpecialsBefore(RenderPlayerEvent.Specials.Pre event) {
        ModelPoseMPM model = MpmModelInstaller.mainModel;
        if (model == null || model.isMpmAnimating() || savedRenderer != null) {
            return;
        }
        int pose = PoseRenderState.resolvePose(event.entityPlayer);
        if (pose <= 0 || !PoseConfig.isItemMirrored(pose)) {
            return;
        }
        PoseDefinition def = PoseRegistry.get(pose);
        if (def == null) {
            return;
        }
        if (flippedRenderer == null) {
            flippedRenderer = new FlippedItemRenderer(Minecraft.getMinecraft());
        }
        if (def.isRightArmRaised()) {
            FlippedItemRenderer.handSign = 1.0F;
        } else {
            FlippedItemRenderer.handSign = -1.0F;
            model.beginItemInLeftArm();
            movedToLeftArm = true;
        }
        savedRenderer = RenderManager.instance.itemRenderer;
        RenderManager.instance.itemRenderer = flippedRenderer;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onSpecialsAfter(RenderPlayerEvent.Specials.Pre event) {
        restore();
    }

    /** 保険：途中で描画が止まっても差し替えや印が残らないよう、毎フレームの最初に戻す（FMLのイベントバスに登録）。 */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PoseRenderState.thirdPersonDepth = 0;
            restore();
        }
    }

    private void restore() {
        if (savedRenderer != null) {
            RenderManager.instance.itemRenderer = savedRenderer;
            savedRenderer = null;
        }
        if (movedToLeftArm) {
            movedToLeftArm = false;
            if (MpmModelInstaller.mainModel != null) {
                MpmModelInstaller.mainModel.endItemInLeftArm();
            }
        }
    }
}
