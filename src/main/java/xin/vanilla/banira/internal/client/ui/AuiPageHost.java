package xin.vanilla.banira.internal.client.ui;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.internal.compat.aui.AuiDocumentAccess;

public final class AuiPageHost extends ApricityScreen implements UiPointerFeedback {
    private static final Logger LOGGER = LogManager.getLogger();
    private final DocumentView view;
    private final UiSessionHost sessions;
    private final UiPagePointer pointer;
    private boolean failed;

    private AuiPageHost(UiPageSpec page, DocumentView view, UiSessionHost sessions) {
        super(page.template());
        this.view = view;
        this.sessions = sessions;
        pointer = new UiPagePointer(this, () -> !failed && !view.closed() && sessions.ownsScreen(this), page::theme);
        view.ownPointer(pointer);
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

    @Override
    protected void init() {
        if (view.closed()) {
            sessions.resumeScreen(this);
            return;
        }
        if (view.attached()) {
            sessions.reload();
            return;
        }
        super.init();
        if (getLinkedDocument() == null) throw new IllegalStateException("AUI document was not created");
        view.attach(getLinkedDocument());
        pointer.sync();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        sessions.resize();
    }

    @Override
    public void onClose() {
        if (!view.dismissOverlay()) view.requestClose();
    }

    @Override
    public void removed() {
        try {
            pointer.close();
            sessions.suspendScreen(view);
        } finally {
            super.removed();
        }
    }

    @Override
    public void pointerButton(int button, boolean down) {
        if (down && button == 0) {
            lombok.var position = xin.vanilla.banira.client.util.InputStateManager.getGuiCursorPos();
            view.scrollInput(position.key(), position.val());
        }
        pointer.button(button, down);
    }

    @Override
    public void pointerScroll(double delta) {
        if (delta != 0) {
            lombok.var position = xin.vanilla.banira.client.util.InputStateManager.getGuiCursorPos();
            view.scrollInput(position.key(), position.val());
        }
        pointer.scroll(delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return sessionKey(key, scan, modifiers, true) || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        return sessionKey(key, scan, modifiers, false) || super.keyReleased(key, scan, modifiers);
    }

    private boolean sessionKey(int key, int scan, int modifiers, boolean pressed) {
        if (failed || view.closed() || !sessions.ownsScreen(this) || sessions.page() == null) return false;
        try {
            return sessions.page().session().key(key, scan, modifiers, pressed);
        } catch (RuntimeException | LinkageError failure) {
            failed = true;
            LOGGER.warn("AUI key binding failed; returning to native interface", failure);
            sessions.fallback();
            return true;
        }
    }

    @Override
    public void render(PoseStack graphics, int mouseX, int mouseY, float partialTick) {
        if (failed || view.closed()) return;
        try {
            if (!view.currentDocument()) {
                sessions.reload();
                return;
            }
            if (!view.prepareFrame()) return;
            super.render(graphics, mouseX, mouseY, partialTick);
            view.afterLayout();
            view.access.render(graphics);
            pointer.render(graphics, mouseX, mouseY);
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
        private boolean firstLayoutPrepared;

        DocumentView() {
            super(null, null);
        }

        void ownPointer(UiPagePointer pointer) {
            resources.own(pointer);
        }

        boolean attached() {
            return document != null;
        }

        void attach(Document document) {
            this.document = document;
            generation = document.getRefreshGeneration();
            resources.own(() -> {
                if (!document.isDisposed()) document.remove();
            });
            access = new AuiDocumentAccess(document, screen());
            resources.own(access);
            scrollRestoration(access::richLayoutReady, access::restoreContentScroll);
            super.attach(access);
        }

        boolean currentDocument() {
            return document != null && !document.isDisposed() && document.getRefreshGeneration() == generation;
        }

        boolean prepareFrame() {
            access.prepare();
            if (!firstLayoutPrepared) {
                if (!access.layoutReady()) return false;
                // Realized rows must join the completed initial layout before its first paint.
                afterLayout();
                access.prepare();
                access.commitLayout();
                firstLayoutPrepared = true;
            }
            return true;
        }

        @Override
        public void close() {
            try {
                super.close();
            } finally {
                document = null;
                access = null;
            }
        }
    }
}
