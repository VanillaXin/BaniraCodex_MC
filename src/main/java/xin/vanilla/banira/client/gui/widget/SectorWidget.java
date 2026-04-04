package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;

/**
 * 扇形Widget
 */
@Accessors(chain = true, fluent = true)
public class SectorWidget extends BaseShapeWidget {
    @Getter
    @Setter
    private double startAngle = 0;

    @Getter
    @Setter
    private double endAngle = 90;

    @Getter
    @Setter
    private int segments = 0;

    public SectorWidget(BaniraScreen screen) {
        super(screen);
    }

    public SectorWidget(BaniraScreen screen, ScreenCoordinate bounds) {
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
        double radius = Math.min(width, height) / 2;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distSq = dx * dx + dy * dy;
        if (distSq > radius * radius) {
            return false;
        }
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;
        double s = startAngle % 360;
        double e = endAngle % 360;
        if (s <= e) {
            return angle >= s && angle <= e;
        }
        return angle >= s || angle <= e;
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

        ShapeDrawArgs.SectorParams sectorParams = new ShapeDrawArgs.SectorParams()
                .centerX((float) centerX)
                .centerY((float) centerY)
                .radius(radius)
                .angles(startAngle, endAngle)
                .segments(segments);

        ShapeDrawArgs shapeArgs = new ShapeDrawArgs()
                .stack(stack)
                .type(ShapeDrawArgs.ShapeType.SECTOR)
                .color(bgColor)
                .sector(sectorParams);
        BaseShapeWidget.drawShape(shapeArgs);

        renderChildren(stack, partialTicks);
    }
}
