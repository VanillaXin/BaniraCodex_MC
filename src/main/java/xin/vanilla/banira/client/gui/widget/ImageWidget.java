package xin.vanilla.banira.client.gui.widget;

import xin.vanilla.banira.internal.client.ui.NativeGuiGraphics;

import org.joml.Matrix3x2fStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.data.TransformArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.enums.EnumPosition;

/**
 * 图片Widget
 */
@Accessors(chain = true, fluent = true)
public class ImageWidget extends BaseWidget {
    @Getter
    @Setter
    private Texture texture;

    @Getter
    @Setter
    private String textureId;

    @Getter
    @Setter
    private boolean flipHorizontal = false;

    @Getter
    @Setter
    private boolean flipVertical = false;

    public ImageWidget(BaniraScreen screen) {
        super(screen);
    }

    public ImageWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public ImageWidget(BaniraScreen screen, ScreenCoordinate bounds, Texture texture) {
        super(screen, bounds);
        this.texture = texture;
    }

    @Override
    protected boolean needsSelfUpdate() {
        return false;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, float partialTicks) {
        Matrix3x2fStack stack = NativeGuiGraphics.bind(graphics);
        if (!visible) {
            return;
        }

        TransformArgs args = new TransformArgs(stack)
                .x(x())
                .y(y())
                .width(bounds().width())
                .height(bounds().height())
                .scale(scale())
                .angle(rotation())
                .center(EnumPosition.CENTER)
                .alpha(alpha())
                .flipHorizontal(flipHorizontal)
                .flipVertical(flipVertical)
                .blend(true);
        AbstractGuiUtils.renderByTransform(args,
                drawArgs -> {
                    blit(drawArgs.stack(),
                            texture,
                            (int) drawArgs.x(),
                            (int) drawArgs.y(),
                            (int) drawArgs.width(),
                            (int) drawArgs.height()
                    );
                }
        );

        renderChildren(graphics, partialTicks);
    }


    public static void blit(Matrix3x2fStack stack, Texture texture, int x, int y, int width, int height) {
        AbstractGuiUtils.blit(stack, texture.location(), x, y, width, height, texture.u0(), texture.v0(), texture.uWidth(), texture.vHeight(), texture.uvWidth(), texture.uvHeight());
    }

    public static void blitBlend(Matrix3x2fStack stack, Texture texture, int x, int y, int width, int height) {
        AbstractGuiUtils.blitBlend(stack, texture.location(), x, y, width, height, texture.u0(), texture.v0(), texture.uWidth(), texture.vHeight(), texture.uvWidth(), texture.uvHeight());
    }

}
