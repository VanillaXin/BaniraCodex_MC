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
 * 椭圆Widget
 */
@Accessors(chain = true, fluent = true)
public class EllipseWidget extends BaseShapeWidget {
    @Getter
    @Setter
    private double rotation = 0;

    @Getter
    @Setter
    private int segments = 0;

    public EllipseWidget(BaniraScreen screen) {
        super(screen);
    }

    public EllipseWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    @Override
    protected boolean hitTest(double mouseX, double mouseY, double absX, double absY) {
        if (renderCoordinate == null) {
            return false;
        }
        double width = renderCoordinate.width();
        double height = renderCoordinate.height();
        double centerX = absX + width / 2;
        double centerY = absY + height / 2;
        double rx = width / 2;
        double ry = height / 2;
        if (rx <= 0 || ry <= 0) {
            return false;
        }
        double dx = (mouseX - centerX) / rx;
        double dy = (mouseY - centerY) / ry;
        return dx * dx + dy * dy <= 1;
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        ScreenCoordinate coord = renderCoordinate;
        if (coord == null) {
            return;
        }

        double x = x();
        double y = y();
        double width = coord.width();
        double height = coord.height();
        double centerX = x + width / 2;
        double centerY = y + height / 2;
        float radiusX = (float) width / 2;
        float radiusY = (float) height / 2;

        ShapeDrawArgs.EllipseParams ellipseParams = new ShapeDrawArgs.EllipseParams()
                .centerX((float) centerX)
                .centerY((float) centerY)
                .radiusX(radiusX)
                .radiusY(radiusY)
                .rotation(rotation)
                .segments(segments)
                .border((float) borderThickness);

        if (borderThickness <= 0) {
            ShapeDrawArgs shapeArgs = new ShapeDrawArgs()
                    .stack(stack)
                    .type(ShapeDrawArgs.ShapeType.ELLIPSE)
                    .color(bgColor)
                    .ellipse(ellipseParams);
            BaseShapeWidget.drawShape(shapeArgs);
        } else {
            ellipseParams.border((float) borderThickness);
            ShapeDrawArgs borderArgs = new ShapeDrawArgs()
                    .stack(stack)
                    .type(ShapeDrawArgs.ShapeType.ELLIPSE)
                    .color(borderColor)
                    .ellipse(ellipseParams);
            BaseShapeWidget.drawShape(borderArgs);
        }

        renderChildren(stack, partialTicks);
    }
}
