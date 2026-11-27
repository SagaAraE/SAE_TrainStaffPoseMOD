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

    /**
     * 体全体をずらす量 {横（傾く方向がプラス）, 高さ（上がプラス）}（スキンのピクセル単位）。
     * 乗車中の 側面監視（横＋高さ）・起立（高さのみ）だけ。プレビューではずらさない。
     */
    public static float[] resolveOffsets(Entity entity, int pose) {
        if (previewing || !(entity instanceof EntityPlayer) || entity.ridingEntity == null
                || !Poses.usesRidingHeight(pose)) {
            return null;
        }
        ClientPoseCache.Entry e = ClientPoseCache.get(entity.getEntityId());
        if (e == null) {
            return null;
        }
        float side = Poses.isSideWatch(pose) ? e.sideShift : 0F;
        return new float[] {side, e.height};
    }
}
