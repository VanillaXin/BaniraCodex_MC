package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
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
    public boolean needsUpdate() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        PoseStack stack = graphics.pose();
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


    public static void blit(PoseStack stack, Texture texture, int x, int y, int width, int height) {
        AbstractGuiUtils.blit(stack, texture.location(), x, y, width, height, texture.u0(), texture.v0(), texture.uWidth(), texture.vHeight(), texture.uvWidth(), texture.uvHeight());
    }

    public static void blitBlend(PoseStack stack, Texture texture, int x, int y, int width, int height) {
        AbstractGuiUtils.blitBlend(stack, texture.location(), x, y, width, height, texture.u0(), texture.v0(), texture.uWidth(), texture.vHeight(), texture.uvWidth(), texture.uvHeight());
    }

}
