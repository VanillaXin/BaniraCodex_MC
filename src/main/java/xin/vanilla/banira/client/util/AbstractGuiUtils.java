package xin.vanilla.banira.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.TransformArgs;
import xin.vanilla.banira.client.data.TransformDrawArgs;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.enums.EnumRotationCenter;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.*;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AbstractGui工具类
 */
@OnlyIn(Dist.CLIENT)
public final class AbstractGuiUtils {

    public static final int ITEM_ICON_SIZE = 16;

    private static final Random random = new Random();

    // region 设置深度

    /**
     * 以默认深度绘制
     */
    public static void renderByDepth(MatrixStack stack, Consumer<MatrixStack> drawFunc) {
        AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.FOREGROUND, drawFunc);
    }

    /**
     * 以指定深度绘制
     *
     * @param depth 深度
     */
    public static void renderByDepth(MatrixStack stack, EnumRenderDepth depth, Consumer<MatrixStack> drawFunc) {
        if (depth != null) {
            // 保存当前深度测试状态
            boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

            try {
                stack.pushPose();
                stack.translate(0, 0, depth.getDepth());

                // 启用深度测试
                RenderSystem.enableDepthTest();
                // 设置深度函数
                RenderSystem.depthFunc(GL11.GL_LEQUAL); // 小于等于当前深度的像素通过测试, 允许相同深度的像素显示
                // RenderSystem.depthFunc(GL11.GL_ALWAYS);

                // 执行绘制
                drawFunc.accept(stack);
            } finally {
                // 恢复矩阵状态
                stack.popPose();

                // 恢复之前的深度测试状态
                if (!depthTest) {
                    RenderSystem.disableDepthTest();
                } else {
                    RenderSystem.enableDepthTest();
                }
                RenderSystem.depthFunc(depthFunc);
            }
        } else {
            drawFunc.accept(stack);
        }
    }

    // endregion 设置深度

    // region 绘制纹理

    public static void bindTexture(ResourceLocation resourceLocation) {
        Minecraft.getInstance().getTextureManager().bind(resourceLocation);
    }

    public static void blit(MatrixStack stack, int x0, int y0, int z, int destWidth, int destHeight, TextureAtlasSprite sprite) {
        AbstractGui.blit(stack, x0, y0, z, destWidth, destHeight, sprite);
    }

    public static void blitBlend(MatrixStack stack, int x0, int y0, int z, int destWidth, int destHeight, TextureAtlasSprite sprite) {
        blitByBlend(() ->
                AbstractGui.blit(stack, x0, y0, z, destWidth, destHeight, sprite)
        );
    }

    public static void blit(MatrixStack stack, int x0, int y0, int z, double u0, double v0, int width, int height, int textureHeight, int textureWidth) {
        AbstractGui.blit(stack, x0, y0, z, (float) u0, (float) v0, width, height, textureHeight, textureWidth);
    }

    public static void blitBlend(MatrixStack stack, int x0, int y0, int z, double u0, double v0, int width, int height, int textureHeight, int textureWidth) {
        blitByBlend(() ->
                AbstractGui.blit(stack, x0, y0, z, (float) u0, (float) v0, width, height, textureHeight, textureWidth)
        );
    }

    /**
     * 使用指定的纹理坐标和尺寸信息绘制一个矩形区域。
     *
     * @param x0            矩形的左上角x坐标。
     * @param y0            矩形的左上角y坐标。
     * @param destWidth     目标矩形的宽度，决定了图像在屏幕上的宽度。
     * @param destHeight    目标矩形的高度，决定了图像在屏幕上的高度。
     * @param u0            源图像上矩形左上角的u轴坐标。
     * @param v0            源图像上矩形左上角的v轴坐标。
     * @param srcWidth      源图像上矩形的宽度，用于确定从源图像上裁剪的部分。
     * @param srcHeight     源图像上矩形的高度，用于确定从源图像上裁剪的部分。
     * @param textureWidth  整个纹理的宽度，用于计算纹理坐标。
     * @param textureHeight 整个纹理的高度，用于计算纹理坐标。
     */
    public static void blit(MatrixStack stack, int x0, int y0, int destWidth, int destHeight, double u0, double v0, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        AbstractGui.blit(stack, x0, y0, destWidth, destHeight, (float) u0, (float) v0, srcWidth, srcHeight, textureWidth, textureHeight);
    }

    public static void blitBlend(MatrixStack stack, int x0, int y0, int destWidth, int destHeight, double u0, double v0, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        blitByBlend(() ->
                AbstractGui.blit(stack, x0, y0, destWidth, destHeight, (float) u0, (float) v0, srcWidth, srcHeight, textureWidth, textureHeight)
        );
    }

    public static void blit(MatrixStack stack, int x0, int y0, double u0, double v0, int destWidth, int destHeight, int textureWidth, int textureHeight) {
        AbstractGui.blit(stack, x0, y0, (float) u0, (float) v0, destWidth, destHeight, textureWidth, textureHeight);
    }

    public static void blitBlend(MatrixStack stack, int x0, int y0, double u0, double v0, int destWidth, int destHeight, int textureWidth, int textureHeight) {
        blitByBlend(() ->
                AbstractGui.blit(stack, x0, y0, (float) u0, (float) v0, destWidth, destHeight, textureWidth, textureHeight)
        );
    }

    /**
     * 启用混合模式来绘制纹理
     */
    public static void blitByBlend(Runnable drawFunc) {
        // 启用混合模式来正确处理透明度
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawFunc.run();
        RenderSystem.disableBlend();
    }

    /**
     * 变换后绘制
     *
     * @param args 变换参数
     */
    public static void renderByTransform(TransformArgs args, Consumer<TransformDrawArgs> drawFunc) {

        // 保存当前矩阵状态
        args.stack().pushPose();

        // 计算目标点
        double tranX = 0, tranY = 0;
        double tranW = 0, tranH = 0;
        // 旋转角度为0不需要进行变换
        if (args.angle() % 360 == 0) args.center(EnumRotationCenter.TOP_LEFT);
        switch (args.center()) {
            case CENTER:
                tranW = args.getWidthScaled() / 2.0;
                tranH = args.getHeightScaled() / 2.0;
                tranX = args.x() + tranW;
                tranY = args.y() + tranH;
                break;
            case TOP_LEFT:
                tranX = args.x();
                tranY = args.y();
                break;
            case TOP_RIGHT:
                tranW = args.getWidthScaled();
                tranX = args.x() + tranW;
                tranY = args.y();
                break;
            case TOP_CENTER:
                tranW = args.getWidthScaled() / 2.0;
                tranX = args.x() + tranW;
                tranY = args.y();
                break;
            case BOTTOM_LEFT:
                tranH = args.getHeightScaled();
                tranX = args.x();
                tranY = args.y() + tranH;
                break;
            case BOTTOM_RIGHT:
                tranW = args.getWidthScaled();
                tranH = args.getHeightScaled();
                tranX = args.x() + tranW;
                tranY = args.y() + tranH;
                break;
            case BOTTOM_CENTER:
                tranW = args.getWidthScaled() / 2.0;
                tranH = args.getHeightScaled();
                tranX = args.x() + tranW;
                tranY = args.y() + tranH;
                break;
        }
        // 移至目标点
        args.stack().translate(tranX, tranY, 0);

        // 缩放
        args.stack().scale((float) args.scale(), (float) args.scale(), 1);

        // 旋转
        if (args.angle() % 360 != 0) {
            args.stack().mulPose(Vector3f.ZP.rotationDegrees((float) args.angle()));
        }

        // 翻转
        if (args.flipHorizontal()) {
            args.stack().mulPose(Vector3f.YP.rotationDegrees(180));
        }
        if (args.flipVertical()) {
            args.stack().mulPose(Vector3f.XP.rotationDegrees(180));
        }

        // 返回原点
        args.stack().translate(-tranW, -tranH, 0);

        // 关闭背面剔除
        RenderSystem.disableCull();
        // 绘制方法
        TransformDrawArgs drawArgs = new TransformDrawArgs(args.stack());
        drawArgs.x(0).y(0).width(args.width()).height(args.height());

        // 启用混合模式
        if (args.blend() || args.alpha() < 0xFF) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // 设置透明度
            if (args.alpha() < 0xFF)
                RenderSystem.color4f(1, 1, 1, (float) args.alpha() / 0xFF);
        }

        drawFunc.accept(drawArgs);

        // 关闭混合模式
        if (args.blend() || args.alpha() < 0xFF) {
            // 还原透明度
            if (args.alpha() < 0xFF)
                RenderSystem.color4f(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }

        // 恢复背面剔除
        RenderSystem.enableCull();

        // 恢复矩阵状态
        args.stack().popPose();
    }

    // endregion 绘制纹理

    // region 绘制文字


    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int multilineTextHeight(Text text) {
        return AbstractGuiUtils.multilineTextHeight(text.font(), text.content());
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int multilineTextHeight(String text) {
        return multilineTextHeight(getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int multilineTextHeight(FontRenderer font, String text) {
        return StringUtils.replaceLineBreak(text).split("\n").length * font.lineHeight;
    }

    public static int getStringWidth(String... texts) {
        return getStringWidth(getFont(), texts);
    }

    public static int getStringWidth(FontRenderer font, String... texts) {
        return getStringWidth(font, Arrays.asList(texts));
    }

    public static int getStringWidth(Collection<String> texts) {
        return getStringWidth(getFont(), texts);
    }

    public static int getStringWidth(FontRenderer font, Collection<String> texts) {
        int width = 0;
        for (String s : texts) {
            width = Math.max(width, font.width(s));
        }
        return width;
    }

    public static int getStringHeight(String... texts) {
        return AbstractGuiUtils.getStringHeight(getFont(), texts);
    }

    public static int getStringHeight(FontRenderer font, String... texts) {
        return AbstractGuiUtils.getStringHeight(font, Arrays.asList(texts));
    }

    public static int getStringHeight(Collection<String> texts) {
        return AbstractGuiUtils.getStringHeight(getFont(), texts);
    }

    public static int getStringHeight(FontRenderer font, Collection<String> texts) {
        return AbstractGuiUtils.multilineTextHeight(font, String.join("\n", texts));
    }

    public static int getTextWidth(Collection<Text> texts) {
        return AbstractGuiUtils.getTextWidth(getFont(), texts);
    }

    public static int getTextWidth(FontRenderer font, Collection<Text> texts) {
        int width = 0;
        for (Text text : texts) {
            for (String string : StringUtils.replaceLineBreak(text.content()).split("\n")) {
                width = Math.max(width, font.width(string));
            }
        }
        return width;
    }

    public static int getTextHeight(Collection<Text> texts) {
        return AbstractGuiUtils.getTextHeight(getFont(), texts);
    }

    public static int getTextHeight(FontRenderer font, Collection<Text> texts) {
        return AbstractGuiUtils.multilineTextHeight(font, texts.stream().map(Text::content).collect(Collectors.joining("\n")));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int multilineTextWidth(Text text) {
        return AbstractGuiUtils.multilineTextWidth(text.font(), text.content());
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int multilineTextWidth(String text) {
        return multilineTextWidth(getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int multilineTextWidth(FontRenderer font, String text) {
        int width = 0;
        if (StringUtils.isNotNullOrEmpty(text)) {
            for (String s : StringUtils.replaceLineBreak(text).split("\n")) {
                width = Math.max(width, font.width(s));
            }
        }
        return width;
    }

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
                AbstractGuiUtils.drawLimitedText(clone.y(y + i * clone.text().font().lineHeight));
            }
        }
    }

    /**
     * 将文本按最大宽度自动换行
     *
     * @param text     要换行的文本
     * @param maxWidth 最大宽度
     * @return 换行后的文本列表
     */
    private static List<String> wrapText(FontRenderer font, String text, int maxWidth) {
        List<String> wrappedLines = new ArrayList<>();
        if (maxWidth <= 0 || text == null || text.isEmpty()) {
            if (text != null && !text.isEmpty()) {
                wrappedLines.add(text);
            }
            return wrappedLines;
        }

        // 定义分隔符模式，\p{Punct} 匹配所有标点符号
        String separatorPattern = "[\\s\\p{Punct}]+";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(separatorPattern);

        // 使用正则表达式分割文本，保留分隔符
        List<String> segments = StringUtils.splitStrings(text, pattern);

        // 如果文本中没有分隔符，直接按字符处理
        if (segments.isEmpty()) {
            return splitLongSegment(font, text, maxWidth);
        }

        StringBuilder currentLine = new StringBuilder();

        for (String segment : segments) {
            // 判断是否是分隔符
            boolean isSeparator = pattern.matcher(segment).matches();

            // 构建测试行
            String testLine;
            if (currentLine.length() == 0) {
                testLine = segment;
            } else if (isSeparator) {
                testLine = currentLine + segment;
            } else {
                // 检查前一个字符是否是分隔符
                String lastChar = currentLine.length() > 0 ?
                        String.valueOf(currentLine.charAt(currentLine.length() - 1)) : "";
                boolean lastIsSeparator = !lastChar.isEmpty() && pattern.matcher(lastChar).matches();

                if (lastIsSeparator) {
                    testLine = currentLine + segment;
                } else {
                    testLine = currentLine + " " + segment;
                }
            }

            int testWidth = font.width(testLine);

            if (testWidth > maxWidth && currentLine.length() > 0) {
                // 当前行已满，保存当前行并开始新行
                wrappedLines.add(currentLine.toString());

                // 处理当前段
                if (isSeparator) {
                    // 分隔符保留在下一行开头
                    currentLine = new StringBuilder(segment);
                } else {
                    // 检查单个段是否超过最大宽度
                    if (font.width(segment) > maxWidth) {
                        // 强制换行
                        List<String> splitSegments = splitLongSegment(font, segment, maxWidth);
                        if (!splitSegments.isEmpty()) {
                            currentLine = new StringBuilder(splitSegments.get(0));
                            // 将剩余部分添加到新行
                            for (int i = 1; i < splitSegments.size(); i++) {
                                wrappedLines.add(splitSegments.get(i));
                            }
                        } else {
                            currentLine = new StringBuilder();
                        }
                    } else {
                        // 开始新行
                        currentLine = new StringBuilder(segment);
                    }
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        // 添加最后一行
        if (currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
        }

        // 若仍然超过最大宽度，进行强制换行
        List<String> finalLines = new ArrayList<>();
        for (String line : wrappedLines) {
            if (font.width(line) > maxWidth) {
                finalLines.addAll(splitLongSegment(font, line, maxWidth));
            } else {
                finalLines.add(line);
            }
        }

        return finalLines.isEmpty() ? wrappedLines : finalLines;
    }

    /**
     * 将超长的文本段按最大宽度强制换行
     *
     * @param segment  要分割的文本段
     * @param maxWidth 最大宽度
     * @return 分割后的文本列表
     */
    private static List<String> splitLongSegment(FontRenderer font, String segment, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (segment == null || segment.isEmpty() || maxWidth <= 0) {
            if (segment != null && !segment.isEmpty()) {
                lines.add(segment);
            }
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();

        // 逐字符处理
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            String testLine = currentLine.toString() + c;
            int testWidth = font.width(testLine);

            if (testWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(String.valueOf(c));
            } else {
                currentLine.append(c);
            }
        }

        // 添加最后一行
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * 计算文字绘制后的最终宽高
     *
     * @param args 绘制参数
     */
    public static KeyValue<Integer, Integer> calculateLimitedTextSize(@Nonnull FontDrawArgs args) {
        Text text = args.text();
        if (StringUtils.isNullOrEmpty(text.content())) {
            return new KeyValue<>(0, 0);
        }

        String ellipsis = "...";
        FontRenderer font = text.font();
        int ellipsisWidth = font.width(ellipsis);

        // 缩放比例
        float scale = args.fontSize() > 0 ? args.fontSize() / font.lineHeight : 1.0f;

        // 实际绘制区域
        double drawX = args.x() + args.paddingLeft();
        // 可用宽度 = 原始可用宽度 / 缩放比例
        int availableWidth = args.maxWidth() > 0 ? (int) ((args.maxWidth() - args.paddingLeft() - args.paddingRight()) / scale) : 0;

        if (args.inScreen() && availableWidth > 0 && !args.wrap()) {
            KeyValue<Integer, Integer> screenSize = getScreenSize();
            int screenWidth = screenSize.key();

            // 确保文本不超出屏幕右边界
            if (drawX + availableWidth > screenWidth - args.marginRight()) {
                availableWidth = Math.max(0, screenWidth - (int) drawX - args.marginRight());
            }
            // 确保文本不超出屏幕左边界
            if (drawX < args.marginLeft()) {
                availableWidth = Math.max(0, availableWidth - args.marginLeft() + (int) args.x());
            }
        }

        // 拆分文本行
        List<String> lines = new ArrayList<>();
        String[] originalLines = StringUtils.replaceLineBreak(text.content()).split("\n");

        // 如果启用自动换行且设置了最大宽度，对每行进行换行处理
        if (args.wrap() && availableWidth > 0) {
            for (String originalLine : originalLines) {
                lines.addAll(wrapText(font, originalLine, availableWidth));
            }
        } else {
            lines.addAll(Arrays.asList(originalLines));
        }

        int actualMaxLine = args.maxLine();
        if (actualMaxLine <= 0 || actualMaxLine >= lines.size()) {
            actualMaxLine = lines.size();
        }

        List<String> outputLines = new ArrayList<>();
        if (actualMaxLine > 1 && lines.size() > actualMaxLine) {
            switch (args.position()) {
                case START:
                    // 显示最后 maxLine 行，开头加省略号
                    outputLines.add(ellipsis);
                    outputLines.addAll(lines.subList(lines.size() - actualMaxLine + 1, lines.size()));
                    break;
                case MIDDLE:
                    // 显示前后各一部分，中间加省略号
                    int midStart = actualMaxLine / 2;
                    int midEnd = lines.size() - (actualMaxLine - midStart) + 1;
                    outputLines.addAll(lines.subList(0, midStart));
                    outputLines.add(ellipsis);
                    outputLines.addAll(lines.subList(midEnd, lines.size()));
                    break;
                case END:
                    // 显示前 maxLine 行，结尾加省略号
                    outputLines.addAll(lines.subList(0, actualMaxLine - 1));
                    outputLines.add(ellipsis);
                    break;
                default:
                    outputLines.addAll(lines);
                    break;
            }
        } else {
            if (actualMaxLine == 1) {
                outputLines.add(lines.get(0));
            } else {
                // 正常显示所有行
                outputLines.addAll(lines);
            }
        }

        // 处理每行的截断
        List<String> finalLines = new ArrayList<>();
        for (String line : outputLines) {
            // 若宽度超出可用宽度，进行截断并加省略号
            line = ellipsisString(args, ellipsis, font, ellipsisWidth, availableWidth, line);
            finalLines.add(line);
        }

        // 计算文本区域尺寸
        int maxLineWidth = getStringWidth(font, finalLines);
        if (availableWidth > 0) {
            maxLineWidth = Math.min(maxLineWidth, availableWidth);
        }
        // 实际行高使用
        float actualLineHeight = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;
        int totalHeight = (int) (finalLines.size() * actualLineHeight);

        // 最终宽高
        int finalWidth = (int) Math.ceil(maxLineWidth * scale) + args.paddingLeft() + args.paddingRight();
        int finalHeight = totalHeight + args.paddingTop() + args.paddingBottom();

        return new KeyValue<>(finalWidth, finalHeight);
    }

    private static String ellipsisString(@Nonnull FontDrawArgs args, String ellipsis, FontRenderer font, int ellipsisWidth, int availableWidth, String line) {
        if (availableWidth > 0 && font.width(line) > availableWidth) {
            if (args.position() == EnumEllipsisPosition.START) {
                // 截断前部
                while (font.width(ellipsis + line) > availableWidth && line.length() > 1) {
                    line = line.substring(1);
                }
                line = ellipsis + line;
            } else if (args.position() == EnumEllipsisPosition.END) {
                // 截断后部
                while (font.width(line + ellipsis) > availableWidth && line.length() > 1) {
                    line = line.substring(0, line.length() - 1);
                }
                line = line + ellipsis;
            } else {
                // 截断两侧（默认处理）
                int halfWidth = (availableWidth - ellipsisWidth) / 2;
                String start = line, end = line;
                while (font.width(start) > halfWidth && start.length() > 1) {
                    start = start.substring(0, start.length() - 1);
                }
                while (font.width(end) > halfWidth && end.length() > 1) {
                    end = end.substring(1);
                }
                line = start + ellipsis + end;
            }
        }
        return line;
    }

    /**
     * 绘制限制长度的文本，超出部分以省略号表示，可选择省略号的位置
     */
    public static void drawLimitedText(@Nonnull FontDrawArgs args) {
        Text text = args.text();
        if (StringUtils.isNotNullOrEmpty(text.content())) {
            String ellipsis = "...";
            FontRenderer font = text.font();
            int ellipsisWidth = font.width(ellipsis);

            // 计算缩放比例
            float scale = args.fontSize() > 0 ? args.fontSize() / font.lineHeight : 1.0f;

            // 实际绘制区域
            double drawX = args.x() + args.paddingLeft();
            double drawY = args.y() + args.paddingTop();
            // 可用宽度需 = 原始可用宽度 / 缩放比例
            int availableWidth = args.maxWidth() > 0 ? (int) ((args.maxWidth() - args.paddingLeft() - args.paddingRight()) / scale) : 0;

            if (args.inScreen() && availableWidth > 0) {
                KeyValue<Integer, Integer> screenSize = getScreenSize();
                int screenWidth = screenSize.key();

                // 确保文本不超出屏幕右边界
                if (drawX + availableWidth > screenWidth - args.marginRight()) {
                    availableWidth = Math.max(0, screenWidth - (int) drawX - args.marginRight());
                }
                // 确保文本不超出屏幕左边界
                if (drawX < args.marginLeft()) {
                    drawX = args.marginLeft();
                    availableWidth = Math.max(0, availableWidth - args.marginLeft() + (int) args.x());
                }
            }

            // 拆分文本行
            List<String> lines = new ArrayList<>();
            String[] originalLines = StringUtils.replaceLineBreak(text.content()).split("\n");

            // 若启用自动换行且设置了最大宽度，对每行进行换行处理
            if (args.wrap() && availableWidth > 0) {
                for (String originalLine : originalLines) {
                    lines.addAll(wrapText(font, originalLine, availableWidth));
                }
            } else {
                lines.addAll(Arrays.asList(originalLines));
            }

            int actualMaxLine = args.maxLine();
            if (actualMaxLine <= 0 || actualMaxLine >= lines.size()) {
                actualMaxLine = lines.size();
            }

            List<String> outputLines = new ArrayList<>();
            if (actualMaxLine > 1 && lines.size() > actualMaxLine) {
                switch (args.position()) {
                    case START:
                        // 显示最后 maxLine 行，开头加省略号
                        outputLines.add(ellipsis);
                        outputLines.addAll(lines.subList(lines.size() - actualMaxLine + 1, lines.size()));
                        break;
                    case MIDDLE:
                        // 显示前后各一部分，中间加省略号
                        int midStart = actualMaxLine / 2;
                        int midEnd = lines.size() - (actualMaxLine - midStart) + 1;
                        outputLines.addAll(lines.subList(0, midStart));
                        outputLines.add(ellipsis);
                        outputLines.addAll(lines.subList(midEnd, lines.size()));
                        break;
                    case END:
                        // 显示前 maxLine 行，结尾加省略号
                        outputLines.addAll(lines.subList(0, actualMaxLine - 1));
                        outputLines.add(ellipsis);
                        break;
                    default:
                        outputLines.addAll(lines);
                        break;
                }
            } else {
                if (actualMaxLine == 1) {
                    outputLines.add(lines.get(0));
                } else {
                    // 正常显示所有行
                    outputLines.addAll(lines);
                }
            }

            // 计算文本区域尺寸
            int maxLineWidth = getStringWidth(font, outputLines);
            if (availableWidth > 0) {
                maxLineWidth = Math.min(maxLineWidth, availableWidth);
            }
            // 实际行高使用
            final float actualLineHeight = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;
            float totalHeight = outputLines.size() * actualLineHeight;

            // 绘制背景
            MatrixStack stack = text.stack();
            if (args.bgArgb() != 0) {
                int bgX = (int) args.x();
                int bgY = (int) args.y();
                // 背景宽度需要考虑缩放后的文本宽度
                int bgWidth = (int) (maxLineWidth * scale) + args.paddingLeft() + args.paddingRight();
                int bgHeight = (int) (totalHeight + args.paddingTop() + args.paddingBottom());

                // 绘制圆角矩形背景
                AbstractGuiUtils.fill(stack, bgX, bgY, bgWidth, bgHeight, args.bgArgb(), args.bgBorderRadius());

                // 绘制边框
                if (args.bgBorderThickness() > 0) {
                    int borderArgb = ColorUtils.softenArgb(args.bgArgb());

                    AbstractGuiUtils.fillOutLine(stack, bgX, bgY, bgWidth, bgHeight, args.bgBorderThickness(), borderArgb, args.bgBorderRadius());
                }
            }

            // 应用缩放变换
            boolean needsScale = Math.abs(scale - 1.0f) > 0.001f;
            if (needsScale) {
                stack.pushPose();
                // 移动到绘制起始位置
                stack.translate(drawX, drawY, 0);
                // 应用缩放
                stack.scale(scale, scale, 1.0f);
                // 调整绘制坐标
                drawX = 0;
                drawY = 0;
            }

            // 绘制文本
            int index = 0;
            for (String line : outputLines) {
                // 若宽度超出可用宽度，进行截断并加省略号
                line = ellipsisString(args, ellipsis, font, ellipsisWidth, availableWidth, line);

                // 计算水平偏移
                float xOffset;
                EnumAlignment alignment = args.align() != null ? args.align() : text.align();
                switch (alignment) {
                    case CENTER:
                        xOffset = (maxLineWidth - font.width(line)) / 2.0f;
                        break;
                    case END:
                        xOffset = maxLineWidth - font.width(line);
                        break;
                    default:
                        xOffset = 0;
                        break;
                }

                // 计算垂直位置
                float yPos;
                if (needsScale) {
                    yPos = (float) drawY + index * font.lineHeight;
                } else {
                    yPos = (float) drawY + index * actualLineHeight;
                }

                // 绘制文本背景
                if (!text.bgColorEmpty()) {
                    if (needsScale) {
                        AbstractGuiUtils.fill(stack, (int) (xOffset), (int) (yPos), font.width(line), font.lineHeight, text.bgColorArgb());
                    } else {
                        AbstractGuiUtils.fill(stack, (int) (drawX + xOffset), (int) (yPos), font.width(line), font.lineHeight, text.bgColorArgb());
                    }
                }

                // 绘制文本
                if (text.shadow()) {
                    font.drawShadow(stack, text.copyWithoutChildren().text(line).toComponent().toTextComponent(LanguageHelper.getClientLanguage()), (float) drawX + xOffset, yPos, text.colorArgb());
                } else {
                    font.draw(stack, text.copyWithoutChildren().text(line).toComponent().toTextComponent(LanguageHelper.getClientLanguage()), (float) drawX + xOffset, yPos, text.colorArgb());
                }

                index++;
            }

            // 恢复矩阵状态
            if (needsScale) {
                stack.popPose();
            }
        }
    }

    // endregion 绘制文字

    // region 绘制图标

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param width          目标矩形的宽度，决定了图像在屏幕上的宽度
     * @param height         目标矩形的高度，决定了图像在屏幕上的高度
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(MatrixStack stack, EffectInstance effectInstance, int x, int y, int width, int height, boolean showText) {
        AbstractGuiUtils.drawEffectIcon(stack, getFont(), effectInstance, x, y, width, height, showText);
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param width          目标矩形的宽度，决定了图像在屏幕上的宽度
     * @param height         目标矩形的高度，决定了图像在屏幕上的高度
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(MatrixStack stack, FontRenderer font, EffectInstance effectInstance, int x, int y, int width, int height, boolean showText) {
        ResourceLocation effectIcon = TextureUtils.getEffectTexture(BaniraCodex.resourceFactory(), effectInstance);
        if (effectIcon != null) {
            AbstractGuiUtils.bindTexture(effectIcon);
            AbstractGuiUtils.blit(stack, x, y, 0, 0, width, height, width, height);
        }
        if (showText) {
            // 效果等级
            if (effectInstance.getAmplifier() >= 0) {
                Component amplifierString = Component.literal(NumberUtils.intToRoman(effectInstance.getAmplifier() + 1));
                int amplifierWidth = font.width(amplifierString.toString());
                float fontX = x + width - (float) amplifierWidth / 2;
                float fontY = y - 1;
                int argb = 0xFFFFFFFF;
                font.drawShadow(stack, amplifierString.color(Color.argb(argb)).toTextComponent(), fontX, fontY, argb);
            }
            // 效果持续时间
            if (effectInstance.getDuration() > 0) {
                Component durationString = Component.literal(DateUtils.toMaxUnitString(effectInstance.getDuration(), DateUtils.DateUnit.SECOND, 0, 1));
                int durationWidth = font.width(durationString.toString());
                float fontX = x + width - (float) durationWidth / 2 - 2;
                float fontY = y + (float) height / 2 + 1;
                int argb = 0xFFFFFFFF;
                font.drawShadow(stack, durationString.color(Color.argb(argb)).toTextComponent(), fontX, fontY, argb);
            }
        }
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(MatrixStack stack, EffectInstance effectInstance, int x, int y, boolean showText) {
        AbstractGuiUtils.drawEffectIcon(stack, getFont(), effectInstance, x, y, showText);
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(MatrixStack stack, FontRenderer font, EffectInstance effectInstance, int x, int y, boolean showText) {
        AbstractGuiUtils.drawEffectIcon(stack, font, effectInstance, x, y, ITEM_ICON_SIZE, ITEM_ICON_SIZE, showText);
    }

    public static void renderItem(ItemRenderer itemRenderer, ItemStack itemStack, int x, int y, boolean showText) {
        AbstractGuiUtils.renderItem(itemRenderer, getFont(), itemStack, x, y, showText);
    }

    public static void renderItem(ItemRenderer itemRenderer, FontRenderer font, ItemStack itemStack, int x, int y, boolean showText) {
        itemRenderer.renderGuiItem(itemStack, x, y);
        if (showText) {
            itemRenderer.renderGuiItemDecorations(font, itemStack, x, y, String.valueOf(itemStack.getCount()));
        }
    }

    //  endregion 绘制图标

    //  region 绘制形状

    /**
     * 绘制一个“像素”矩形
     *
     * @param x    像素的 X 坐标
     * @param y    像素的 Y 坐标
     * @param argb 像素的颜色
     */
    public static void drawPixel(MatrixStack stack, int x, int y, int argb) {
        AbstractGui.fill(stack, x, y, x + 1, y + 1, argb);
    }

    /**
     * 绘制一个正方形
     */
    public static void fill(MatrixStack stack, int x, int y, int width, int argb) {
        AbstractGuiUtils.fill(stack, x, y, width, width, argb);
    }

    /**
     * 绘制一个矩形
     */
    public static void fill(MatrixStack stack, int x, int y, int width, int height, int argb) {
        AbstractGuiUtils.fill(stack, x, y, width, height, argb, 0);
    }

    /**
     * 绘制一个圆角矩形
     *
     * @param x      矩形的左上角X坐标
     * @param y      矩形的左上角Y坐标
     * @param width  矩形的宽度
     * @param height 矩形的高度
     * @param argb   矩形的颜色
     * @param radius 圆角半径(0-10)
     */
    public static void fill(MatrixStack stack, int x, int y, int width, int height, int argb, int radius) {
        // 如果半径为0，则直接绘制普通矩形
        if (radius <= 0) {
            AbstractGui.fill(stack, x, y, x + width, y + height, argb);
            return;
        }

        // 限制半径最大值为10
        radius = Math.min(radius, 10);

        // 1. 绘制中间的矩形部分（去掉圆角占用的区域）
        AbstractGuiUtils.fill(stack, x + radius + 1, y + radius + 1, width - 2 * (radius + 1), height - 2 * (radius + 1), argb);

        // 2. 绘制四条边（去掉圆角占用的部分）
        // 上边
        AbstractGuiUtils.fill(stack, x + radius + 1, y, width - 2 * radius - 2, radius, argb);
        AbstractGuiUtils.fill(stack, x + radius + 1, y + radius, width - 2 * (radius + 1), 1, argb);
        // 下边
        AbstractGuiUtils.fill(stack, x + radius + 1, y + height - radius, width - 2 * radius - 2, radius, argb);
        AbstractGuiUtils.fill(stack, x + radius + 1, y + height - radius - 1, width - 2 * (radius + 1), 1, argb);
        // 左边
        AbstractGuiUtils.fill(stack, x, y + radius + 1, radius, height - 2 * radius - 2, argb);
        AbstractGuiUtils.fill(stack, x + radius, y + radius + 1, 1, height - 2 * (radius + 1), argb);
        // 右边
        AbstractGuiUtils.fill(stack, x + width - radius, y + radius + 1, radius, height - 2 * radius - 2, argb);
        AbstractGuiUtils.fill(stack, x + width - radius - 1, y + radius + 1, 1, height - 2 * (radius + 1), argb);

        // 3. 绘制四个圆角
        // 左上角
        AbstractGuiUtils.drawCircleQuadrant(stack, x + radius, y + radius, radius, argb, 1);
        // 右上角
        AbstractGuiUtils.drawCircleQuadrant(stack, x + width - radius - 1, y + radius, radius, argb, 2);
        // 左下角
        AbstractGuiUtils.drawCircleQuadrant(stack, x + radius, y + height - radius - 1, radius, argb, 3);
        // 右下角
        AbstractGuiUtils.drawCircleQuadrant(stack, x + width - radius - 1, y + height - radius - 1, radius, argb, 4);
    }

    /**
     * 绘制一个圆的四分之一部分（圆角辅助函数）
     *
     * @param centerX  圆角中心点X坐标
     * @param centerY  圆角中心点Y坐标
     * @param radius   圆角半径
     * @param argb     圆角颜色
     * @param quadrant 指定绘制的象限（1=左上，2=右上，3=左下，4=右下）
     */
    private static void drawCircleQuadrant(MatrixStack stack, int centerX, int centerY, int radius, int argb, int quadrant) {
        for (int dx = 0; dx <= radius; dx++) {
            for (int dy = 0; dy <= radius; dy++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    drawCircleQuadrantPixel(stack, centerX, centerY, argb, quadrant, dx, dy);
                }
            }
        }
    }

    private static void drawCircleQuadrantPixel(MatrixStack stack, int centerX, int centerY, int argb, int quadrant, int dx, int dy) {
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
     * 绘制一个矩形边框
     *
     * @param thickness 边框厚度
     * @param argb      边框颜色
     */
    public static void fillOutLine(MatrixStack stack, int x, int y, int width, int height, int thickness, int argb) {
        // 上边
        AbstractGuiUtils.fill(stack, x, y, width, thickness, argb);
        // 下边
        AbstractGuiUtils.fill(stack, x, y + height - thickness, width, thickness, argb);
        // 左边
        AbstractGuiUtils.fill(stack, x, y, thickness, height, argb);
        // 右边
        AbstractGuiUtils.fill(stack, x + width - thickness, y, thickness, height, argb);
    }

    /**
     * 绘制一个圆角矩形边框
     *
     * @param x         矩形左上角X坐标
     * @param y         矩形左上角Y坐标
     * @param width     矩形宽度
     * @param height    矩形高度
     * @param thickness 边框厚度
     * @param argb      边框颜色
     * @param radius    圆角半径（0-10）
     */
    public static void fillOutLine(MatrixStack stack, int x, int y, int width, int height, int thickness, int argb, int radius) {
        if (thickness <= 0) return;
        if (radius <= 0) {
            // 若没有圆角，直接绘制普通边框
            AbstractGuiUtils.fillOutLine(stack, x, y, width, height, thickness, argb);
        } else {
            // 限制圆角半径的最大值为10
            radius = Math.min(radius, 10);

            // 1. 绘制四条边（去掉圆角区域）
            // 上边
            AbstractGuiUtils.fill(stack, x + radius, y, width - 2 * radius, thickness, argb);
            // 下边
            AbstractGuiUtils.fill(stack, x + radius, y + height - thickness, width - 2 * radius, thickness, argb);
            // 左边
            AbstractGuiUtils.fill(stack, x, y + radius, thickness, height - 2 * radius, argb);
            // 右边
            AbstractGuiUtils.fill(stack, x + width - thickness, y + radius, thickness, height - 2 * radius, argb);

            // 2. 绘制四个圆角
            // 左上角
            drawCircleBorder(stack, x + radius, y + radius, radius, thickness, argb, 1);
            // 右上角
            drawCircleBorder(stack, x + width - radius - 1, y + radius, radius, thickness, argb, 2);
            // 左下角
            drawCircleBorder(stack, x + radius, y + height - radius - 1, radius, thickness, argb, 3);
            // 右下角
            drawCircleBorder(stack, x + width - radius - 1, y + height - radius - 1, radius, thickness, argb, 4);
        }
    }

    /**
     * 绘制一个圆角的边框区域
     *
     * @param centerX   圆角中心点X坐标
     * @param centerY   圆角中心点Y坐标
     * @param radius    圆角半径
     * @param thickness 边框厚度
     * @param argb      边框颜色
     * @param quadrant  指定绘制的象限（1=左上，2=右上，3=左下，4=右下）
     */
    private static void drawCircleBorder(MatrixStack stack, int centerX, int centerY, int radius, int thickness, int argb, int quadrant) {
        for (int dx = 0; dx <= radius; dx++) {
            for (int dy = 0; dy <= radius; dy++) {
                double sqrt = Math.sqrt(dx * dx + dy * dy);
                if (sqrt <= radius && sqrt >= radius - thickness) {
                    drawCircleQuadrantPixel(stack, centerX, centerY, argb, quadrant, dx, dy);
                }
            }
        }
    }

    //  endregion 绘制形状

    //  region 绘制弹出层提示

    /**
     * 绘制九宫格纹理背景
     *
     * @param texture    纹理资源位置
     * @param x          绘制起始X坐标
     * @param y          绘制起始Y坐标
     * @param destWidth  目标宽度（缩放后的最终宽度）
     * @param destHeight 目标高度（缩放后的最终高度）
     * @param scale      缩放比例（用于根据右参考线高度缩放）
     */
    public static void drawNinePatch(MatrixStack stack, ResourceLocation texture, int x, int y, int destWidth, int destHeight, float scale) {
        TextureUtils.NinePatchInfo info = TextureUtils.parseNinePatch(texture);
        bindTexture(texture);
        if (info == null) {
            KeyValue<Integer, Integer> texSize = TextureUtils.getTextureSize(texture);
            blit(stack, x, y, 0, 0, destWidth, destHeight, texSize.key(), texSize.val());
            return;
        }

        // 内容区域的边界（引导线占1像素）
        int contentStartX = 1;
        int contentStartY = 1;

        // 原始尺寸
        float originalDestWidth = destWidth / scale;
        float originalDestHeight = destHeight / scale;

        // 原始尺寸的固定和可拉伸区域
        int totalFixedWidth = 0;
        int totalFixedHeight = 0;
        int totalStretchableWidth = 0;
        int totalStretchableHeight = 0;

        // 原始尺寸水平方向的固定和可拉伸区域
        for (int i = 0; i < info.horizontalDivisions.length - 1; i++) {
            int regionWidth = info.horizontalDivisions[i + 1] - info.horizontalDivisions[i];
            if (info.horizontalStretchable[i]) {
                totalStretchableWidth += regionWidth;
            } else {
                totalFixedWidth += regionWidth;
            }
        }

        // 原始尺寸垂直方向的固定和可拉伸区域
        for (int i = 0; i < info.verticalDivisions.length - 1; i++) {
            int regionHeight = info.verticalDivisions[i + 1] - info.verticalDivisions[i];
            if (info.verticalStretchable[i]) {
                totalStretchableHeight += regionHeight;
            } else {
                totalFixedHeight += regionHeight;
            }
        }

        // 原始尺寸需要拉伸的尺寸
        float stretchWidth = Math.max(0, originalDestWidth - totalFixedWidth);
        float stretchHeight = Math.max(0, originalDestHeight - totalFixedHeight);

        // 拉伸比例
        float stretchWidthRatio = totalStretchableWidth > 0 ? stretchWidth / totalStretchableWidth : 1.0f;
        float stretchHeightRatio = totalStretchableHeight > 0 ? stretchHeight / totalStretchableHeight : 1.0f;

        // 应用缩放变换
        boolean needsScale = Math.abs(scale - 1.0f) > 0.001f;
        if (needsScale) {
            stack.pushPose();
            // 移动到绘制起始位置
            stack.translate(x, y, 0);
            // 应用缩放
            stack.scale(scale, scale, 1.0f);
            x = 0;
            y = 0;
        }

        // 绘制各个区域
        float currentY = y;
        for (int v = 0; v < info.verticalDivisions.length - 1; v++) {
            int srcVStart = contentStartY + info.verticalDivisions[v];
            int srcVEnd = contentStartY + info.verticalDivisions[v + 1] - 1;
            int srcVHeight = srcVEnd - srcVStart + 1;

            float destVHeight;
            if (info.verticalStretchable[v]) {
                destVHeight = srcVHeight * stretchHeightRatio;
            } else {
                destVHeight = srcVHeight;
            }

            float currentX = x;
            for (int h = 0; h < info.horizontalDivisions.length - 1; h++) {
                int srcHStart = contentStartX + info.horizontalDivisions[h];
                int srcHEnd = contentStartX + info.horizontalDivisions[h + 1] - 1;
                int srcHWidth = srcHEnd - srcHStart + 1;

                float destHWidth;
                if (info.horizontalStretchable[h]) {
                    destHWidth = srcHWidth * stretchWidthRatio;
                } else {
                    destHWidth = srcHWidth;
                }

                // 绘制当前区域
                blit(stack, (int) currentX, (int) currentY, (int) destHWidth, (int) destVHeight,
                        srcHStart, srcVStart, srcHWidth, srcVHeight,
                        info.texWidth, info.texHeight);

                currentX += destHWidth;
            }

            currentY += destVHeight;
        }

        // 恢复矩阵状态
        if (needsScale) {
            stack.popPose();
        }
    }

    /**
     * 绘制弹出层消息
     */
    public static void drawPopupMessageWithSeason(FontDrawArgs args) {
        ResourceLocation texture;
        switch (DateUtils.getSeason()) {
            case SUMMER:
                texture = TextureUtils.loadCustomTexture(BaniraCodex.resourceFactory(), "textures/gui/aotake_cat.png");
                break;
            case AUTUMN:
                texture = TextureUtils.loadCustomTexture(BaniraCodex.resourceFactory(), "textures/gui/narcissus_cat.png");
                break;
            case WINTER:
                texture = TextureUtils.loadCustomTexture(BaniraCodex.resourceFactory(), "textures/gui/snowflake_cat.png");
                break;
            default:
                texture = TextureUtils.loadCustomTexture(BaniraCodex.resourceFactory(), "textures/gui/sakura_cat.png");
                break;
        }
        AbstractGuiUtils.drawPopupMessage(args.texture(texture));
    }

    /**
     * 绘制弹出层消息
     */
    public static void drawPopupMessage(FontDrawArgs args) {
        // 计算文字最终绘制大小
        KeyValue<Integer, Integer> textSize = calculateLimitedTextSize(args);
        int textWidth = textSize.key();
        int textHeight = textSize.val();

        // 检查是否是.9.png格式纹理
        final TextureUtils.NinePatchInfo ninePatchInfo = args.texture() != null ? TextureUtils.parseNinePatch(args.texture()) : null;

        float calculatedTextureScale = 1.0f;
        int calculatedPaddingLeft = args.paddingLeft();
        int calculatedPaddingRight = args.paddingRight();
        int calculatedPaddingTop = args.paddingTop();
        int calculatedPaddingBottom = args.paddingBottom();

        if (ninePatchInfo != null) {
            Color color = Color.argb(ninePatchInfo.textColor);
            if (!color.isEmpty()) {
                args.text().color(color);
            }
            // 计算背景绘制大小与内外边距
            FontRenderer font = args.text().font();
            float targetFontSize = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;

            // 根据右参考线的高度计算缩放比例
            if (ninePatchInfo.rightGuideHeight > 0) {
                calculatedTextureScale = targetFontSize / ninePatchInfo.rightGuideHeight;
            }

            // 根据下参考线计算内边距
            if (ninePatchInfo.bottomGuideLeftPadding > 0) {
                calculatedPaddingLeft += (int) (ninePatchInfo.bottomGuideLeftPadding * calculatedTextureScale);
            }
            if (ninePatchInfo.bottomGuideRightPadding > 0) {
                calculatedPaddingRight += (int) (ninePatchInfo.bottomGuideRightPadding * calculatedTextureScale);
            }

            // 计算上内边距
            if (ninePatchInfo.rightGuideTopPadding > 0) {
                calculatedPaddingTop += (int) (ninePatchInfo.rightGuideTopPadding * calculatedTextureScale);
            }

            // 计算下内边距
            if (ninePatchInfo.rightGuideBottomPadding > 0) {
                calculatedPaddingBottom += (int) (ninePatchInfo.rightGuideBottomPadding * calculatedTextureScale);
            }

            // 重新计算文本尺寸
            FontDrawArgs recalcArgs = args.clone()
                    .paddingLeft(calculatedPaddingLeft)
                    .paddingRight(calculatedPaddingRight)
                    .paddingTop(calculatedPaddingTop)
                    .paddingBottom(calculatedPaddingBottom);
            textSize = calculateLimitedTextSize(recalcArgs);
            textWidth = textSize.key();
            textHeight = textSize.val();
        }

        final float textureScale = calculatedTextureScale;
        final int finalCalculatedPaddingLeft = calculatedPaddingLeft;
        final int finalCalculatedPaddingRight = calculatedPaddingRight;
        final int finalCalculatedPaddingTop = calculatedPaddingTop;
        final int finalCalculatedPaddingBottom = calculatedPaddingBottom;

        // 计算消息框的总宽度和高度
        int msgWidth = textWidth;
        int msgHeight = textHeight;

        // 计算调整后的坐标
        double adjustedX = args.x();
        double adjustedY = args.y();
        int finalMaxWidth = args.maxWidth();

        if (args.inScreen()) {
            KeyValue<Integer, Integer> screenSize = getScreenSize();
            int screenWidth = screenSize.key();
            int screenHeight = screenSize.val();

            // 若启用了自动换行，根据 maxWidth 计算文本尺寸
            if (args.wrap() && finalMaxWidth > 0) {
                FontDrawArgs maxWidthRecalcArgs = args.clone()
                        .paddingLeft(finalCalculatedPaddingLeft)
                        .paddingRight(finalCalculatedPaddingRight)
                        .paddingTop(finalCalculatedPaddingTop)
                        .paddingBottom(finalCalculatedPaddingBottom)
                        .maxWidth(finalMaxWidth);
                KeyValue<Integer, Integer> maxWidthTextSize = calculateLimitedTextSize(maxWidthRecalcArgs);
                msgWidth = maxWidthTextSize.key();
                msgHeight = maxWidthTextSize.val();
            }

            // 初始化调整后的坐标
            // 横向居中
            adjustedX = args.x() - msgWidth / 2.0;
            // 放置于鼠标上方
            adjustedY = args.y() - msgHeight - 5;

            // 检查顶部空间是否充足
            boolean hasTopSpace = adjustedY >= args.marginTop();
            // 检查左右空间是否充足
            boolean hasLeftSpace = adjustedX >= args.marginLeft();
            boolean hasRightSpace = adjustedX + msgWidth <= screenWidth - args.marginRight();

            // 若顶部空间不足，调整到鼠标下方
            if (!hasTopSpace) {
                adjustedY = args.y() + 1 + 5;
            }
            //
            else {
                // 若左侧空间不足，靠右
                if (!hasLeftSpace) {
                    adjustedX = args.marginLeft();
                }
                // 若右侧空间不足，靠左
                else if (!hasRightSpace) {
                    adjustedX = screenWidth - msgWidth - args.marginRight();
                }
            }

            // 若调整后仍然超出屏幕范围，强制限制在屏幕内
            adjustedX = Math.max(args.marginLeft(), Math.min(adjustedX, screenWidth - msgWidth - args.marginRight()));
            adjustedY = Math.max(args.marginTop(), Math.min(adjustedY, screenHeight - msgHeight - args.marginBottom()));

            // 若启用了自动换行，计算实际的可用宽度用于文本绘制时的换行限制
            if (args.wrap()) {
                int actualAvailableWidth = screenWidth - (int) adjustedX - args.marginRight();
                // 如果设置了 maxWidth，取两者中的较小值
                if (finalMaxWidth > 0) {
                    actualAvailableWidth = Math.min(actualAvailableWidth, finalMaxWidth);
                }
                // 确保可用宽度不小于内边距
                actualAvailableWidth = Math.max(actualAvailableWidth, finalCalculatedPaddingLeft + finalCalculatedPaddingRight);
                // 更新 finalMaxWidth 为实际使用的可用宽度
                finalMaxWidth = actualAvailableWidth;
            }
        }

        final int finalMaxWidthForText = finalMaxWidth;

        double finalAdjustedX = adjustedX;
        double finalAdjustedY = adjustedY;
        int finalMsgWidth = msgWidth;
        int finalMsgHeight = msgHeight;
        AbstractGuiUtils.renderByDepth(args.text().stack(), EnumRenderDepth.POPUP_TIPS, (stack) -> {

            // 绘制背景
            FontDrawArgs bgArgs = args.clone().x(finalAdjustedX).y(finalAdjustedY);
            if (bgArgs.texture() != null && ninePatchInfo != null) {
                // 九宫格纹理绘制
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                drawNinePatch(stack, bgArgs.texture(), (int) bgArgs.x(), (int) bgArgs.y(), finalMsgWidth, finalMsgHeight, textureScale);
                RenderSystem.disableBlend();
            } else if (bgArgs.texture() != null) {
                // 普通纹理绘制
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                bindTexture(bgArgs.texture());
                KeyValue<Integer, Integer> texSize = TextureUtils.getTextureSize(bgArgs.texture());
                blit(stack, (int) bgArgs.x(), (int) bgArgs.y(), 0, 0, finalMsgWidth, finalMsgHeight, texSize.key(), texSize.val());
                RenderSystem.disableBlend();
            } else {
                int borderRadius = bgArgs.bgBorderRadius();
                int borderThickness = bgArgs.bgBorderThickness();
                // 绘制圆角矩形背景
                AbstractGuiUtils.fill(bgArgs.text().stack(), (int) bgArgs.x(), (int) bgArgs.y(), finalMsgWidth, finalMsgHeight, bgArgs.bgArgb(), borderRadius);

                // 计算边框颜色
                int borderArgb = ColorUtils.softenArgb(bgArgs.bgArgb());

                // 绘制圆角矩形边框
                AbstractGuiUtils.fillOutLine(bgArgs.text().stack(), (int) bgArgs.x(), (int) bgArgs.y(), finalMsgWidth, finalMsgHeight, borderThickness, borderArgb, borderRadius);
            }

            // 绘制文本
            FontDrawArgs clone = args.clone()
                    .x(finalAdjustedX)
                    .y(finalAdjustedY)
                    .bgArgb(0x00000000)
                    .position(EnumEllipsisPosition.MIDDLE)
                    .paddingLeft(finalCalculatedPaddingLeft)
                    .paddingRight(finalCalculatedPaddingRight)
                    .paddingTop(finalCalculatedPaddingTop)
                    .paddingBottom(finalCalculatedPaddingBottom);
            if (args.wrap() && finalMaxWidthForText > 0) {
                clone.maxWidth(finalMaxWidthForText);
            } else if (args.maxWidth() > 0) {
                clone.maxWidth(args.maxWidth());
            }
            AbstractGuiUtils.drawLimitedText(clone);
        });
    }

    //  endregion 绘制弹出层提示

    // region 杂项

    /**
     * 获取指定坐标点像素颜色
     */
    public static int getPixelArgb(double guiX, double guiY) {
        Minecraft mc = Minecraft.getInstance();
        MainWindow window = mc.getWindow();

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

    public static FontRenderer getFont() {
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

    // endregion 杂项

    // region 重写方法签名

    public static TextFieldWidget newTextFieldWidget(int x, int y, int width, int height, Component content) {
        return newTextFieldWidget(getFont(), x, y, width, height, content);
    }

    public static TextFieldWidget newTextFieldWidget(FontRenderer font, int x, int y, int width, int height, Component content) {
        return new TextFieldWidget(font, x, y, width, height, content.toTextComponent());
    }

    public static Button newButton(int x, int y, int width, int height, Component content, Button.IPressable onPress) {
        return new Button(x, y, width, height, content.toTextComponent(), onPress);
    }

    // endregion 重写方法签名
}

