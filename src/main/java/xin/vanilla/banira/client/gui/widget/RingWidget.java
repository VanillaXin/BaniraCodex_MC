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
 * 圆环Widget
 */
@Accessors(chain = true, fluent = true)
public class RingWidget extends BaseShapeWidget {
    @Getter
    @Setter
    private float innerRadius = 0;

    @Getter
    @Setter
    private double startAngle = 0;

    @Getter
    @Setter
    private double endAngle = 90;

    @Getter
    @Setter
    private int segments = 0;

    public RingWidget(BaniraScreen screen) {
        super(screen);
    }

    public RingWidget(BaniraScreen screen, ScreenCoordinate bounds) {
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
        double width = width();
        double height = height();
        double centerX = x + width / 2;
        double centerY = y + height / 2;
        float outerRadius = (float) Math.min(width, height) / 2;

        ShapeDrawArgs.SectorRingParams ringParams = new ShapeDrawArgs.SectorRingParams()
                .centerX((float) centerX)
                .centerY((float) centerY)
                .outerRadius(outerRadius)
                .innerRadius(innerRadius)
                .angles(startAngle, endAngle)
                .segments(segments);

        ShapeDrawArgs shapeArgs = new ShapeDrawArgs()
                .stack(stack)
                .type(ShapeDrawArgs.ShapeType.SECTOR_RING)
                .color(bgColor)
                .sectorRing(ringParams);
        BaseShapeWidget.drawShape(shapeArgs);

        renderChildren(stack, partialTicks);
    }
}
