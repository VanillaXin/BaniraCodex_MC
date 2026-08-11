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
import xin.vanilla.banira.client.util.TextureUtils;

/**
 * 九宫格图片Widget
 */
@Accessors(chain = true, fluent = true)
public class NinePatchImageWidget extends BaseWidget {
    @Getter
    @Setter
    private Texture texture;

    @Getter
    @Setter
    private String textureId;

    @Getter
    @Setter
    private int leftWidth = 0;

    @Getter
    @Setter
    private int rightWidth = 0;

    @Getter
    @Setter
    private int topHeight = 0;

    @Getter
    @Setter
    private int bottomHeight = 0;

    public NinePatchImageWidget(BaniraScreen screen) {
        super(screen);
    }

    public NinePatchImageWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public NinePatchImageWidget(BaniraScreen screen, ScreenCoordinate bounds, Texture texture) {
        super(screen, bounds);
        this.texture = texture;
    }

    @Override
    protected boolean needsSelfUpdate() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        PoseStack stack = graphics.pose();
        if (!visible) {
            return;
        }
        if (texture == null) {
            return;
        }

        TransformArgs args = new TransformArgs(stack)
                .x(x())
                .y(y())
                .width(bounds().width())
                .height(bounds().height())
                .scale(scale())
                .angle(rotation())
                .center(rotationCenter())
                .alpha(alpha())
                .blend(true);
        AbstractGuiUtils.renderByTransform(args,
                drawArgs -> {
                    drawNinePatch(drawArgs.stack(),
                            texture(),
                            (int) drawArgs.x(),
                            (int) drawArgs.y(),
                            (int) drawArgs.width(),
                            (int) drawArgs.height(),
                            0
                    );
                }
        );

        renderChildren(graphics, partialTicks);
    }

    /**
     * 绘制九宫格纹理背景
     *
     * @param stack      矩阵栈
     * @param texture    纹理对象，包含资源位置和范围信息
     * @param x          绘制起始X坐标
     * @param y          绘制起始Y坐标
     * @param destWidth  目标宽度（缩放后的最终宽度）
     * @param destHeight 目标高度（缩放后的最终高度）
     * @param scale      缩放比例（用于根据右参考线高度缩放，<=0 时自动计算）
     */
    public static void drawNinePatch(PoseStack stack, Texture texture, int x, int y, int destWidth, int destHeight, double scale) {
        if (texture == null) {
            return;
        }

        TextureUtils.NinePatchInfo info = TextureUtils.parseNinePatch(texture);
        if (info == null) {
            ImageWidget.blit(stack, texture, x, y, destWidth, destHeight);
            return;
        }
        if (scale <= 0) {
            if (info.rightGuideHeight > 0) {
                scale = (double) AbstractGuiUtils.getFont().lineHeight / info.rightGuideHeight;
            }
        }

        int contentStartX = texture.u0() + 1;
        int contentStartY = texture.v0() + 1;

        double originalDestWidth = destWidth / scale;
        double originalDestHeight = destHeight / scale;

        int totalFixedWidth = 0;
        int totalFixedHeight = 0;
        int totalStretchableWidth = 0;
        int totalStretchableHeight = 0;

        for (int i = 0; i < info.horizontalDivisions.length - 1; i++) {
            int regionWidth = info.horizontalDivisions[i + 1] - info.horizontalDivisions[i];
            if (info.horizontalStretchable[i]) {
                totalStretchableWidth += regionWidth;
            } else {
                totalFixedWidth += regionWidth;
            }
        }

        for (int i = 0; i < info.verticalDivisions.length - 1; i++) {
            int regionHeight = info.verticalDivisions[i + 1] - info.verticalDivisions[i];
            if (info.verticalStretchable[i]) {
                totalStretchableHeight += regionHeight;
            } else {
                totalFixedHeight += regionHeight;
            }
        }

        double stretchWidth = Math.max(0, originalDestWidth - totalFixedWidth);
        double stretchHeight = Math.max(0, originalDestHeight - totalFixedHeight);

        double stretchWidthRatio = totalStretchableWidth > 0 ? stretchWidth / totalStretchableWidth : 1.0f;
        double stretchHeightRatio = totalStretchableHeight > 0 ? stretchHeight / totalStretchableHeight : 1.0f;

        boolean needsScale = Math.abs(scale - 1.0f) > 0.001f;
        if (needsScale) {
            stack.pushPose();
            stack.translate(x, y, 0);
            stack.scale((float) scale, (float) scale, 1.0f);
            x = 0;
            y = 0;
        }

        double currentY = y;
        for (int v = 0; v < info.verticalDivisions.length - 1; v++) {
            int srcVStart = contentStartY + info.verticalDivisions[v];
            int srcVEnd = contentStartY + info.verticalDivisions[v + 1] - 1;
            int srcVHeight = srcVEnd - srcVStart + 1;

            double destVHeight;
            if (info.verticalStretchable[v]) {
                destVHeight = srcVHeight * stretchHeightRatio;
            } else {
                destVHeight = srcVHeight;
            }

            double currentX = x;
            for (int h = 0; h < info.horizontalDivisions.length - 1; h++) {
                int srcHStart = contentStartX + info.horizontalDivisions[h];
                int srcHEnd = contentStartX + info.horizontalDivisions[h + 1] - 1;
                int srcHWidth = srcHEnd - srcHStart + 1;

                double destHWidth;
                if (info.horizontalStretchable[h]) {
                    destHWidth = srcHWidth * stretchWidthRatio;
                } else {
                    destHWidth = srcHWidth;
                }

                AbstractGuiUtils.blit(stack, texture.location(), (int) currentX, (int) currentY, (int) destHWidth, (int) destVHeight,
                        srcHStart, srcVStart, srcHWidth, srcVHeight,
                        texture.uvWidth(), texture.uvHeight());

                currentX += destHWidth;
            }

            currentY += destVHeight;
        }

        if (needsScale) {
            stack.popPose();
        }
    }

}
