package xin.vanilla.banira.client.data;

import lombok.Data;
import lombok.experimental.Accessors;
import org.joml.Matrix3x2fStack;

/**
 * 变换绘制参数
 */
@Data
@Accessors(chain = true, fluent = true)
public class TransformDrawArgs {
    private final Matrix3x2fStack stack;
    private double x;
    private double y;
    private double width;
    private double height;
    /**
     * 透明度系数 0-255，255 表示不透明
     */
    private int alpha = 0xFF;
}
