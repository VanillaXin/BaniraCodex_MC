package xin.vanilla.banira.client.data;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.client.component.Text;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

/**
 * 字体绘制参数
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class FontDrawArgs implements Cloneable {
    /**
     * 消息内容
     */
    private final Text text;
    /**
     * 鼠标坐标X
     */
    private double x;
    /**
     * 鼠标坐标Y
     */
    private double y;
    /**
     * 上外边距
     */
    private int marginTop = 2;
    /**
     * 下外边距
     */
    private int marginBottom = 2;
    /**
     * 左外边距
     */
    private int marginLeft = 2;
    /**
     * 右外边距
     */
    private int marginRight = 2;
    /**
     * 上内边距
     */
    private int paddingTop = 4;
    /**
     * 下内边距
     */
    private int paddingBottom = 4;
    /**
     * 左内边距
     */
    private int paddingLeft = 8;
    /**
     * 右内边距
     */
    private int paddingRight = 8;

    /**
     * 背景颜色
     */
    private int bgArgb = 0x88000000;
    /**
     * 背景圆角半径
     */
    private int bgBorderRadius = 2;
    /**
     * 背景边框厚度
     */
    private int bgBorderThickness = 1;
    /**
     * 背景材质
     */
    private ResourceLocation texture;

    /**
     * 是否限制在屏幕内
     */
    private boolean inScreen = true;

    /**
     * 最大宽度
     */
    private int maxWidth;
    /**
     * 最大行数
     */
    private int maxLine;
    /**
     * 自动换行
     */
    private boolean wrap = true;
    /**
     * 省略号位置
     */
    private EnumEllipsisPosition position = EnumEllipsisPosition.NONE;
    /**
     * 文本对齐方式
     */
    private EnumAlignment align = EnumAlignment.START;
    /**
     * 字体大小（行高），默认为9
     */
    private float fontSize = 9.0f;

    private FontDrawArgs(Text text) {
        this.text = text;
    }

    public FontDrawArgs clone() {
        try {
            return (FontDrawArgs) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public FontDrawArgs margin(int margin) {
        this.marginTop = margin;
        this.marginBottom = margin;
        this.marginLeft = margin;
        this.marginRight = margin;
        return this;
    }

    public FontDrawArgs padding(int padding) {
        this.paddingTop = padding;
        this.paddingBottom = padding;
        this.paddingLeft = padding;
        this.paddingRight = padding;
        return this;
    }

    public int maxWidth() {
        return Math.max(0, Math.min(AbstractGuiUtils.getScreenSize().getKey(), maxWidth));
    }

    public static FontDrawArgs of(Text text) {
        return new FontDrawArgs(text);
    }

    public static FontDrawArgs of(String text) {
        return new FontDrawArgs(Text.literal(text));
    }

    public static FontDrawArgs of(String text, MatrixStack stack) {
        return new FontDrawArgs(Text.literal(text).stack(stack));
    }

    public static FontDrawArgs of(String text, MatrixStack stack, FontRenderer font) {
        return new FontDrawArgs(Text.literal(text).stack(stack).font(font));
    }

}
