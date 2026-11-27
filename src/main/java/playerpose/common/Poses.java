package playerpose.common;

/** ポーズ番号の定数。0 = ポーズなし、1〜9 = 各ポーズ。 */
public final class Poses {

    public static final int NONE = 0;
    public static final int COUNT = 9;

    /** 番号は円形メニューを下（解除=0）から時計回りに一周する順番（PoseRegistry と同じ） */
    public static final int SIDE_WATCH_RAISE_LEFT = 3;
    public static final int SIDE_WATCH_LEFT = 4;
    /** 5 起立（OPでなくても使える） */
    public static final int STAND = 5;
    public static final int SIDE_WATCH_RIGHT = 6;
    public static final int SIDE_WATCH_RAISE_RIGHT = 7;

    /** 調整値の範囲（スキンのピクセル単位。16 = 1ブロック）。サーバーでもこの範囲に収める。 */
    public static final float SIDE_SHIFT_MIN = -16F;
    public static final float SIDE_SHIFT_MAX = 16F;
    public static final float HEIGHT_MIN = -8F;
    public static final float HEIGHT_MAX = 24F;

    /** 初期値：横はスキン3ピクセル分、高さは0.5ブロック。 */
    public static final float DEFAULT_SIDE_SHIFT = 3F;
    public static final float DEFAULT_HEIGHT = 8F;

    private Poses() {
    }

    public static boolean isValid(int pose) {
        return pose >= 1 && pose <= COUNT;
    }

    /** 側面監視の仲間（3・4・6・7）。乗車中の横移動・高さと視点追従の対象。 */
    public static boolean isSideWatch(int pose) {
        return pose == SIDE_WATCH_LEFT || pose == SIDE_WATCH_RIGHT
                || pose == SIDE_WATCH_RAISE_LEFT || pose == SIDE_WATCH_RAISE_RIGHT;
    }

    /** 乗車中に高さ調整を当てるポーズ（側面監視と起立）。 */
    public static boolean usesRidingHeight(int pose) {
        return isSideWatch(pose) || pose == STAND;
    }

    public static float clamp(float v, float min, float max) {
        if (Float.isNaN(v)) {
            return 0F;
        }
        return v < min ? min : (v > max ? max : v);
    }

    /** 言語ファイルのキー（playerpose.pose.1 など）。 */
    public static String nameKey(int pose) {
        return "playerpose.pose." + pose;
    }
}
