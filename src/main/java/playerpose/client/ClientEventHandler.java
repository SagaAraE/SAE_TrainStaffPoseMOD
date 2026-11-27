package playerpose.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import playerpose.common.ClientPermission;
import org.lwjgl.input.Keyboard;
import net.minecraftforge.event.world.WorldEvent;
import playerpose.common.ClientPoseCache;

/** クライアントのtick処理など。 */
public class ClientEventHandler {

    /**
     * 選択画面を開くキー（初期値 Home。操作設定から変更できる）。
     * 以前の版（初期値 P）の設定が残らないよう、キーの名前も変えている。
     */
    public static final KeyBinding OPEN_WHEEL =
            new KeyBinding("key.playerpose.openWheel", Keyboard.KEY_HOME, "key.categories.playerpose");

    /** 次のポーズ（円形メニューを時計回り。初期値 ↑） */
    public static final KeyBinding NEXT_POSE =
            new KeyBinding("key.playerpose.next", Keyboard.KEY_UP, "key.categories.playerpose");
    /** 前のポーズ（円形メニューを反時計回り。初期値 ↓） */
    public static final KeyBinding PREV_POSE =
            new KeyBinding("key.playerpose.prev", Keyboard.KEY_DOWN, "key.categories.playerpose");

    public static void registerKeys() {
        ClientRegistry.registerKeyBinding(OPEN_WHEEL);
        ClientRegistry.registerKeyBinding(NEXT_POSE);
        ClientRegistry.registerKeyBinding(PREV_POSE);
    }

    /**
     * 矢印キーで最後に選んだポーズと、その時刻。サーバーからの同期が届く前に続けて押されても、
     * 押した回数どおりに進むように使う（1秒以内なら、同期より先にこちらを基準にする）。
     */
    private static int lastCycled = -1;
    private static long lastCycledAt;

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
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc0 = Minecraft.getMinecraft();
        while (OPEN_WHEEL.isPressed()) {
            if (mc0.thePlayer != null && mc0.currentScreen == null) {
                openWheelIfAllowed(mc0);
            }
        }
        while (NEXT_POSE.isPressed()) {
            cycle(mc0, 1);
        }
        while (PREV_POSE.isPressed()) {
            cycle(mc0, -1);
        }

        if (openWheelDelay < 0) {
            return;
        }
        if (--openWheelDelay <= 0) {
            openWheelDelay = -1;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.currentScreen == null) {
                openWheelIfAllowed(mc);
            }
        }
    }

    /** 矢印キーで、円形メニューの並び順に1つ進める（step=+1 時計回り / -1 反時計回り）。 */
    private static void cycle(Minecraft mc, int step) {
        if (mc.thePlayer == null || mc.currentScreen != null) {
            return;
        }
        long now = System.currentTimeMillis();
        int current = (lastCycled >= 0 && now - lastCycledAt < 1000L)
                ? lastCycled : ClientPoseCache.getPose(mc.thePlayer.getEntityId());
        int next = playerpose.pose.PoseMath.cyclePose(current, step, new playerpose.pose.PoseMath.Selectable() {
            @Override
            public boolean canSelect(int pose) {
                return ClientPermission.canSelect(pose);
            }
        });
        if (next == current) {
            return;
        }
        lastCycled = next;
        lastCycledAt = now;
        PoseConfig.selectPose(next);
        PoseHud.show(next);
    }

    /**
     * 選択画面を開く。OPでない人も起立は使えるので画面は開き、
     * 使えないポーズは画面上で暗く表示する（GuiPoseWheel）。
     */
    static void openWheelIfAllowed(Minecraft mc) {
        mc.displayGuiScreen(new GuiPoseWheel());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            ClientPoseCache.clear();
        }
    }

    /** サーバーから切断したら、使用権限の記録を初期状態に戻す（次のサーバーで改めて知らせが届く）。 */
    @SubscribeEvent
    public void onDisconnect(cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientPermission.reset();
    }
}
