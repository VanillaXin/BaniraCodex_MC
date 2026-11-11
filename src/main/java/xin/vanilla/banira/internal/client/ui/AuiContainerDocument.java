package xin.vanilla.banira.internal.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.render.Base;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.internal.compat.aui.AuiDocumentAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AuiContainerDocument extends NativeUiView implements UiContainerLayerHost.Renderer {
    private final Document document;
    private final AuiDocumentAccess access;
    private final Map<String, UiDomAccess.Node> nodes = new LinkedHashMap<>();
    private final Map<String, String> styles = new HashMap<>();
    private final Map<String, String> labels = new HashMap<>();
    private String tokens;
    private final long generation;

    private AuiContainerDocument(UiPageSpec page) {
        super(null);
        document = Document.create(page.template());
        if (document == null) throw new IllegalStateException("Container document unavailable");
        resources.own(() -> {
            if (!document.isDisposed()) document.remove();
        });
        try {
            document.setManuallyRendered(true);
            document.setReloadPersistent(true);
            document.applyViewport(false);
            generation = document.getRefreshGeneration();
            if (document.interceptsMouseEvents())
                throw new IllegalStateException("Container appearance must not intercept inventory input");
            access = new AuiDocumentAccess(document, () -> !closed());
            resources.own(access);
        } catch (RuntimeException | LinkageError failure) {
            close();
            throw failure;
        }
    }

    public static NativeUiView create(UiPageSpec page) {
        return new AuiContainerDocument(page);
    }

    private UiDomAccess.Node node(String id) {
        return nodes.computeIfAbsent(id, key -> {
            UiDomAccess.Node found = access.find(key);
            if (found == null) {
                if (!key.matches("slot_[0-9]{1,3}")) throw new IllegalStateException("Unknown container region");
                found = access.create("div");
                found.attribute("class", "slot");
                access.overlayRoot().append(found);
            }
            return found;
        });
    }

    @Override
    protected void changed(String id) {
        if (access == null || !live()) return;
        if (id.equals("theme")) {
            if (theme != null) {
                String next = UiColorBridge.tokens(theme);
                if (!next.equals(tokens)) {
                    tokens = next;
                    access.overlayRoot().attribute("style", next);
                }
            }
            return;
        }
        UiDomAccess.Node node = node(id);
        if (texts.containsKey(id)) {
            String next = texts.get(id).toVanilla().getString();
            if (!next.equals(labels.put(id, next))) node.text(next);
        }
        if (gameElements.containsKey(id)) node.gameElement(gameElements.get(id));
    }

    @Override
    public ScreenCoordinate bounds(String id) {
        UiDomAccess.Node node = nodes.get(id);
        return live() && node != null ? node.bounds() : new ScreenCoordinate(0, 0, 0, 0);
    }

    @Override
    public void render(PoseStack graphics, Map<String, ScreenCoordinate> regions) {
        if (!live() || document.isDisposed() || document.getRefreshGeneration() != generation)
            throw new IllegalStateException("Container document expired");
        document.applyViewport(false);
        try (Document.ContextScope ignored = Document.withContext(document)) {
            for (String id : new ArrayList<>(nodes.keySet()))
                if (!regions.containsKey(id)) {
                    String hidden = "display:none;pointer-events:none";
                    if (!hidden.equals(styles.put(id, hidden))) node(id).attribute("style", hidden);
                    node(id).gameElement(null);
                }
            for (lombok.var entry : regions.entrySet()) {
                String id = entry.getKey();
                ScreenCoordinate b = access.guiBounds(entry.getValue());
                String style = "position:absolute;pointer-events:none;left:" + b.x() + "px;top:" + b.y() + "px;width:" + b.width() + "px;height:" + b.height() + "px;"
                        + (enabled.getOrDefault(id, true) ? "" : "opacity:0.45;");
                if (!style.equals(styles.put(id, style))) node(id).attribute("style", style);
                if (gameElements.containsKey(id)) node(id).gameElement(gameElements.get(id));
            }
            document.commitPendingStyleRecalcForRender();
            document.commitRenderState();
            for (lombok.var entry : regions.entrySet()) {
                ScreenCoordinate actual = bounds(entry.getKey()), expected = entry.getValue();
                if (!near(actual.x(), expected.x()) || !near(actual.y(), expected.y())
                        || !near(actual.width(), expected.width()) || !near(actual.height(), expected.height()))
                    throw new IllegalStateException("Container layout differs from native slot geometry");
            }
            xin.vanilla.banira.internal.compat.aui.LegacyUiGraphics.flush();
            try {
                Base.drawOverlayDocument(graphics, document);
                access.render(graphics);
                xin.vanilla.banira.internal.compat.aui.LegacyUiGraphics.flush();
            } catch (RuntimeException | LinkageError failure) {
                throw new AuiHudHost.PaintedFailure(failure);
            }
        }
    }

    private static boolean near(double actual, double expected) {
        return Double.isFinite(actual) && Math.abs(actual - expected) <= 0.51;
    }
}
