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
