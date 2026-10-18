package xin.vanilla.banira.internal.client.ui;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.internal.compat.aui.AuiDocumentAccess;

public final class AuiPageHost extends ApricityScreen {
    private static final Logger LOGGER = LogManager.getLogger();
    private final DocumentView view;
    private final UiSessionHost sessions;
    private boolean failed;

    private AuiPageHost(UiPageSpec page, DocumentView view, UiSessionHost sessions) {
        super(page.template());
        this.view = view; this.sessions = sessions;
        view.failureHandler(failure -> {
            if (failed) return;
            failed = true;
            LOGGER.warn("AUI input binding failed; returning to native interface", failure);
            sessions.fallback();
        });
        setPauseGame(false);
        setShowDefaultBackground(false);
    }

    public static NativeUiView create(UiPageSpec page, Object previousScreen, UiSessionHost sessions) {
        DocumentView view = new DocumentView();
        view.screen(new AuiPageHost(page, view, sessions));
        return view;
    }

    @Override protected void init() {
        if (view.closed()) { sessions.resumeScreen(this); return; }
        super.init();
        if (getLinkedDocument() == null) throw new IllegalStateException("AUI document was not created");
        view.attach(getLinkedDocument());
    }

    @Override public void resize(Minecraft minecraft, int width, int height) { sessions.resize(); }
    @Override public void onClose() { if (!view.dismissOverlay()) view.requestClose(); }
    @Override public void removed() { sessions.suspendScreen(view); super.removed(); }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        return sessionKey(key, scan, modifiers, true) || super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        return sessionKey(key, scan, modifiers, false) || super.keyReleased(key, scan, modifiers);
    }
    private boolean sessionKey(int key, int scan, int modifiers, boolean pressed) {
        if (failed || view.closed() || !sessions.ownsScreen(this) || sessions.page() == null) return false;
        try { return sessions.page().session().key(key, scan, modifiers, pressed); }
        catch (RuntimeException | LinkageError failure) {
            failed = true;
            LOGGER.warn("AUI key binding failed; returning to native interface", failure);
            sessions.fallback(); return true;
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (failed || view.closed()) return;
        try {
            if (!view.currentDocument()) { sessions.reload(); return; }
            view.access.prepare();
            super.render(graphics, mouseX, mouseY, partialTick);
            view.afterLayout();
            view.access.render(graphics);
        } catch (RuntimeException | LinkageError failure) {
            failed = true;
            LOGGER.warn("AUI renderer failed; returning to native interface", failure);
            sessions.fallback();
        }
    }

    private static final class DocumentView extends UiDomView {
        private Document document;
        private long generation;
        private AuiDocumentAccess access;

        DocumentView() { super(null, null); }
        void attach(Document document) {
            this.document = document;
            generation = document.getRefreshGeneration();
            resources.own(() -> { if (!document.isDisposed()) document.remove(); });
            access = new AuiDocumentAccess(document, screen());
            resources.own(access);
            super.attach(access);
        }
        boolean currentDocument() {
            return document != null && !document.isDisposed() && document.getRefreshGeneration() == generation;
        }
        @Override public void close() { try { super.close(); } finally { document = null; access = null; } }
    }
}
