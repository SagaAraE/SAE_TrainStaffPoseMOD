package playerpose.pose;

/** Minecraftに依存しない計算部分（単体テストできるように分けている）。 */
public final class PoseMath {

    /** 腰の高さ（モデル座標。首が0、脚の付け根が12）。 */
    public static final float HIP_Y = 12F;
    /** 目の高さ（モデル座標。首より4ピクセル上）。 */
    public static final float EYE_Y = -4F;

    /** モデルの1ピクセルが何ブロックか（プレイヤーは 1/16 を 0.9375 倍して描かれる）。 */
    public static final float PIXEL_TO_BLOCK = 0.0625F * 0.9375F;

    /** 選択ホイールの分割数。 */
    public static final int SEGMENTS = 10;

    /**
     * 扇形 k（真上から時計回り）に割り当てるポーズ番号。0 は「解除」。
     * 上：5 起立 / 右側（上から）：6 右側面監視・7 右側面監視（手上げ）・8 右扉抑え・9 右手上げ /
     * 下：0 解除 / 左側（下から）：1 左手上げ・2 左扉抑え・3 左側面監視（手上げ）・4 左側面監視
     * 番号は「解除から時計回り」の順になっている。
     */
    public static final int[] SEGMENT_POSE = {5, 6, 7, 8, 9, 0, 1, 2, 3, 4};

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

    /**
     * 上半身を leanDeg 傾け、頭を（見た目で）headDeg 傾けた時に、目の位置がどれだけ動くか
     * （モデル座標のピクセル。{横(+X=左), 下(+Y)}）。
     * 首は上半身と一緒に腰を中心に回り、目は首を中心に頭の角度で回る。
     */
    public static float[] eyeShift(float leanDeg, float headDeg) {
        double l = Math.toRadians(leanDeg);
        double h = Math.toRadians(headDeg);
        float[] neck = rotateAroundHip(0F, 0F, (float) Math.sin(l), (float) Math.cos(l));
        double eyeX = neck[0] + (-EYE_Y) * Math.sin(h);
        double eyeY = neck[1] + EYE_Y * Math.cos(h);
        return new float[] {(float) eyeX, (float) (eyeY - EYE_Y)};
    }

    /** ポーズ番号が何番目の扇形にあるか（無ければ「解除」の扇形）。 */
    public static int segmentOf(int pose) {
        for (int k = 0; k < SEGMENTS; k++) {
            if (SEGMENT_POSE[k] == pose) {
                return k;
            }
        }
        return segmentOf0();
    }

    private static int segmentOf0() {
        for (int k = 0; k < SEGMENTS; k++) {
            if (SEGMENT_POSE[k] == 0) {
                return k;
            }
        }
        return 0;
    }

    /** 選べるかどうかの判定（矢印キーで、使えないポーズを飛ばすのに使う） */
    public interface Selectable {
        boolean canSelect(int pose);
    }

    /**
     * 矢印キーでの切り替え先。円形メニューを時計回り（step=+1、↑キー）または反時計回り（step=-1、↓キー）に
     * 1つ進め、選べないポーズは飛ばす。選べるものが他に無ければ今のポーズのまま。
     */
    public static int cyclePose(int current, int step, Selectable selectable) {
        int k = segmentOf(current);
        for (int i = 0; i < SEGMENTS; i++) {
            k = ((k + step) % SEGMENTS + SEGMENTS) % SEGMENTS;
            int pose = SEGMENT_POSE[k];
            if (selectable == null || selectable.canSelect(pose)) {
                return pose;
            }
        }
        return current;
    }

    /** 扇形 k の中心角（度、0 = 右、時計回りがプラス）。k=0 が真上。 */
    public static float segmentCenterDeg(int k) {
        return -90F + (360F / SEGMENTS) * k;
    }

    /**
     * 画面中心からの (dx, dy) がどこに当たるか。
     * 中央の円 → HIT_CENTER、扇形 → 0〜(SEGMENTS-1)、外側すぎる → HIT_NONE。
     */
    public static int hitTest(float dx, float dy, float inner, float outer) {
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < inner) {
            return HIT_CENTER;
        }
        if (r > outer * 1.25F) {
            return HIT_NONE;
        }
        double step = 360.0D / SEGMENTS;
        double deg = Math.toDegrees(Math.atan2(dy, dx)); // 画面座標なので時計回りがプラス
        int k = (int) Math.floor((deg + 90.0D + step / 2.0D) / step);
        return ((k % SEGMENTS) + SEGMENTS) % SEGMENTS;
    }

    /** バニラの歩き（腕）の振り角（ラジアン）。rightSide で位相が逆になる。 */
    public static float armWalkSwing(float limbSwing, float limbSwingAmount, boolean rightSide) {
        float phase = limbSwing * 0.6662F + (rightSide ? (float) Math.PI : 0F);
        return (float) Math.cos(phase) * 2.0F * limbSwingAmount * 0.5F;
    }

    /** バニラの歩き（脚）の振り角（ラジアン）。 */
    public static float legWalkSwing(float limbSwing, float limbSwingAmount, boolean rightSide) {
        float phase = limbSwing * 0.6662F + (rightSide ? 0F : (float) Math.PI);
        return (float) Math.cos(phase) * 1.4F * limbSwingAmount;
    }

    /**
     * カメラの回転（GL の glRotatef(roll,Z) → glRotatef(pitch,X) → glRotatef(yaw,Y) の順に掛かるもの）を
     * ワールドのベクトル v に当てた結果を返す。カメラをずらす量を「視点の座標」に直すのに使う。
     */
    public static double[] toViewSpace(double yawDeg, double pitchDeg, double rollDeg, double[] v) {
        double[] a = rotY(v, Math.toRadians(yawDeg));
        double[] b = rotX(a, Math.toRadians(pitchDeg));
        return rotZ(b, Math.toRadians(rollDeg));
    }

    private static double[] rotX(double[] v, double t) {
        double c = Math.cos(t), s = Math.sin(t);
        return new double[] {v[0], c * v[1] - s * v[2], s * v[1] + c * v[2]};
    }

    private static double[] rotY(double[] v, double t) {
        double c = Math.cos(t), s = Math.sin(t);
        return new double[] {c * v[0] + s * v[2], v[1], -s * v[0] + c * v[2]};
    }

    private static double[] rotZ(double[] v, double t) {
        double c = Math.cos(t), s = Math.sin(t);
        return new double[] {c * v[0] - s * v[1], s * v[0] + c * v[1], v[2]};
    }
}
