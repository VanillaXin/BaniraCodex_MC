package xin.vanilla.banira.internal.client.ui;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.event.Event;
import com.sighs.apricityui.layout.Position;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class AuiPageHost extends ApricityScreen {
    private static final Logger LOGGER = LogManager.getLogger();
    private final DocumentView view;
    private final UiSessionHost sessions;
    private boolean failed;

    private AuiPageHost(UiPageSpec page, DocumentView view, UiSessionHost sessions) {
        super(page.template());
        this.view = view;
        this.sessions = sessions;
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
        super.init();
        if (getLinkedDocument() == null) throw new IllegalStateException("AUI document was not created");
        view.attach(getLinkedDocument());
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) { sessions.resize(); }

    @Override
    public void onClose() { view.requestClose(); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (failed) return;
        try {
            if (!view.currentDocument()) { sessions.reload(); return; }
            super.render(graphics, mouseX, mouseY, partialTick);
        } catch (RuntimeException | LinkageError failure) {
            failed = true;
            LOGGER.warn("AUI renderer failed; returning to native interface", failure);
            sessions.fallback();
        }
    }

    private static final class DocumentView extends NativeUiView {
        private final Map<String, Element> elements = new LinkedHashMap<>();
        private Document document;
        private long documentGeneration;

        DocumentView() { super(null); }

        void attach(Document document) {
            this.document = document;
            documentGeneration = document.getRefreshGeneration();
            resources.own(() -> { if (!document.isDisposed()) document.remove(); });
            for (String id : texts.keySet()) changed(id);
            for (String id : values.keySet()) changed(id);
            for (String id : enabled.keySet()) changed(id);
            for (String id : actionIds()) {
                Element element = element(id);
                if (element == null) continue;
                Consumer<Event> listener = event -> { if (currentDocument()) activate(id); };
                element.addEventListener("click", listener);
                resources.own(() -> element.removeEventListener("click", listener, false));
            }
        }

        boolean currentDocument() {
            return document != null && !document.isDisposed()
                    && document.getRefreshGeneration() == documentGeneration;
        }

        private Element element(String id) {
            if (document == null) return null;
            if (!elements.containsKey(id)) elements.put(id, document.getElementById(id));
            return elements.get(id);
        }

        @Override
        protected void changed(String id) {
            Element element = element(id);
            if (element == null) return;
            if (texts.containsKey(id)) element.setInnerText(texts.get(id).toVanilla().getString());
            if (enabled.containsKey(id)) {
                if (enabled.get(id)) element.removeAttribute("disabled");
                else element.setAttribute("disabled", "");
            }
            UiValue value = values.get(id);
            if (value != null && value.kind() == UiValue.Kind.TEXT) element.setValue(value.asText());
            if (value != null && value.kind() == UiValue.Kind.DECIMAL) element.setValue(value.asDecimal().toPlainString());
        }

        @Override
        public ScreenCoordinate bounds(String id) {
            Element element = element(id);
            if (element == null) return super.bounds(id);
            Element.DOMRect rect = element.getBoundingClientRect();
            Position start = document.documentToGuiPosition(new Position(rect.x, rect.y));
            Position end = document.documentToGuiPosition(new Position(rect.right, rect.bottom));
            return new ScreenCoordinate(start.x, start.y, end.x - start.x, end.y - start.y);
        }

        @Override
        public void close() {
            try { super.close(); }
            finally { elements.clear(); document = null; }
        }
    }
}
