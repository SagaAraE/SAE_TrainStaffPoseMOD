package playerpose.pose;

/**
 * 1つのポーズの角度データ（単位：度）。
 *
 * 角度の向き（モデル座標。+X = キャラの左、+Y = 下）:
 *  ・X回転：マイナスで腕・脚が前に出る
 *  ・Z回転：右腕・右脚はプラスで外側へ、左腕・左脚はマイナスで外側へ
 *  ・bodyLean：上半身を腰を中心に傾ける角度。プラスでキャラの左へ傾く
 *  ・sideShift：体全体の見た目を横へずらす量（ブロック）。プラスでキャラの左へ
 *
 * 頭の X/Y は視線に追従した値に「足す」。それ以外の部位は値で「上書き」する。
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
    public float bodyLean;
    public float sideShift;

    public PoseDefinition set(int part, float x, float y, float z) {
        parts[part][0] = x;
        parts[part][1] = y;
        parts[part][2] = z;
        return this;
    }

    public PoseDefinition lean(float degrees) {
        bodyLean = degrees;
        return this;
    }

    public PoseDefinition shift(float blocks) {
        sideShift = blocks;
        return this;
    }

    public PoseDefinition copy() {
        PoseDefinition d = new PoseDefinition();
        for (int i = 0; i < PART_COUNT; i++) {
            System.arraycopy(parts[i], 0, d.parts[i], 0, 3);
        }
        d.bodyLean = bodyLean;
        d.sideShift = sideShift;
        return d;
    }

    /** 左右反転版：左右の部位を入れ替え、Y回転・Z回転・傾き・横ずらしの向きを逆にする。 */
    public PoseDefinition mirror() {
        PoseDefinition d = new PoseDefinition();
        for (int i = 0; i < PART_COUNT; i++) {
            float[] src = parts[mirrorPart(i)];
            d.parts[i][0] = src[0];
            d.parts[i][1] = -src[1];
            d.parts[i][2] = -src[2];
        }
        d.bodyLean = -bodyLean;
        d.sideShift = -sideShift;
        return d;
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
