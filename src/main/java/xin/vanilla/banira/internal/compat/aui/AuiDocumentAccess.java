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
import xin.vanilla.banira.client.notification.NotificationHudState;
import xin.vanilla.banira.client.notification.NotificationUnreadHud;
import xin.vanilla.banira.client.notification.NotificationStyleInteractionHelper;
import xin.vanilla.banira.internal.client.InputStateManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class AuiDocumentAccess implements UiDomAccess, AutoCloseable {
    private final Document document;
    private final java.util.function.BooleanSupplier owner;
    private final long generation;
    private final Map<Element, AuiNode> nodes = new IdentityHashMap<>();
    private final Set<AuiNode> icons = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<AuiNode> richTexts = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<AuiNode> gameElements = Collections.newSetFromMap(new IdentityHashMap<>());
    private final AuiNode body;
    private final Element content;
    private int background = 0xfff4f5f7;
    private BaniraColorConfig theme;
    private boolean preview;
    private double hudX, hudY;
    private int popupLayers;
    private boolean closed;
    private int count;

    public AuiDocumentAccess(Document document, Object screen) {
        this(document, () -> Minecraft.getInstance().screen == screen);
    }

    public AuiDocumentAccess(Document document, java.util.function.BooleanSupplier owner) {
        this.document = document; this.owner = Objects.requireNonNull(owner, "owner");
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

    public ScreenCoordinate guiBounds(ScreenCoordinate bounds) {
        Position start = document.guiToDocumentPosition(new Position(bounds.x(), bounds.y()));
        Position end = document.guiToDocumentPosition(new Position(bounds.x() + bounds.width(), bounds.y() + bounds.height()));
        return new ScreenCoordinate(start.x, start.y, end.x - start.x, end.y - start.y);
    }

    private void checkBudget() { if (count > 2000) throw new IllegalStateException("UI document node budget"); }
    private AuiNode wrap(Element element) { return element == null ? null : nodes.computeIfAbsent(element, AuiNode::new); }

    @Override public Node find(String id) {
        AuiNode node = wrap(document.getElementById(id));
        if (node != null && (id.equals("text") || id.equals("message"))) {
            node.richSlot = true;
            Element detail = document.getElementById("detail");
            node.clip = wrap(detail == null ? content : detail);
        }
        return node;
    }
    @Override public void theme(BaniraColorConfig theme) { this.theme = theme; background = theme.bgSurface(); }
    @Override public void hudPosition(double x, double y) { preview = true; hudX = x; hudY = y; }
    @Override public Node create(String tag) {
        if (!Arrays.asList("div", "button", "input", "span", "texture").contains(tag)) throw new IllegalArgumentException("Unsupported UI element");
        if (count >= 1700) throw new IllegalStateException("UI document node budget");
        count++;
        Element element = tag.equals("texture") ? new com.sighs.apricityui.element.Texture(document) : document.createElement(tag);
        AuiNode node = wrap(element); node.created = true; return node;
    }
    @Override public Node overlayRoot() { return body; }
    @Override public boolean current() {
        return !closed && !document.isDisposed() && document.getRefreshGeneration() == generation
                && owner.getAsBoolean();
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
        if (preview && popupLayers == 0 && theme != null) {
            Minecraft mc = Minecraft.getInstance();
            NotificationUnreadHud.draw(graphics, NotificationHudState.position(hudX, mc.getWindow().getGuiScaledWidth(), NotificationUnreadHud.WIDTH),
                    NotificationHudState.position(hudY, mc.getWindow().getGuiScaledHeight(), NotificationUnreadHud.HEIGHT), 100, theme);
        }
        if (popupLayers == 0) for (AuiNode node : richTexts) {
            ScreenCoordinate bounds = node.bounds(), clip = node.clip == null ? contentBounds() : node.clip.bounds();
            AuiGameElements.renderText(graphics, node.rich, bounds, clip);
            xin.vanilla.banira.common.data.KeyValue<Integer, Integer> mouse = InputStateManager.getGuiCursorPos();
            NotificationStyleInteractionHelper.renderHoverTooltip(graphics, mouse.key(), mouse.val(),
                    Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight(),
                    node.rich.styleAt(bounds, clip, mouse.key(), mouse.val()));
        }
        for (AuiNode node : icons) {
            if (popupLayers > 0 && !node.inPopup) continue;
            AuiGameElements.renderOption(graphics, node.icon, node.bounds(), node.clip == null ? null : node.clip.bounds());
        }
        if (popupLayers == 0) for (AuiNode node : gameElements) {
            AuiGameElements.renderElement(graphics, node.gameElement, node.bounds(), node.clip == null ? contentBounds() : node.clip.bounds(), theme);
        }
    }

    private ScreenCoordinate contentBounds() { return content == null ? body.bounds() : wrap(content).bounds(); }

    public void prepare() {
        if (!current()) return;
        if (preview) {
            AuiNode node = wrap(document.getElementById("preview"));
            if (node != null) {
                Minecraft mc = Minecraft.getInstance();
                int x = NotificationHudState.position(hudX, mc.getWindow().getGuiScaledWidth(), NotificationUnreadHud.WIDTH);
                int y = NotificationHudState.position(hudY, mc.getWindow().getGuiScaledHeight(), NotificationUnreadHud.HEIGHT);
                Position start = document.guiToDocumentPosition(new Position(x, y));
                Position end = document.guiToDocumentPosition(new Position(x + NotificationUnreadHud.WIDTH, y + NotificationUnreadHud.HEIGHT));
                node.attribute("style", "position:fixed;background:transparent;border:0;padding:0;left:" + start.x + "px;top:" + start.y
                        + "px;width:" + (end.x - start.x) + "px;height:" + (end.y - start.y) + "px");
                AuiNode toolbar = wrap(document.getElementById("toolbar"));
                if (toolbar != null) toolbar.attribute("style", hudY > .5 ? "top:10px;bottom:auto" : "top:auto;bottom:10px");
            }
        }
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
        for (AuiNode node : new ArrayList<>(nodes.values())) { node.disposeTooltip(); node.disposeRichClick(); }
        Tooltip.hide(document);
        icons.clear(); richTexts.clear(); gameElements.clear(); nodes.clear();
    }

    private final class AuiNode implements Node {
        final Element element;
        final Map<String, String> attributes = new HashMap<>();
        AuiNode parent, clip;
        Tooltip.Binding tooltip;
        String tooltipText = "";
        DropdownOption icon;
        xin.vanilla.banira.api.client.ui.UiGameElement gameElement;
        AuiGameElements.TextLayout rich;
        AutoCloseable richClick;
        boolean created, removed, clips, richSlot, popupLayer, inPopup;

        AuiNode(Element element) { this.element = element; clips = element == content; }
        @Override public void text(String text) { element.setInnerText(text); }
        @Override public void component(Component component) {
            if (!richSlot) return;
            rich = new AuiGameElements.TextLayout(ColorUtils.readableVanillaComponentCopy(component.toVanilla(), background));
            richTexts.add(this);
            attribute("style", "color:transparent");
            if (richClick == null) richClick = on("click", event -> {
                if (popupLayers > 0 || ColorPicker.isOpen() || event.button() != 0) return;
                net.minecraft.network.chat.Style style = rich.styleAt(bounds(), clip == null ? contentBounds() : clip.bounds(), event.x(), event.y());
                if (NotificationStyleInteractionHelper.tryClickStyle(Minecraft.getInstance(), style)) event.consume();
            });
        }
        @Override public String value() { return element.getValue(); }
        @Override public void value(String value) { if (!Objects.equals(element.getValue(), value)) element.setValue(value); }
        @Override public boolean checked() { return element.isChecked(); }
        @Override public void checked(boolean checked) { if (element.isChecked() != checked) element.setChecked(checked); }
        @Override public void attribute(String name, String value) {
            if (name.equals("src") && element instanceof com.sighs.apricityui.element.Texture) {
                if (net.minecraft.resources.ResourceLocation.tryParse(value) == null) throw new IllegalArgumentException("HUD texture identity");
                if (!Objects.equals(attributes.put(name, value), value)) element.setAttribute(name, value);
                return;
            }
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
            removed = true; disposeTooltip(); disposeRichClick(); icons.remove(this); richTexts.remove(this); gameElements.remove(this);
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
        void disposeRichClick() {
            if (richClick == null) return;
            try { richClick.close(); }
            catch (Exception failure) { throw new IllegalStateException("Cannot release text listener", failure); }
            finally { richClick = null; }
        }
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
        @Override public void gameElement(xin.vanilla.banira.api.client.ui.UiGameElement value) {
            if (value == null) { gameElement = null; gameElements.remove(this); return; }
            gameElement = value; gameElements.add(this);
            AuiNode ancestor = parent;
            while (ancestor != null && !ancestor.clips) ancestor = ancestor.parent;
            clip = ancestor != null ? ancestor : value.kind() == xin.vanilla.banira.api.client.ui.UiGameElement.Kind.DIAGNOSTIC ? wrap(content) : body;
            if (value.kind() != xin.vanilla.banira.api.client.ui.UiGameElement.Kind.DIAGNOSTIC) attribute("style", "width:24px;height:20px;flex-shrink:0");
        }
        @Override public AutoCloseable on(String name, Consumer<Event> action) {
            Consumer<com.sighs.apricityui.event.Event> listener = event -> {
                if (!current()) return;
                action.accept(new Event() {
                    @Override public int keyCode() { return event instanceof KeyEvent ? ((KeyEvent) event).keyCode : -1; }
                    @Override public int button() { return event instanceof com.sighs.apricityui.event.MouseEvent
                            ? ((com.sighs.apricityui.event.MouseEvent) event).button : -1; }
                    @Override public double x() { return InputStateManager.getGuiCursorPos().key(); }
                    @Override public double y() { return InputStateManager.getGuiCursorPos().val(); }
                    @Override public void consume() { event.preventDefault(); event.stopPropagation(); }
                });
            };
            element.addEventListener(name, listener);
            return () -> element.removeEventListener(name, listener, false);
        }
    }
}
