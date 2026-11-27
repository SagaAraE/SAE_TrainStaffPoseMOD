package playerpose.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;
import playerpose.common.Poses;

/**
 * 矢印キーでポーズを切り替えた直後に、画面の上の方へポーズ名を短く表示する。
 * 約1.5秒表示したあと、0.5秒かけて消える。
 */
public class PoseHud {

    private static final long SHOW_MS = 1500L;
    private static final long FADE_MS = 500L;

    private static String text;
    private static long shownAt;

    public static void show(int pose) {
        text = pose <= Poses.NONE
                ? StatCollector.translateToLocal("playerpose.hud.released")
                : StatCollector.translateToLocal(Poses.nameKey(pose));
        shownAt = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || text == null) {
            return;
        }
        long age = System.currentTimeMillis() - shownAt;
        if (age > SHOW_MS + FADE_MS) {
            text = null;
            return;
        }
        float alpha = age <= SHOW_MS ? 1.0F : 1.0F - (float) (age - SHOW_MS) / FADE_MS;
        int a = Math.max(8, (int) (alpha * 255)); // 低すぎる値は描画されないため下限を設ける
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = event.resolution.getScaledWidth();
        int y = event.resolution.getScaledHeight() / 5;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        font.drawStringWithShadow(text, (width - font.getStringWidth(text)) / 2, y, (a << 24) | 0xFFFFFF);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
