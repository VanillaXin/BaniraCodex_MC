package xin.vanilla.banira.internal.compat.aui;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import xin.vanilla.banira.internal.client.ui.UiSharedHudPool;

public final class AuiSharedHudDocuments {
    private static final String STYLE = "position:absolute;left:0;top:0;width:100%;height:100%;pointer-events:none;";
    public static final class SharedDocument {
        private final Document document;
        private final long generation;
        private SharedDocument(Document document) { this.document = document; generation = document.getRefreshGeneration(); }
        public Document document() { return document; }
    }
    public static final class Branch {
        private final Element root;
        private AuiDocumentAccess access;
        private Branch(Element root) { this.root = root; }
        public Element root() { return root; }
        public void access(AuiDocumentAccess access) { this.access = access; }
        private void style(String value) {
            if (access == null) root.setAttribute("style", value);
            else access.overlayRoot().attribute("style", value);
        }
    }
    private static final UiSharedHudPool<SharedDocument, Branch> POOL = new UiSharedHudPool<>(new UiSharedHudPool.Platform<>() {
        public SharedDocument open() {
            Document document = Document.create("banira_codex/hud/overlay.html");
            if (document == null) throw new IllegalStateException("Shared HUD document unavailable");
            try {
                document.setManuallyRendered(true); document.setReloadPersistent(true); document.applyViewport(false);
                return new SharedDocument(document);
            } catch (RuntimeException | LinkageError failure) { document.remove(); throw failure; }
        }
        public boolean current(SharedDocument owner) {
            return !owner.document.isDisposed() && owner.document.getRefreshGeneration() == owner.generation;
        }
        public Branch branch(SharedDocument owner) {
            Element root = Element.init(owner.document.createElement("div"));
            try {
                root.setAttribute("style", STYLE + "display:none;");
                owner.document.getElementsByTagName("body").get(0).appendChild(root);
                return new Branch(root);
            } catch (RuntimeException | LinkageError failure) { root.remove(); throw failure; }
        }
        public void show(SharedDocument owner, Branch branch) { branch.style(STYLE); }
        public void hide(SharedDocument owner, Branch branch) { branch.style(STYLE + "display:none;"); }
        public void remove(SharedDocument owner, Branch branch) {
            try { if (!owner.document.isDisposed()) branch.root.remove(); }
            finally { branch.access = null; }
        }
        public void close(SharedDocument owner) { if (!owner.document.isDisposed()) owner.document.remove(); }
    });

    private AuiSharedHudDocuments() { }
    public static UiSharedHudPool<SharedDocument, Branch>.Lease acquire() { return POOL.acquire(); }
}
