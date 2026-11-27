package playerpose.common;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * サーバー側の設定ファイル（config/playerpose-server.cfg）。
 * 各プレイヤーのPC用の設定（config/playerpose.cfg）とは別のファイルにしている
 * （シングルプレイでは同じPCに両方が置かれるため）。
 * 「OPのみ使用可能」は /posemode コマンドで稼働中に切り替えられ、切り替えるとこのファイルにも保存される。
 */
public final class ServerConfig {

    private static final String CATEGORY = "server";
    private static final String KEY_OP_ONLY = "opOnly";

    /** true = OPのみ使用可能、false = 全員使用可能 */
    private static volatile boolean opOnly = true;

    private static Configuration config;

    private ServerConfig() {
    }

    public static void init(File configDir) {
        config = new Configuration(new File(configDir, "playerpose-server.cfg"));
        config.load();
        opOnly = config.get(CATEGORY, KEY_OP_ONLY, true,
                "true = マルチプレイではOPのみポーズを使用できる / false = 全員使用できる。"
                        + "サーバー稼働中は /posemode op | all で切り替えられる（シングルプレイでは常に使用できる）")
                .getBoolean(true);
        if (config.hasChanged()) {
            config.save();
        }
    }

    public static boolean isOpOnly() {
        return opOnly;
    }

    /** 切り替えて、設定ファイルにも保存する。 */
    public static void setOpOnly(boolean value) {
        opOnly = value;
        if (config != null) {
            config.get(CATEGORY, KEY_OP_ONLY, true).set(value);
            config.save();
        }
    }
}
