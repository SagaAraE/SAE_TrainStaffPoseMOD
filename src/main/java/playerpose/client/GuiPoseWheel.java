package playerpose.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import playerpose.common.ClientPoseCache;
import playerpose.common.Poses;
import playerpose.network.MsgPoseRequest;
import playerpose.network.PoseNetwork;
import playerpose.pose.PoseMath;

/**
 * 円形のポーズ選択画面（フォートナイトのエモート選択風）。
 *  ・6分割の扇形にマウスを乗せてクリック、または数字キー1〜6で決定
 *  ・中央クリック / 0キーで解除、Esc / 円の外クリックで閉じる
 *  ・中央に自分のキャラクターのプレビュー（マウスを乗せたポーズを取る）
 *    実際の三人称（後ろから見た視点）と左右が一致するよう、背中側から描く
 *  ・選択画面を開くキー（初期値 P）をもう一度押しても閉じる
 */
public class GuiPoseWheel extends GuiScreen {

    private static final int ARC_STEPS = 20;
    private static final float GAP_DEG = 1.5F;

    private int cx;
    private int cy;
    private float outer;
    private float inner;

    private void layout() {
        cx = width / 2;
        cy = height / 2;
        outer = Math.min(width, height) * 0.40F;
        inner = outer * 0.45F;
    }

    @Override
    public void initGui() {
        super.initGui();
        layout();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        layout();

        int hit = PoseMath.hitTest(mouseX - cx, mouseY - cy, inner, outer);
        int current = ClientPoseCache.getPose(mc.thePlayer.getEntityId());

        // ---- 扇形と中央の円 ----
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        for (int k = 0; k < 6; k++) {
            int pose = PoseMath.SEGMENT_POSE[k];
            if (hit == k) {
                drawSegment(k, 1.0F, 1.0F, 1.0F, 0.40F);
            } else if (pose == current) {
                drawSegment(k, 0.25F, 0.55F, 1.0F, 0.55F);
            } else {
                drawSegment(k, 0.0F, 0.0F, 0.0F, 0.55F);
            }
        }
        if (hit == PoseMath.HIT_CENTER) {
            drawDisc(inner - 2F, 0.8F, 0.2F, 0.2F, 0.45F);
        } else {
            drawDisc(inner - 2F, 0.0F, 0.0F, 0.0F, 0.40F);
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // ---- 文字 ----
        float labelR = (inner + outer) / 2F;
        for (int k = 0; k < 6; k++) {
            int pose = PoseMath.SEGMENT_POSE[k];
            double a = Math.toRadians(PoseMath.segmentCenterDeg(k));
            int x = cx + (int) Math.round(Math.cos(a) * labelR);
            int y = cy + (int) Math.round(Math.sin(a) * labelR);
            int color = pose == current ? 0x88CCFF : 0xFFFFFF;
            drawCenteredString(fontRendererObj, String.valueOf(pose), x, y - 10, color);
            drawCenteredString(fontRendererObj, StatCollector.translateToLocal(Poses.nameKey(pose)), x, y + 1, color);
        }
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("playerpose.gui.title"),
                cx, (int) (cy - outer) - 14, 0xFFFFFF);
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("playerpose.gui.hint"),
                cx, (int) (cy + outer) + 6, 0xAAAAAA);

        // ---- 中央のプレビュー ----
        int preview;
        if (hit >= 0) {
            preview = PoseMath.SEGMENT_POSE[hit];
        } else if (hit == PoseMath.HIT_CENTER) {
            preview = Poses.NONE;
        } else {
            preview = current;
        }
        drawPreview(preview);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPreview(int pose) {
        int size = Math.max(10, (int) (inner * 0.95F / 1.8F)); // キャラの高さ(1.8)が中央の円に収まる大きさ
        int feetY = cy + (int) (size * 0.9F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        PoseRenderState.previewing = true;
        PoseRenderState.previewPose = pose;
        GL11.glPushMatrix();
        try {
            // インベントリ画面と同じ描き方だと正面向き（鏡に映したように左右が逆に見える）になるので、
            // キャラの足元を軸に縦方向へ180度回し、背中側から見せる。
            // 奥行き(Z=50)も軸に含めないと、回したキャラが選択画面の円より奥へ行って隠れてしまう。
            GL11.glTranslatef(cx, feetY, 50.0F);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(-cx, -feetY, -50.0F);
            GuiInventory.func_147046_a(cx, feetY, size, 0.0F, 0.0F, mc.thePlayer);
        } finally {
            GL11.glPopMatrix();
            PoseRenderState.previewing = false;
            PoseRenderState.previewPose = Poses.NONE;
        }
    }

    private void drawSegment(int k, float r, float g, float b, float a) {
        float center = PoseMath.segmentCenterDeg(k);
        double start = Math.toRadians(center - 30F + GAP_DEG);
        double end = Math.toRadians(center + 30F - GAP_DEG);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLE_STRIP);
        t.setColorRGBA_F(r, g, b, a);
        for (int i = 0; i <= ARC_STEPS; i++) {
            double ang = start + (end - start) * i / ARC_STEPS;
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            t.addVertex(cx + cos * outer, cy + sin * outer, zLevel);
            t.addVertex(cx + cos * inner, cy + sin * inner, zLevel);
        }
        t.draw();
    }

    private void drawDisc(float radius, float r, float g, float b, float a) {
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLE_FAN);
        t.setColorRGBA_F(r, g, b, a);
        t.addVertex(cx, cy, zLevel);
        int steps = 48;
        for (int i = 0; i <= steps; i++) {
            double ang = Math.PI * 2 * i / steps;
            t.addVertex(cx + Math.cos(ang) * radius, cy + Math.sin(ang) * radius, zLevel);
        }
        t.draw();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return;
        }
        int hit = PoseMath.hitTest(mouseX - cx, mouseY - cy, inner, outer);
        if (hit >= 0) {
            select(PoseMath.SEGMENT_POSE[hit]);
        } else if (hit == PoseMath.HIT_CENTER) {
            select(Poses.NONE);
        } else {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_6) {
            select(keyCode - Keyboard.KEY_1 + 1);
        } else if (keyCode == Keyboard.KEY_0) {
            select(Poses.NONE);
        } else if (keyCode == ClientEventHandler.KEY_OPEN_WHEEL.getKeyCode() && keyCode != Keyboard.KEY_NONE) {
            mc.displayGuiScreen(null); // 開くキーをもう一度押したら閉じる
        } else {
            super.keyTyped(typedChar, keyCode); // Esc で閉じる
        }
    }

    private void select(int pose) {
        PoseNetwork.CHANNEL.sendToServer(new MsgPoseRequest(pose));
        mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
