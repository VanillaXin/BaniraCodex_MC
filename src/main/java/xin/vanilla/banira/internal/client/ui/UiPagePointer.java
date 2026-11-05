package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.internal.config.ClientConfig;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class UiPagePointer implements AutoCloseable {
    private final Object screen;
    private final BooleanSupplier current;
    private final Supplier<BaniraColorConfig> theme;
    private final UiPointerState state = new UiPointerState();
    private BaniraColorConfig lastTheme;
    private boolean active;
    private boolean auiPainted;

    UiPagePointer(Object screen, BooleanSupplier current, Supplier<BaniraColorConfig> theme) {
        this.screen = screen;
        this.current = current;
        this.theme = theme;
    }

    boolean sync() {
        boolean enabled = current.getAsBoolean() && ClientConfig.get().useCustomCursor();
        if (enabled && UiSystemCursor.acquire(this, screen)) active = true;
        else close();
        return active;
    }

    void button(int button, boolean down) {
        if (sync()) state.button(button, down);
    }

    void scroll(double delta) {
        if (sync()) state.scroll(delta);
    }

    void render(GuiGraphicsExtractor graphics, int x, int y) {
        if (!sync()) return;
        BaniraColorConfig colors = theme.get();
        if (colors == null) colors = BaniraColorConfig.forSeason(null);
        if (colors != lastTheme) {
            state.themeChanged();
            lastTheme = colors;
        }
        HudDrawList frame = state.frame(colors, x, y, EnumRenderDepth.MOUSE.depth());
        auiPainted = SharedHudHosts.render(SharedHudHosts.POINTER, graphics, frame);
        if (!auiPainted) frame.renderNative(graphics);
    }

    @Override
    public void close() {
        UiSystemCursor.release(this);
        state.reset();
        active = false;
        auiPainted = false;
    }
}
