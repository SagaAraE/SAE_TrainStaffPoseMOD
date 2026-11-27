package playerpose.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.opengl.GL11;

/** 数値を選ぶスライダー（バニラの設定画面のスライダーと同じ見た目）。 */
public class GuiValueSlider extends GuiButton {

    /** 値を表示用の文字にする。 */
    public interface Formatter {
        String format(float value);
    }

    private final String label;
    private final float min;
    private final float max;
    private final float step;
    private final Formatter formatter;
    private float value;
    private boolean dragging;

    public GuiValueSlider(int id, int x, int y, int w, String label,
                          float min, float max, float step, float value, Formatter formatter) {
        super(id, x, y, w, 20, "");
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.formatter = formatter;
        setValue(value);
    }

    public float getValue() {
        return value;
    }

    public void setValue(float v) {
        float snapped = Math.round(v / step) * step;
        value = snapped < min ? min : (snapped > max ? max : snapped);
        displayString = label + ": " + formatter.format(value);
    }

    private float fraction() {
        return (value - min) / (max - min);
    }

    private void setFromMouse(int mouseX) {
        float f = (float) (mouseX - (xPosition + 4)) / (float) (width - 8);
        f = f < 0F ? 0F : (f > 1F ? 1F : f);
        setValue(min + (max - min) * f);
    }

    @Override
    public int getHoverState(boolean mouseOver) {
        return 0; // 背景は押せないボタンの見た目
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        if (dragging) {
            setFromMouse(mouseX);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int pos = (int) (fraction() * (float) (width - 8));
        drawTexturedModalRect(xPosition + pos, yPosition, 0, 66, 4, 20);
        drawTexturedModalRect(xPosition + pos + 4, yPosition, 196, 66, 4, 20);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            setFromMouse(mouseX);
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }
}
