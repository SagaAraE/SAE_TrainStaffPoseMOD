package playerpose.common;

/** ポーズ番号の定数。0 = ポーズなし、1〜6 = 各ポーズ。 */
public final class Poses {

    public static final int NONE = 0;
    public static final int COUNT = 6;

    private Poses() {
    }

    public static boolean isValid(int pose) {
        return pose >= 1 && pose <= COUNT;
    }

    /** 言語ファイルのキー（playerpose.pose.1 など）。 */
    public static String nameKey(int pose) {
        return "playerpose.pose." + pose;
    }
}
