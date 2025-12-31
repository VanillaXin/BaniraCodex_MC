package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

/**
 * 圆形Widget
 */
@Accessors(chain = true, fluent = true)
public class CircleWidget extends BaseShapeWidget {
    @Getter
    @Setter
    private int segments = 0;

    public CircleWidget(BaniraScreen screen) {
        super(screen);
    }

    public CircleWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        double x = x();
        double y = y();
        double width = renderCoordinate.width();
        double height = renderCoordinate.height();
        double centerX = x + width / 2;
        double centerY = y + height / 2;
        float radius = (float) Math.min(width, height) / 2;

        ShapeDrawArgs.CircleParams circleParams = new ShapeDrawArgs.CircleParams()
                .centerX((float) centerX)
                .centerY((float) centerY)
                .radius(radius)
                .segments(segments)
                .border((float) borderThickness);

        if (borderThickness <= 0) {
            ShapeDrawArgs shapeArgs = new ShapeDrawArgs()
                    .stack(stack)
                    .type(ShapeDrawArgs.ShapeType.CIRCLE)
                    .color(bgColor)
                    .circle(circleParams);
            BaseShapeWidget.drawShape(shapeArgs);
        } else {
            circleParams.border((float) borderThickness);
            ShapeDrawArgs borderArgs = new ShapeDrawArgs()
                    .stack(stack)
                    .type(ShapeDrawArgs.ShapeType.CIRCLE)
                    .color(borderColor)
                    .circle(circleParams);
            BaseShapeWidget.drawShape(borderArgs);
        }

        renderChildren(stack, partialTicks);
    }
}
