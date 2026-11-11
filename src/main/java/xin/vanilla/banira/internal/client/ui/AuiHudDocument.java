package xin.vanilla.banira.internal.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.loader.ClientLoader;
import com.sighs.apricityui.render.Base;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.internal.compat.aui.AuiDocumentAccess;
import xin.vanilla.banira.internal.compat.aui.AuiSharedHudDocuments;

public final class AuiHudDocument extends UiDomView implements SharedHudHosts.Renderer, MinimapUiRows.Renderer, AuiHudHost.ManualRenderer {
    private final Document document;
    private final UiSharedHudPool<AuiSharedHudDocuments.SharedDocument, AuiSharedHudDocuments.Branch>.Lease shared;
    private final AuiDocumentAccess access;
    private final HudDomPresentation presentation;
    private final long generation;
    private final boolean foreground;
    private final Object resourceSnapshot;
    private final ProgressDomPresentation progress;
    private xin.vanilla.banira.api.client.ui.UiHudFrame progressFrame;

    private AuiHudDocument(UiHudSpec spec) {
        super(null, null);
        foreground = SharedHudHosts.POINTER.equals(spec.id());
        shared = spec.shared() ? AuiSharedHudDocuments.acquire() : null;
        document = shared == null ? Document.create(spec.template()) : shared.document().document();
        if (document == null) throw new IllegalStateException("HUD document unavailable");
        resources.own(shared == null ? () -> {
            if (!document.isDisposed()) document.remove();
        } : shared);
        try {
            document.setManuallyRendered(true);
            document.setReloadPersistent(true);
            document.applyViewport(false);
            generation = document.getRefreshGeneration();
            resourceSnapshot = spec.manual() ? ClientLoader.listFinalStaticResources() : null;
            access = new AuiDocumentAccess(document, () -> !closed(), shared == null ? null : shared.branch().root());
            if (shared != null) shared.branch().access(access);
            resources.own(access);
            presentation = new HudDomPresentation(access, access::guiBounds);
            resources.own(presentation);
            progress = spec.manual() ? new ProgressDomPresentation(access, access::guiBounds) : null;
            if (progress != null) resources.own(progress);
            attach(access);
        } catch (RuntimeException | LinkageError failure) {
            close();
            throw failure;
        }
    }

    public static NativeUiView create(UiHudSpec spec) {
        return new AuiHudDocument(spec);
    }

    @Override
    public boolean currentDocument() {
        // AUI preserves persistent documents but replaces its resource cache on reload.
        return !closed() && !document.isDisposed() && generation == document.getRefreshGeneration()
                && (resourceSnapshot == null || resourceSnapshot == ClientLoader.listFinalStaticResources());
    }

    @Override
    public void hudFrame(xin.vanilla.banira.api.client.ui.UiHudFrame frame) {
        if (!live() || progress == null) throw new IllegalStateException("Manual HUD frame unavailable");
        progressFrame = java.util.Objects.requireNonNull(frame);
    }

    @Override
    public void renderHud(PoseStack graphics) {
        if (progress == null || progressFrame == null || !live() || !currentDocument())
            throw new IllegalStateException("Manual HUD document expired");
        if (shared != null) shared.show();
        document.applyViewport(false);
        boolean painted = false;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            progress.update(progressFrame);
            document.commitPendingStyleRecalcForRender();
            document.commitRenderState();
            xin.vanilla.banira.internal.compat.aui.LegacyUiGraphics.flush();
            painted = true;
            try {
                Base.drawOverlayDocument(graphics, document);
            } finally {
                xin.vanilla.banira.internal.compat.aui.LegacyUiGraphics.flush();
            }
        } catch (RuntimeException | LinkageError failure) {
            if (painted) throw new AuiHudHost.PaintedFailure(failure);
            throw failure;
        }
    }

    @Override
    public void render(PoseStack graphics, HudDrawList frame) {
        if (!live() || document.isDisposed() || generation != document.getRefreshGeneration())
            throw new IllegalStateException("HUD document expired");
        if (shared != null) shared.show();
        document.applyViewport(false);
        presentation.update(frame);
        access.prepare();
        try {
            if (foreground) Base.drawOverlayDocument(graphics, document, Base.getFlatOverlayZ());
            else Base.drawOverlayDocument(graphics, document);
            afterLayout();
            presentation.afterLayout();
            access.render(graphics);
            frame.renderContents(graphics, false);
        } catch (RuntimeException | LinkageError failure) {
            throw new AuiHudHost.PaintedFailure(failure);
        }
    }

    @Deprecated
    @Override
    public void renderRow(PoseStack graphics, String key, net.minecraft.network.chat.Component text,
                          xin.vanilla.banira.client.data.ScreenCoordinate bounds,
                          int color, boolean shadow, float depth) {
        throw new UnsupportedOperationException("Minimap rows use native map rendering");
    }
}
