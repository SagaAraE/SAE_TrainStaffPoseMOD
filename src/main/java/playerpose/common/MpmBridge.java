package playerpose.common;

import net.minecraft.entity.player.EntityPlayer;
import noppes.mpm.ModelData;
import noppes.mpm.PlayerDataController;
import noppes.mpm.constants.EnumAnimation;

/** MPM（More Player Models 1.7.10b）の状態を読むための窓口。 */
public final class MpmBridge {

    private MpmBridge() {
    }

    /** MPMのモーション（/sit, /sleep など）を再生中なら true。 */
    public static boolean isAnimating(EntityPlayer player) {
        try {
            PlayerDataController controller = PlayerDataController.instance;
            if (controller == null) {
                return false;
            }
            ModelData data = controller.getPlayerData(player);
            return data != null && data.animation != null && data.animation != EnumAnimation.NONE;
        } catch (Throwable t) {
            return false;
        }
    }
}
