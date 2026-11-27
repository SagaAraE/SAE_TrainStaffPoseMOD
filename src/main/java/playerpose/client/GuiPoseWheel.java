package playerpose.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import playerpose.common.ClientPermission;
import playerpose.common.ClientPoseCache;
import playerpose.common.Poses;
import playerpose.pose.PoseMath;

/**
 * 円形のポーズ選択画面（フォートナイトのエモート選択風、10分割）。
 *  ・扇形にマウスを乗せてクリック、または数字キー1〜9で決定
 *  ・下の扇形 / 中央クリック / 0キーで解除、Esc / Home / 円の外クリックで閉じる
 *  ・中央に自分のキャラクターのプレビュー（マウスを乗せたポーズを取る）
 *  ・「調整」ボタンで、側面監視・起立の乗車中の位置を調整する画面へ
 */
public class GuiPoseWheel extends GuiScreen {

    private static final int ARC_STEPS = 16;
    private static final float GAP_DEG = 1.5F;
    private static final float HALF_DEG = 180F / PoseMath.SEGMENTS;
    private static final int BUTTON_ADJUST = 1;

    private int cx;
    private int cy;
    private float outer;
    private float inner;

    private void layout() {
        cx = width / 2;
        cy = height / 2 - 6;
        outer = Math.min(width, height) * 0.40F;
        inner = outer * 0.38F;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        layout();
        buttonList.clear();
        buttonList.add(new GuiButton(BUTTON_ADJUST, cx - 40, (int) (cy + outer) + 18, 80, 20,
                StatCollector.translateToLocal("playerpose.gui.adjust")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_ADJUST) {
            mc.displayGuiScreen(new GuiPoseAdjust());
        }
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
        for (int k = 0; k < PoseMath.SEGMENTS; k++) {
            int pose = PoseMath.SEGMENT_POSE[k];
            if (!ClientPermission.canSelect(pose)) {
                drawSegment(k, 0.0F, 0.0F, 0.0F, 0.25F); // 使えないポーズは薄く
            } else if (hit == k) {
                if (pose == Poses.NONE) {
                    drawSegment(k, 0.8F, 0.2F, 0.2F, 0.50F);
                } else {
                    drawSegment(k, 1.0F, 1.0F, 1.0F, 0.40F);
                }
            } else if (pose != Poses.NONE && pose == current) {
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
        for (int k = 0; k < PoseMath.SEGMENTS; k++) {
            int pose = PoseMath.SEGMENT_POSE[k];
            double a = Math.toRadians(PoseMath.segmentCenterDeg(k));
            int x = cx + (int) Math.round(Math.cos(a) * labelR);
            int y = cy + (int) Math.round(Math.sin(a) * labelR);
            if (pose == Poses.NONE) {
                drawCenteredString(fontRendererObj, StatCollector.translateToLocal("playerpose.gui.release"),
                        x, y - 4, 0xFFAAAA);
                continue;
            }
            int color = !ClientPermission.canSelect(pose) ? 0x666666 : (pose == current ? 0x88CCFF : 0xFFFFFF);
            drawCenteredString(fontRendererObj, String.valueOf(pose), x, y - 10, color);
            drawCenteredString(fontRendererObj, StatCollector.translateToLocal(Poses.nameKey(pose)), x, y + 1, color);
        }
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("playerpose.gui.title"),
                cx, (int) (cy - outer) - 14, 0xFFFFFF);
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("playerpose.gui.hint"),
                cx, (int) (cy + outer) + 5, 0xAAAAAA);

        // ---- 中央のプレビュー ----
        int preview;
        if (hit >= 0 && ClientPermission.canSelect(PoseMath.SEGMENT_POSE[hit])) {
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
        drawPlayerPreview(mc, cx, feetY, size, pose);
    }

    /**
     * 自分のキャラクターにポーズを取らせて描く（調整画面でも使う）。
     * 正面を向いたキャラは左右が逆に見えるので、鏡に映したように左右反転して描く
     * （キャラの右手が画面の右側に来る＝ゲーム内で後ろから見たときと同じ並び）。
     */
    static void drawPlayerPreview(net.minecraft.client.Minecraft mc, int x, int feetY, int size, int pose) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        PoseRenderState.previewing = true;
        PoseRenderState.previewPose = pose;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, 0.0F, 0.0F);
        GL11.glScalef(-1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(-x, 0.0F, 0.0F);
        GL11.glFrontFace(GL11.GL_CW);
        try {
            GuiInventory.func_147046_a(x, feetY, size, 0.0F, 0.0F, mc.thePlayer);
        } finally {
            GL11.glFrontFace(GL11.GL_CCW);
            GL11.glPopMatrix();
            PoseRenderState.previewing = false;
            PoseRenderState.previewPose = Poses.NONE;
        }
    }

    private void drawSegment(int k, float r, float g, float b, float a) {
        float center = PoseMath.segmentCenterDeg(k);
        double start = Math.toRadians(center - HALF_DEG + GAP_DEG);
        double end = Math.toRadians(center + HALF_DEG - GAP_DEG);
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
        super.mouseClicked(mouseX, mouseY, button); // 「調整」ボタン
        if (button != 0 || mc.currentScreen != this) {
            return;
        }
        for (Object o : buttonList) {
            if (((GuiButton) o).mousePressed(mc, mouseX, mouseY)) { // ボタンの上なら扇形の判定はしない
                return;
            }
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
        if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_9) {
            select(keyCode - Keyboard.KEY_1 + 1);
        } else if (keyCode == Keyboard.KEY_0) {
            select(Poses.NONE);
        } else if (keyCode == ClientEventHandler.OPEN_WHEEL.getKeyCode()) {
            mc.displayGuiScreen(null); // もう一度押すと閉じる
        } else {
            super.keyTyped(typedChar, keyCode); // Esc で閉じる
        }
    }

    private void select(int pose) {
        if (!ClientPermission.canSelect(pose)) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentTranslation("playerpose.msg.noPermission"));
            return; // 画面は閉じずに、起立を選び直せるようにする
        }
        PoseConfig.selectPose(pose);
        mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
