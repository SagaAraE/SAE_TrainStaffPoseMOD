package playerpose.pose;

/**
 * ポーズ1〜6の定義。基本は 1・3・5 の3つで、2・4・6 はその左右反転を自動で作る。
 *  1 左扉抑え / 2 右扉抑え / 3 左側面監視 / 4 右側面監視 / 5 右手上げ / 6 左手上げ
 *
 * 初期値は画像から目で読み取ったもの。実機では設定ファイル（config/playerpose.cfg）を
 * 編集して「/pose reload」で調整できる。
 */
public final class PoseRegistry {

    private static final PoseDefinition[] DEFS = new PoseDefinition[7];

    static {
        setBase(defaultPose1(), defaultPose3(), defaultPose5());
    }

    private PoseRegistry() {
    }

    /** 1 左扉抑え：右手を上げ、左手足を少し斜めに。 */
    public static PoseDefinition defaultPose1() {
        return new PoseDefinition()
                .set(PoseDefinition.RIGHT_ARM, 0F, 0F, 120F)
                .set(PoseDefinition.LEFT_ARM, 0F, 0F, -15F)
                .set(PoseDefinition.LEFT_LEG, 0F, 0F, -12F);
    }

    /** 3 左側面監視：上半身を左へ傾ける。 */
    public static PoseDefinition defaultPose3() {
        return new PoseDefinition().lean(20F);
    }

    /** 5 右手上げ：右手を大きく上げる。 */
    public static PoseDefinition defaultPose5() {
        return new PoseDefinition()
                .set(PoseDefinition.RIGHT_ARM, -10F, 0F, 140F);
    }

    public static synchronized void setBase(PoseDefinition pose1, PoseDefinition pose3, PoseDefinition pose5) {
        DEFS[1] = pose1;
        DEFS[2] = pose1.mirror();
        DEFS[3] = pose3;
        DEFS[4] = pose3.mirror();
        DEFS[5] = pose5;
        DEFS[6] = pose5.mirror();
    }

    /** 1〜6 以外は null。 */
    public static PoseDefinition get(int pose) {
        if (pose < 1 || pose > 6) {
            return null;
        }
        return DEFS[pose];
    }
}
