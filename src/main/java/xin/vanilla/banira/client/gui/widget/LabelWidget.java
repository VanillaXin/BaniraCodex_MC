package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.FontRenderer;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.common.util.Translator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 标签Widget
 */
@Accessors(chain = true, fluent = true)
public class LabelWidget extends BaseWidget implements ITextWidget {
    @Getter
    private Text text = Text.empty();

    /**
     * 是否自动换行
     */
    @Getter
    @Setter
    private boolean textWrap = true;

    /**
     * 省略号位置，maxWidth>0 时生效
     */
    @Getter
    @Setter
    private EnumEllipsisPosition textEllipsisPosition = EnumEllipsisPosition.NONE;

    /**
     * 垂直对齐方式，CENTER 时文本在高度内垂直居中
     */
    @Getter
    @Setter
    private EnumAlignment textVerticalAlign = EnumAlignment.START;

    /**
     * 为 true 时，若文本因 maxWidth 被截断/省略，悬停显示完整内容。
     */
    @Getter
    @Setter
    private boolean showFullTextTooltipWhenTruncated = false;

    public LabelWidget(BaniraScreen screen) {
        super(screen);
    }

    public LabelWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public LabelWidget(BaniraScreen screen, ScreenCoordinate bounds, Text text) {
        super(screen, bounds);
        this.text = text;
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        if (text != null && !text.colorEmpty() && (text.color() & 0xFFFFFF) == 0xFFFFFF) {
            text.color(theme.textPrimary());
        }
    }

    @Override
    public boolean needsUpdate() {
        return false;
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        FontDrawArgs args = FontDrawArgs.of(text.stack(stack)).x(x()).y(y()).maxWidth((int) width())
                .wrap(textWrap).position(textEllipsisPosition);
        if (textVerticalAlign == EnumAlignment.CENTER && height() > 0) {
            KeyValue<Integer, Integer> size = calculateLimitedTextSize(args);
            int textHeight = size.val();
            if (textHeight > 0) {
                args.y(y() + Math.max(0, (height() - textHeight) / 2.0));
            }
        }
        drawLimitedText(args);

        maybeDeferTruncationTooltip(stack);

        renderChildren(stack, partialTicks);
    }

    /**
     * 当前标签在给定宽度与换行/省略设置下是否发生了可见截断。
     */
    private boolean isLabelTextTruncated(MatrixStack stack) {
        int mw = (int) width();
        if (mw <= 0 || StringUtils.isNullOrEmpty(text.content())) {
            return false;
        }
        FontDrawArgs fitted = FontDrawArgs.of(text.stack(stack)).x(0).y(0).maxWidth(mw)
                .wrap(textWrap).position(textEllipsisPosition);
        FontDrawArgs natural = fitted.clone().maxWidth(20000).position(EnumEllipsisPosition.NONE);
        int wNatural = calculateLimitedTextSize(natural).key();
        int wFitted = calculateLimitedTextSize(fitted).key();
        return wNatural > wFitted;
    }

    private void maybeDeferTruncationTooltip(MatrixStack stack) {
        if (!showFullTextTooltipWhenTruncated || screen == null || !enabled) {
            return;
        }
        if (!isLabelTextTruncated(stack)) {
            return;
        }
        double mx = screen.inputState().mouseX();
        double my = screen.inputState().mouseY();
        if (!isMouseInside(mx, my)) {
            return;
        }
        if (screen.isAnyDropdownSelectOpen()) {
            return;
        }
        BaniraColorConfig theme = screen.getEffectiveTheme();
        EnumSeason tipSeason = screen.season();
        boolean useTexture = theme.tooltipUseTexture();
        FontRenderer fontForTip = text.font() != null ? text.font() : screen.getFont();
        Text tipText = text.clone();
        int mouseX = (int) mx;
        int mouseY = (int) my;
        screen.addDeferredTooltipRender(s -> {
            s.pushPose();
            s.last().pose().setIdentity();
            TooltipWidget.drawPopupMessage(s,
                    FontDrawArgs.ofPopo(tipText.stack(s).font(fontForTip)).x(mouseX).y(mouseY).popupUseTexture(useTexture),
                    theme, tipSeason);
            s.popPose();
        });
    }

    /**
     * 计算文字绘制后的最终宽高
     */
    public static KeyValue<Integer, Integer> calculateLimitedTextSize(@Nonnull FontDrawArgs args) {
        Text text = args.text();
        if (StringUtils.isNullOrEmpty(text.content())) {
            return new KeyValue<>(0, 0);
        }

        String ellipsis = "...";
        FontRenderer font = text.font();
        int ellipsisWidth = font.width(ellipsis);

        float scale = args.fontSize() > 0 ? args.fontSize() / font.lineHeight : 1.0f;

        double drawX = args.x() + args.paddingLeft();
        int availableWidth = args.maxWidth() > 0 ? (int) ((args.maxWidth() - args.paddingLeft() - args.paddingRight()) / scale) : 0;

        if (args.inScreen() && availableWidth > 0 && !args.wrap()) {
            KeyValue<Integer, Integer> screenSize = AbstractGuiUtils.getScreenSize();
            int screenWidth = screenSize.key();

            if (drawX + availableWidth > screenWidth - args.marginRight()) {
                availableWidth = Math.max(0, screenWidth - (int) drawX - args.marginRight());
            }
            if (drawX < args.marginLeft()) {
                availableWidth = Math.max(0, availableWidth - args.marginLeft() + (int) args.x());
            }
        }

        List<String> lines = new ArrayList<>();
        String[] originalLines = StringUtils.replaceLineBreak(text.content()).split("\n");

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
                    outputLines.add(ellipsis);
                    outputLines.addAll(lines.subList(lines.size() - actualMaxLine + 1, lines.size()));
                    break;
                case MIDDLE:
                    int midStart = actualMaxLine / 2;
                    int midEnd = lines.size() - (actualMaxLine - midStart) + 1;
                    outputLines.addAll(lines.subList(0, midStart));
                    outputLines.add(ellipsis);
                    outputLines.addAll(lines.subList(midEnd, lines.size()));
                    break;
                case END:
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
                outputLines.addAll(lines);
            }
        }

        List<String> finalLines = new ArrayList<>();
        for (String line : outputLines) {
            line = ellipsisString(args, ellipsis, font, ellipsisWidth, availableWidth, line);
            finalLines.add(line);
        }

        int maxLineWidth = AbstractGuiUtils.getStringWidth(font, finalLines);
        if (availableWidth > 0) {
            maxLineWidth = Math.min(maxLineWidth, availableWidth);
        }
        float actualLineHeight = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;
        int totalHeight = (int) (finalLines.size() * actualLineHeight);

        int finalWidth = (int) Math.ceil(maxLineWidth * scale) + args.paddingLeft() + args.paddingRight();
        int finalHeight = totalHeight + args.paddingTop() + args.paddingBottom();

        return new KeyValue<>(finalWidth, finalHeight);
    }

    /**
     * 绘制限制长度的文本，超出部分以省略号表示
     */
    public static void drawLimitedText(@Nonnull FontDrawArgs args) {
        Text text = args.text();
        if (StringUtils.isNotNullOrEmpty(text.content())) {
            String ellipsis = "...";
            FontRenderer font = text.font();
            int ellipsisWidth = font.width(ellipsis);

            float scale = args.fontSize() > 0 ? args.fontSize() / font.lineHeight : 1.0f;

            double drawX = args.x() + args.paddingLeft();
            double drawY = args.y() + args.paddingTop();
            int availableWidth = args.maxWidth() > 0 ? (int) ((args.maxWidth() - args.paddingLeft() - args.paddingRight()) / scale) : 0;

            if (args.inScreen() && availableWidth > 0) {
                KeyValue<Integer, Integer> screenSize = AbstractGuiUtils.getScreenSize();
                int screenWidth = screenSize.key();

                if (drawX + availableWidth > screenWidth - args.marginRight()) {
                    availableWidth = Math.max(0, screenWidth - (int) drawX - args.marginRight());
                }
                if (drawX < args.marginLeft()) {
                    drawX = args.marginLeft();
                    availableWidth = Math.max(0, availableWidth - args.marginLeft() + (int) args.x());
                }
            }

            List<String> lines = new ArrayList<>();
            String[] originalLines = StringUtils.replaceLineBreak(text.content()).split("\n");

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
                        outputLines.add(ellipsis);
                        outputLines.addAll(lines.subList(lines.size() - actualMaxLine + 1, lines.size()));
                        break;
                    case MIDDLE:
                        int midStart = actualMaxLine / 2;
                        int midEnd = lines.size() - (actualMaxLine - midStart) + 1;
                        outputLines.addAll(lines.subList(0, midStart));
                        outputLines.add(ellipsis);
                        outputLines.addAll(lines.subList(midEnd, lines.size()));
                        break;
                    case END:
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
                    outputLines.addAll(lines);
                }
            }

            final float actualLineHeight = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;
            float totalHeight = outputLines.size() * actualLineHeight;

            String[] processedLines = new String[outputLines.size()];
            int[] lineWidths = new int[outputLines.size()];
            int maxLineWidth = 0;

            for (int i = 0; i < outputLines.size(); i++) {
                String line = outputLines.get(i);
                line = ellipsisString(args, ellipsis, font, ellipsisWidth, availableWidth, line);
                processedLines[i] = line;
                int width = font.width(line);
                lineWidths[i] = width;
                if (width > maxLineWidth) {
                    maxLineWidth = width;
                }
            }

            if (availableWidth > 0) {
                maxLineWidth = Math.min(maxLineWidth, availableWidth);
            }

            MatrixStack stack = text.stack();
            if (args.bgArgb() != 0) {
                int bgX = (int) args.x();
                int bgY = (int) args.y();
                int bgWidth = (int) (maxLineWidth * scale) + args.paddingLeft() + args.paddingRight();
                int bgHeight = (int) (totalHeight + args.paddingTop() + args.paddingBottom());

                AbstractGuiUtils.drawRoundedRect(stack, bgX, bgY, bgWidth, bgHeight, args.bgArgb(), args.bgBorderRadius());

                if (args.bgBorderThickness() > 0) {
                    int borderArgb = ColorUtils.softenArgb(args.bgArgb());
                    AbstractGuiUtils.drawRoundedRectOutLineRough(stack, bgX, bgY, bgWidth, bgHeight, args.bgBorderThickness(), borderArgb, args.bgBorderRadius());
                }
            }

            boolean needsScale = Math.abs(scale - 1.0f) > 0.001f;
            if (needsScale) {
                stack.pushPose();
                stack.translate(drawX, drawY, 0);
                stack.scale(scale, scale, 1.0f);
                drawX = 0;
                drawY = 0;
            }

            Text textTemplate = text.copyWithoutChildren();
            EnumAlignment alignment = args.align() != null ? args.align() : text.align();
            float alignWidth = availableWidth > 0 ? availableWidth : maxLineWidth;
            boolean hasShadow = text.shadow();
            int textColor = text.colorArgb();
            boolean hasBgColor = !text.bgColorEmpty();
            int bgColor = hasBgColor ? text.bgColorArgb() : 0;

            for (int index = 0; index < processedLines.length; index++) {
                String line = processedLines[index];
                int lineWidth = lineWidths[index];

                float xOffset;
                switch (alignment) {
                    case CENTER:
                        xOffset = (alignWidth - lineWidth) / 2.0f;
                        break;
                    case END:
                        xOffset = alignWidth - lineWidth;
                        break;
                    default:
                        xOffset = 0;
                        break;
                }

                float yPos;
                if (needsScale) {
                    yPos = (float) drawY + index * font.lineHeight;
                } else {
                    yPos = (float) drawY + index * actualLineHeight;
                }

                if (hasBgColor) {
                    if (needsScale) {
                        AbstractGuiUtils.fill(stack, (int) (xOffset), (int) (yPos), lineWidth, font.lineHeight, bgColor);
                    } else {
                        AbstractGuiUtils.fill(stack, (int) (drawX + xOffset), (int) (yPos), lineWidth, font.lineHeight, bgColor);
                    }
                }

                Text lineText = textTemplate.text(line);
                if (hasShadow) {
                    font.drawShadow(stack, lineText.toComponent().toVanilla(Translator.getClientLanguage()), (float) drawX + xOffset, yPos, textColor);
                } else {
                    font.draw(stack, lineText.toComponent().toVanilla(Translator.getClientLanguage()), (float) drawX + xOffset, yPos, textColor);
                }
            }

            if (needsScale) {
                stack.popPose();
            }
        }
    }

    private static List<String> wrapText(FontRenderer font, String text, int maxWidth) {
        List<String> wrappedLines = new ArrayList<>();
        if (maxWidth <= 0 || text == null || text.isEmpty()) {
            if (text != null && !text.isEmpty()) {
                wrappedLines.add(text);
            }
            return wrappedLines;
        }

        String separatorPattern = "[\\s\\p{Punct}]+";
        Pattern pattern = Pattern.compile(separatorPattern);
        List<String> segments = StringUtils.splitStrings(text, pattern);

        if (segments.isEmpty()) {
            return splitLongSegment(font, text, maxWidth);
        }

        StringBuilder currentLine = new StringBuilder();

        for (String segment : segments) {
            boolean isSeparator = pattern.matcher(segment).matches();

            String testLine;
            if (currentLine.length() == 0) {
                testLine = segment;
            } else if (isSeparator) {
                testLine = currentLine + segment;
            } else {
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
                wrappedLines.add(currentLine.toString());

                if (isSeparator) {
                    currentLine = new StringBuilder(segment);
                } else {
                    if (font.width(segment) > maxWidth) {
                        List<String> splitSegments = splitLongSegment(font, segment, maxWidth);
                        if (!splitSegments.isEmpty()) {
                            currentLine = new StringBuilder(splitSegments.get(0));
                            for (int i = 1; i < splitSegments.size(); i++) {
                                wrappedLines.add(splitSegments.get(i));
                            }
                        } else {
                            currentLine = new StringBuilder();
                        }
                    } else {
                        currentLine = new StringBuilder(segment);
                    }
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
        }

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

    private static List<String> splitLongSegment(FontRenderer font, String segment, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (segment == null || segment.isEmpty() || maxWidth <= 0) {
            if (segment != null && !segment.isEmpty()) {
                lines.add(segment);
            }
            return lines;
        }

        int segmentWidth = font.width(segment);
        if (segmentWidth <= maxWidth) {
            lines.add(segment);
            return lines;
        }

        int startIdx = 0;

        while (startIdx < segment.length()) {
            int endIdx = Math.min(startIdx + maxWidth / 4, segment.length());
            String testLine = segment.substring(startIdx, endIdx);
            int testWidth = font.width(testLine);

            if (testWidth <= maxWidth) {
                while (endIdx < segment.length()) {
                    int nextEndIdx = endIdx + 1;
                    String nextTestLine = segment.substring(startIdx, nextEndIdx);
                    int nextWidth = font.width(nextTestLine);
                    if (nextWidth > maxWidth) {
                        break;
                    }
                    testLine = nextTestLine;
                    testWidth = nextWidth;
                    endIdx = nextEndIdx;
                }
                lines.add(testLine);
                startIdx = endIdx;
            } else {
                int left = startIdx;
                int right = endIdx;
                while (left < right) {
                    int mid = (left + right) / 2;
                    String midLine = segment.substring(startIdx, mid);
                    if (font.width(midLine) <= maxWidth) {
                        left = mid + 1;
                    } else {
                        right = mid;
                    }
                }
                if (left <= startIdx + 1) {
                    left = startIdx + 1;
                }
                lines.add(segment.substring(startIdx, left - 1));
                startIdx = left - 1;
            }
        }

        return lines;
    }

    private static String ellipsisString(@Nonnull FontDrawArgs args, String ellipsis, FontRenderer font, int ellipsisWidth, int availableWidth, String line) {
        if (availableWidth <= 0) {
            return line;
        }

        int lineWidth = font.width(line);
        if (lineWidth <= availableWidth) {
            return line;
        }

        if (args.position() == EnumEllipsisPosition.START) {
            int left = 0;
            int right = line.length();
            while (left < right) {
                int mid = (left + right + 1) / 2;
                String testLine = ellipsis + line.substring(mid);
                if (font.width(testLine) <= availableWidth) {
                    right = mid - 1;
                } else {
                    left = mid;
                }
            }
            if (left >= line.length() - 1) {
                left = Math.max(0, line.length() - 1);
            }
            return ellipsis + line.substring(left + 1);
        } else if (args.position() == EnumEllipsisPosition.END) {
            int left = 0;
            int right = line.length();
            while (left < right) {
                int mid = (left + right) / 2;
                String testLine = line.substring(0, mid) + ellipsis;
                if (font.width(testLine) <= availableWidth) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
            if (left <= 1) {
                left = Math.min(line.length(), 1);
            }
            return line.substring(0, left - 1) + ellipsis;
        } else {
            int halfWidth = (availableWidth - ellipsisWidth) / 2;
            if (halfWidth <= 0) {
                return ellipsis;
            }

            int left = 0;
            int right = line.length();
            while (left < right) {
                int mid = (left + right + 1) / 2;
                if (font.width(line.substring(0, mid)) <= halfWidth) {
                    left = mid;
                } else {
                    right = mid - 1;
                }
            }
            String start = line.substring(0, left);

            left = 0;
            right = line.length();
            while (left < right) {
                int mid = (left + right) / 2;
                if (font.width(line.substring(mid)) <= halfWidth) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            String end = line.substring(left);

            if (start.length() + end.length() >= line.length()) {
                return start + ellipsis;
            }

            return start + ellipsis + end;
        }
    }

    public LabelWidget text(String text) {
        this.text = Text.literal(text);
        return this;
    }

    public LabelWidget text(Component text) {
        this.text = Text.from(text);
        return this;
    }

    public LabelWidget text(Text text) {
        this.text = text;
        return this;
    }
}
