package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.common.util.ColorUtils;

public final class UiPointerState {
    private long frames;
    private int buttons;
    private float scroll;
    private Integer light, dark;
    private int main, pressed;

    public void colors(int light, int dark) { this.light = light; this.dark = dark; frames = 0; }
    public void themeChanged() { frames = 0; }
    public void reset() { buttons = 0; scroll = 0; frames = 0; }
    public void button(int button, boolean down) {
        if (button < 0 || button > 2) return;
        int mask = 1 << button;
        buttons = down ? buttons | mask : buttons & ~mask;
    }
    public void scroll(double delta) {
        if (!Double.isFinite(delta)) return;
        scroll = Math.max(-8, Math.min(8, scroll + (float) Math.max(-6, Math.min(6, delta * 3))));
    }
    public HudDrawList frame(BaniraColorConfig theme, int x, int y, double depth) {
        if (frames++ % 10 == 0) {
            boolean useDark = ColorUtils.getBrightnessFromArgb(theme.bgSurface()) < 0.5f;
            main = useDark ? (dark == null ? theme.cursorDarkMain() : dark)
                    : (light == null ? theme.cursorLightMain() : light);
            pressed = useDark ? theme.cursorDarkPressed() : theme.cursorLightPressed();
        }
        scroll *= 0.72f;
        if (Math.abs(scroll) < 0.5f) scroll = 0;
        int offset = (int) scroll;
        int left = (buttons & 1) != 0 ? pressed : main;
        int right = (buttons & 2) != 0 ? pressed : main;
        int center = (buttons & 4) != 0 ? pressed : main;
        HudDrawList frame = new HudDrawList();
        frame.depth(depth);
        int at = y + offset;
        frame.box(x, at, 1, 1, center, 0, 0);
        frame.box(x - 4, at + 2, 3, 1, left, 0, 0);
        frame.box(x - 2, at + 2, 1, 3, left, 0, 0);
        frame.box(x - 4, at - 2, 3, 1, left, 0, 0);
        frame.box(x - 2, at - 4, 1, 3, left, 0, 0);
        frame.box(x + 2, at + 2, 3, 1, right, 0, 0);
        frame.box(x + 2, at + 2, 1, 3, right, 0, 0);
        frame.box(x + 2, at - 2, 3, 1, right, 0, 0);
        frame.box(x + 2, at - 4, 1, 3, right, 0, 0);
        if (offset != 0) {
            int alpha = (int) (0x90 * Math.min(1, Math.abs(scroll) / 4));
            frame.box(x + 6, Math.min(y, at), 1, Math.abs(offset) + 1,
                    (alpha << 24) | (main & 0xFFFFFF), 0, 0);
        }
        return frame;
    }
}
