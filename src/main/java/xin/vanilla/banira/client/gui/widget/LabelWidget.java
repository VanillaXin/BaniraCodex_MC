package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.Font;
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
import java.util.*;
import java.util.regex.Pattern;

import static xin.vanilla.banira.client.data.BaniraColorToken.TEXT_PRIMARY;

/**
 * 标签Widget
 */
@Accessors(chain = true, fluent = true)
public class LabelWidget extends BaseWidget implements ITextWidget {
    private static final Pattern WRAP_SEPARATOR_PATTERN = Pattern.compile("[\\s\\p{Punct}]+");
    private static final int TEXT_LAYOUT_CACHE_LIMIT = 512;
    private static final Object TEXT_LAYOUT_CACHE_LOCK = new Object();

    /**
     * 静态绘制路径没有组件实例可复用，使用小型 LRU 缓存减少每帧重复换行和测宽。
     */
    private static final Map<TextLayoutCacheKey, TextLayout> TEXT_LAYOUT_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TextLayoutCacheKey, TextLayout> eldest) {
            return size() > TEXT_LAYOUT_CACHE_LIMIT;
        }
    };

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
            text.color(theme.color(TEXT_PRIMARY));
        }
    }

    @Override
    protected boolean needsSelfUpdate() {
        return false;
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        FontDrawArgs args = FontDrawArgs.of(text.stack(stack)).x(x()).y(y()).maxWidth((int) width())
                .wrap(textWrap).position(textEllipsisPosition);
        if (textVerticalAlign == EnumAlignment.CENTER && height() > 0) {
            KeyValue<Integer, Integer> size = calculateCachedTextSize(args);
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
    private boolean isLabelTextTruncated(PoseStack stack) {
        int mw = (int) width();
        if (mw <= 0 || StringUtils.isNullOrEmpty(text.content())) {
            return false;
        }
        FontDrawArgs fitted = FontDrawArgs.of(text.stack(stack)).x(0).y(0).maxWidth(mw)
                .wrap(textWrap).position(textEllipsisPosition);
        FontDrawArgs natural = fitted.clone().maxWidth(20000).position(EnumEllipsisPosition.NONE);
        int wNatural = calculateCachedTextSize(natural).key();
        int wFitted = calculateCachedTextSize(fitted).key();
        return wNatural > wFitted;
    }

    private void maybeDeferTruncationTooltip(PoseStack stack) {
        if (!showFullTextTooltipWhenTruncated || screen == null || !enabled) {
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
        if (!isLabelTextTruncated(stack)) {
            return;
        }
        BaniraColorConfig theme = screen.getEffectiveTheme();
        EnumSeason tipSeason = screen.season();
        boolean useTexture = theme.tooltipUseTexture();
        Font fontForTip = text.font() != null ? text.font() : screen.getFont();
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

    private Font cachedTextSizeFont;
    private String cachedTextSizeContent;
    private int cachedTextSizeMaxWidth;
    private int cachedTextSizeMaxLine;
    private boolean cachedTextSizeWrap;
    private EnumEllipsisPosition cachedTextSizePosition;
    private float cachedTextSizeFontSize;
    private int cachedTextSizePaddingTop;
    private int cachedTextSizePaddingBottom;
    private int cachedTextSizePaddingLeft;
    private int cachedTextSizePaddingRight;
    private boolean cachedTextSizeInScreen;
    private int cachedTextSizeMarginLeft;
    private int cachedTextSizeMarginRight;
    private int cachedTextSizeX;
    private KeyValue<Integer, Integer> cachedTextSize = new KeyValue<>(0, 0);

    private KeyValue<Integer, Integer> calculateCachedTextSize(@Nonnull FontDrawArgs args) {
        Text text = args.text();
        Font font = text.font();
        String content = text.content();
        int maxWidth = args.maxWidth();
        int x = (int) args.x();
        if (isTextSizeCacheMiss(args, font, content, maxWidth, x)) {
            cachedTextSize = calculateLimitedTextSize(args);
            cachedTextSizeFont = font;
            cachedTextSizeContent = content;
            cachedTextSizeMaxWidth = maxWidth;
            cachedTextSizeMaxLine = args.maxLine();
            cachedTextSizeWrap = args.wrap();
            cachedTextSizePosition = args.position();
            cachedTextSizeFontSize = args.fontSize();
            cachedTextSizePaddingTop = args.paddingTop();
            cachedTextSizePaddingBottom = args.paddingBottom();
            cachedTextSizePaddingLeft = args.paddingLeft();
            cachedTextSizePaddingRight = args.paddingRight();
            cachedTextSizeInScreen = args.inScreen();
            cachedTextSizeMarginLeft = args.marginLeft();
            cachedTextSizeMarginRight = args.marginRight();
            cachedTextSizeX = x;
        }
        return cachedTextSize;
    }

    private boolean isTextSizeCacheMiss(FontDrawArgs args, Font font, String content, int maxWidth, int x) {
        return cachedTextSizeFont != font
                || !Objects.equals(cachedTextSizeContent, content)
                || cachedTextSizeMaxWidth != maxWidth
                || cachedTextSizeMaxLine != args.maxLine()
                || cachedTextSizeWrap != args.wrap()
                || cachedTextSizePosition != args.position()
                || Float.compare(cachedTextSizeFontSize, args.fontSize()) != 0
                || cachedTextSizePaddingTop != args.paddingTop()
                || cachedTextSizePaddingBottom != args.paddingBottom()
                || cachedTextSizePaddingLeft != args.paddingLeft()
                || cachedTextSizePaddingRight != args.paddingRight()
                || cachedTextSizeInScreen != args.inScreen()
                || cachedTextSizeMarginLeft != args.marginLeft()
                || cachedTextSizeMarginRight != args.marginRight()
                || cachedTextSizeX != x;
    }

    /**
     * 计算文字绘制后的最终宽高
     */
    public static KeyValue<Integer, Integer> calculateLimitedTextSize(@Nonnull FontDrawArgs args) {
        TextLayout layout = prepareTextLayout(args, false);
        if (layout == null) {
            return new KeyValue<>(0, 0);
        }
        return new KeyValue<>(layout.finalWidth, layout.finalHeight);
    }

    /**
     * 绘制限制长度的文本，超出部分以省略号表示
     */
    public static void drawLimitedText(@Nonnull FontDrawArgs args) {
        Text text = args.text();
        TextLayout layout = prepareTextLayout(args, true);
        if (layout == null) {
            return;
        }

        PoseStack stack = text.stack();
        if (args.bgArgb() != 0) {
            int bgX = (int) args.x();
            int bgY = (int) args.y();

            AbstractGuiUtils.drawRoundedRect(stack, bgX, bgY, layout.finalWidth, layout.finalHeight, args.bgArgb(), args.bgBorderRadius());

            if (args.bgBorderThickness() > 0) {
                int borderArgb = ColorUtils.softenArgb(args.bgArgb());
                AbstractGuiUtils.drawRoundedRectOutLineRough(stack, bgX, bgY, layout.finalWidth, layout.finalHeight,
                        args.bgBorderThickness(), borderArgb, args.bgBorderRadius());
            }
        }

        double drawX = layout.drawX;
        double drawY = layout.drawY;
        boolean needsScale = Math.abs(layout.scale - 1.0f) > 0.001f;
        if (needsScale) {
            stack.pushPose();
            stack.translate(drawX, drawY, 0);
            stack.scale(layout.scale, layout.scale, 1.0f);
            drawX = 0;
            drawY = 0;
        }

        Text textTemplate = text.copyWithoutChildren();
        EnumAlignment alignment = args.align() != null ? args.align() : text.align();
        float alignWidth = layout.availableWidth > 0 ? layout.availableWidth : layout.maxLineWidth;
        boolean hasShadow = text.shadow();
        int textColor = text.colorArgb();
        boolean hasBgColor = !text.bgColorEmpty();
        int bgColor = hasBgColor ? text.bgColorArgb() : 0;

        for (int index = 0; index < layout.lines.length; index++) {
            String line = layout.lines[index];
            int lineWidth = layout.lineWidths[index];

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

            float yPos = needsScale
                    ? (float) drawY + index * layout.font.lineHeight
                    : (float) drawY + index * layout.lineHeight;

            if (hasBgColor) {
                if (needsScale) {
                    AbstractGuiUtils.fill(stack, (int) xOffset, (int) yPos, lineWidth, layout.font.lineHeight, bgColor);
                } else {
                    AbstractGuiUtils.fill(stack, (int) (drawX + xOffset), (int) yPos, lineWidth, layout.font.lineHeight, bgColor);
                }
            }

            Text lineText = textTemplate.text(line);
            // 未换行且未截断时直接保留富文本子样式，搜索命中片段才能正确着色。
            boolean preserveStyledComponent = layout.lines.length == 1
                    && line.equals(text.content());
            net.minecraft.network.chat.Component renderedText = preserveStyledComponent
                    ? text.toComponent().toVanilla(Translator.getClientLanguage())
                    : lineText.toComponent().toVanilla(Translator.getClientLanguage());
            if (hasShadow) {
                layout.font.drawShadow(stack, renderedText, (float) drawX + xOffset, yPos, textColor);
            } else {
                layout.font.draw(stack, renderedText, (float) drawX + xOffset, yPos, textColor);
            }
        }

        if (needsScale) {
            stack.popPose();
        }
    }

    private static TextLayout prepareTextLayout(@Nonnull FontDrawArgs args, boolean forDraw) {
        Text text = args.text();
        String content = text.content();
        if (StringUtils.isNullOrEmpty(content)) {
            return null;
        }

        Font font = text.font();
        float scale = args.fontSize() > 0 ? args.fontSize() / font.lineHeight : 1.0f;
        double drawX = args.x() + args.paddingLeft();
        double drawY = args.y() + args.paddingTop();
        int availableWidth = args.maxWidth() > 0 ? (int) ((args.maxWidth() - args.paddingLeft() - args.paddingRight()) / scale) : 0;

        if (args.inScreen() && availableWidth > 0 && (forDraw || !args.wrap())) {
            int screenWidth = AbstractGuiUtils.getScreenSize().key();

            if (drawX + availableWidth > screenWidth - args.marginRight()) {
                availableWidth = Math.max(0, screenWidth - (int) drawX - args.marginRight());
            }
            if (drawX < args.marginLeft()) {
                if (forDraw) {
                    drawX = args.marginLeft();
                }
                availableWidth = Math.max(0, availableWidth - args.marginLeft() + (int) args.x());
            }
        }

        TextLayoutCacheKey cacheKey = new TextLayoutCacheKey(font, content, scale, drawX, drawY, availableWidth,
                args.maxLine(), args.wrap(), args.position(), args.paddingLeft(), args.paddingRight(),
                args.paddingTop(), args.paddingBottom());
        synchronized (TEXT_LAYOUT_CACHE_LOCK) {
            TextLayout cached = TEXT_LAYOUT_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        List<String> outputLines = collectOutputLines(args, font, content, availableWidth);
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        String[] processedLines = new String[outputLines.size()];
        int[] lineWidths = new int[outputLines.size()];
        int maxLineWidth = 0;

        for (int i = 0; i < outputLines.size(); i++) {
            String line = ellipsisString(args, ellipsis, font, ellipsisWidth, availableWidth, outputLines.get(i));
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

        float actualLineHeight = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;
        float totalHeight = processedLines.length * actualLineHeight;
        int finalWidth = (int) Math.ceil(maxLineWidth * scale) + args.paddingLeft() + args.paddingRight();
        int finalHeight = (int) totalHeight + args.paddingTop() + args.paddingBottom();
        TextLayout layout = new TextLayout(font, scale, drawX, drawY, availableWidth, processedLines, lineWidths,
                maxLineWidth, actualLineHeight, finalWidth, finalHeight);
        synchronized (TEXT_LAYOUT_CACHE_LOCK) {
            TEXT_LAYOUT_CACHE.put(cacheKey, layout);
        }
        return layout;
    }

    private static List<String> collectOutputLines(FontDrawArgs args, Font font, String content, int availableWidth) {
        List<String> lines = new ArrayList<>();
        String[] originalLines = StringUtils.replaceLineBreak(content).split("\n");

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
                    outputLines.add("...");
                    outputLines.addAll(lines.subList(lines.size() - actualMaxLine + 1, lines.size()));
                    break;
                case MIDDLE:
                    int midStart = actualMaxLine / 2;
                    int midEnd = lines.size() - (actualMaxLine - midStart) + 1;
                    outputLines.addAll(lines.subList(0, midStart));
                    outputLines.add("...");
                    outputLines.addAll(lines.subList(midEnd, lines.size()));
                    break;
                case END:
                    outputLines.addAll(lines.subList(0, actualMaxLine - 1));
                    outputLines.add("...");
                    break;
                default:
                    outputLines.addAll(lines);
                    break;
            }
        } else if (actualMaxLine == 1) {
            outputLines.add(lines.get(0));
        } else {
            outputLines.addAll(lines);
        }
        return outputLines;
    }

    private static final class TextLayout {
        private final Font font;
        private final float scale;
        private final double drawX;
        private final double drawY;
        private final int availableWidth;
        private final String[] lines;
        private final int[] lineWidths;
        private final int maxLineWidth;
        private final float lineHeight;
        private final int finalWidth;
        private final int finalHeight;

        private TextLayout(Font font, float scale, double drawX, double drawY, int availableWidth,
                           String[] lines, int[] lineWidths, int maxLineWidth, float lineHeight,
                           int finalWidth, int finalHeight) {
            this.font = font;
            this.scale = scale;
            this.drawX = drawX;
            this.drawY = drawY;
            this.availableWidth = availableWidth;
            this.lines = lines;
            this.lineWidths = lineWidths;
            this.maxLineWidth = maxLineWidth;
            this.lineHeight = lineHeight;
            this.finalWidth = finalWidth;
            this.finalHeight = finalHeight;
        }
    }

    private static final class TextLayoutCacheKey {
        private final Font font;
        private final String content;
        private final float scale;
        private final double drawX;
        private final double drawY;
        private final int availableWidth;
        private final int maxLine;
        private final boolean wrap;
        private final EnumEllipsisPosition position;
        private final int paddingLeft;
        private final int paddingRight;
        private final int paddingTop;
        private final int paddingBottom;

        private TextLayoutCacheKey(Font font, String content, float scale, double drawX, double drawY, int availableWidth,
                                   int maxLine, boolean wrap, EnumEllipsisPosition position,
                                   int paddingLeft, int paddingRight, int paddingTop, int paddingBottom) {
            this.font = font;
            this.content = content;
            this.scale = scale;
            this.drawX = drawX;
            this.drawY = drawY;
            this.availableWidth = availableWidth;
            this.maxLine = maxLine;
            this.wrap = wrap;
            this.position = position;
            this.paddingLeft = paddingLeft;
            this.paddingRight = paddingRight;
            this.paddingTop = paddingTop;
            this.paddingBottom = paddingBottom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TextLayoutCacheKey that)) return false;
            return Float.compare(that.scale, scale) == 0
                    && Double.compare(that.drawX, drawX) == 0
                    && Double.compare(that.drawY, drawY) == 0
                    && availableWidth == that.availableWidth
                    && maxLine == that.maxLine
                    && wrap == that.wrap
                    && paddingLeft == that.paddingLeft
                    && paddingRight == that.paddingRight
                    && paddingTop == that.paddingTop
                    && paddingBottom == that.paddingBottom
                    && font == that.font
                    && Objects.equals(content, that.content)
                    && position == that.position;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(font), content, scale, drawX, drawY, availableWidth,
                    maxLine, wrap, position, paddingLeft, paddingRight, paddingTop, paddingBottom);
        }
    }

    private static List<String> wrapText(Font font, String text, int maxWidth) {
        List<String> wrappedLines = new ArrayList<>();
        if (maxWidth <= 0 || text == null || text.isEmpty()) {
            if (text != null && !text.isEmpty()) {
                wrappedLines.add(text);
            }
            return wrappedLines;
        }

        Pattern pattern = WRAP_SEPARATOR_PATTERN;
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

    private static List<String> splitLongSegment(Font font, String segment, int maxWidth) {
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

    private static String ellipsisString(@Nonnull FontDrawArgs args, String ellipsis, Font font, int ellipsisWidth, int availableWidth, String line) {
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
