package playerpose.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import playerpose.common.ClientPoseCache;
import playerpose.common.Poses;

/** 描画中の状態（描画はメインスレッドだけなので static で持つ）。 */
public final class PoseRenderState {

    /** 三人称描画（RenderPlayerEvent.Pre〜Post）の入れ子の深さ。0 なら一人称の腕などなのでポーズを当てない。 */
    public static int thirdPersonDepth = 0;

    /** 選択画面のプレビュー描画中。 */
    public static boolean previewing = false;
    /** プレビューで見せるポーズ番号。 */
    public static int previewPose = Poses.NONE;

    private PoseRenderState() {
    }

    /** このエンティティに今当てるべきポーズ番号（無ければ 0）。 */
    public static int resolvePose(Entity entity) {
        if (thirdPersonDepth <= 0 || !(entity instanceof EntityPlayer)) {
            return Poses.NONE;
        }
        if (previewing) {
            return entity == Minecraft.getMinecraft().thePlayer ? previewPose : Poses.NONE;
        }
        return ClientPoseCache.getPose(entity.getEntityId());
    }
}
