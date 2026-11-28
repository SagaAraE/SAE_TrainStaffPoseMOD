package playerpose.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import playerpose.common.ClientPoseCache;
import playerpose.common.Poses;

/**
 * 乗車中の位置の調整画面。
 * ・側面監視：傾く方向への横移動と高さ / 起立：高さ（どちらも乗車中のみ反映）
 * ・背景を暗くせず、三人称（後ろから）視点に切り替えて、ゲーム内の自分を見ながら調整できる
 * ・ポーズ中なら値の変更をすぐサーバーへ送り、周りの人にも同じ位置で見える
 * ・側面監視中に視点を頭に合わせるかどうかのオン・オフと、視点をさらに外側・上下へずらす量（自分の画面だけ）
 * ・右下のパネル：手上げ・側面監視（手上げ）で持つアイテムの根元の位置（横・縦）と回す角度（自分の画面だけ）
 * ・閉じると値を設定ファイルに保存し、元の視点に戻す
 */
public class GuiPoseAdjust extends GuiScreen {

    private static final int ID_SIDE = 1;
    private static final int ID_HEIGHT = 2;
    private static final int ID_RESET = 3;
    private static final int ID_DONE = 4;
    private static final int ID_CAMERA = 5;
    private static final int ID_CAMERA_SIDE = 6;
    private static final int ID_CAMERA_HEIGHT = 7;
    private static final int ID_ITEM_U = 8;
    private static final int ID_ITEM_V = 9;
    private static final int ID_ITEM_TILT = 10;

    private GuiValueSlider sideSlider;
    private GuiValueSlider heightSlider;
    private GuiButton cameraButton;
    private GuiValueSlider cameraSideSlider;
    private GuiValueSlider cameraHeightSlider;
    private GuiValueSlider itemUSlider;
    private GuiValueSlider itemVSlider;
    private GuiValueSlider itemTiltSlider;

    private int previousView = -1;
    private float lastSentSide = Float.NaN;
    private float lastSentHeight = Float.NaN;
    private int ticksSinceSend = 0;

    private int panelX;
    private int panelY;
    private static final int PANEL_W = 170;
    private int itemPanelX;
    private int itemPanelY;
    private static final int ITEM_PANEL_H = 108;

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        if (previousView < 0) {
            previousView = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1; // 後ろから見る視点
        }
        panelX = 8;
        panelY = height - 190;
        buttonList.clear();
        sideSlider = new GuiValueSlider(ID_SIDE, panelX + 10, panelY + 30, PANEL_W - 20,
                StatCollector.translateToLocal("playerpose.adjust.side"),
                Poses.SIDE_SHIFT_MIN, Poses.SIDE_SHIFT_MAX, 0.5F, PoseConfig.sideShift,
                new GuiValueSlider.Formatter() {
                    @Override
                    public String format(float v) {
                        return String.format("%.1fpx", v);
                    }
                });
        heightSlider = new GuiValueSlider(ID_HEIGHT, panelX + 10, panelY + 54, PANEL_W - 20,
                StatCollector.translateToLocal("playerpose.adjust.height"),
                Poses.HEIGHT_MIN, Poses.HEIGHT_MAX, 0.5F, PoseConfig.ridingHeight,
                new GuiValueSlider.Formatter() {
                    @Override
                    public String format(float v) {
                        return String.format("%.2f", v / 16F) + StatCollector.translateToLocal("playerpose.adjust.blocks");
                    }
                });
        buttonList.add(sideSlider);
        buttonList.add(heightSlider);
        cameraButton = new GuiButton(ID_CAMERA, panelX + 10, panelY + 78, PANEL_W - 20, 20, cameraLabel());
        buttonList.add(cameraButton);
        cameraSideSlider = new GuiValueSlider(ID_CAMERA_SIDE, panelX + 10, panelY + 102, PANEL_W - 20,
                StatCollector.translateToLocal("playerpose.adjust.cameraSide"),
                PoseConfig.CAMERA_EXTRA_MIN, PoseConfig.CAMERA_EXTRA_MAX, 0.05F, PoseConfig.cameraExtraSide,
                new GuiValueSlider.Formatter() {
                    @Override
                    public String format(float v) {
                        return String.format("%.2f", v) + StatCollector.translateToLocal("playerpose.adjust.blocks");
                    }
                });
        buttonList.add(cameraSideSlider);
        cameraHeightSlider = new GuiValueSlider(ID_CAMERA_HEIGHT, panelX + 10, panelY + 126, PANEL_W - 20,
                StatCollector.translateToLocal("playerpose.adjust.cameraHeight"),
                PoseConfig.CAMERA_HEIGHT_MIN, PoseConfig.CAMERA_HEIGHT_MAX, 0.05F, PoseConfig.cameraExtraHeight,
                new GuiValueSlider.Formatter() {
                    @Override
                    public String format(float v) {
                        return String.format("%+.2f", v) + StatCollector.translateToLocal("playerpose.adjust.blocks");
                    }
                });
        buttonList.add(cameraHeightSlider);
        itemPanelX = width - PANEL_W - 8;
        itemPanelY = height - ITEM_PANEL_H - 8;
        GuiValueSlider.Formatter ratio = new GuiValueSlider.Formatter() {
            @Override
            public String format(float v) {
                return String.format("%.2f", v);
            }
        };
        itemUSlider = new GuiValueSlider(ID_ITEM_U, itemPanelX + 10, itemPanelY + 30, PANEL_W - 20,
                StatCollector.translateToLocal("playerpose.adjust.itemU"), 0F, 1F, 0.01F, PoseConfig.itemGripU, ratio);
        itemVSlider = new GuiValueSlider(ID_ITEM_V, itemPanelX + 10, itemPanelY + 54, PANEL_W - 20,
                StatCollector.translateToLocal("playerpose.adjust.itemV"), 0F, 1F, 0.01F, PoseConfig.itemGripV, ratio);
        itemTiltSlider = new GuiValueSlider(ID_ITEM_TILT, itemPanelX + 10, itemPanelY + 78, PANEL_W - 20,
                StatCollector.translateToLocal("playerpose.adjust.itemTilt"),
                PoseConfig.ITEM_TILT_MIN, PoseConfig.ITEM_TILT_MAX, 1F, PoseConfig.itemTiltDeg,
                new GuiValueSlider.Formatter() {
                    @Override
                    public String format(float v) {
                        return String.format("%+.0f°", v);
                    }
                });
        buttonList.add(itemUSlider);
        buttonList.add(itemVSlider);
        buttonList.add(itemTiltSlider);
        buttonList.add(new GuiButton(ID_RESET, panelX + 10, panelY + 154, (PANEL_W - 24) / 2, 20,
                StatCollector.translateToLocal("playerpose.adjust.reset")));
        buttonList.add(new GuiButton(ID_DONE, panelX + 14 + (PANEL_W - 24) / 2, panelY + 154, (PANEL_W - 24) / 2, 20,
                StatCollector.translateToLocal("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_RESET) {
            sideSlider.setValue(Poses.DEFAULT_SIDE_SHIFT);
            heightSlider.setValue(Poses.DEFAULT_HEIGHT);
            cameraSideSlider.setValue(0F);
            cameraHeightSlider.setValue(0F);
            itemUSlider.setValue(PoseConfig.DEFAULT_ITEM_GRIP_U);
            itemVSlider.setValue(PoseConfig.DEFAULT_ITEM_GRIP_V);
            itemTiltSlider.setValue(PoseConfig.DEFAULT_ITEM_TILT);
        } else if (button.id == ID_DONE) {
            mc.displayGuiScreen(null);
        } else if (button.id == ID_CAMERA) {
            PoseConfig.cameraFollowHead = !PoseConfig.cameraFollowHead;
            cameraButton.displayString = cameraLabel();
        }
    }

    private static String cameraLabel() {
        return StatCollector.translateToLocal("playerpose.adjust.camera") + ": "
                + StatCollector.translateToLocal(PoseConfig.cameraFollowHead ? "options.on" : "options.off");
    }

    @Override
    public void updateScreen() {
        PoseConfig.sideShift = sideSlider.getValue();
        PoseConfig.ridingHeight = heightSlider.getValue();
        PoseConfig.cameraExtraSide = cameraSideSlider.getValue(); // 自分の画面だけなので送らない
        PoseConfig.cameraExtraHeight = cameraHeightSlider.getValue();
        applyItemSliders();
        ticksSinceSend++;
        boolean changed = PoseConfig.sideShift != lastSentSide || PoseConfig.ridingHeight != lastSentHeight;
        if (changed && ticksSinceSend >= 2) { // 送りすぎないよう、最短2tickごと
            sendIfPosing();
        }
    }

    /** アイテムの持ち方は自分の画面だけなので、送らずにそのまま反映する。 */
    private void applyItemSliders() {
        PoseConfig.itemGripU = itemUSlider.getValue();
        PoseConfig.itemGripV = itemVSlider.getValue();
        PoseConfig.itemTiltDeg = itemTiltSlider.getValue();
    }

    private void sendIfPosing() {
        lastSentSide = PoseConfig.sideShift;
        lastSentHeight = PoseConfig.ridingHeight;
        ticksSinceSend = 0;
        int pose = ClientPoseCache.getPose(mc.thePlayer.getEntityId());
        if (Poses.usesRidingHeight(pose)) {
            PoseConfig.sendPose(pose);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 背景は暗くしない（ゲーム内の自分を見ながら調整するため）
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + 182, 0x90000000);
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("playerpose.adjust.title"),
                panelX + PANEL_W / 2, panelY + 6, 0xFFFFFF);

        int pose = ClientPoseCache.getPose(mc.thePlayer.getEntityId());
        String status;
        if (!Poses.usesRidingHeight(pose)) {
            status = StatCollector.translateToLocal("playerpose.adjust.needPose");
        } else if (mc.thePlayer.ridingEntity == null) {
            status = StatCollector.translateToLocal("playerpose.adjust.needRiding");
        } else {
            status = StatCollector.translateToLocal("playerpose.adjust.live");
        }
        drawCenteredString(fontRendererObj, status, panelX + PANEL_W / 2, panelY + 18, 0xFFDD88);

        drawRect(itemPanelX, itemPanelY, itemPanelX + PANEL_W, itemPanelY + ITEM_PANEL_H, 0x90000000);
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("playerpose.adjust.itemTitle"),
                itemPanelX + PANEL_W / 2, itemPanelY + 6, 0xFFFFFF);
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("playerpose.adjust.itemNote"),
                itemPanelX + PANEL_W / 2, itemPanelY + 18, 0xFFDD88);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        PoseConfig.sideShift = sideSlider.getValue();
        PoseConfig.ridingHeight = heightSlider.getValue();
        PoseConfig.cameraExtraSide = cameraSideSlider.getValue();
        PoseConfig.cameraExtraHeight = cameraHeightSlider.getValue();
        applyItemSliders();
        if (PoseConfig.sideShift != lastSentSide || PoseConfig.ridingHeight != lastSentHeight) {
            sendIfPosing();
        }
        PoseConfig.saveOffsets();
        if (previousView >= 0) {
            mc.gameSettings.thirdPersonView = previousView;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
