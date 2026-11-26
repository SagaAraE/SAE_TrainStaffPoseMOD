package playerpose.client;

import net.minecraftforge.common.config.Configuration;
import playerpose.pose.PoseDefinition;
import playerpose.pose.PoseRegistry;

import java.io.File;

/**
 * クライアントの設定ファイル（config/playerpose.cfg）。
 * 実機での角度調整用に、基本ポーズ（1・3・5）の角度もここに書く。
 * 編集後「/pose reload」で再起動せずに反映できる。
 *
 * 注意：角度は各プレイヤーの手元の設定で描画される。調整が終わったら
 * PoseRegistry の初期値に書き写して配布するのがおすすめ。
 */
public final class PoseConfig {

    private static Configuration config;

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

    private static void load() {
        // 乗車中のY座標調整は廃止したので、古い設定ファイルに残っている項目を消す
        boolean removedOld = false;
        if (config.hasKey("general", "ridingYOffset")) {
            config.getCategory("general").remove("ridingYOffset");
            removedOld = true;
        }

        PoseDefinition p1 = readPose("pose1", "1 左扉抑え（2 右扉抑え はこの左右反転）", PoseRegistry.defaultPose1());
        PoseDefinition p3 = readPose("pose3", "3 左側面監視（4 右側面監視 はこの左右反転）", PoseRegistry.defaultPose3());
        PoseDefinition p5 = readPose("pose5", "5 右手上げ（6 左手上げ はこの左右反転）", PoseRegistry.defaultPose5());
        PoseRegistry.setBase(p1, p3, p5);

        if (removedOld || config.hasChanged()) {
            config.save();
        }
    }

    private static PoseDefinition readPose(String category, String comment, PoseDefinition defaults) {
        config.addCustomCategoryComment(category, comment
                + "\n各部位は \"X,Y,Z\"（度）。X:マイナスで前へ / Z:右腕右脚はプラス、左腕左脚はマイナスで外側へ"
                + "\nbodyLean:上半身を腰から傾ける角度（プラスで左へ）"
                + "\nsideShift:体全体の見た目を横へずらす量（ブロック、プラスで左へ）");
        PoseDefinition def = new PoseDefinition();
        for (int i = 0; i < PoseDefinition.PART_COUNT; i++) {
            String value = config.get(category, PoseDefinition.PART_KEYS[i],
                    PoseDefinition.format(defaults.parts[i])).getString();
            float[] v = PoseDefinition.parse(value, defaults.parts[i]);
            def.set(i, v[0], v[1], v[2]);
        }
        def.lean((float) config.get(category, "bodyLean", (double) defaults.bodyLean).getDouble(defaults.bodyLean));
        def.shift((float) config.get(category, "sideShift", (double) defaults.sideShift).getDouble(defaults.sideShift));
        return def;
    }
}
