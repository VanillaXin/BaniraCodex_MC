package xin.vanilla.banira.internal.client.ui;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.render.Base;
import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.internal.compat.aui.AuiDocumentAccess;

public final class AuiHudDocument extends UiDomView implements SharedHudHosts.Renderer {
    private final Document document;
    private final AuiDocumentAccess access;
    private final HudDomPresentation presentation;
    private final long generation;
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
            attach(access);
        } catch (RuntimeException | LinkageError failure) { close(); throw failure; }
    }
    public static NativeUiView create(UiHudSpec spec) { return new AuiHudDocument(spec); }
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
}
