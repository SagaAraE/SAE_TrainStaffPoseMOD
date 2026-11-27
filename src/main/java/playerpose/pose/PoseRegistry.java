package playerpose.pose;

/**
 * ポーズ1〜9の定義。番号は円形メニューを下（解除=0）から時計回りに一周する順番。
 *  1 左手上げ / 2 左扉抑え / 3 左側面監視（手上げ） / 4 左側面監視 / 5 起立 /
 *  6 右側面監視 / 7 右側面監視（手上げ） / 8 右扉抑え / 9 右手上げ
 *
 * 基本形は 扉抑え（左）・側面監視（左）・側面監視（手上げ）（左）・手上げ（右）・起立 の5つ。
 * 反対向きはその左右反転を自動で作る。
 *
 * 角度の初期値は目で読み取ったもの。実機では設定ファイル（config/playerpose.cfg）を
 * 編集して「/pose reload」で調整できる（歩くと振れる部位の指定はここで固定）。
 */
public final class PoseRegistry {

    public static final int RAISE_LEFT = 1;
    public static final int DOOR_LEFT = 2;
    public static final int SIDE_WATCH_RAISE_LEFT = 3;
    public static final int SIDE_WATCH_LEFT = 4;
    public static final int STAND = 5;
    public static final int SIDE_WATCH_RIGHT = 6;
    public static final int SIDE_WATCH_RAISE_RIGHT = 7;
    public static final int DOOR_RIGHT = 8;
    public static final int RAISE_RIGHT = 9;

    private static final PoseDefinition[] DEFS = new PoseDefinition[10];

    /** 頭の傾きを胴より少しゆるくする量（度）。胴 20° に対して頭は 15°。 */
    public static final float HEAD_EASE = 5F;

    static {
        setBase(defaultDoorHold(), defaultSideWatch(), defaultRaiseHand(), defaultStand(), defaultSideWatchRaise());
    }

    private PoseRegistry() {
    }

    /** 扉抑え（左）：右手を高く上げ（165°）、左手足を斜めに。 */
    public static PoseDefinition defaultDoorHold() {
        return new PoseDefinition()
                .set(PoseDefinition.RIGHT_ARM, 0F, 0F, 165F)
                .set(PoseDefinition.LEFT_ARM, 0F, 0F, -15F)
                .set(PoseDefinition.LEFT_LEG, 0F, 0F, -30F);
    }

    /** 側面監視（左）：上半身を左へ傾ける。頭は胴より少しだけ起こす。 */
    public static PoseDefinition defaultSideWatch() {
        return new PoseDefinition().lean(20F)
                .set(PoseDefinition.HEAD, 0F, 0F, -HEAD_EASE);
    }

    /** 手上げ（右）：右手を大きく上げる。下ろした左腕と脚は歩くと振れる。 */
    public static PoseDefinition defaultRaiseHand() {
        return new PoseDefinition()
                .set(PoseDefinition.RIGHT_ARM, -10F, 0F, 140F)
                .swing(PoseDefinition.LEFT_ARM, PoseDefinition.RIGHT_LEG, PoseDefinition.LEFT_LEG);
    }

    /** 起立：腕も脚もまっすぐ。歩くと腕と脚が振れる。 */
    public static PoseDefinition defaultStand() {
        return new PoseDefinition()
                .swing(PoseDefinition.RIGHT_ARM, PoseDefinition.LEFT_ARM,
                        PoseDefinition.RIGHT_LEG, PoseDefinition.LEFT_LEG);
    }

    /**
     * 側面監視（手上げ）（左）：側面監視と同じく上半身を左へ20°傾け、傾いた側の左手を上げる。
     * 腕の角度は、傾きを打ち消して周りから見て手上げと同じ高さになるようにしている
     * （体に対して -160° ＋ 傾き 20° ＝ 見た目 140°）。
     */
    public static PoseDefinition defaultSideWatchRaise() {
        return new PoseDefinition()
                .lean(20F)
                .set(PoseDefinition.HEAD, 0F, 0F, -HEAD_EASE)
                .set(PoseDefinition.LEFT_ARM, -10F, 0F, -160F);
    }

    /**
     * 基本形を登録する。設定ファイルから読んだ角度を渡すときも、
     * 歩くと振れる部位の指定は初期値（default〜）のものを使う。
     */
    public static synchronized void setBase(PoseDefinition doorHold, PoseDefinition sideWatch,
                                            PoseDefinition raiseHand, PoseDefinition stand,
                                            PoseDefinition sideWatchRaise) {
        PoseDefinition door = defaultDoorHold().withAnglesOf(doorHold);
        PoseDefinition watch = defaultSideWatch().withAnglesOf(sideWatch);
        PoseDefinition raise = defaultRaiseHand().withAnglesOf(raiseHand);
        PoseDefinition st = defaultStand().withAnglesOf(stand);
        PoseDefinition watchRaise = defaultSideWatchRaise().withAnglesOf(sideWatchRaise);
        DEFS[DOOR_LEFT] = door;
        DEFS[DOOR_RIGHT] = door.mirror();
        DEFS[SIDE_WATCH_LEFT] = watch;
        DEFS[SIDE_WATCH_RIGHT] = watch.mirror();
        DEFS[RAISE_RIGHT] = raise;
        DEFS[RAISE_LEFT] = raise.mirror();
        DEFS[STAND] = st;
        DEFS[SIDE_WATCH_RAISE_LEFT] = watchRaise;
        DEFS[SIDE_WATCH_RAISE_RIGHT] = watchRaise.mirror();
    }

    /** 1〜9 以外は null。 */
    public static PoseDefinition get(int pose) {
        if (pose < 1 || pose > 9) {
            return null;
        }
        return DEFS[pose];
    }
}
