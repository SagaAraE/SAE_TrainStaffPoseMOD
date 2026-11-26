package playerpose.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.input.Keyboard;
import playerpose.common.ClientPoseCache;

/** クライアントのtick処理・キー入力など。 */
public class ClientEventHandler {

    /** 選択画面を開くキー（初期値 P。操作設定で変更できる）。 */
    public static final KeyBinding KEY_OPEN_WHEEL =
            new KeyBinding("key.playerpose.openWheel", Keyboard.KEY_P, "key.categories.playerpose");

    public static void registerKeys() {
        ClientRegistry.registerKeyBinding(KEY_OPEN_WHEEL);
    }

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

    /** ゲーム画面（GUIを開いていない状態）でキーが押されたとき。FMLのイベントバスに登録する。 */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!KEY_OPEN_WHEEL.isPressed()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.currentScreen == null) {
            mc.displayGuiScreen(new GuiPoseWheel());
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            ClientPoseCache.clear();
        }
    }
}
