package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;

/**
 * 矩形Widget
 */
@Accessors(chain = true, fluent = true)
public class RectangleWidget extends BaseShapeWidget {
    @Getter
    @Setter
    private float topLeftRadius = 0;

    @Getter
    @Setter
    private float topRightRadius = 0;

    @Getter
    @Setter
    private float bottomLeftRadius = 0;

    @Getter
    @Setter
    private float bottomRightRadius = 0;

    @Getter
    @Setter
    private ShapeDrawArgs.RoundedCornerMode cornerMode = ShapeDrawArgs.RoundedCornerMode.FINE;

    public RectangleWidget(BaniraScreen screen) {
        super(screen);
    }

    public RectangleWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public RectangleWidget radius(float radius) {
        this.topLeftRadius = radius;
        this.topRightRadius = radius;
        this.bottomLeftRadius = radius;
        this.bottomRightRadius = radius;
        return this;
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        ShapeDrawArgs.RectParams rectParams = new ShapeDrawArgs.RectParams()
                .x((float) x())
                .y((float) y())
                .width((float) width())
                .height((float) height())
                .border((float) borderThickness)
                .cornerMode(cornerMode);

        if (topLeftRadius > 0 || topRightRadius > 0 || bottomLeftRadius > 0 || bottomRightRadius > 0) {
            rectParams.radius(topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius);
        }

        if (borderThickness <= 0) {
            ShapeDrawArgs shapeArgs = new ShapeDrawArgs()
                    .stack(stack)
                    .type(ShapeDrawArgs.ShapeType.RECT)
                    .color(bgColor)
                    .rect(rectParams);
            BaseShapeWidget.drawShape(shapeArgs);
        } else {
            rectParams.border((float) borderThickness);
            ShapeDrawArgs borderArgs = new ShapeDrawArgs()
                    .stack(stack)
                    .type(ShapeDrawArgs.ShapeType.RECT)
                    .color(borderColor)
                    .rect(rectParams);
            BaseShapeWidget.drawShape(borderArgs);
        }

        renderChildren(stack, partialTicks);
    }
}
