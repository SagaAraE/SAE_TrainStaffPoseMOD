package playerpose.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import noppes.mpm.ModelData;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.PlayerDataController;
import noppes.mpm.client.Client;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.constants.EnumPackets;
import org.apache.logging.log4j.LogManager;

/** クライアント側から MPM を操作する窓口。 */
public final class MpmClientBridge {

    private MpmClientBridge() {
    }

    /**
     * 自分の MPM のモーションを解除する（ポーズを取る直前に呼ぶ）。
     * サーバー側の MPM の状態は歩いて解除しても残ることがあるので、
     * 画面上でモーション中かどうかに関わらず MPM 自身の通信で「解除」を送り、両方をそろえる。
     * サーバーから周りの人にも解除が配られる。
     */
    public static void cancelOwnAnimation() {
        try {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player == null || PlayerDataController.instance == null) {
                return;
            }
            ModelData data = PlayerDataController.instance.getPlayerData(player);
            if (data != null) {
                data.setAnimation(EnumAnimation.NONE.ordinal());
            }
            if (MorePlayerModels.HasServerSide) {
                Client.sendData(EnumPackets.ANIMATION, EnumAnimation.NONE.ordinal());
            }
        } catch (Throwable t) {
            LogManager.getLogger("PlayerPose").warn("MPMのモーション解除に失敗しました", t);
        }
    }
}
