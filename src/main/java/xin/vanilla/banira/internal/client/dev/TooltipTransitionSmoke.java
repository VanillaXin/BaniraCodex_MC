package xin.vanilla.banira.internal.client.dev;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.tooltip.TooltipBounds;
import xin.vanilla.banira.client.gui.tooltip.TooltipTransitionFrame;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Opt-in client-only check of the real popup renderer at deterministic animation times. */
final class TooltipTransitionSmoke extends Screen {
    private static TooltipTransitionSmoke screen;
    private int stage;
    private int mode;
    private TooltipBounds previous;
    private double popupHeight;
    private RuntimeException failure;

    private TooltipTransitionSmoke() {
        super(BaniraComponent.get().literal("Tooltip transition smoke").toChat());
    }

    static boolean tick(Minecraft client) {
        if (screen == null) {
            screen = new TooltipTransitionSmoke();
            client.setScreen(screen);
        }
        if (screen.failure != null) throw screen.failure;
        return screen.mode == 2;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        if (failure != null || mode == 2) return;
        try {
            renderStep(stack);
        } catch (Exception error) {
            failure = new IllegalStateException("Tooltip transition smoke failed at " + mode + "/" + stage + ": " + error, error);
        }
    }

    private void renderStep(PoseStack stack) throws Exception {
        fill(stack, 0, 0, width, height, 0xFF30363C);
        if (stage == 0) TooltipWidget.cancelPopupTransition();
        double pointerY = stage == 0 ? height / 2.0D : popupHeight + (stage >= 2 && stage <= 4 ? 8 : 10);
        Object model = field(TooltipWidget.class, "POPUP_TRANSITION").get(null);
        if (stage == 1 || stage == 4 || stage == 7) {
            field(model.getClass(), "transitionStartedAt").setLong(model, System.nanoTime() - 1_000_000_000L);
        } else if (stage == 3 || stage == 6) {
            field(model.getClass(), "transitionStartedAt").setLong(model, System.nanoTime() - 35_000_000L);
        }

        int pointerX = width / 2;
        TooltipWidget.beginPopupFrame(this, pointerX, pointerY);
        Text text = Text.from(BaniraComponent.get().literal("Default text\n")
                .append(BaniraComponent.get().literal("Yellow text\n").color(Color.argb(0xFFFFFF00)))
                .append(BaniraComponent.get().literal("Cyan text").color(Color.argb(0xFF00FFFF))));
        TooltipWidget.drawPopupMessage(stack, FontDrawArgs.ofPopo(text.stack(stack).font(font))
                .x(pointerX).y(pointerY).margin(4).preserveTextStyles(true)
                .popupUseTexture(mode == 1), null, EnumSeason.SPRING);
        TooltipWidget.flushPopupFrame(stack);
        fill(stack, pointerX - 3, (int) pointerY, pointerX + 4, (int) pointerY + 1, 0xFFFFFFFF);
        fill(stack, pointerX, (int) pointerY - 3, pointerX + 1, (int) pointerY + 4, 0xFFFFFFFF);

        TooltipBounds target = (TooltipBounds) field(model.getClass(), "targetBounds").get(model);
        Method currentFrame = model.getClass().getDeclaredMethod("currentFrame", long.class);
        currentFrame.setAccessible(true);
        TooltipBounds visible = ((TooltipTransitionFrame<?>) currentFrame.invoke(model, System.nanoTime())).bounds();
        if (stage == 0) popupHeight = target.height();
        if (stage == 2 || stage == 5) {
            TooltipBounds start = (TooltipBounds) field(model.getClass(), "startBounds").get(model);
            require(start.equals(previous), "flip did not start at the prior visible bounds");
            require(Math.abs(start.y() - target.y()) > popupHeight, "fixture did not cross the pointer");
        }
        if (stage == 3 || stage == 6) {
            require(visible.y() > Math.min(previous.y(), target.y())
                    && visible.y() < Math.max(previous.y(), target.y()), "missing intermediate position");
            require(visible.width() == target.width() && visible.height() == target.height(), "flip resized the popup");
        }
        require(visible.x() >= 4 && visible.y() >= 4
                && visible.x() + visible.width() <= width - 4
                && visible.y() + visible.height() <= height - 4, "popup left screen bounds");
        if (stage == 1 || stage == 4 || stage == 7) previous = target;
        if (stage == 1 || stage == 3 || stage == 4 || stage == 6) screenshot(mode + "-" + stage);
        if (++stage == 8) {
            BaniraNetworkSmokeStatus.append("PASS tooltip-flip mode=" + (mode == 0 ? "color" : "texture")
                    + " forward=true reverse=true intermediate=true bounds=true");
            stage = 0;
            mode++;
        }
    }

    private void screenshot(String suffix) throws Exception {
        Minecraft client = Minecraft.getInstance();
        Path output = Paths.get(System.getProperty("banira.networkSmoke.status")).getParent()
                .resolve("tooltip-flip-" + suffix + ".png");
        Files.createDirectories(output.getParent());
        try (NativeImage image = Screenshot.takeScreenshot(client.getWindow().getWidth(),
                client.getWindow().getHeight(), client.getMainRenderTarget())) {
            image.writeToFile(output);
        }
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
