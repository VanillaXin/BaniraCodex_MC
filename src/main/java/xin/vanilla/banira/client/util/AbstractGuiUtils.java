package xin.vanilla.banira.client.util;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.data.TransformArgs;
import xin.vanilla.banira.client.data.TransformDrawArgs;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.component.TextList;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.common.util.Translator;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AbstractGui工具类
 */
public final class AbstractGuiUtils {
    private AbstractGuiUtils() {
    }

    public static final int ITEM_ICON_SIZE = 16;

    /**
     * 相对图标中心的格点坐标
     */
    private static final int[][] NINE_DOT_CLOSE_ICON_GRID = {
            {-2, -2}, {-1, -1}, {0, 0}, {1, 1}, {2, 2},
            {2, -2}, {1, -1}, {-1, 1}, {-2, 2}
    };

    private static final Random random = new Random();


    public static void renderByDepth(PoseStack stack, Consumer<PoseStack> drawFunc) {
        AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.DEFAULT, drawFunc);
    }

    public static void renderByDepth(PoseStack stack, EnumRenderDepth depth, Consumer<PoseStack> drawFunc) {
        if (depth != null) {
            renderByDepth(stack, depth.depth(), drawFunc);
        } else {
            drawFunc.accept(stack);
        }
    }

    public static void renderByDepth(PoseStack stack, int depth, Consumer<PoseStack> drawFunc) {
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

        try {
            stack.pushPose();
            stack.translate(0, 0, depth);

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);

            drawFunc.accept(stack);
        } finally {
            stack.popPose();

            if (!depthTest) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }
            RenderSystem.depthFunc(depthFunc);
        }
    }


    // region 绘制纹理

    @Deprecated
    public static void bindTexture(ResourceLocation location) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    /**
     * 恢复与 {@link net.minecraft.client.gui.GuiGraphics} 绘制链常见的 GUI 状态，供自定义绘制链结束后调用。
     */
    public static void restoreGuiRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    /**
     * 与 {@link net.minecraft.client.gui.GuiGraphics} 中 {@code innerBlit(ResourceLocation,...)} 等价的纹理四边形绘制（使用当前 PoseStack）。
     */
    private static void innerBlitTexture(
            PoseStack poseStack,
            ResourceLocation texture,
            int x0,
            int x1,
            int y0,
            int y1,
            int z,
            float u0,
            float u1,
            float v0,
            float v1
    ) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix4f, (float) x0, (float) y0, (float) z).setUv(u0, v0);
        bufferBuilder.addVertex(matrix4f, (float) x0, (float) y1, (float) z).setUv(u0, v1);
        bufferBuilder.addVertex(matrix4f, (float) x1, (float) y1, (float) z).setUv(u1, v1);
        bufferBuilder.addVertex(matrix4f, (float) x1, (float) y0, (float) z).setUv(u1, v0);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static void blitInner(
            PoseStack poseStack,
            ResourceLocation texture,
            int x0,
            int x1,
            int y0,
            int y1,
            int z,
            float uOffset,
            float vOffset,
            int regionWidth,
            int regionHeight,
            int textureWidth,
            int textureHeight
    ) {
        innerBlitTexture(
                poseStack,
                texture,
                x0,
                x1,
                y0,
                y1,
                z,
                (uOffset + 0.0F) / (float) textureWidth,
                (uOffset + (float) regionWidth) / (float) textureWidth,
                (vOffset + 0.0F) / (float) textureHeight,
                (vOffset + (float) regionHeight) / (float) textureHeight
        );
    }

    public static void blit(PoseStack stack, ResourceLocation texture, int x0, int y0, int z, int destWidth, int destHeight, TextureAtlasSprite sprite) {
        AbstractGuiUtils.bindTexture(texture);
        innerBlitTexture(
                stack,
                sprite.atlasLocation(),
                x0,
                x0 + destWidth,
                y0,
                y0 + destHeight,
                z,
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1()
        );
    }

    public static void blitBlend(PoseStack stack, ResourceLocation texture, int x0, int y0, int z, int destWidth, int destHeight, TextureAtlasSprite sprite) {
        blitByBlend(() -> blit(stack, texture, x0, y0, z, destWidth, destHeight, sprite));
    }

    public static void blit(PoseStack stack, ResourceLocation texture, int x0, int y0, int z, double u0, double v0, int width, int height, int textureHeight, int textureWidth) {
        AbstractGuiUtils.bindTexture(texture);
        blitInner(stack, texture, x0, x0 + width, y0, y0 + height, z, (float) u0, (float) v0, width, height, textureWidth, textureHeight);
    }

    public static void blitBlend(PoseStack stack, ResourceLocation texture, int x0, int y0, int z, double u0, double v0, int width, int height, int textureHeight, int textureWidth) {
        blitByBlend(() -> blit(stack, texture, x0, y0, z, u0, v0, width, height, textureHeight, textureWidth));
    }

    public static void blit(PoseStack stack, ResourceLocation texture, int x0, int y0, int destWidth, int destHeight, double u0, double v0, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        AbstractGuiUtils.bindTexture(texture);
        blitInner(stack, texture, x0, x0 + destWidth, y0, y0 + destHeight, 0, (float) u0, (float) v0, srcWidth, srcHeight, textureWidth, textureHeight);
    }

    public static void blitBlend(PoseStack stack, ResourceLocation texture, int x0, int y0, int destWidth, int destHeight, double u0, double v0, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        blitByBlend(() -> blit(stack, texture, x0, y0, destWidth, destHeight, u0, v0, srcWidth, srcHeight, textureWidth, textureHeight));
    }

    public static void blit(PoseStack stack, ResourceLocation texture, int x0, int y0, double u0, double v0, int destWidth, int destHeight, int textureWidth, int textureHeight) {
        AbstractGuiUtils.bindTexture(texture);
        blitInner(stack, texture, x0, x0 + destWidth, y0, y0 + destHeight, 0, (float) u0, (float) v0, destWidth, destHeight, textureWidth, textureHeight);
    }

    public static void blitBlend(PoseStack stack, ResourceLocation texture, int x0, int y0, double u0, double v0, int destWidth, int destHeight, int textureWidth, int textureHeight) {
        blitByBlend(() -> blit(stack, texture, x0, y0, u0, v0, destWidth, destHeight, textureWidth, textureHeight));
    }

    /**
     * 启用混合模式来绘制纹理
     */
    public static void blitByBlend(Runnable drawFunc) {
        // 启用混合模式来正确处理透明度
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawFunc.run();
        // 勿在此处 disableBlend：会破坏后续 Screen/控件与字体的混合绘制
    }

    /**
     * 变换后绘制
     *
     * @param args 变换参数
     */
    public static void renderByTransform(TransformArgs args, Consumer<TransformDrawArgs> drawFunc) {

        // 保存当前矩阵状态
        args.stack().pushPose();

        // 计算目标点：pivot 为矩形内固定点（用未缩放宽高），tranW/tranH 为缩放后回移量
        double pivotX = 0, pivotY = 0;
        double tranW = 0, tranH = 0;
        EnumPosition center = args.center();
        switch (center) {
            case CENTER:
                pivotX = args.width() / 2.0;
                pivotY = args.height() / 2.0;
                tranW = pivotX * args.scale();
                tranH = pivotY * args.scale();
                break;
            case TOP_LEFT:
                break;
            case TOP_RIGHT:
                pivotX = args.width();
                tranW = pivotX * args.scale();
                break;
            case TOP_CENTER:
                pivotX = args.width() / 2.0;
                tranW = pivotX * args.scale();
                break;
            case BOTTOM_LEFT:
                pivotY = args.height();
                tranH = pivotY * args.scale();
                break;
            case BOTTOM_RIGHT:
                pivotX = args.width();
                pivotY = args.height();
                tranW = pivotX * args.scale();
                tranH = pivotY * args.scale();
                break;
            case BOTTOM_CENTER:
                pivotX = args.width() / 2.0;
                pivotY = args.height();
                tranW = pivotX * args.scale();
                tranH = pivotY * args.scale();
                break;
            case LEFT_CENTER:
                pivotY = args.height() / 2.0;
                tranH = pivotY * args.scale();
                break;
            case RIGHT_CENTER:
                pivotX = args.width();
                pivotY = args.height() / 2.0;
                tranW = pivotX * args.scale();
                tranH = pivotY * args.scale();
                break;
        }
        double tranX = args.x() + pivotX;
        double tranY = args.y() + pivotY;
        // 移至目标点
        args.stack().translate(tranX, tranY, 0);

        // 缩放
        args.stack().scale((float) args.scale(), (float) args.scale(), 1);

        // 旋转
        if (args.angle() % 360 != 0) {
            args.stack().mulPose(new Quaternionf().rotationZ((float) Math.toRadians(args.angle())));
        }

        // 翻转
        if (args.flipHorizontal()) {
            args.stack().mulPose(new Quaternionf().rotationY((float) Math.PI));
        }
        if (args.flipVertical()) {
            args.stack().mulPose(new Quaternionf().rotationX((float) Math.PI));
        }

        // 返回原点
        args.stack().translate(-tranW, -tranH, 0);

        // 关闭背面剔除
        RenderSystem.disableCull();
        // 绘制方法
        TransformDrawArgs drawArgs = new TransformDrawArgs(args.stack());
        drawArgs.x(0).y(0).width(args.width()).height(args.height()).alpha((int) args.alpha());

        // 启用混合模式
        if (args.blend() || args.alpha() < 0xFF) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        drawFunc.accept(drawArgs);

        // 勿在此处 disableBlend：会破坏后续 GUI 绘制
        if (args.blend() || args.alpha() < 0xFF) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        // 恢复背面剔除
        RenderSystem.enableCull();

        // 恢复矩阵状态
        args.stack().popPose();
    }

    // endregion 绘制纹理


    // region 文本高度

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Text... text) {
        return getTextHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Collection<Text> text) {
        return getTextHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(TextList text) {
        return getTextHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentHeight(Component... text) {
        return getComponentHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentHeight(Collection<Component> text) {
        return getComponentHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringHeight(String... text) {
        return getStringHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringHeight(Collection<String> text) {
        return getStringHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Font font, Text... text) {
        return getStringHeight(font, Arrays.stream(text).map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Font font, Collection<Text> text) {
        return getStringHeight(font, text.stream().map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Font font, TextList text) {
        return getStringHeight(font, text.stream().map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentHeight(Font font, Component... text) {
        return getStringHeight(font, Arrays.stream(text).map(component -> component.getString(Translator.getClientLanguage())).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentHeight(Font font, Collection<Component> text) {
        return getStringHeight(font, text.stream().map(component -> component.getString(Translator.getClientLanguage())).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringHeight(Font font, String... text) {
        return getStringHeight(font, Arrays.asList(text));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringHeight(Font font, Collection<String> text) {
        return text.stream().mapToInt(t -> StringUtils.replaceLineBreak(t).split("\n").length * font.lineHeight).sum();
    }

    // endregion 文本高度


    // region 文本宽度

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Text... text) {
        return getTextWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Collection<Text> text) {
        return getTextWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(TextList text) {
        return getTextWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentWidth(Component... text) {
        return getComponentWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentWidth(Collection<Component> text) {
        return getComponentWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringWidth(String... text) {
        return getStringWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringWidth(Collection<String> text) {
        return getStringWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Font font, Text... text) {
        return getStringWidth(font, Arrays.stream(text).map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Font font, Collection<Text> text) {
        return getStringWidth(font, text.stream().map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Font font, TextList text) {
        return getStringWidth(font, text.stream().map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentWidth(Font font, Component... text) {
        return getStringWidth(font, Arrays.stream(text).map(component -> component.getString(Translator.getClientLanguage())).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentWidth(Font font, Collection<Component> text) {
        return getStringWidth(font, text.stream().map(component -> component.getString(Translator.getClientLanguage())).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringWidth(Font font, String... text) {
        return getStringWidth(font, Arrays.asList(text));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringWidth(Font font, Collection<String> text) {
        return text.stream()
                .map(t -> StringUtils.replaceLineBreak(t).split("\n"))
                .flatMap(Arrays::stream)
                .mapToInt(font::width)
                .max().orElse(0);
    }

    // endregion 文本宽度


    // region 绘制文字

    /**
     * 绘制多行文本，以\n为换行符
     *
     * @param argbs 文本颜色
     */
    public static void drawMultilineText(@Nonnull FontDrawArgs args, int... argbs) {
        if (StringUtils.isNotNullOrEmpty(args.text().content())) {
            String[] lines = StringUtils.replaceLineBreak(args.text().content()).split("\n");
            FontDrawArgs clone = args.clone();
            double y = clone.y();
            for (int i = 0; i < lines.length; i++) {
                int argb;
                if (argbs.length == lines.length) {
                    argb = argbs[i];
                } else if (argbs.length > 0) {
                    argb = argbs[i % argbs.length];
                } else {
                    argb = args.text().colorArgb();
                }
                clone.text().text(lines[i]).color(Color.argb(argb));
                LabelWidget.drawLimitedText(clone.y(y + i * clone.text().font().lineHeight));
            }
        }
    }

    // wrapText, splitLongSegment, calculateLimitedTextSize, ellipsisString, drawLimitedText 已迁移至 LabelWidget

    // endregion 绘制文字


    // region 绘制形状

    /**
     * 绘制有宽度的线段
     */
    public static void drawLine(PoseStack stack, float x1, float y1, float x2, float y2, float lineWidth, int color) {
        if (lineWidth <= 0) return;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0) return;

        float nx = dx / length;
        float ny = dy / length;
        float perpX = -ny;
        float perpY = nx;
        float halfWidth = lineWidth * 0.5f;

        // 计算四个顶点
        float x1Top = x1 + perpX * halfWidth;
        float y1Top = y1 + perpY * halfWidth;
        float x1Bottom = x1 - perpX * halfWidth;
        float y1Bottom = y1 - perpY * halfWidth;
        float x2Top = x2 + perpX * halfWidth;
        float y2Top = y2 + perpY * halfWidth;
        float x2Bottom = x2 - perpX * halfWidth;
        float y2Bottom = y2 - perpY * halfWidth;

        setupBlendRender();

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        addVertexWithColor(builder, m4, x1Top, y1Top, 0, color);
        addVertexWithColor(builder, m4, x2Top, y2Top, 0, color);
        addVertexWithColor(builder, m4, x1Bottom, y1Bottom, 0, color);

        addVertexWithColor(builder, m4, x2Top, y2Top, 0, color);
        addVertexWithColor(builder, m4, x2Bottom, y2Bottom, 0, color);
        addVertexWithColor(builder, m4, x1Bottom, y1Bottom, 0, color);

        finishBlendRender(builder);
    }

    /**
     * 绘制带方形端帽的线段，适合拼接箭头、对勾等需要无缝连接的图形。
     */
    public static void drawLineWithSquareCaps(PoseStack stack, float x1, float y1, float x2, float y2,
                                              float lineWidth, int color) {
        if (lineWidth <= 0) return;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0) return;
        float capExtension = lineWidth * 0.5f;
        float offsetX = dx / length * capExtension;
        float offsetY = dy / length * capExtension;
        drawLine(stack, x1 - offsetX, y1 - offsetY, x2 + offsetX, y2 + offsetY, lineWidth, color);
    }

    /**
     * 绘制扇环形状
     */
    private static void drawSectorRingShape(ShapeDrawArgs args) {
        ShapeDrawArgs.SectorRingParams params = args.sectorRing();
        PoseStack stack = args.stack();
        int color = args.color();

        int segments = params.segments();
        if (segments <= 0) {
            segments = calculateCircleSegments(params.outerRadius());
        }

        float actualInnerRadius = params.getActualInnerRadius();

        if (actualInnerRadius > 0) {
            drawFilledSectorRing(stack, params, actualInnerRadius, segments, color);
        } else {
            drawFilledSectorRingFromCenter(stack, params, segments, color);
        }
    }

    /**
     * 绘制实心扇环
     */
    public static void drawFilledSectorRing(PoseStack stack, ShapeDrawArgs.SectorRingParams params, float innerRadius, int segments, int color) {
        float centerX = params.centerX();
        float centerY = params.centerY();
        float outerRadius = params.outerRadius();
        double startAngle = params.startAngle();
        double endAngle = params.endAngle();
        boolean useRadians = params.useRadians();

        if (innerRadius >= outerRadius || innerRadius < 0) return;

        double startRad, endRad;
        if (useRadians) {
            startRad = startAngle;
            endRad = endAngle;
        } else {
            startRad = Math.toRadians(startAngle);
            endRad = Math.toRadians(endAngle);
        }

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleRange = endRad - startRad;
        if (angleRange < 0) angleRange += 2.0 * Math.PI;
        double angleStep = angleRange / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = startRad + i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * outerRadius, centerY + sin * outerRadius, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadius, centerY + sin * innerRadius, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制实心扇环
     */
    public static void drawFilledSectorRingFromCenter(PoseStack stack, ShapeDrawArgs.SectorRingParams params, int segments, int color) {
        float centerX = params.centerX();
        float centerY = params.centerY();
        float radius = params.outerRadius();
        double startAngle = params.startAngle();
        double endAngle = params.endAngle();
        boolean useRadians = params.useRadians();

        double startRad, endRad;
        if (useRadians) {
            startRad = startAngle;
            endRad = endAngle;
        } else {
            startRad = Math.toRadians(startAngle);
            endRad = Math.toRadians(endAngle);
        }

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleRange = endRad - startRad;
        if (angleRange < 0) angleRange += 2.0 * Math.PI;
        double angleStep = angleRange / segments;

        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = startRad + i * angleStep;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radius);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    // endregion 绘制形状


    // region 绘制矩形

    /**
     * 绘制像素点
     *
     * @param x    像素的 X 坐标
     * @param y    像素的 Y 坐标
     * @param argb 像素的颜色
     */
    public static void drawPixel(PoseStack stack, int x, int y, int argb) {
        fillEx(stack, x, y, 1f, 1f, argb);
    }

    /**
     * 绘制正方形
     */
    public static void fill(PoseStack stack, int x, int y, int width, int argb) {
        AbstractGuiUtils.fill(stack, x, y, width, width, argb);
    }

    /**
     * 绘制矩形
     */
    public static void fill(PoseStack stack, int x, int y, int width, int height, int argb) {
        AbstractGuiUtils.drawRoundedRect(stack, x, y, width, height, argb, 0);
    }

    /**
     * 绘制矩形
     */
    public static void fillEx(PoseStack stack, float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0) return;

        setupBlendRender();

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addVertexWithColor(builder, m4, x + width, y, 0, color);
        addVertexWithColor(builder, m4, x, y, 0, color);
        addVertexWithColor(builder, m4, x, y + height, 0, color);
        addVertexWithColor(builder, m4, x + width, y + height, 0, color);

        finishBlendRender(builder);
    }

    /**
     * 在正方形区域内用9个实心方点绘制「×」
     */
    public static void drawNineDotCloseIcon(PoseStack stack, float boxX, float boxY, float boxSize, int argb) {
        drawNineDotCloseIcon(stack, boxX, boxY, boxSize, argb, 0.056f, 1f);
    }

    /**
     * @param dotSizeRatio 单点边长相对 {@code boxSize}，实际边长至少约 1 逻辑像素
     * @param stepScale    网格步长 = {@code dotS * stepScale}。轴对齐方块沿 45° 排列时，取 {@code 1} 则相邻块中心距为
     *                     {@code dotS * sqrt(2)}，外角刚好相接不重叠；略大于 1 则块之间留出缝隙
     */
    public static void drawNineDotCloseIcon(PoseStack stack, float boxX, float boxY, float boxSize, int argb,
                                            float dotSizeRatio, float stepScale) {
        if (boxSize <= 0f) {
            return;
        }
        float dotS = Math.max(1f, boxSize * dotSizeRatio);
        float step = dotS * stepScale;
        float centerX = boxX + boxSize * 0.5f;
        float centerY = boxY + boxSize * 0.5f;
        float half = dotS * 0.5f;
        setupBlendRender();
        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int[] g : NINE_DOT_CLOSE_ICON_GRID) {
            float cx = centerX + g[0] * step;
            float cy = centerY + g[1] * step;
            float x0 = cx - half;
            float y0 = cy - half;
            addVertexWithColor(builder, m4, x0 + dotS, y0, 0, argb);
            addVertexWithColor(builder, m4, x0, y0, 0, argb);
            addVertexWithColor(builder, m4, x0, y0 + dotS, 0, argb);
            addVertexWithColor(builder, m4, x0 + dotS, y0 + dotS, 0, argb);
        }
        finishBlendRender(builder);
    }

    /**
     * 绘制矩形边框
     *
     * @param thickness 边框厚度
     * @param argb      边框颜色
     */
    public static void fillOutLine(PoseStack stack, int x, int y, int width, int height, int thickness, int argb) {
        // 上边
        AbstractGuiUtils.fill(stack, x, y, width - thickness, thickness, argb);
        // 下边
        AbstractGuiUtils.fill(stack, x + thickness, y + height - thickness, width - thickness, thickness, argb);
        // 左边
        AbstractGuiUtils.fill(stack, x, y + thickness, thickness, height - thickness, argb);
        // 右边
        AbstractGuiUtils.fill(stack, x + width - thickness, y, thickness, height - thickness, argb);
    }

    /**
     * 绘制精细圆角矩形。需要低成本像素圆角时应显式调用粗糙绘制入口。
     */
    public static void drawRoundedRect(PoseStack stack, int x, int y, int width, int height, int argb, int radius) {
        if (radius <= 0) {
            fillEx(stack, x, y, width, height, argb);
            return;
        }
        drawRoundedRect(stack, (float) x, (float) y, (float) width, (float) height, (float) radius, argb);
    }

    /**
     * 绘制低成本像素圆角。常规界面应使用默认的精细圆角入口。
     */
    public static void drawRoundedRectRough(PoseStack stack, int x, int y,
                                            int width, int height, int argb, int radius) {
        if (radius <= 0) {
            fillEx(stack, x, y, width, height, argb);
            return;
        }
        radius = Math.min(radius, 10);
        AbstractGuiUtils.fill(stack, x + radius + 1, y + radius + 1,
                width - 2 * (radius + 1), height - 2 * (radius + 1), argb);

        AbstractGuiUtils.fill(stack, x + radius + 1, y, width - 2 * radius - 2, radius, argb);
        AbstractGuiUtils.fill(stack, x + radius + 1, y + radius, width - 2 * (radius + 1), 1, argb);
        AbstractGuiUtils.fill(stack, x + radius + 1, y + height - radius,
                width - 2 * radius - 2, radius, argb);
        AbstractGuiUtils.fill(stack, x + radius + 1, y + height - radius - 1,
                width - 2 * (radius + 1), 1, argb);
        AbstractGuiUtils.fill(stack, x, y + radius + 1, radius,
                height - 2 * radius - 2, argb);
        AbstractGuiUtils.fill(stack, x + radius, y + radius + 1, 1,
                height - 2 * (radius + 1), argb);
        AbstractGuiUtils.fill(stack, x + width - radius, y + radius + 1,
                radius, height - 2 * radius - 2, argb);
        AbstractGuiUtils.fill(stack, x + width - radius - 1, y + radius + 1,
                1, height - 2 * (radius + 1), argb);

        AbstractGuiUtils.drawCircleQuadrant(stack, x + radius, y + radius, radius, argb, 1);
        AbstractGuiUtils.drawCircleQuadrant(stack, x + width - radius - 1, y + radius, radius, argb, 2);
        AbstractGuiUtils.drawCircleQuadrant(stack, x + radius, y + height - radius - 1, radius, argb, 3);
        AbstractGuiUtils.drawCircleQuadrant(stack, x + width - radius - 1,
                y + height - radius - 1, radius, argb, 4);
    }

    /**
     * 绘制四分之一圆
     *
     * @param centerX  圆角中心点X坐标
     * @param centerY  圆角中心点Y坐标
     * @param radius   圆角半径
     * @param argb     圆角颜色
     * @param quadrant 指定绘制的象限（1=左上，2=右上，3=左下，4=右下）
     */
    private static void drawCircleQuadrant(PoseStack stack, int centerX, int centerY, int radius, int argb, int quadrant) {
        for (int dx = 0; dx <= radius; dx++) {
            for (int dy = 0; dy <= radius; dy++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    drawCircleQuadrantPixel(stack, centerX, centerY, argb, quadrant, dx, dy);
                }
            }
        }
    }

    private static void drawCircleQuadrantPixel(PoseStack stack, int centerX, int centerY, int argb, int quadrant, int dx, int dy) {
        switch (quadrant) {
            case 1: // 左上角
                AbstractGuiUtils.drawPixel(stack, centerX - dx, centerY - dy, argb);
                break;
            case 2: // 右上角
                AbstractGuiUtils.drawPixel(stack, centerX + dx, centerY - dy, argb);
                break;
            case 3: // 左下角
                AbstractGuiUtils.drawPixel(stack, centerX - dx, centerY + dy, argb);
                break;
            case 4: // 右下角
                AbstractGuiUtils.drawPixel(stack, centerX + dx, centerY + dy, argb);
                break;
        }
    }

    /**
     * 绘制圆角矩形边框
     *
     * @param x         矩形左上角X坐标
     * @param y         矩形左上角Y坐标
     * @param width     矩形宽度
     * @param height    矩形高度
     * @param thickness 边框厚度
     * @param argb      边框颜色
     * @param radius    圆角半径（0-10）
     */
    public static void drawRoundedRectOutLineRough(PoseStack stack, int x, int y, int width, int height, int thickness, int argb, int radius) {
        drawRoundedRectOutLineRough(stack, x, y, width, height, thickness, argb, radius, radius, radius, radius);
    }

    /**
     * 绘制圆角矩形边框（粗糙模式，支持四个不同的圆角半径）
     */
    public static void drawRoundedRectOutLineRough(PoseStack stack, int x, int y, int width, int height, int thickness, int argb,
                                                   int topLeft, int topRight, int bottomLeft, int bottomRight) {
        if (thickness <= 0) return;

        float halfW = width * 0.5f;
        float halfH = height * 0.5f;
        topLeft = (int) Math.min(Math.min(topLeft, 10), Math.min(halfW, halfH));
        topRight = (int) Math.min(Math.min(topRight, 10), Math.min(halfW, halfH));
        bottomLeft = (int) Math.min(Math.min(bottomLeft, 10), Math.min(halfW, halfH));
        bottomRight = (int) Math.min(Math.min(bottomRight, 10), Math.min(halfW, halfH));

        if (topLeft <= 0 && topRight <= 0 && bottomLeft <= 0 && bottomRight <= 0) {
            AbstractGuiUtils.fillOutLine(stack, x, y, width, height, thickness, argb);
            return;
        }

        // 绘制四条内边
        // 上边
        int topStartX = topLeft > 0 ? x + topLeft + 1 : x + thickness;
        int topEndX = topRight > 0 ? x + width - topRight - 1 : x + width - thickness;
        if (topEndX > topStartX) {
            AbstractGuiUtils.fill(stack, topStartX, y, topEndX - topStartX, thickness, argb);
        }

        // 下边
        int bottomStartX = bottomLeft > 0 ? x + bottomLeft + 1 : x + thickness;
        int bottomEndX = bottomRight > 0 ? x + width - bottomRight - 1 : x + width - thickness;
        if (bottomEndX > bottomStartX) {
            AbstractGuiUtils.fill(stack, bottomStartX, y + height - thickness, bottomEndX - bottomStartX, thickness, argb);
        }

        // 左边
        int leftStartY = topLeft > 0 ? y + topLeft + 1 : y + thickness;
        int leftEndY = bottomLeft > 0 ? y + height - bottomLeft - 1 : y + height - thickness;
        if (leftEndY > leftStartY) {
            AbstractGuiUtils.fill(stack, x, leftStartY, thickness, leftEndY - leftStartY, argb);
        }

        // 右边
        int rightStartY = topRight > 0 ? y + topRight + 1 : y + thickness;
        int rightEndY = bottomRight > 0 ? y + height - bottomRight - 1 : y + height - thickness;
        if (rightEndY > rightStartY) {
            AbstractGuiUtils.fill(stack, x + width - thickness, rightStartY, thickness, rightEndY - rightStartY, argb);
        }

        // 绘制四个角
        // 左上角
        if (topLeft == 0) {
            AbstractGuiUtils.fill(stack, x, y, thickness, thickness, argb);
        } else {
            drawCircleBorder(stack, x + topLeft, y + topLeft, topLeft, thickness, argb, 1);
        }
        // 右上角
        if (topRight == 0) {
            AbstractGuiUtils.fill(stack, x + width - thickness, y, thickness, thickness, argb);
        } else {
            drawCircleBorder(stack, x + width - topRight - 1, y + topRight, topRight, thickness, argb, 2);
        }
        // 左下角
        if (bottomLeft == 0) {
            AbstractGuiUtils.fill(stack, x, y + height - thickness, thickness, thickness, argb);
        } else {
            drawCircleBorder(stack, x + bottomLeft, y + height - bottomLeft - 1, bottomLeft, thickness, argb, 3);
        }
        // 右下角
        if (bottomRight == 0) {
            AbstractGuiUtils.fill(stack, x + width - thickness, y + height - thickness, thickness, thickness, argb);
        } else {
            drawCircleBorder(stack, x + width - bottomRight - 1, y + height - bottomRight - 1, bottomRight, thickness, argb, 4);
        }
    }

    /**
     * 绘制圆角边框
     *
     * @param centerX   圆角中心点X坐标
     * @param centerY   圆角中心点Y坐标
     * @param radius    圆角半径
     * @param thickness 边框厚度
     * @param argb      边框颜色
     * @param quadrant  指定绘制的象限（1=左上，2=右上，3=左下，4=右下）
     */
    private static void drawCircleBorder(PoseStack stack, int centerX, int centerY, int radius, int thickness, int argb, int quadrant) {
        for (int dx = 0; dx <= radius; dx++) {
            for (int dy = 0; dy <= radius; dy++) {
                double sqrt = Math.sqrt(dx * dx + dy * dy);
                if (sqrt <= radius && sqrt >= radius - thickness) {
                    drawCircleQuadrantPixel(stack, centerX, centerY, argb, quadrant, dx, dy);
                }
            }
        }
    }

    /**
     * 绘制非统一圆角矩形边框
     * 分别绘制矩形内边（原边长-两端圆角半径之和），再判断四个角是否有半径，若有则绘制，若无则补齐直角边（不能重叠）
     *
     * @param x           矩形左上角X坐标
     * @param y           矩形左上角Y坐标
     * @param width       矩形宽度
     * @param height      矩形高度
     * @param topLeft     左上角圆角半径
     * @param topRight    右上角圆角半径
     * @param bottomLeft  左下角圆角半径
     * @param bottomRight 右下角圆角半径
     * @param border      边框厚度
     * @param color       边框颜色
     */
    public static void drawRoundedRectOutLine(PoseStack stack, float x, float y, float width, float height,
                                              float topLeft, float topRight, float bottomLeft, float bottomRight,
                                              float border, int color, ShapeDrawArgs.RoundedCornerMode mode) {
        if (border <= 0) return;

        // 计算绘制模式
        float maxRadius = Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight));
        boolean useRough = false;
        if (mode == ShapeDrawArgs.RoundedCornerMode.ROUGH) {
            useRough = true;
        } else if (mode == ShapeDrawArgs.RoundedCornerMode.AUTO) {
            useRough = maxRadius <= 10;
        }

        // 粗糙模式
        if (useRough) {
            drawRoundedRectOutLineRough(stack, (int) x, (int) y, (int) width, (int) height,
                    (int) border, color, (int) topLeft, (int) topRight, (int) bottomLeft, (int) bottomRight);
        }
        // 精细模式
        else {
            drawRoundedRectOutLineFine(stack, x, y, width, height,
                    topLeft, topRight, bottomLeft, bottomRight, border, color);
        }
    }

    /**
     * 绘制圆角矩形边框
     */
    private static void drawRoundedRectOutLineFine(PoseStack stack, float x, float y, float width, float height,
                                                   float topLeft, float topRight, float bottomLeft, float bottomRight,
                                                   float border, int color) {
        if (border <= 0) return;

        // 限制圆角半径
        float halfW = width * 0.5f;
        float halfH = height * 0.5f;
        topLeft = clampRadius(topLeft, halfW, halfH);
        topRight = clampRadius(topRight, halfW, halfH);
        bottomLeft = clampRadius(bottomLeft, halfW, halfH);
        bottomRight = clampRadius(bottomRight, halfW, halfH);

        if (topLeft <= 0 && topRight <= 0 && bottomLeft <= 0 && bottomRight <= 0) {
            fillOutLine(stack, (int) x, (int) y, (int) width, (int) height, (int) border, color);
            return;
        }

        // 上边
        float topStartX;
        // 左上角为实心扇形
        if (topLeft > 0 && topLeft <= border) {
            topStartX = (x + border);
        }
        // 左上角为扇环
        else if (topLeft > 0) {
            topStartX = (x + topLeft);
        }
        // 左上角为直角
        else {
            topStartX = (x + border);
        }

        float topEndX;
        if (topRight > 0 && topRight <= border) {
            topEndX = (x + width - border);
        } else if (topRight > 0) {
            topEndX = (x + width - topRight);
        } else {
            topEndX = (x + width);
        }
        if (topEndX > topStartX) {
            fillEx(stack, topStartX, y, topEndX - topStartX, border, color);
        }

        // 下边
        float bottomStartX;
        if (bottomLeft > 0 && bottomLeft <= border) {
            bottomStartX = (x + border);
        } else if (bottomLeft > 0) {
            bottomStartX = (x + bottomLeft);
        } else {
            bottomStartX = (x);
        }

        float bottomEndX;
        if (bottomRight > 0 && bottomRight <= border) {
            bottomEndX = (x + width - border);
        } else if (bottomRight > 0) {
            bottomEndX = (x + width - bottomRight);
        } else {
            bottomEndX = (x + width - border);
        }
        if (bottomEndX > bottomStartX) {
            fillEx(stack, bottomStartX, (y + height - border), bottomEndX - bottomStartX, border, color);
        }

        // 左边
        float leftStartY;
        if (topLeft > 0 && topLeft <= border) {
            leftStartY = (y + border);
        } else if (topLeft > 0) {
            leftStartY = (y + topLeft);
        } else {
            leftStartY = (y);
        }

        float leftEndY;
        if (bottomLeft > 0 && bottomLeft <= border) {
            leftEndY = (y + height - border);
        } else if (bottomLeft > 0) {
            leftEndY = (y + height - bottomLeft);
        } else {
            leftEndY = (y + height - border);
        }
        if (leftEndY > leftStartY) {
            fillEx(stack, x, leftStartY, border, leftEndY - leftStartY, color);
        }

        // 右边
        float rightStartY;
        if (topRight > 0 && topRight <= border) {
            rightStartY = (y + border);
        } else if (topRight > 0) {
            rightStartY = (y + topRight);
        } else {
            rightStartY = (y + border);
        }

        float rightEndY;
        if (bottomRight > 0 && bottomRight <= border) {
            rightEndY = (y + height - border);
        } else if (bottomRight > 0) {
            rightEndY = (y + height - bottomRight);
        } else {
            rightEndY = (y + height);
        }
        if (rightEndY > rightStartY) {
            fillEx(stack, (x + width - border), rightStartY, border, rightEndY - rightStartY, color);
        }

        // 绘制四个圆角
        // 计算分段数
        float maxRadius = Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight));
        int segments = calculateOptimalSegments(maxRadius);

        // 左上角
        if (topLeft > 0) {
            float centerX = x + topLeft;
            float centerY = y + topLeft;
            if (topLeft <= border) {
                // 绘制实心扇形
                drawSector(stack, centerX, centerY, topLeft, 180, 270, segments, color);
                // 填充边与圆角之间的连接区域
                float radiusSize = topLeft;
                float remainingSize = border - radiusSize;
                fillEx(stack, centerX, centerY - radiusSize, remainingSize, radiusSize, color);
                fillEx(stack, centerX - radiusSize, centerY, radiusSize, remainingSize, color);
                fillEx(stack, centerX, centerY, remainingSize, remainingSize, color);
            } else {
                // 绘制扇环
                drawSectorRing(stack, centerX, centerY, topLeft, 180, 270, border, segments, color);
            }
        }

        // 右上角
        if (topRight > 0) {
            float centerX = x + width - topRight;
            float centerY = y + topRight;
            if (topRight <= border) {
                // 绘制实心扇形
                drawSector(stack, centerX, centerY, topRight, 270, 360, segments, color);
                // 填充边与圆角之间的连接区域
                float radiusSize = topRight;
                float remainingSize = border - radiusSize;
                fillEx(stack, centerX - remainingSize, centerY - radiusSize, remainingSize, radiusSize, color);
                fillEx(stack, centerX, centerY, radiusSize, remainingSize, color);
                fillEx(stack, centerX - remainingSize, centerY, remainingSize, remainingSize, color);
            } else {
                // 绘制扇环
                drawSectorRing(stack, centerX, centerY, topRight, 270, 360, border, segments, color);
            }
        }

        // 右下角
        if (bottomRight > 0) {
            float centerX = x + width - bottomRight;
            float centerY = y + height - bottomRight;
            if (bottomRight <= border) {
                // 绘制实心扇形
                drawSector(stack, centerX, centerY, bottomRight, 0, 90, segments, color);
                // 填充边与圆角之间的连接区域
                float radiusSize = topRight;
                float remainingSize = border - radiusSize;
                fillEx(stack, centerX, centerY - remainingSize, radiusSize, remainingSize, color);
                fillEx(stack, centerX - remainingSize, centerY, remainingSize, radiusSize, color);
                fillEx(stack, centerX - remainingSize, centerY - remainingSize, remainingSize, remainingSize, color);
            } else {
                // 绘制扇环
                drawSectorRing(stack, centerX, centerY, bottomRight, 0, 90, border, segments, color);
            }
        }

        // 左下角
        if (bottomLeft > 0) {
            float centerX = x + bottomLeft;
            float centerY = y + height - bottomLeft;
            if (bottomLeft <= border) {
                // 绘制实心扇形
                drawSector(stack, centerX, centerY, bottomLeft, 90, 180, segments, color);
                // 填充边与圆角之间的连接区域
                float radiusSize = topRight;
                float remainingSize = border - radiusSize;
                fillEx(stack, centerX - radiusSize, centerY - remainingSize, radiusSize, remainingSize, color);
                fillEx(stack, centerX, centerY, remainingSize, radiusSize, color);
                fillEx(stack, centerX, centerY - remainingSize, remainingSize, remainingSize, color);
            } else {
                // 绘制扇环
                drawSectorRing(stack, centerX, centerY, bottomLeft, 90, 180, border, segments, color);
            }
        }
    }

    // endregion 绘制矩形


    // region 绘制圆

    private static void addVertexWithColor(BufferBuilder builder, Matrix4f m4, float x, float y, float z, int argb) {
        builder.addVertex(m4, x, y, z).setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    /**
     * 计算圆角分段数
     */
    private static int calculateOptimalSegments(float radius) {
        if (radius <= 0) return 0;
        return Math.min(Math.max(32, (int) (radius * 2.5f + 16)), 128);
    }

    /**
     * 计算圆形高质量分段数
     */
    public static int calculateCircleSegments(float radius) {
        if (radius <= 0) return 0;
        return Math.min(Math.max(64, (int) (radius * 6.0f + 32)), 256);
    }

    private static void setupBlendRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void finishBlendRender(BufferBuilder builder) {
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * 绘制圆角矩形框
     */
    public static void drawRoundedRect(PoseStack stack, float x, float y, float w, float h, float r, int color) {
        drawRoundedRect(stack, x, y, w, h, r, calculateOptimalSegments(r), color);
    }

    /**
     * 绘制带可变四角圆角的填充矩形
     */
    public static void drawRoundedRect(PoseStack stack, float x, float y, float w, float h, float topLeft, float topRight, float bottomLeft, float bottomRight, int color) {
        float maxRadius = Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight));
        drawRoundedRect(stack, x, y, w, h, topLeft, topRight, bottomLeft, bottomRight, calculateOptimalSegments(maxRadius), color);
    }

    /**
     * 绘制带可变四角圆角的填充矩形
     */
    public static void drawRoundedRect(PoseStack stack, float x, float y, float w, float h, float topLeft, float topRight, float bottomLeft, float bottomRight, int part, int color) {
        float halfW = w * 0.5f;
        float halfH = h * 0.5f;
        topLeft = clampRadius(topLeft, halfW, halfH);
        topRight = clampRadius(topRight, halfW, halfH);
        bottomLeft = clampRadius(bottomLeft, halfW, halfH);
        bottomRight = clampRadius(bottomRight, halfW, halfH);

        if (topLeft <= 0f && topRight <= 0f && bottomLeft <= 0f && bottomRight <= 0f) {
            fill(stack, (int) x, (int) y, (int) w, (int) h, color);
            return;
        }
        if (part < 1) part = 1;

        List<float[]> verts = new ArrayList<>();
        verts.add(new float[]{x + topLeft, y});
        if (topRight > 0) addArc(verts, x + w - topRight, y + topRight, topRight, 270f, 360f, part);
        else verts.add(new float[]{x + w, y});

        verts.add(new float[]{x + w, y + topRight});
        if (bottomRight > 0) addArc(verts, x + w - bottomRight, y + h - bottomRight, bottomRight, 0f, 90f, part);
        else verts.add(new float[]{x + w, y + h});

        verts.add(new float[]{x + w - bottomRight, y + h});
        if (bottomLeft > 0) addArc(verts, x + bottomLeft, y + h - bottomLeft, bottomLeft, 90f, 180f, part);
        else verts.add(new float[]{x, y + h});

        verts.add(new float[]{x, y + h - bottomLeft});
        if (topLeft > 0) addArc(verts, x + topLeft, y + topLeft, topLeft, 180f, 270f, part);
        else verts.add(new float[]{x, y});

        Matrix4f m4 = stack.last().pose();
        setupBlendRender();
        RenderSystem.disableCull();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float centerX = x + w * 0.5f;
        float centerY = y + h * 0.5f;

        if (verts.size() >= 2) {
            for (int i = 0; i < verts.size(); i++) {
                float[] a = verts.get(i);
                float[] b = verts.get((i + 1) % verts.size());
                addVertexWithColor(builder, m4, centerX, centerY, 0f, color);
                addVertexWithColor(builder, m4, a[0], a[1], 0f, color);
                addVertexWithColor(builder, m4, b[0], b[1], 0f, color);
            }
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static float clampRadius(float r, float halfW, float halfH) {
        if (r <= 0f) return 0f;
        return Math.min(r, Math.min(halfW, halfH));
    }

    private static void addArc(List<float[]> verts, float cx, float cy, float r, float startDeg, float endDeg, int part) {
        for (int i = 0; i <= part; i++) {
            float t = (float) i / part;
            float ang = (float) Math.toRadians(startDeg + (endDeg - startDeg) * t);
            verts.add(new float[]{cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r});
        }
    }

    /**
     * 绘制圆角矩形
     */
    public static void drawRoundedRect(PoseStack stack, float x, float y, float w, float h, float r, int part, int color) {
        drawRoundedRect(stack, x, y, w, h, r, r, r, r, part, color);
    }

    /**
     * 绘制填充圆形
     */
    public static void drawCircle(PoseStack stack, float centerX, float centerY, float radius, int color) {
        drawCircle(stack, centerX, centerY, radius, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制填充圆形
     */
    public static void drawCircle(PoseStack stack, float centerX, float centerY, float radius, int segments, int color) {
        if (radius <= 0 || segments < 3) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleStep = 2.0 * Math.PI / segments;
        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radius);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制圆环
     */
    public static void drawCircleRing(PoseStack stack, float centerX, float centerY, float radius, float lineWidth, int color) {
        drawCircleRing(stack, centerX, centerY, radius, lineWidth, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制圆环
     */
    public static void drawCircleRing(PoseStack stack, float centerX, float centerY, float radius, float lineWidth, int segments, int color) {
        if (radius <= 0 || lineWidth <= 0 || segments < 3) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadius = Math.max(0, radius - lineWidth);
        double angleStep = 2.0 * Math.PI / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * radius, centerY + sin * radius, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadius, centerY + sin * innerRadius, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制填充椭圆
     */
    public static void drawEllipse(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipse(stack, centerX, centerY, radiusX, radiusY, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制填充椭圆
     */
    public static void drawEllipse(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, int segments, int color) {
        if (radiusX <= 0 || radiusY <= 0 || segments < 3) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleStep = 2.0 * Math.PI / segments;
        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radiusX);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radiusY);
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制椭圆环
     */
    public static void drawEllipseRing(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, float lineWidth, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipseRing(stack, centerX, centerY, radiusX, radiusY, lineWidth, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制椭圆环
     */
    public static void drawEllipseRing(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, float lineWidth, int segments, int color) {
        if (radiusX <= 0 || radiusY <= 0 || lineWidth <= 0 || segments < 3) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadiusX = Math.max(0, radiusX - lineWidth);
        float innerRadiusY = Math.max(0, radiusY - lineWidth);
        double angleStep = 2.0 * Math.PI / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * radiusX, centerY + sin * radiusY, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadiusX, centerY + sin * innerRadiusY, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制填充椭圆
     *
     * @param rotation 旋转角度, 0为正右, 顺时针
     */
    public static void drawEllipse(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, double rotation, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipse(stack, centerX, centerY, radiusX, radiusY, rotation, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制填充椭圆
     *
     * @param rotation 旋转角度, 0为正右, 顺时针
     */
    public static void drawEllipse(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, double rotation, int segments, int color) {
        drawEllipseRad(stack, centerX, centerY, radiusX, radiusY, Math.toRadians(rotation), segments, color);
    }

    /**
     * 绘制填充椭圆
     *
     * @param rotation 旋转弧度, 0为正右, 顺时针
     */
    public static void drawEllipseRad(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, double rotation, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipseRad(stack, centerX, centerY, radiusX, radiusY, rotation, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制填充椭圆
     *
     * @param rotation 旋转弧度, 0为正右, 顺时针
     */
    public static void drawEllipseRad(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, double rotation, int segments, int color) {
        if (radiusX <= 0 || radiusY <= 0 || segments < 3) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleStep = 2.0 * Math.PI / segments;
        float cosRot = (float) Math.cos(rotation);
        float sinRot = (float) Math.sin(rotation);

        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            float x = (float) (Math.cos(angle) * radiusX);
            float y = (float) (Math.sin(angle) * radiusY);
            xCoords[i] = centerX + x * cosRot - y * sinRot;
            yCoords[i] = centerY + x * sinRot + y * cosRot;
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制椭圆环
     *
     * @param rotation 旋转角度, 0为正右, 顺时针
     */
    public static void drawEllipseRing(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, double rotation, float lineWidth, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipseRing(stack, centerX, centerY, radiusX, radiusY, rotation, lineWidth, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制椭圆环
     *
     * @param rotation 旋转角度, 0为正右, 顺时针
     */
    public static void drawEllipseRing(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, double rotation, float lineWidth, int segments, int color) {
        drawEllipseRingRad(stack, centerX, centerY, radiusX, radiusY, Math.toRadians(rotation), lineWidth, segments, color);
    }

    /**
     * 绘制椭圆环
     *
     * @param rotation 旋转弧度, 0为正右, 顺时针
     */
    public static void drawEllipseRingRad(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, double rotation, float lineWidth, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipseRingRad(stack, centerX, centerY, radiusX, radiusY, rotation, lineWidth, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制椭圆环
     *
     * @param rotation 旋转弧度, 0为正右, 顺时针
     */
    public static void drawEllipseRingRad(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, double rotation, float lineWidth, int segments, int color) {
        if (radiusX <= 0 || radiusY <= 0 || lineWidth <= 0 || segments < 3) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadiusX = Math.max(0, radiusX - lineWidth);
        float innerRadiusY = Math.max(0, radiusY - lineWidth);
        double angleStep = 2.0 * Math.PI / segments;
        float cosRot = (float) Math.cos(rotation);
        float sinRot = (float) Math.sin(rotation);

        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            float xOuter = (float) (Math.cos(angle) * radiusX);
            float yOuter = (float) (Math.sin(angle) * radiusY);
            float xInner = (float) (Math.cos(angle) * innerRadiusX);
            float yInner = (float) (Math.sin(angle) * innerRadiusY);

            addVertexWithColor(builder, m4, centerX + xOuter * cosRot - yOuter * sinRot, centerY + xOuter * sinRot + yOuter * cosRot, 0, color);
            addVertexWithColor(builder, m4, centerX + xInner * cosRot - yInner * sinRot, centerY + xInner * sinRot + yInner * cosRot, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制扇形
     *
     * @param startAngle 起始角度, 0为正右
     * @param endAngle   结束角度, start至end顺时针旋转
     */
    public static void drawSector(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, int color) {
        drawSectorRad(stack, centerX, centerY, radius, Math.toRadians(startAngle), Math.toRadians(endAngle), calculateCircleSegments(radius), color);
    }

    /**
     * 绘制扇形
     *
     * @param startAngle 起始角度, 0为正右
     * @param endAngle   结束角度, start至end顺时针旋转
     */
    public static void drawSector(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, int segments, int color) {
        drawSectorRad(stack, centerX, centerY, radius, Math.toRadians(startAngle), Math.toRadians(endAngle), segments, color);
    }

    /**
     * 绘制扇形
     *
     * @param startAngle 起始弧度, 0为正右
     * @param endAngle   结束弧度, start至end顺时针旋转
     */
    public static void drawSectorRad(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, int color) {
        drawSectorRad(stack, centerX, centerY, radius, startAngle, endAngle, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制扇形
     *
     * @param startAngle 起始弧度, 0为正右
     * @param endAngle   结束弧度, start至end顺时针旋转
     */
    public static void drawSectorRad(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, int segments, int color) {
        if (radius <= 0 || segments < 2) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleRange = endAngle - startAngle;
        if (angleRange < 0) angleRange += 2.0 * Math.PI;
        double angleStep = angleRange / segments;

        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = startAngle + i * angleStep;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radius);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制扇环
     *
     * @param startAngle 起始角度, 0为正右
     * @param endAngle   结束角度, start至end顺时针旋转
     */
    public static void drawSectorRing(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, float lineWidth, int color) {
        drawSectorRingRad(stack, centerX, centerY, radius, Math.toRadians(startAngle), Math.toRadians(endAngle), lineWidth, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制扇环
     *
     * @param startAngle 起始角度, 0为正右
     * @param endAngle   结束角度, start至end顺时针旋转
     */
    public static void drawSectorRing(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, float lineWidth, int segments, int color) {
        drawSectorRingRad(stack, centerX, centerY, radius, Math.toRadians(startAngle), Math.toRadians(endAngle), lineWidth, segments, color);
    }

    /**
     * 绘制扇环
     *
     * @param startAngle 起始弧度, 0为正右
     * @param endAngle   结束弧度, start至end顺时针旋转
     */
    public static void drawSectorRingRad(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, float lineWidth, int color) {
        drawSectorRingRad(stack, centerX, centerY, radius, startAngle, endAngle, lineWidth, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制扇环
     *
     * @param startAngle 起始弧度, 0为正右
     * @param endAngle   结束弧度, start至end顺时针旋转
     */
    public static void drawSectorRingRad(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, float lineWidth, int segments, int color) {
        if (radius <= 0 || lineWidth <= 0 || segments < 2) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadius = Math.max(0, radius - lineWidth);
        double angleRange = endAngle - startAngle;
        if (angleRange < 0) angleRange += 2.0 * Math.PI;
        double angleStep = angleRange / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = startAngle + i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * radius, centerY + sin * radius, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadius, centerY + sin * innerRadius, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制多边形
     *
     * @param stack    矩阵栈
     * @param centerX  中心X坐标
     * @param centerY  中心Y坐标
     * @param radius   外接圆半径
     * @param sides    边数（n边形，n >= 3）
     * @param rotation 旋转角度
     */
    public static void drawPolygon(PoseStack stack, float centerX, float centerY, float radius, int sides, double rotation, int color) {
        if (radius <= 0 || sides < 3) return;

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double rotationRad = Math.toRadians(rotation);
        double angleStep = 2.0 * Math.PI / sides;

        // 计算所有顶点坐标
        float[] xCoords = new float[sides];
        float[] yCoords = new float[sides];
        for (int i = 0; i < sides; i++) {
            double angle = i * angleStep + rotationRad;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radius);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        // 使用三角形条带模式绘制（从中心点开始）
        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i < sides; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            addVertexWithColor(builder, m4, centerX, centerY, 0, color);
        }

        // 闭合多边形
        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制多边形边框
     */
    public static void drawPolygonBorder(PoseStack stack, ShapeDrawArgs.PolygonParams polygon, int color) {
        float centerX = polygon.centerX();
        float centerY = polygon.centerY();
        float radius = polygon.radius();
        int sides = polygon.sides();
        double rotation = polygon.rotation();
        float borderWidth = polygon.border();

        if (radius <= 0 || sides < 3 || borderWidth <= 0) return;

        double rotationRad = Math.toRadians(rotation);
        double angleStep = 2.0 * Math.PI / sides;
        float innerRadius = Math.max(0, radius - borderWidth);

        Matrix4f m4 = stack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // 绘制每条边的边框
        for (int i = 0; i <= sides; i++) {
            double angle = (i % sides) * angleStep + rotationRad;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * radius, centerY + sin * radius, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadius, centerY + sin * innerRadius, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    // endregion 绘制圆


    // region 杂项

    /**
     * 获取指定坐标点像素颜色
     */
    public static int getPixelArgb(double guiX, double guiY) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();

        // 将 GUI 坐标（左上为原点）转换为物理屏幕坐标（左下为原点）
        int pixelX = (int) (guiX * window.getGuiScale());
        int pixelY = (int) (guiY * window.getGuiScale());
        int glY = window.getHeight() - pixelY - 1;

        // 创建 ByteBuffer 存储像素数据（RGBA）
        ByteBuffer buffer = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(pixelX, glY, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        int r = buffer.get(0) & 0xFF;
        int g = buffer.get(1) & 0xFF;
        int b = buffer.get(2) & 0xFF;
        int a = buffer.get(3) & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static Font getFont() {
        return Minecraft.getInstance().font;
    }

    public static KeyValue<Integer, Integer> getScreenSize() {
        if (Minecraft.getInstance().screen != null) {
            return new KeyValue<>(Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height);
        } else {
            return getGuiScaledSize();
        }
    }

    public static KeyValue<Integer, Integer> getGuiScaledSize() {
        return new KeyValue<>(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    public static KeyValue<Integer, Integer> getGuiSize() {
        return new KeyValue<>(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight());
    }

    /**
     * 启用裁剪（Scissor），将后续渲染限制在指定矩形区域内。
     * 使用 GUI 坐标（左上角为原点）。
     */
    public static void enableScissor(int guiX, int guiY, int guiWidth, int guiHeight) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        int scale = (int) window.getGuiScale();
        int x = guiX * scale;
        int y = window.getHeight() - (guiY + guiHeight) * scale;
        int w = Math.max(0, guiWidth * scale);
        int h = Math.max(0, guiHeight * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, w, h);
    }

    /**
     * 禁用裁剪
     */
    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static final Deque<int[]> scissorStack = new ArrayDeque<>();

    /**
     * 压入裁剪区域：与当前裁剪取交集，用于嵌套裁剪。
     * 使用后必须调用 {@link #popScissor()} 恢复。
     */
    public static void pushScissor(int guiX, int guiY, int guiWidth, int guiHeight) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        int scale = (int) window.getGuiScale();
        int winW = window.getWidth() / scale;
        int winH = window.getHeight() / scale;
        int[] prev = new int[5];
        prev[0] = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) ? 1 : 0;
        if (prev[0] == 1) {
            int[] box = new int[4];
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
            prev[1] = box[0] / scale;
            prev[2] = (window.getHeight() - box[1] - box[3]) / scale;
            prev[3] = box[2] / scale;
            prev[4] = box[3] / scale;
        } else {
            prev[1] = 0;
            prev[2] = 0;
            prev[3] = winW;
            prev[4] = winH;
        }
        scissorStack.push(prev);

        int left = Math.max(guiX, prev[1]);
        int top = Math.max(guiY, prev[2]);
        int right = Math.min(guiX + guiWidth, prev[1] + prev[3]);
        int bottom = Math.min(guiY + guiHeight, prev[2] + prev[4]);
        int w = Math.max(0, right - left);
        int h = Math.max(0, bottom - top);
        enableScissor(left, top, w, h);
    }

    /**
     * 弹出裁剪区域，恢复之前的状态。
     */
    public static void popScissor() {
        int[] prev = scissorStack.poll();
        if (prev == null) {
            disableScissor();
            return;
        }
        if (prev[0] == 1) {
            enableScissor(prev[1], prev[2], prev[3], prev[4]);
        } else {
            disableScissor();
        }
    }

    public static String getClipboard() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null ? minecraft.keyboardHandler.getClipboard() : "";
    }

    public static void setClipboard(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(text != null ? text : "");
        }
    }

    // endregion 杂项
}
