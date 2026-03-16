package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;

/**
 * 多边形Widget
 */
@Accessors(chain = true, fluent = true)
public class PolygonWidget extends BaseShapeWidget {
    @Getter
    @Setter
    private int sides = 3;

    @Getter
    @Setter
    private double rotation = 0;

    public PolygonWidget(BaniraScreen screen) {
        super(screen);
    }

    public PolygonWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    @Override
    protected boolean hitTest(double mouseX, double mouseY, double absX, double absY) {
        if (renderCoordinate == null || sides < 3) {
            return false;
        }
        double width = renderCoordinate.width();
        double height = renderCoordinate.height();
        double centerX = absX + width / 2;
        double centerY = absY + height / 2;
        double radius = Math.min(width, height) / 2;
        int n = sides;
        double radRot = Math.toRadians(rotation);
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double a = radRot + i * 2 * Math.PI / n;
            xs[i] = centerX + radius * Math.cos(a);
            ys[i] = centerY - radius * Math.sin(a);
        }
        return pointInPolygon(mouseX, mouseY, xs, ys);
    }

    private static boolean pointInPolygon(double px, double py, double[] xs, double[] ys) {
        int n = xs.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if (((ys[i] > py) != (ys[j] > py))
                    && (px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i]) + xs[i])) {
                inside = !inside;
            }
        }
        return inside;
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        if (sides < 3) {
            sides = 3;
        }

        double x = x();
        double y = y();
        double width = renderCoordinate.width();
        double height = renderCoordinate.height();
        double centerX = x + width / 2;
        double centerY = y + height / 2;
        float radius = (float) Math.min(width, height) / 2;

        ShapeDrawArgs.PolygonParams polygonParams = new ShapeDrawArgs.PolygonParams()
                .centerX((float) centerX)
                .centerY((float) centerY)
                .radius(radius)
                .sides(sides)
                .rotation(rotation)
                .border((float) borderThickness);

        if (borderThickness <= 0) {
            ShapeDrawArgs shapeArgs = new ShapeDrawArgs()
                    .stack(stack)
                    .type(ShapeDrawArgs.ShapeType.POLYGON)
                    .color(bgColor)
                    .polygon(polygonParams);
            BaseShapeWidget.drawShape(shapeArgs);
        } else {
            polygonParams.border((float) borderThickness);
            ShapeDrawArgs borderArgs = new ShapeDrawArgs()
                    .stack(stack)
                    .type(ShapeDrawArgs.ShapeType.POLYGON)
                    .color(borderColor)
                    .polygon(polygonParams);
            BaseShapeWidget.drawShape(borderArgs);
        }

        renderChildren(stack, partialTicks);
    }
}
