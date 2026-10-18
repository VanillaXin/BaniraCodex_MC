package xin.vanilla.banira.internal.client.ui;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.render.Base;
import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.internal.compat.aui.AuiDocumentAccess;

public final class AuiHudDocument extends UiDomView implements SharedHudHosts.Renderer, MinimapUiRows.Renderer, AuiHudHost.ManualRenderer {
    private final Document document;
    private final AuiDocumentAccess access;
    private final HudDomPresentation presentation;
    private final long generation;
    private final MinimapDomPresentation mapRows;
    private final ProgressDomPresentation progress;
    private xin.vanilla.banira.api.client.ui.UiHudFrame progressFrame;
    private AuiHudDocument(UiHudSpec spec) {
        super(null, null);
        document = Document.create(spec.template());
        if (document == null) throw new IllegalStateException("HUD document unavailable");
        resources.own(() -> { if (!document.isDisposed()) document.remove(); });
        try {
            document.setManuallyRendered(true);
            document.setReloadPersistent(true);
            document.applyViewport(false);
            generation = document.getRefreshGeneration();
            access = new AuiDocumentAccess(document, () -> !closed());
            resources.own(access);
            presentation = new HudDomPresentation(access, access::guiBounds);
            resources.own(presentation);
            mapRows = MinimapUiRows.ID.equals(spec.id()) ? new MinimapDomPresentation(access) : null;
            if (mapRows != null) resources.own(mapRows);
            progress = spec.manual() ? new ProgressDomPresentation(access, access::guiBounds) : null;
            if (progress != null) resources.own(progress);
            attach(access);
        } catch (RuntimeException | LinkageError failure) { close(); throw failure; }
    }
    public static NativeUiView create(UiHudSpec spec) { return new AuiHudDocument(spec); }
    @Override public void hudFrame(xin.vanilla.banira.api.client.ui.UiHudFrame frame) {
        if (!live() || progress == null) throw new IllegalStateException("Manual HUD frame unavailable");
        progressFrame = java.util.Objects.requireNonNull(frame);
    }

    @Override public void renderHud(GuiGraphics graphics) {
        if (progress == null || progressFrame == null || !live() || document.isDisposed() || generation != document.getRefreshGeneration())
            throw new IllegalStateException("Manual HUD document expired");
        document.applyViewport(false);
        boolean painted = false;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            progress.update(progressFrame);
            document.commitPendingStyleRecalcForRender(); document.commitRenderState();
            graphics.flush();
            painted = true;
            try { Base.drawOverlayDocument(graphics.pose(), document); }
            finally { graphics.flush(); }
        } catch (RuntimeException | LinkageError failure) {
            if (painted) throw new AuiHudHost.PaintedFailure(failure);
            throw failure;
        }
    }
    @Override public void render(GuiGraphics graphics, HudDrawList frame) {
        if (!live() || document.isDisposed() || generation != document.getRefreshGeneration()) throw new IllegalStateException("HUD document expired");
        document.applyViewport(false);
        presentation.update(frame);
        access.prepare();
        try {
            Base.drawOverlayDocument(graphics.pose(), document);
            afterLayout();
            access.render(graphics);
            frame.renderContents(graphics, false);
        } catch (RuntimeException | LinkageError failure) { throw new AuiHudHost.PaintedFailure(failure); }
    }

    @Override public void renderRow(GuiGraphics graphics, String key, net.minecraft.network.chat.Component text,
                                    xin.vanilla.banira.client.data.ScreenCoordinate bounds,
                                    int color, boolean shadow, float depth) {
        if (mapRows == null || !live() || document.isDisposed() || generation != document.getRefreshGeneration())
            throw new IllegalStateException("Minimap UI document expired");
        document.applyViewport(false);
        try (Document.ContextScope ignored = Document.withContext(document)) {
            mapRows.select(key, text.getString(), color, shadow);
            document.commitPendingStyleRecalcForRender();
            document.commitRenderState();
            var measured = mapRows.measure();
            var target = MinimapUiRows.fit(measured, bounds);
            if (target == null) throw new IllegalStateException("Minimap UI measurement unavailable");
            double scale = target.width() / measured.width();
            graphics.pose().pushPose();
            try {
                graphics.pose().last().pose().identity();
                graphics.pose().last().normal().identity();
                graphics.pose().translate(target.x() - measured.x() * scale, target.y() - measured.y() * scale, depth);
                graphics.pose().scale((float) scale, (float) scale, 1);
                try { Base.drawOverlayDocument(graphics.pose(), document); }
                catch (RuntimeException | LinkageError failure) { throw new AuiHudHost.PaintedFailure(failure); }
            } finally { graphics.pose().popPose(); }
        }
    }
}
