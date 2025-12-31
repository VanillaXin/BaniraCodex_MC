package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import javax.annotation.Nullable;
import java.util.List;

public interface IWidget {

    void render(MatrixStack stack, float partialTicks);

    void update();

    boolean handleMouseClick(double mouseX, double mouseY, int mouseButton);

    boolean handleMouseRelease(double mouseX, double mouseY, int mouseButton);

    boolean handleKeyPress(int keyCode, int scanCode, int modifiers);

    boolean handleKeyRelease(int keyCode, int scanCode, int modifiers);

    boolean handleCharTyped(char codePoint, int modifiers);

    boolean handleMouseDrag(double mouseX, double mouseY, int mouseButton, double dragX, double dragY);

    boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta);

    ScreenCoordinate bounds();

    void visible(boolean visible);

    boolean visible();

    void enabled(boolean enabled);

    boolean enabled();

    void property(String key, Object value);

    @Nullable
    Object property(String key);

    @Nullable
    String id();

    void id(String id);

    boolean isMouseInside(double mouseX, double mouseY);

    @Nullable
    IWidget parent();

    void parent(@Nullable IWidget parent);

    List<IWidget> children();

    void addChild(IWidget child);

    boolean removeChild(IWidget child);

    @Nullable
    IWidget findChildById(String childId);

    @Nullable
    <W extends IWidget> W findChildByType(Class<W> type);

    void clearChildren();

    double absoluteX();

    double absoluteY();
}
