package xin.vanilla.banira.internal.compat.aui;

import com.sighs.apricityui.element.AbstractText;
import com.sighs.apricityui.event.KeyEvent;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.layout.Position;
import com.sighs.apricityui.ui.ColorPicker;
import com.sighs.apricityui.ui.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.internal.client.ui.UiDomAccess;
import xin.vanilla.banira.internal.client.ui.UiColorBridge;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class AuiDocumentAccess implements UiDomAccess, AutoCloseable {
    private final Document document;
    private final Object screen;
    private final long generation;
    private final Map<Element, AuiNode> nodes = new IdentityHashMap<>();
    private final Set<AuiNode> icons = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<AuiNode> richTexts = Collections.newSetFromMap(new IdentityHashMap<>());
    private final AuiNode body;
    private final Element content;
    private int background = 0xfff4f5f7;
    private int popupLayers;
    private boolean closed;
    private int count;

    public AuiDocumentAccess(Document document, Object screen) {
        this.document = document; this.screen = screen;
        generation = document.getRefreshGeneration();
        content = document.getElementById("content");
        List<Element> bodies = document.getElementsByTagName("body");
        if (bodies.isEmpty()) throw new IllegalStateException("UI document has no body");
        body = wrap(bodies.get(0));
        count = count(document.getDocumentElement());
        checkBudget();
    }

    private int count(com.sighs.apricityui.init.Node node) {
        int total = 1;
        for (com.sighs.apricityui.init.Node child : node.getChildNodes()) {
            total += count(child);
            if (total > 2000) throw new IllegalStateException("UI document node budget");
        }
        return total;
    }

    private void checkBudget() { if (count > 2000) throw new IllegalStateException("UI document node budget"); }
    private AuiNode wrap(Element element) { return element == null ? null : nodes.computeIfAbsent(element, AuiNode::new); }

    @Override public Node find(String id) {
        AuiNode node = wrap(document.getElementById(id));
        if (node != null && (id.equals("text") || id.equals("message"))) node.richSlot = true;
        return node;
    }
    @Override public void theme(BaniraColorConfig theme) { background = theme.bgSurface(); }
    @Override public Node create(String tag) {
        if (!Arrays.asList("div", "button", "input", "span").contains(tag)) throw new IllegalArgumentException("Unsupported UI element");
        if (count >= 1700) throw new IllegalStateException("UI document node budget");
        count++;
        AuiNode node = wrap(document.createElement(tag)); node.created = true; return node;
    }
    @Override public Node overlayRoot() { return body; }
    @Override public boolean current() {
        return !closed && !document.isDisposed() && document.getRefreshGeneration() == generation
                && Minecraft.getInstance().screen == screen;
    }
    @Override public AutoCloseable color(Node anchor, String initial, Consumer<String> result) {
        Tooltip.hide(document);
        CompletableFuture<Optional<String>> future = ColorPicker.pickIn(document, ((AuiNode) anchor).element, UiColorBridge.toCss(initial));
        try { count = count(document.getDocumentElement()); checkBudget(); }
        catch (RuntimeException failure) { if (!future.isDone()) ColorPicker.closeActive(); throw failure; }
        future.thenAccept(value -> {
            if (current()) result.accept(value.map(css -> UiColorBridge.fromCss(css, com.sighs.apricityui.parser.Color::parse)).orElse(null));
        });
        return () -> { if (!future.isDone()) ColorPicker.closeActive(); };
    }

    public void render(GuiGraphics graphics) {
        if (!current() || ColorPicker.isOpen()) return;
        if (popupLayers == 0) for (AuiNode node : richTexts) {
            AuiGameElements.renderText(graphics, node.rich, node.bounds(), contentBounds());
        }
        for (AuiNode node : icons) {
            if (popupLayers > 0 && !node.inPopup) continue;
            AuiGameElements.renderOption(graphics, node.icon, node.bounds(), node.clip == null ? null : node.clip.bounds());
        }
    }

    private ScreenCoordinate contentBounds() { return content == null ? body.bounds() : wrap(content).bounds(); }

    public void prepare() {
        if (!current()) return;
        for (AuiNode node : richTexts) {
            ScreenCoordinate bounds = node.bounds();
            if (!node.rich.prepare((int) bounds.width())) continue;
            Position start = document.guiToDocumentPosition(new Position(0, 0));
            Position end = document.guiToDocumentPosition(new Position(0, node.rich.height()));
            node.attribute("style", "color:transparent;min-height:" + Math.max(0, end.y - start.y) + "px");
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        for (AuiNode node : new ArrayList<>(nodes.values())) node.disposeTooltip();
        Tooltip.hide(document);
        icons.clear(); richTexts.clear(); nodes.clear();
    }

    private final class AuiNode implements Node {
        final Element element;
        final Map<String, String> attributes = new HashMap<>();
        AuiNode parent, clip;
        Tooltip.Binding tooltip;
        String tooltipText = "";
        DropdownOption icon;
        AuiGameElements.TextLayout rich;
        boolean created, removed, clips, richSlot, popupLayer, inPopup;

        AuiNode(Element element) { this.element = element; clips = element == content; }
        @Override public void text(String text) { element.setInnerText(text); }
        @Override public void component(Component component) {
            if (!richSlot) return;
            rich = new AuiGameElements.TextLayout(ColorUtils.readableVanillaComponentCopy(component.toVanilla(), background));
            richTexts.add(this);
            attribute("style", "color:transparent");
        }
        @Override public String value() { return element.getValue(); }
        @Override public void value(String value) { if (!Objects.equals(element.getValue(), value)) element.setValue(value); }
        @Override public boolean checked() { return element.isChecked(); }
        @Override public void checked(boolean checked) { if (element.isChecked() != checked) element.setChecked(checked); }
        @Override public void attribute(String name, String value) {
            if (!Arrays.asList("class", "style", "type", "maxlength", "readonly", "disabled", "placeholder", "aria-invalid").contains(name)) {
                throw new IllegalArgumentException("Unsupported UI attribute");
            }
            if (Objects.equals(attributes.put(name, value), value)) return;
            element.setAttribute(name, value);
            if (name.equals("class") && value.contains("popup-options")) clips = true;
            if (name.equals("class") && value.equals("popup-layer") && !popupLayer) { popupLayer = true; popupLayers++; }
            if (name.equals("disabled")) { disposeTooltip(); Tooltip.hide(document); }
        }
        @Override public void removeAttribute(String name) {
            if (attributes.remove(name) != null || element.hasAttribute(name)) element.removeAttribute(name);
            if (name.equals("disabled") && tooltip == null && !tooltipText.isEmpty()) tooltip = Tooltip.bind(element, tooltipText);
        }
        @Override public void append(Node node) {
            AuiNode child = (AuiNode) node; child.parent = this; element.appendChild(child.element); child.removed = false;
        }
        @Override public void remove() {
            if (removed) return;
            removed = true; disposeTooltip(); icons.remove(this); richTexts.remove(this);
            if (popupLayer) { popupLayers--; popupLayer = false; }
            element.remove(); nodes.remove(element);
            if (created) { count--; created = false; }
        }
        @Override public ScreenCoordinate bounds() {
            Element.DOMRect rect = element.getBoundingClientRect();
            Position start = document.documentToGuiPosition(new Position(rect.x, rect.y));
            Position end = document.documentToGuiPosition(new Position(rect.right, rect.bottom));
            return new ScreenCoordinate(start.x, start.y, end.x - start.x, end.y - start.y);
        }
        @Override public double scrollTop() { return element.getScrollTop(); }
        @Override public void scrollTop(double value) { if (element.getScrollTop() != value) element.setScrollTop(value); }
        @Override public double clientHeight() { return element.getClientHeight(); }
        @Override public boolean focused() { return document.getFocusedElement() == element; }
        @Override public void focus() { if (!focused()) element.focus(); }
        @Override public void blur() { if (focused()) element.blur(); }
        @Override public int selectionStart() { return element instanceof AbstractText ? ((AbstractText) element).getCursor() : 0; }
        @Override public int selectionEnd() {
            if (!(element instanceof AbstractText)) return 0;
            AbstractText text = (AbstractText) element;
            return text.getSelectionStart() == text.getCursor() ? text.getSelectionEnd() : text.getSelectionStart();
        }
        @Override public void selection(int caret, int anchor) {
            // The vendor range stores its anchor first and caret second.
            if (element instanceof AbstractText) ((AbstractText) element).setSelectionRange(anchor, caret);
        }
        @Override public void tooltip(String text) {
            if (Objects.equals(tooltipText, text)) return;
            disposeTooltip(); tooltipText = text;
            if (!text.isEmpty() && !attributes.containsKey("disabled")) tooltip = Tooltip.bind(element, text);
        }
        void disposeTooltip() { if (tooltip != null) { tooltip.close(); tooltip = null; } }
        @Override public void icon(DropdownOption option) {
            icon = option;
            if (option == null || !option.hasIcon()) { icons.remove(this); return; }
            icons.add(this);
            AuiNode ancestor = parent;
            while (ancestor != null && !ancestor.clips) ancestor = ancestor.parent;
            clip = ancestor;
            inPopup = ancestor != null && ancestor != wrap(content);
            attribute("style", "height:22px;flex-shrink:0;padding-left:22px");
        }
        @Override public AutoCloseable on(String name, Consumer<Event> action) {
            Consumer<com.sighs.apricityui.event.Event> listener = event -> {
                if (!current()) return;
                action.accept(new Event() {
                    @Override public int keyCode() { return event instanceof KeyEvent ? ((KeyEvent) event).keyCode : -1; }
                    @Override public void consume() { event.preventDefault(); event.stopPropagation(); }
                });
            };
            element.addEventListener(name, listener);
            return () -> element.removeEventListener(name, listener, false);
        }
    }
}
