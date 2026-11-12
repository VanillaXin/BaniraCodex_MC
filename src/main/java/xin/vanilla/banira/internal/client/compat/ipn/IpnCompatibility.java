package xin.vanilla.banira.internal.client.compat.ipn;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;
import xin.vanilla.banira.client.util.TextureUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class IpnCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOURCE = "inventoryprofilesnext";
    private static boolean ready;
    private static Object handler;
    private static Method currentWidgets;
    private static Method onClick;
    private static Class<?> sortButton;
    private static Screen owner;
    private static List<Button> buttons = Collections.emptyList();
    private static List<ExternalInventoryAction> actions = Collections.emptyList();

    private IpnCompatibility() {
    }

    public static void init() throws ReflectiveOperationException {
        if (ready) return;
        ClassLoader loader = IpnCompatibility.class.getClassLoader();
        Class<?> handlerType = Class.forName("org.anti_ad.mc.ipnext.gui.inject.ContainerScreenEventHandler", false, loader);
        sortButton = Class.forName("org.anti_ad.mc.ipnext.gui.inject.base.SortButtonWidget", false, loader);
        handler = handlerType.getField("INSTANCE").get(null);
        currentWidgets = handlerType.getMethod("getCurrentWidgets");
        onClick = sortButton.getMethod("onClick", int.class);
        if (onClick.getReturnType() != void.class) throw new NoSuchMethodException("IPN onClick(int):void");
        sortButton.getMethod("getTexturePt");
        sortButton.getMethod("getTexture");
        if (!IpnContract.hooksApplied()) {
            LOGGER.warn("IPN button bridge disabled; native hooks were not fully applied");
            return;
        }
        ExternalInventoryButtonManager.get().registerProvider(new ExternalInventoryActionProvider() {
            @Override
            public String sourceId() {
                return SOURCE;
            }

            @Override
            public List<ExternalInventoryAction> actions(Screen screen) {
                return ready && screen != null && screen == owner ? actions : Collections.emptyList();
            }
        });
        ready = true;
    }

    public static void update() {
        if (!ready) return;
        Screen screen = Minecraft.getInstance().screen;
        try {
            List<Button> next = new ArrayList<>();
            Object roots = currentWidgets.invoke(handler);
            if (!(roots instanceof Iterable<?>)) throw new IllegalStateException("Unsupported IPN widget list");
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            if (screen != null) {
                for (Object root : (Iterable<?>) roots) {
                    if (call(root, "getScreen") == screen) collect(root, root, next, visited);
                }
            }
            if (owner == screen && next.equals(buttons)) return;
            List<ExternalInventoryAction> replacements = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (Button button : next) {
                if (!ids.add(button.id)) throw new IllegalStateException("Duplicate IPN action " + button.id);
                Texture texture = Texture.of(button.texture).u0(button.x).v0(button.y)
                        .uWidth(button.width).vHeight(button.height);
                if (!TextureUtils.isTextureAvailable(button.texture)
                        || texture.uvWidth() <= 0 || texture.uvHeight() <= 0
                        || button.width <= 0 || button.height <= 0 || button.x < 0 || button.y < 0
                        || (long) button.x + button.width > texture.uvWidth()
                        || (long) button.y + button.height > texture.uvHeight()) {
                    throw new IllegalStateException("Unavailable IPN button texture " + button.id);
                }
                replacements.add(new ExternalInventoryAction(button.id, BaniraComponent.get().literal(button.label),
                        QuickIcon.resource(texture), context -> activate(screen, button)));
            }
            owner = screen;
            buttons = Collections.unmodifiableList(new ArrayList<>(next));
            actions = Collections.unmodifiableList(new ArrayList<>(replacements));
            ExternalInventoryButtonManager.get().refreshCurrentScreen();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            disable(error);
        }
    }

    private static void collect(Object root, Object widget, List<Button> out, Set<Object> visited)
            throws ReflectiveOperationException {
        if (widget == null || !visited.add(widget)) return;
        if (visited.size() > 512) throw new IllegalStateException("IPN widget tree exceeds traversal limit");
        if (!Boolean.TRUE.equals(call(widget, "getVisible")) || !Boolean.TRUE.equals(call(widget, "getActive"))) return;
        if (sortButton.isInstance(widget) && !Boolean.TRUE.equals(call(widget, "getUnderManagement"))) {
            Object holder = call(widget, "getTexture");
            Object point = call(widget, "getTexturePt");
            Object id = call(holder, "getId");
            if (!(id instanceof ResourceLocation))
                throw new IllegalStateException("Unsupported IPN texture identifier");
            Object rawId = call(widget, "getId");
            if (!(rawId instanceof String)) throw new IllegalStateException("Unsupported IPN button identifier");
            String key = ((String) rawId).trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
            if (key.isEmpty()) throw new IllegalStateException("Empty IPN button identifier");
            if (call(widget, "getClickEvent") == null)
                throw new IllegalStateException("Missing IPN button action " + key);
            String label = (String) call(widget, "getTooltipText");
            out.add(new Button(root, widget, key, label == null || label.isEmpty() ? key : label,
                    (ResourceLocation) id, (int) call(point, "getX"), (int) call(point, "getY"),
                    (int) call(widget, "getWidth"), (int) call(widget, "getHeight")));
        }
        Object children = call(widget, "getChildren");
        if (!(children instanceof Iterable<?>)) throw new IllegalStateException("Unsupported IPN widget children");
        for (Object child : (Iterable<?>) children) collect(root, child, out, visited);
    }

    private static Object call(Object target, String name) throws ReflectiveOperationException {
        return target.getClass().getMethod(name).invoke(target);
    }

    private static void activate(Screen screen, Button button) {
        if (!ready || screen == null || Minecraft.getInstance().screen != screen || owner != screen) return;
        update();
        if (!ready || Minecraft.getInstance().screen != screen || owner != screen || !buttons.contains(button)
                || !ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE)) return;
        try {
            if (call(button.root, "getScreen") == screen
                    && Boolean.TRUE.equals(call(button.widget, "getVisible"))
                    && Boolean.TRUE.equals(call(button.widget, "getActive"))
                    && !Boolean.TRUE.equals(call(button.widget, "getUnderManagement"))) {
                onClick.invoke(button.widget, 0);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            disable(error);
        }
    }

    private static void disable(Throwable error) {
        ready = false;
        owner = null;
        buttons = Collections.emptyList();
        actions = Collections.emptyList();
        ExternalInventoryButtonManager.get().refreshCurrentScreen();
        LOGGER.warn("IPN button bridge disabled; native buttons remain active", error);
    }

    public static boolean suppress(Object widget) {
        if (!ready || owner == null || owner != Minecraft.getInstance().screen
                || !ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE)) return false;
        for (Button button : buttons) if (button.widget == widget) return true;
        return false;
    }

    private static final class Button {
        private final Object root;
        private final Object widget;
        private final String id;
        private final String label;
        private final ResourceLocation texture;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private Button(Object root, Object widget, String id, String label, ResourceLocation texture,
                       int x, int y, int width, int height) {
            this.root = root;
            this.widget = widget;
            this.id = id;
            this.label = label;
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Button)) return false;
            Button button = (Button) other;
            return root == button.root && widget == button.widget && id.equals(button.id) && label.equals(button.label)
                    && texture.equals(button.texture) && x == button.x && y == button.y
                    && width == button.width && height == button.height;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(root), System.identityHashCode(widget), id, label,
                    texture, x, y, width, height);
        }
    }
}
