package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

/**
 * 基础形状
 */
@Accessors(chain = true, fluent = true)
public abstract class BaseShapeWidget extends BaseWidget {
    @Getter
    @Setter
    protected int bgColor = BaniraColorConfig.winter().bgSurface();

    @Getter
    @Setter
    protected int borderColor = BaniraColorConfig.winter().border();

    @Getter
    @Setter
    protected double borderThickness = 0.0;

    protected BaseShapeWidget(BaniraScreen screen) {
        super(screen);
    }

    protected BaseShapeWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    @Override
    public void update() {
        super.update();
    }

    /**
     * 统一绘制形状方法
     */
    public static void drawShape(ShapeDrawArgs args) {
        if (args == null || args.stack() == null) return;

        switch (args.type()) {
            case RECT:
                drawRectShape(args);
                break;
            case CIRCLE:
                drawCircleShape(args);
                break;
            case ELLIPSE:
                drawEllipseShape(args);
                break;
            case SECTOR:
                drawSectorShape(args);
                break;
            case SECTOR_RING:
                drawSectorRingShape(args);
                break;
            case POLYGON:
                drawPolygonShape(args);
                break;
        }
    }

    private static void drawRectShape(ShapeDrawArgs args) {
        ShapeDrawArgs.RectParams rect = args.rect();
        MatrixStack stack = args.stack();
        int color = args.color();

        if (rect.border() > 0) {
            if (rect.hasRadius()) {
                ShapeDrawArgs.RoundedCornerMode mode = rect.cornerMode();
                AbstractGuiUtils.drawRoundedRectOutLine(stack, rect.x(), rect.y(), rect.width(), rect.height(),
                        rect.topLeft(), rect.topRight(), rect.bottomLeft(), rect.bottomRight(),
                        rect.border(), color, mode);
            } else {
                AbstractGuiUtils.fillOutLine(stack, (int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), (int) rect.border(), color);
            }
        } else {
            if (rect.hasRadius() && rect.isUniformRadius()) {
                ShapeDrawArgs.RoundedCornerMode mode = args.rect().cornerMode();
                if (mode == ShapeDrawArgs.RoundedCornerMode.ROUGH || (mode == ShapeDrawArgs.RoundedCornerMode.AUTO && rect.topLeft() <= 10)) {
                    AbstractGuiUtils.drawRoundedRect(stack, (int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), color, (int) rect.topLeft());
                } else {
                    AbstractGuiUtils.drawRoundedRect(stack, rect.x(), rect.y(), rect.width(), rect.height(), rect.topLeft(), color);
                }
            } else if (rect.hasRadius() && !rect.isUniformRadius()) {
                float maxRadius = Math.max(Math.max(rect.topLeft(), rect.topRight()), Math.max(rect.bottomLeft(), rect.bottomRight()));
                ShapeDrawArgs.RoundedCornerMode mode = args.rect().cornerMode();
                if (mode == ShapeDrawArgs.RoundedCornerMode.ROUGH || (mode == ShapeDrawArgs.RoundedCornerMode.AUTO && maxRadius <= 10)) {
                    AbstractGuiUtils.drawRoundedRect(stack, (int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), color, (int) maxRadius);
                } else {
                    AbstractGuiUtils.drawRoundedRect(stack, rect.x(), rect.y(), rect.width(), rect.height(), rect.topLeft(), rect.topRight(), rect.bottomLeft(), rect.bottomRight(), color);
                }
            } else {
                AbstractGuiUtils.fill(stack, (int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), color);
            }
        }
    }

    private static void drawCircleShape(ShapeDrawArgs args) {
        ShapeDrawArgs.CircleParams circle = args.circle();
        MatrixStack stack = args.stack();
        int color = args.color();

        int segments = circle.segments();
        if (segments <= 0) {
            segments = AbstractGuiUtils.calculateCircleSegments(circle.radius());
        }

        if (circle.border() > 0) {
            AbstractGuiUtils.drawCircleRing(stack, circle.centerX(), circle.centerY(), circle.radius(), circle.border(), segments, color);
        } else {
            AbstractGuiUtils.drawCircle(stack, circle.centerX(), circle.centerY(), circle.radius(), segments, color);
        }
    }

    private static void drawEllipseShape(ShapeDrawArgs args) {
        ShapeDrawArgs.EllipseParams ellipse = args.ellipse();
        MatrixStack stack = args.stack();
        int color = args.color();

        int segments = ellipse.segments();
        if (segments <= 0) {
            float maxRadius = Math.max(ellipse.radiusX(), ellipse.radiusY());
            segments = AbstractGuiUtils.calculateCircleSegments(maxRadius);
        }

        if (ellipse.border() > 0) {
            if (ellipse.rotation() != 0) {
                AbstractGuiUtils.drawEllipseRing(stack, ellipse.centerX(), ellipse.centerY(), ellipse.radiusX(), ellipse.radiusY(), ellipse.rotation(), ellipse.border(), segments, color);
            } else {
                AbstractGuiUtils.drawEllipseRing(stack, ellipse.centerX(), ellipse.centerY(), ellipse.radiusX(), ellipse.radiusY(), ellipse.border(), segments, color);
            }
        } else {
            if (ellipse.rotation() != 0) {
                AbstractGuiUtils.drawEllipseRad(stack, ellipse.centerX(), ellipse.centerY(), ellipse.radiusX(), ellipse.radiusY(), Math.toRadians(ellipse.rotation()), segments, color);
            } else {
                AbstractGuiUtils.drawEllipse(stack, ellipse.centerX(), ellipse.centerY(), ellipse.radiusX(), ellipse.radiusY(), segments, color);
            }
        }
    }

    private static void drawPolygonShape(ShapeDrawArgs args) {
        ShapeDrawArgs.PolygonParams polygon = args.polygon();
        MatrixStack stack = args.stack();
        int color = args.color();

        if (polygon.border() > 0) {
            AbstractGuiUtils.drawPolygonBorder(stack, polygon, color);
        } else {
            AbstractGuiUtils.drawPolygon(stack, polygon.centerX(), polygon.centerY(), polygon.radius(),
                    polygon.sides(), polygon.rotation(), color);
        }
    }

    private static void drawSectorShape(ShapeDrawArgs args) {
        ShapeDrawArgs.SectorParams sector = args.sector();
        MatrixStack stack = args.stack();
        int color = args.color();

        int segments = sector.segments();
        if (segments <= 0) {
            segments = AbstractGuiUtils.calculateCircleSegments(sector.radius());
        }

        if (sector.useRadians()) {
            AbstractGuiUtils.drawSectorRad(stack, sector.centerX(), sector.centerY(), sector.radius(), sector.startAngle(), sector.endAngle(), segments, color);
        } else {
            AbstractGuiUtils.drawSector(stack, sector.centerX(), sector.centerY(), sector.radius(), sector.startAngle(), sector.endAngle(), segments, color);
        }
    }

    private static void drawSectorRingShape(ShapeDrawArgs args) {
        ShapeDrawArgs.SectorRingParams params = args.sectorRing();
        MatrixStack stack = args.stack();
        int color = args.color();

        int segments = params.segments();
        if (segments <= 0) {
            segments = AbstractGuiUtils.calculateCircleSegments(params.outerRadius());
        }

        float actualInnerRadius = params.getActualInnerRadius();

        if (actualInnerRadius > 0) {
            AbstractGuiUtils.drawFilledSectorRing(stack, params, actualInnerRadius, segments, color);
        } else {
            AbstractGuiUtils.drawFilledSectorRingFromCenter(stack, params, segments, color);
        }
    }
}
