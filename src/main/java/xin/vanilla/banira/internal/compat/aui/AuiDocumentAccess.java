package xin.vanilla.banira.internal.compat.aui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.apricityui.element.AbstractText;
import com.sighs.apricityui.event.KeyEvent;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.layout.Position;
import com.sighs.apricityui.ui.ColorPicker;
import com.sighs.apricityui.ui.Tooltip;
import net.minecraft.client.Minecraft;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.client.notification.NotificationHudState;
import xin.vanilla.banira.client.notification.NotificationStyleInteractionHelper;
import xin.vanilla.banira.client.notification.NotificationUnreadHud;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.internal.client.ui.UiColorBridge;
import xin.vanilla.banira.internal.client.ui.UiDomAccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final List<AuiNode> modalRoots = new ArrayList<>();
    private final AuiNode body;
    private final Element content;
    private final Element scopeRoot;
    private final AuiTransientTooltip tooltips;
    private final AuiWindowsIme ime = new AuiWindowsIme();
    private final Element transientStylesheet;
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
        this(document, owner, null);
    }

    public AuiDocumentAccess(Document document, java.util.function.BooleanSupplier owner, Element scopeRoot) {
        this.document = document;
        this.owner = Objects.requireNonNull(owner, "owner");
        this.scopeRoot = scopeRoot;
        generation = document.getRefreshGeneration();
        content = document.getElementById("content");
        List<Element> bodies = document.getElementsByTagName("body");
        if (bodies.isEmpty()) throw new IllegalStateException("UI document has no body");
        body = wrap(scopeRoot == null ? bodies.get(0) : scopeRoot);
        count = count(scopeRoot == null ? document.getDocumentElement() : scopeRoot);
        checkBudget();
        linkParents(scopeRoot == null ? document.getDocumentElement() : scopeRoot, null);
        String transientStyles;
        try (lombok.var resource = Minecraft.getInstance().getResourceManager().getResource(
                new net.minecraft.resources.ResourceLocation("banira_codex", "aui/transients.css"));
             lombok.var input = resource.getInputStream()) {
            byte[] css = com.google.common.io.ByteStreams.toByteArray(com.google.common.io.ByteStreams.limit(input, 16385));
            if (css.length > 16384) throw new IllegalStateException("Transient stylesheet budget");
            transientStyles = new String(css, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot load AUI transient styles", failure);
        }
        transientStylesheet = Element.init(document.createElement("style"));
        transientStylesheet.setAttribute("style", "display:none");
        transientStylesheet.setInnerText(transientStyles);
        bodies.get(0).appendChild(transientStylesheet);
        AuiTransientTooltip tooltip = null;
        try {
            tooltip = new AuiTransientTooltip(document, bodies.get(0));
            count += 4;
            checkBudget();
        } catch (RuntimeException | LinkageError failure) {
            if (tooltip != null) tooltip.close();
            transientStylesheet.remove();
            throw failure;
        }
        tooltips = tooltip;
    }

    private void linkParents(com.sighs.apricityui.init.Node node, AuiNode parent) {
        if (node instanceof Element) {
            AuiNode current = wrap((Element) node);
            current.parent = parent;
            parent = current;
        }
        for (com.sighs.apricityui.init.Node child : node.getChildNodes()) linkParents(child, parent);
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

    private void checkBudget() {
        if (count > 2000) throw new IllegalStateException("UI document node budget");
    }

    private AuiNode wrap(Element element) {
        return element == null ? null : nodes.computeIfAbsent(element, AuiNode::new);
    }

    @Override
    public Node find(String id) {
        Element element = scopeRoot != null && (id.equals("overlay") || id.equals("progress"))
                ? scopeRoot : document.getElementById(id);
        if (scopeRoot != null && !nodes.containsKey(element)) return null;
        AuiNode node = wrap(element);
        if (node != null && (id.equals("text") || id.equals("message"))) {
            node.richSlot = true;
            Element detail = document.getElementById("detail");
            node.clip = wrap(detail == null ? content : detail);
        }
        return node;
    }

    @Override
    public void theme(BaniraColorConfig theme) {
        this.theme = theme;
        background = theme.bgSurface();
    }

    @Override
    public void hudPosition(double x, double y) {
        preview = true;
        hudX = x;
        hudY = y;
    }

    @Override
    public Node create(String tag) {
        if (!Arrays.asList("div", "button", "input", "span", "texture").contains(tag))
            throw new IllegalArgumentException("Unsupported UI element");
        if (count >= 1700) throw new IllegalStateException("UI document node budget");
        count++;
        Element element = tag.equals("texture") ? new com.sighs.apricityui.element.Texture(document) : Element.init(document.createElement(tag));
        AuiNode node = wrap(element);
        node.created = true;
        return node;
    }

    @Override
    public Node overlayRoot() {
        return body;
    }

    @Override
    public boolean current() {
        return !closed && !document.isDisposed() && document.getRefreshGeneration() == generation
                && owner.getAsBoolean();
    }

    @Override
    public Node nextFocus(boolean reverse) {
        if (!current()) return null;
        Set<Element> controls = Collections.newSetFromMap(new IdentityHashMap<>());
        controls.addAll(document.getElementsByTagName("button"));
        controls.addAll(document.getElementsByTagName("input"));
        List<Element> order = new ArrayList<>();
        focusOrder(document.getDocumentElement(), controls, order);
        Element target = xin.vanilla.banira.internal.client.ui.UiFocusTraversal.next(order,
                document.getFocusedElement(), this::focusVisible, reverse);
        return wrap(target);
    }

    private void focusOrder(com.sighs.apricityui.init.Node node, Set<Element> controls, List<Element> order) {
        if (node instanceof Element && controls.contains(node)) order.add((Element) node);
        for (com.sighs.apricityui.init.Node child : node.getChildNodes()) focusOrder(child, controls, order);
    }

    private boolean focusVisible(Element element) {
        if (element.isDisabled()) return false;
        Element.DOMRect rect = element.getBoundingClientRect(), viewport = body.element.getBoundingClientRect();
        double left = Math.max(rect.x, viewport.x), top = Math.max(rect.y, viewport.y);
        double right = Math.min(rect.right, viewport.right), bottom = Math.min(rect.bottom, viewport.bottom);
        for (com.sighs.apricityui.init.Node parent = ((com.sighs.apricityui.init.Node) element).getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (!(parent instanceof Element)) continue;
            Element ancestor = (Element) parent;
            if (ancestor.isDisabled()) return false;
            AuiNode known = nodes.get(ancestor);
            if (ancestor == content || (known != null && known.clips)) {
                Element.DOMRect clip = ancestor.getBoundingClientRect();
                left = Math.max(left, clip.x);
                top = Math.max(top, clip.y);
                right = Math.min(right, clip.right);
                bottom = Math.min(bottom, clip.bottom);
            }
        }
        if (!(right > left && bottom > top)) return false;
        com.sighs.apricityui.init.Node hit = document.hitTest(new Position((left + right) / 2, (top + bottom) / 2));
        while (hit != null && hit != element) hit = hit.getParentNode();
        return hit == element;
    }

    @Override
    public AutoCloseable color(Node anchor, String initial, Consumer<String> result) {
        if (scopeRoot == null) Tooltip.hide(document);
        CompletableFuture<Optional<String>> future = ColorPicker.pickIn(document, ((AuiNode) anchor).element, UiColorBridge.toCss(initial));
        try {
            count = count(document.getDocumentElement());
            checkBudget();
        } catch (RuntimeException failure) {
            if (!future.isDone()) ColorPicker.closeActive();
            throw failure;
        }
        future.thenAccept(value -> {
            if (current())
                result.accept(value.map(css -> UiColorBridge.fromCss(css, com.sighs.apricityui.parser.Color::parse)).orElse(null));
        });
        return () -> {
            if (!future.isDone()) ColorPicker.closeActive();
        };
    }

    public void render(PoseStack graphics) {
        if (!current() || ColorPicker.isOpen()) return;
        ScreenCoordinate tooltip = tooltips.visibleBounds(document);
        if (preview && popupLayers == 0 && theme != null) {
            Minecraft mc = Minecraft.getInstance();
            NotificationUnreadHud.draw(graphics, NotificationHudState.position(hudX, mc.getWindow().getGuiScaledWidth(), NotificationUnreadHud.WIDTH),
                    NotificationHudState.position(hudY, mc.getWindow().getGuiScaledHeight(), NotificationUnreadHud.HEIGHT), 100, theme);
        }
        if (popupLayers == 0) for (AuiNode node : richTexts) {
            if (!paintable(node)) continue;
            ScreenCoordinate bounds = node.bounds(), clip = node.clip == null ? contentBounds() : node.clip.bounds();
            AuiGameElements.renderText(graphics, node.rich, bounds, clip);
            xin.vanilla.banira.common.data.KeyValue<Integer, Integer> mouse = InputStateManager.getGuiCursorPos();
            NotificationStyleInteractionHelper.renderHoverTooltip(graphics, mouse.key(), mouse.val(),
                    Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight(),
                    node.rich.styleAt(bounds, clip, mouse.key(), mouse.val()));
        }
        for (AuiNode node : icons) {
            if (!paintable(node)) continue;
            if (popupLayers > 0 && !node.inPopup) continue;
            ScreenCoordinate bounds = node.bounds();
            ScreenCoordinate leading = new ScreenCoordinate(bounds.x() + 2, bounds.y(), Math.min(22, bounds.width() - 2), bounds.height());
            ScreenCoordinate clip = node.clip == null ? body.bounds() : node.clip.bounds();
            for (ScreenCoordinate region : AuiGameElements.uncovered(leading, clip, tooltip))
                AuiGameElements.renderOption(graphics, node.icon, leading, region);
        }
        if (popupLayers == 0) for (AuiNode node : gameElements) {
            if (!paintable(node)) continue;
            ScreenCoordinate bounds = node.bounds(), clip = node.clip == null ? contentBounds() : node.clip.bounds();
            for (ScreenCoordinate region : AuiGameElements.uncovered(bounds, clip, tooltip))
                AuiGameElements.renderElement(graphics, node.gameElement, bounds, region, theme);
        }
    }

    private ScreenCoordinate contentBounds() {
        return content == null ? body.bounds() : wrap(content).bounds();
    }

    private boolean visible(AuiNode node) {
        for (AuiNode ancestor = node; ancestor != null; ancestor = ancestor.parent)
            if (!ancestor.element.isVisible || ancestor.removed) return false;
        return true;
    }

    private boolean paintable(AuiNode node) {
        if (!visible(node)) return false;
        AuiNode activeModal = null;
        for (AuiNode modal : modalRoots)
            if (visible(modal) && !modal.element.isDisabled()) activeModal = modal;
        if (activeModal == null) return true;
        // Retained Minecraft drawings run after HTML; background drawings must not cross a modal.
        for (AuiNode ancestor = node; ancestor != null; ancestor = ancestor.parent)
            if (ancestor == activeModal) return true;
        return false;
    }

    public boolean richLayoutReady() {
        for (AuiNode node : richTexts) {
            boolean hidden = false;
            for (AuiNode ancestor = node; ancestor != null; ancestor = ancestor.parent) {
                if (!ancestor.element.isVisible) {
                    hidden = true;
                    break;
                }
            }
            if (hidden) continue;
            ScreenCoordinate bounds = node.bounds();
            if (bounds.width() <= 0) return false;
            if (!node.rich.preparedFor((int) bounds.width()) || bounds.height() + .5 < node.rich.height()) return false;
            if (node.clip != null && node.clip.element == content) {
                Element.DOMRect text = node.element.getBoundingClientRect(), viewport = content.getBoundingClientRect();
                if (!AuiGameElements.scrollExtentReady(content.scrollHeight, text.bottom - viewport.y + content.getScrollTop()))
                    return false;
            }
        }
        return true;
    }

    public double restoreContentScroll(double value) {
        if (content == null) return 0;
        double maximum = Math.max(0, content.scrollHeight - com.sighs.apricityui.layout.Box.of(content).innerSize().height());
        content.setScrollTop(Math.max(0, Math.min(value, maximum)));
        return content.getTargetScrollTop();
    }

    public void prepare() {
        if (!current()) return;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            prepareCurrent();
        }
    }

    public boolean layoutReady() {
        if (!current()) return false;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            return document.getLayout().prepareRender();
        }
    }

    public void commitLayout() {
        if (!current()) return;
        try (Document.ContextScope ignored = Document.withContext(document)) {
            document.commitPendingStyleRecalcForRender();
            document.commitRenderState();
            document.getLayout().commit();
        }
    }

    private void prepareCurrent() {
        document.applyViewport(false);
        document.commitPendingStyleRecalcForRender();
        document.commitRenderState();
        updateTransientInput();
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
                if (toolbar != null)
                    toolbar.attribute("style", hudY > .5 ? "top:10px;bottom:auto" : "top:auto;bottom:10px");
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

    private void updateTransientInput() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isWindowActive()) {
            tooltips.hide();
            ime.close();
            return;
        }
        Element focused = document.getFocusedElement();
        AuiTransientLayout.ImeArea area = focused instanceof AbstractText && ((AbstractText) focused).canEditText()
                ? AuiTransientLayout.ime(wrap(focused).bounds(), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight()) : null;
        ime.update(mc.getWindow().getWindow(), area);
        if (ColorPicker.isOpen()) {
            tooltips.hide();
            return;
        }
        lombok.var mouse = InputStateManager.getGuiCursorPos();
        Position point = document.guiToDocumentPosition(new Position(mouse.key(), mouse.val()));
        com.sighs.apricityui.init.Node hit = document.hitTest(point);
        while (hit != null) {
            AuiNode node = hit instanceof Element ? nodes.get(hit) : null;
            if (node != null && !AuiTransientLayout.blank(node.tooltipText) && !node.attributes.containsKey("disabled")) {
                if (node.element instanceof AbstractText && node.element == focused) break;
                Element.DOMRect viewport = document.getElementsByTagName("body").get(0).getBoundingClientRect();
                tooltips.update(node.element, node.tooltipText, point, viewport.right - viewport.x, viewport.bottom - viewport.y);
                return;
            }
            hit = hit.getParentNode();
        }
        tooltips.hide();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            ime.close();
        } finally {
            try {
                tooltips.close();
            } finally {
                transientStylesheet.remove();
            }
        }
        for (AuiNode node : new ArrayList<>(nodes.values())) {
            node.disposeTooltip();
            node.disposeRichClick();
        }
        if (scopeRoot == null) Tooltip.hide(document);
        icons.clear();
        richTexts.clear();
        gameElements.clear();
        modalRoots.clear();
        nodes.clear();
    }

    private final class AuiNode implements Node {
        final Element element;
        final Map<String, String> attributes = new HashMap<>();
        AuiNode parent, clip;
        String tooltipText = "";
        DropdownOption icon;
        xin.vanilla.banira.api.client.ui.UiGameElement gameElement;
        AuiGameElements.TextLayout rich;
        AutoCloseable richClick;
        boolean created, removed, clips, richSlot, popupLayer, inPopup;

        AuiNode(Element element) {
            this.element = element;
            clips = element == content;
            if ("true".equals(element.getAttribute("aria-modal"))) modalRoots.add(this);
        }

        @Override
        public void clipChildren() {
            clips = true;
        }

        @Override
        public void text(String text) {
            element.setInnerText(text);
        }

        @Override
        public void component(Component component) {
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

        @Override
        public String value() {
            return element.getValue();
        }

        @Override
        public void value(String value) {
            if (!Objects.equals(element.getValue(), value)) element.setValue(value);
        }

        @Override
        public boolean checked() {
            return element.isChecked();
        }

        @Override
        public void checked(boolean checked) {
            if (element.isChecked() != checked) element.setChecked(checked);
        }

        @Override
        public void attribute(String name, String value) {
            if (name.equals("src") && element instanceof com.sighs.apricityui.element.Texture) {
                if (net.minecraft.resources.ResourceLocation.tryParse(value) == null)
                    throw new IllegalArgumentException("HUD texture identity");
                if (!Objects.equals(attributes.put(name, value), value)) element.setAttribute(name, value);
                return;
            }
            if (!UiDomAccess.supportsAttribute(name)) {
                throw new IllegalArgumentException("Unsupported UI attribute");
            }
            if (Objects.equals(attributes.put(name, value), value)) return;
            element.setAttribute(name, value);
            if (name.equals("class") && value.contains("popup-options")) clips = true;
            if (name.equals("class") && value.equals("popup-layer") && !popupLayer) {
                popupLayer = true;
                popupLayers++;
            }
            if (name.equals("disabled")) {
                disposeTooltip();
                if (scopeRoot == null) Tooltip.hide(document);
            }
        }

        @Override
        public void hudId(String id) {
            if (id == null || !id.matches("[a-z0-9_-]{1,64}")) throw new IllegalArgumentException("HUD part id");
            Element existing = document.getElementById(id);
            if (existing != null && existing != element)
                throw new IllegalArgumentException("HUD part id already owned");
            element.setAttribute("id", id);
        }

        @Override
        public void styleProperty(String name, String value) {
            if (!name.matches("--hud-[a-z-]+")) throw new IllegalArgumentException("Unsupported HUD property");
            element.getStyle().setProperty(name, Objects.requireNonNull(value));
        }

        @Override
        public void removeAttribute(String name) {
            if (attributes.remove(name) != null || element.hasAttribute(name)) element.removeAttribute(name);
        }

        @Override
        public void append(Node node) {
            AuiNode child = (AuiNode) node;
            child.parent = this;
            element.appendChild(child.element);
            child.removed = false;
        }

        @Override
        public void remove() {
            if (removed) return;
            removed = true;
            disposeTooltip();
            disposeRichClick();
            icons.remove(this);
            richTexts.remove(this);
            gameElements.remove(this);
            if (popupLayer) {
                popupLayers--;
                popupLayer = false;
            }
            element.remove();
            nodes.remove(element);
            if (created) {
                count--;
                created = false;
            }
        }

        @Override
        public ScreenCoordinate bounds() {
            Element.DOMRect rect = element.getBoundingClientRect();
            Position start = document.documentToGuiPosition(new Position(rect.x, rect.y));
            Position end = document.documentToGuiPosition(new Position(rect.right, rect.bottom));
            return new ScreenCoordinate(start.x, start.y, end.x - start.x, end.y - start.y);
        }

        @Override
        public double scrollTop() {
            return element.getScrollTop();
        }

        @Override
        public void scrollTop(double value) {
            if (element.getScrollTop() != value) element.setScrollTop(value);
        }

        @Override
        public double clientHeight() {
            return element.getClientHeight();
        }

        @Override
        public double scrollHeight() {
            return element.scrollHeight;
        }

        @Override
        public void scrollBy(double pixels) {
            double maximum = Math.max(0, element.scrollHeight - element.getClientHeight());
            element.setScrollTop(Math.max(0, Math.min(maximum, element.getTargetScrollTop() + pixels)));
        }

        @Override
        public boolean focused() {
            return document.getFocusedElement() == element;
        }

        @Override
        public void focus() {
            if (!focused()) element.focus();
        }

        @Override
        public void blur() {
            if (focused()) element.blur();
        }

        @Override
        public int selectionStart() {
            return element instanceof AbstractText ? ((AbstractText) element).getCursor() : 0;
        }

        @Override
        public int selectionEnd() {
            if (!(element instanceof AbstractText)) return 0;
            AbstractText text = (AbstractText) element;
            return text.getSelectionStart() == text.getCursor() ? text.getSelectionEnd() : text.getSelectionStart();
        }

        @Override
        public void selection(int caret, int anchor) {
            // The vendor range stores its anchor first and caret second.
            if (element instanceof AbstractText) ((AbstractText) element).setSelectionRange(anchor, caret);
        }

        @Override
        public void tooltip(String text) {
            if (Objects.equals(tooltipText, text)) return;
            disposeTooltip();
            tooltipText = text;
        }

        void disposeTooltip() {
            if (tooltips.owns(element)) tooltips.hide();
        }

        void disposeRichClick() {
            if (richClick == null) return;
            try {
                richClick.close();
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot release text listener", failure);
            } finally {
                richClick = null;
            }
        }

        @Override
        public void icon(DropdownOption option) {
            icon = option;
            if (option == null || !option.hasIcon()) {
                icons.remove(this);
                return;
            }
            icons.add(this);
            AuiNode ancestor = parent;
            while (ancestor != null && !ancestor.clips) ancestor = ancestor.parent;
            clip = ancestor;
            inPopup = ancestor != null && ancestor != wrap(content);
            attribute("style", "height:22px;flex-shrink:0;padding-left:22px");
        }

        @Override
        public void gameElement(xin.vanilla.banira.api.client.ui.UiGameElement value) {
            if (value == null) {
                gameElement = null;
                gameElements.remove(this);
                return;
            }
            gameElement = value;
            gameElements.add(this);
            AuiNode ancestor = parent;
            while (ancestor != null && !ancestor.clips) ancestor = ancestor.parent;
            clip = ancestor != null ? ancestor : value.kind() == xin.vanilla.banira.api.client.ui.UiGameElement.Kind.DIAGNOSTIC ? wrap(content) : body;
            if (created && value.kind() != xin.vanilla.banira.api.client.ui.UiGameElement.Kind.DIAGNOSTIC
                    && value.kind() != xin.vanilla.banira.api.client.ui.UiGameElement.Kind.SPRITE
                    && value.kind() != xin.vanilla.banira.api.client.ui.UiGameElement.Kind.ARTWORK
                    && value.kind() != xin.vanilla.banira.api.client.ui.UiGameElement.Kind.PRESET)
                attribute("style", "width:24px;height:24px;flex-shrink:0");
        }

        @Override
        public AutoCloseable on(String name, Consumer<Event> action) {
            Consumer<com.sighs.apricityui.event.Event> listener = event -> {
                if (!current()) return;
                action.accept(new Event() {
                    @Override
                    public int keyCode() {
                        return event instanceof KeyEvent ? ((KeyEvent) event).keyCode : -1;
                    }

                    @Override
                    public int modifiers() {
                        return event instanceof KeyEvent ? ((KeyEvent) event).modifiers : 0;
                    }

                    @Override
                    public int button() {
                        return event instanceof com.sighs.apricityui.event.MouseEvent
                                ? ((com.sighs.apricityui.event.MouseEvent) event).button : -1;
                    }

                    @Override
                    public double x() {
                        return InputStateManager.getGuiCursorPos().key();
                    }

                    @Override
                    public double y() {
                        return InputStateManager.getGuiCursorPos().val();
                    }

                    @Override
                    public double scrollDelta() {
                        return event instanceof com.sighs.apricityui.event.MouseEvent
                                ? ((com.sighs.apricityui.event.MouseEvent) event).scrollDelta : 0;
                    }

                    @Override
                    public void consume() {
                        event.preventDefault();
                        event.stopPropagation();
                    }
                });
            };
            element.addEventListener(name, listener);
            return () -> element.removeEventListener(name, listener, false);
        }
    }
}
