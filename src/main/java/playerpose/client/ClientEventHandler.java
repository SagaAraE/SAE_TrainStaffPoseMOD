package playerpose.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;
import playerpose.common.ClientPoseCache;

/** クライアントのtick処理など。 */
public class ClientEventHandler {

    /** 選択画面を開くまでの残りtick（-1 = 予約なし）。 */
    private static int openWheelDelay = -1;

    /**
     * /pose 実行直後はチャット画面が閉じる処理が走るので、その場で開くと消されてしまう。
     * 少し遅らせて開く。
     */
    public static void requestOpenWheel() {
        openWheelDelay = 2;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || openWheelDelay < 0) {
            return;
        }
        if (--openWheelDelay <= 0) {
            openWheelDelay = -1;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.currentScreen == null) {
                mc.displayGuiScreen(new GuiPoseWheel());
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            ClientPoseCache.clear();
        }
    }
}
