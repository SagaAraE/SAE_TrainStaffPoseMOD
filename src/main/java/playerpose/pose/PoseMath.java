package playerpose.pose;

/** Minecraftに依存しない計算部分（単体テストできるように分けている）。 */
public final class PoseMath {

    /** 腰の高さ（モデル座標。首が0、脚の付け根が12）。 */
    public static final float HIP_Y = 12F;

    /** 選択ホイールの扇形 k（右上から時計回り）に割り当てるポーズ番号。左右の対が左右対称の位置に来る。 */
    public static final int[] SEGMENT_POSE = {2, 4, 5, 6, 3, 1};
    public static final int HIT_NONE = -2;
    public static final int HIT_CENTER = -1;

    private PoseMath() {
    }

    /**
     * 点 (x, y) を腰 (0, HIP_Y) を中心に、sin/cos の角度だけ回した位置を返す。
     * 上半身の各部位の取り付け位置をずらすのに使う。
     */
    public static float[] rotateAroundHip(float x, float y, float sin, float cos) {
        float dx = x;
        float dy = y - HIP_Y;
        return new float[] {dx * cos - dy * sin, dx * sin + dy * cos + HIP_Y};
    }

    /** 扇形 k の中心角（度、0 = 右、時計回りがプラス）。 */
    public static float segmentCenterDeg(int k) {
        return -60F + 60F * k;
    }

    /**
     * 画面中心からの (dx, dy) がどこに当たるか。
     * 中央の円 → HIT_CENTER、扇形 → 0〜5、外側すぎる → HIT_NONE。
     */
    public static int hitTest(float dx, float dy, float inner, float outer) {
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < inner) {
            return HIT_CENTER;
        }
        if (r > outer * 1.25F) {
            return HIT_NONE;
        }
        double deg = Math.toDegrees(Math.atan2(dy, dx)); // 画面座標なので時計回りがプラス
        int k = (int) Math.floor((deg + 90.0D) / 60.0D);
        return ((k % 6) + 6) % 6;
    }
}
