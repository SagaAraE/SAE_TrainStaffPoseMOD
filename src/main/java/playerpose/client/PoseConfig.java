package playerpose.client;

import net.minecraftforge.common.config.Configuration;
import playerpose.common.Poses;
import playerpose.network.MsgPoseRequest;
import playerpose.network.PoseNetwork;
import playerpose.pose.PoseDefinition;
import playerpose.pose.PoseRegistry;

import java.io.File;

/**
 * クライアントの設定ファイル（config/playerpose.cfg）。
 * ・自分の調整値（側面監視の横移動・乗車中の高さ）… 調整画面で変えると自動で保存される
 * ・手持ちアイテムを左右反転して描くポーズ
 * ・基本ポーズ（1・3・5・7）の角度 … 実機調整用。編集後「/pose reload」で反映
 *
 * 注意：角度は各プレイヤーの手元の設定で描画される。調整が終わったら
 * PoseRegistry の初期値に書き写して配布するのがおすすめ。
 * （調整値の方はサーバー経由で全員に共有されるので、この注意は当てはまらない）
 */
public final class PoseConfig {

    /** 手上げ（1・9）と側面監視（手上げ）（3・7） */
    private static final String DEFAULT_MIRRORED_ITEM_POSES = "1,3,7,9";

    /** 設定ファイルの形式の版。3 = ポーズ番号を円形メニューの並び順に振り直した版 */
    private static final int CONFIG_VERSION = 3;
    /** 版 2 以前のポーズ番号 → 新しい番号（添字が古い番号） */
    private static final int[] OLD_TO_NEW_POSE = {0, 2, 8, 4, 6, 9, 1, 5, 3, 7};

    /** 側面監視の横移動（スキンのピクセル単位、傾く方向がプラス） */
    public static float sideShift = Poses.DEFAULT_SIDE_SHIFT;
    /** 乗車中の高さ（スキンのピクセル単位、上がプラス） */
    public static float ridingHeight = Poses.DEFAULT_HEIGHT;
    /** 側面監視中、視点（カメラ）を傾けた頭に合わせる */
    public static boolean cameraFollowHead = true;
    /** 視点を頭の位置からさらに外側（傾く方向）へずらす量（ブロック）。自分の画面だけに効く */
    public static float cameraExtraSide = 0F;
    public static final float CAMERA_EXTRA_MIN = 0F;
    public static final float CAMERA_EXTRA_MAX = 1.5F;
    /** 視点を頭の位置から上下にずらす量（ブロック、上がプラス）。自分の画面だけに効く */
    public static float cameraExtraHeight = 0F;
    public static final float CAMERA_HEIGHT_MIN = -1.0F;
    public static final float CAMERA_HEIGHT_MAX = 1.0F;

    /** 絵を反転するときの軸（持ち手）の位置。テクスチャの左端から何割の所か（0〜1）。 */
    public static float itemFlipPivotU = 0.1875F;
    /** 持ち手の位置。テクスチャの上端から何割の所か（0〜1）。 */
    public static float itemPivotV = 0.8125F;
    /** アイテムを持ち手を中心に外側へ倒す角度（度）。内側に倒れる場合はマイナスにする。 */
    public static float itemTiltDeg = 40F;

    /** 手持ちアイテムを左右反転して描くポーズ（添字 = ポーズ番号）。 */
    private static final boolean[] MIRRORED_ITEM = new boolean[10];

    private static Configuration config;

    static {
        applyMirroredItemPoses(DEFAULT_MIRRORED_ITEM_POSES);
    }

    private PoseConfig() {
    }

    public static void init(File file) {
        config = new Configuration(file);
        load();
    }

    public static void reload() {
        if (config == null) {
            return;
        }
        config.load();
        load();
    }

    public static boolean isItemMirrored(int pose) {
        return pose >= 1 && pose <= 9 && MIRRORED_ITEM[pose];
    }

    /**
     * 選択画面・コマンドからポーズを選んだとき。MPMのモーションを自動で解除してから要求を送る。
     */
    public static void selectPose(int pose) {
        if (pose > 0) {
            MpmClientBridge.cancelOwnAnimation();
        }
        sendPose(pose);
    }

    /** 今の調整値を付けてポーズ要求を送る（0 = 解除）。 */
    public static void sendPose(int pose) {
        PoseNetwork.CHANNEL.sendToServer(new MsgPoseRequest(pose, sideShift, ridingHeight));
    }

    /** 調整値を設定ファイルに保存する。 */
    public static void saveOffsets() {
        if (config == null) {
            return;
        }
        config.get("adjust", "sideShift", (double) Poses.DEFAULT_SIDE_SHIFT).set(sideShift);
        config.get("adjust", "ridingHeight", (double) Poses.DEFAULT_HEIGHT).set(ridingHeight);
        config.get("adjust", "cameraFollowHead", true).set(cameraFollowHead);
        config.get("adjust", "cameraExtraSide", 0D).set(cameraExtraSide);
        config.get("adjust", "cameraExtraHeight", 0D).set(cameraExtraHeight);
        config.save();
    }

    private static void load() {
        net.minecraftforge.common.config.Property versionProp = config.get("general", "configVersion", 1,
                "設定ファイルの形式の版（自動で管理されるので変更しないでください）");
        boolean oldNumbering = versionProp.getInt(1) < CONFIG_VERSION;

        net.minecraftforge.common.config.Property mirroredProp = config.get("general", "mirroredItemPoses",
                DEFAULT_MIRRORED_ITEM_POSES, "手持ちアイテムを上げている腕に持たせ、絵を左右反転して描くポーズ番号（カンマ区切り）");
        if (oldNumbering) {
            mirroredProp.set(convertOldPoseList(mirroredProp.getString()));
            migrateCategory("pose1", "doorHold");
            migrateCategory("pose3", "sideWatch");
            migrateCategory("pose5", "raiseHand");
            migrateCategory("pose7", "stand");
            migrateCategory("pose8_sideWatchRaise", "sideWatchRaise");
            if (config.hasCategory("pose8")) { // 取りやめた座りポーズの古い設定
                config.removeCategory(config.getCategory("pose8"));
            }
            versionProp.set(CONFIG_VERSION);
        }
        applyMirroredItemPoses(mirroredProp.getString());
        itemFlipPivotU = Poses.clamp((float) config.get("general", "itemFlipPivotU", 0.1875D,
                "アイテムの絵を反転するときの軸（持ち手）の位置。テクスチャの左端から何割の所か（0〜1）").getDouble(0.1875D), 0F, 1F);
        itemPivotV = Poses.clamp((float) config.get("general", "itemPivotV", 0.8125D,
                "持ち手の位置。テクスチャの上端から何割の所か（0〜1）。アイテムを倒すときの中心").getDouble(0.8125D), 0F, 1F);
        itemTiltDeg = (float) config.get("general", "itemTiltDeg", 40D,
                "手上げ・側面監視（手上げ）で、アイテムを持ち手を中心に外側へ倒す角度（度）。内側に倒れる場合はマイナスに").getDouble(40D);

        config.addCustomCategoryComment("adjust",
                "調整画面（選択画面の「調整」ボタン）で変える値。単位はスキンのピクセル（16 = 1ブロック）"
                        + "\nsideShift: 側面監視で傾く方向へずらす量（乗車中のみ）"
                        + "\nridingHeight: 側面監視・起立で乗車中に上げる高さ"
                        + "\ncameraFollowHead: 側面監視中、視点を傾けた頭に合わせる（true/false）"
                        + "\ncameraExtraSide: 視点を頭の位置からさらに外側へずらす量（ブロック。自分の画面だけ）"
                        + "\ncameraExtraHeight: 視点を上下にずらす量（ブロック、上がプラス。自分の画面だけ）");
        sideShift = Poses.clamp((float) config.get("adjust", "sideShift", (double) Poses.DEFAULT_SIDE_SHIFT)
                .getDouble(Poses.DEFAULT_SIDE_SHIFT), Poses.SIDE_SHIFT_MIN, Poses.SIDE_SHIFT_MAX);
        ridingHeight = Poses.clamp((float) config.get("adjust", "ridingHeight", (double) Poses.DEFAULT_HEIGHT)
                .getDouble(Poses.DEFAULT_HEIGHT), Poses.HEIGHT_MIN, Poses.HEIGHT_MAX);
        cameraFollowHead = config.get("adjust", "cameraFollowHead", true).getBoolean(true);
        cameraExtraSide = Poses.clamp((float) config.get("adjust", "cameraExtraSide", 0D).getDouble(0D),
                CAMERA_EXTRA_MIN, CAMERA_EXTRA_MAX);
        cameraExtraHeight = Poses.clamp((float) config.get("adjust", "cameraExtraHeight", 0D).getDouble(0D),
                CAMERA_HEIGHT_MIN, CAMERA_HEIGHT_MAX);

        PoseDefinition door = readPose("doorHold", "8 扉抑え（右手上げ）（2 扉抑え（左手上げ） はこの左右反転）",
                PoseRegistry.defaultDoorHold());
        PoseDefinition watch = readPose("sideWatch", "4 左側面監視（6 右側面監視 はこの左右反転）",
                PoseRegistry.defaultSideWatch());
        PoseDefinition raise = readPose("raiseHand", "9 右手上げ（1 左手上げ はこの左右反転）",
                PoseRegistry.defaultRaiseHand());
        PoseDefinition stand = readPose("stand", "5 起立", PoseRegistry.defaultStand());
        PoseDefinition watchRaise = readPose("sideWatchRaise",
                "3 左側面監視（手上げ）（7 右側面監視（手上げ） はこの左右反転）", PoseRegistry.defaultSideWatchRaise());
        PoseRegistry.setBase(door, watch, raise, stand, watchRaise);

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static void applyMirroredItemPoses(String list) {
        for (int i = 0; i < MIRRORED_ITEM.length; i++) {
            MIRRORED_ITEM[i] = false;
        }
        if (list == null) {
            return;
        }
        for (String t : list.split(",")) {
            try {
                int n = Integer.parseInt(t.trim());
                if (n >= 1 && n <= 9) {
                    MIRRORED_ITEM[n] = true;
                }
            } catch (NumberFormatException ignored) {
                // 空欄や数字以外は無視
            }
        }
    }

    /** 以前の版の初期値（設定ファイルに残っていたら新しい初期値に置き換える）。{カテゴリ, 部位, 値} */
    private static final String[][] OLD_DEFAULTS = {
            {"doorHold", "rightArm", "0,0,120"}, // 扉抑えの腕：120° → 165°
            {"doorHold", "leftLeg", "0,0,-12"},  // 扉抑えの脚：12° → 20° → 30°
            {"doorHold", "leftLeg", "0,0,-20"},
    };

    /** 古い名前のカテゴリの値を、新しい名前のカテゴリへ引き継ぐ（新しい方がまだ無いときだけ）。 */
    private static void migrateCategory(String oldName, String newName) {
        if (!config.hasCategory(oldName)) {
            return;
        }
        net.minecraftforge.common.config.ConfigCategory old = config.getCategory(oldName);
        if (!config.hasCategory(newName)) {
            for (java.util.Map.Entry<String, net.minecraftforge.common.config.Property> e : old.entrySet()) {
                config.get(newName, e.getKey(), e.getValue().getString()).set(e.getValue().getString());
            }
        }
        config.removeCategory(old);
    }

    /** 古い番号の並び（例 "5,6,8,9"）を新しい番号に変換する。 */
    private static String convertOldPoseList(String list) {
        StringBuilder sb = new StringBuilder();
        for (String t : list.split(",")) {
            try {
                int n = Integer.parseInt(t.trim());
                if (n >= 1 && n <= 9) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(OLD_TO_NEW_POSE[n]);
                }
            } catch (NumberFormatException ignored) {
                // 数字以外は捨てる
            }
        }
        return sb.toString();
    }

    private static boolean isOldDefault(String category, int part, String value) {
        for (String[] o : OLD_DEFAULTS) {
            if (o[0].equals(category) && o[1].equals(PoseDefinition.PART_KEYS[part]) && o[2].equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static PoseDefinition readPose(String category, String comment, PoseDefinition defaults) {
        config.addCustomCategoryComment(category, comment
                + "\n各部位は \"X,Y,Z\"（度）。X:マイナスで前へ / Z:右腕右脚はプラス、左腕左脚はマイナスで外側へ"
                + "\nbodyLean:上半身を腰から傾ける角度（プラスで左へ）");
        PoseDefinition def = new PoseDefinition();
        for (int i = 0; i < PoseDefinition.PART_COUNT; i++) {
            net.minecraftforge.common.config.Property prop = config.get(category, PoseDefinition.PART_KEYS[i],
                    PoseDefinition.format(defaults.parts[i]));
            if (i == PoseDefinition.HEAD && defaults.bodyLean != 0F && "0,0,0".equals(prop.getString().replace(" ", ""))) {
                prop.set(PoseDefinition.format(defaults.parts[i])); // 以前の版の初期値（頭も胴と同じだけ傾く）を置き換え
            }
            if (isOldDefault(category, i, prop.getString().replace(" ", ""))) {
                prop.set(PoseDefinition.format(defaults.parts[i])); // 以前の版の初期値を、新しい初期値に置き換え
            }
            String value = prop.getString();
            float[] v = PoseDefinition.parse(value, defaults.parts[i]);
            def.set(i, v[0], v[1], v[2]);
        }
        def.lean((float) config.get(category, "bodyLean", (double) defaults.bodyLean).getDouble(defaults.bodyLean));
        return def;
    }
}
