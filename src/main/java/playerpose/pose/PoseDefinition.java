package playerpose.pose;

/**
 * 1つのポーズの角度データ（単位：度）。
 *
 * 角度の向き（モデル座標。+X = キャラの左、+Y = 下）:
 *  ・X回転：マイナスで腕・脚が前に出る
 *  ・Z回転：右腕・右脚はプラスで外側へ、左腕・左脚はマイナスで外側へ
 *  ・bodyLean：上半身を腰を中心に傾ける角度。プラスでキャラの左へ傾く
 *
 * 頭の X/Y は視線に追従した値に「足す」。それ以外の部位は値で「上書き」する。
 * swing[部位] が true の腕・脚は、歩くとバニラと同じように前後に振れる（値に振り分を足す）。
 */
public final class PoseDefinition {

    public static final int HEAD = 0;
    public static final int BODY = 1;
    public static final int RIGHT_ARM = 2;
    public static final int LEFT_ARM = 3;
    public static final int RIGHT_LEG = 4;
    public static final int LEFT_LEG = 5;
    public static final int PART_COUNT = 6;

    /** 設定ファイルのキー名（部位の順番と対応）。 */
    public static final String[] PART_KEYS = {"head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"};

    /** [部位][X,Y,Z] 度 */
    public final float[][] parts = new float[PART_COUNT][3];
    /** 歩くと振れる部位 */
    public final boolean[] swing = new boolean[PART_COUNT];
    public float bodyLean;

    public PoseDefinition set(int part, float x, float y, float z) {
        parts[part][0] = x;
        parts[part][1] = y;
        parts[part][2] = z;
        return this;
    }

    public PoseDefinition swing(int... partsToSwing) {
        for (int p : partsToSwing) {
            swing[p] = true;
        }
        return this;
    }

    public PoseDefinition lean(float degrees) {
        bodyLean = degrees;
        return this;
    }

    /** 角度だけ別の定義のものに入れ替えたコピー（設定ファイルから読んだ角度を当てるのに使う）。 */
    public PoseDefinition withAnglesOf(PoseDefinition other) {
        PoseDefinition d = new PoseDefinition();
        for (int i = 0; i < PART_COUNT; i++) {
            System.arraycopy(other.parts[i], 0, d.parts[i], 0, 3);
            d.swing[i] = swing[i];
        }
        d.bodyLean = other.bodyLean;
        return d;
    }

    /** 左右反転版：左右の部位を入れ替え、Y回転・Z回転・傾きの向きを逆にする。 */
    public PoseDefinition mirror() {
        PoseDefinition d = new PoseDefinition();
        for (int i = 0; i < PART_COUNT; i++) {
            int src = mirrorPart(i);
            d.parts[i][0] = parts[src][0];
            d.parts[i][1] = -parts[src][1];
            d.parts[i][2] = -parts[src][2];
            d.swing[i] = swing[src];
        }
        d.bodyLean = -bodyLean;
        return d;
    }

    /** 上げている腕（角度の大きい方の腕）が右腕なら true。 */
    public boolean isRightArmRaised() {
        return armAmount(parts[RIGHT_ARM]) >= armAmount(parts[LEFT_ARM]);
    }

    private static float armAmount(float[] v) {
        return Math.abs(v[0]) + Math.abs(v[2]);
    }

    private static int mirrorPart(int part) {
        switch (part) {
            case RIGHT_ARM: return LEFT_ARM;
            case LEFT_ARM: return RIGHT_ARM;
            case RIGHT_LEG: return LEFT_LEG;
            case LEFT_LEG: return RIGHT_LEG;
            default: return part;
        }
    }

    /** "x,y,z" 形式に変換（設定ファイル用）。 */
    public static String format(float[] v) {
        return trim(v[0]) + "," + trim(v[1]) + "," + trim(v[2]);
    }

    /** "x,y,z" 形式を読む。読めなければ fallback を返す。 */
    public static float[] parse(String s, float[] fallback) {
        if (s == null) {
            return fallback.clone();
        }
        String[] t = s.split(",");
        if (t.length != 3) {
            return fallback.clone();
        }
        try {
            return new float[] {Float.parseFloat(t[0].trim()), Float.parseFloat(t[1].trim()), Float.parseFloat(t[2].trim())};
        } catch (NumberFormatException e) {
            return fallback.clone();
        }
    }

    private static String trim(float f) {
        if (f == (long) f) {
            return Long.toString((long) f);
        }
        return Float.toString(f);
    }
}
