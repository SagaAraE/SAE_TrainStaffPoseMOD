package playerpose.client;

import cpw.mods.fml.relauncher.ReflectionHelper;
import noppes.mpm.client.RenderEvent;
import noppes.mpm.client.RenderMPM;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MPMの描画処理（RenderEvent.renderer）が持つ3つのモデルを ModelPoseMPM に差し替える。
 * MPMは描画のたびにこの3つをプレイヤーの描画処理へコピーするので、差し替えるだけで使われる。
 *
 * フィールド名は 本番(SRG名) と 開発環境(MCP名) の両方を指定している。
 */
public final class MpmModelInstaller {

    private static final Logger LOG = LogManager.getLogger("PlayerPose");

    private MpmModelInstaller() {
    }

    public static void install() {
        try {
            RenderMPM renderer = RenderEvent.renderer;
            // 引数の値は MPM 1.7.10b の RenderMPM コンストラクタと同じ
            ReflectionHelper.setPrivateValue(RenderMPM.class, renderer, new ModelPoseMPM(0.0F),
                    "field_77109_a", "modelBipedMain");
            ReflectionHelper.setPrivateValue(RenderMPM.class, renderer, new ModelPoseMPM(1.0F),
                    "field_77108_b", "modelArmorChestplate");
            ReflectionHelper.setPrivateValue(RenderMPM.class, renderer, new ModelPoseMPM(0.5F),
                    "field_77111_i", "modelArmor");
            LOG.info("MPMのプレイヤーモデルをポーズ対応モデルに差し替えました");
        } catch (Throwable t) {
            LOG.error("MPMのモデル差し替えに失敗しました。MPMのバージョンが 1.7.10b か確認してください", t);
        }
    }
}
