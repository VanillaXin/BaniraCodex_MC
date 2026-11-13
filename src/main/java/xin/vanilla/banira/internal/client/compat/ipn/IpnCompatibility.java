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
    private static List<Button> buttons = List.of();
    private static List<ExternalInventoryAction> actions = List.of();

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
        sortButton.getMethod("getTexturePt");
        sortButton.getMethod("getTexture");
        if (!IpnContract.hooksApplied()) return;
        ExternalInventoryButtonManager.get().registerProvider(new ExternalInventoryActionProvider() {
            @Override
            public String sourceId() {
                return SOURCE;
            }

            @Override
            public List<ExternalInventoryAction> actions(Screen screen) {
                return ready && IpnContract.hooksApplied() && screen != null
                        && screen == Minecraft.getInstance().screen && screen == owner ? actions : List.of();
            }
        });
        ready = true;
    }

    public static void update() {
        if (!ready) return;
        Screen screen = Minecraft.getInstance().screen;
        try {
            if (!IpnContract.hooksApplied()) throw new IllegalStateException("Incomplete IPN hooks");
            List<Button> next = new ArrayList<>();
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            int[] remaining = {512};
            if (screen != null) {
                int rootIndex = 0;
                for (Object root : roots(currentWidgets.invoke(handler))) {
                    if (--remaining[0] < 0) throw new IllegalStateException("IPN widget tree exceeds traversal limit");
                    String path = "root" + rootIndex++;
                    if (call(root, "getScreen") == screen) collect(root, next, visited, remaining, 0, path);
                }
            }
            if (owner == screen && next.equals(buttons)) return;
            List<ExternalInventoryAction> replacements = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (Button button : next) {
                String id = button.id.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
                if (id.isEmpty() || !ids.add(id))
                    throw new IllegalStateException("Invalid or duplicate IPN action " + id);
                if (TextureUtils.getTextureImage(button.texture) == null)
                    throw new IllegalStateException("Unavailable IPN texture");
                Texture texture = Texture.of(button.texture).u0(button.x).v0(button.y)
                        .uWidth(button.width).vHeight(button.height);
                if (button.x < 0 || button.y < 0 || button.width <= 0 || button.height <= 0
                        || (long) button.x + button.width > texture.uvWidth()
                        || (long) button.y + button.height > texture.uvHeight())
                    throw new IllegalStateException("Invalid IPN texture bounds");
                replacements.add(new ExternalInventoryAction(id, BaniraComponent.get().literal(button.label),
                        QuickIcon.resource(texture), context -> activate(screen, button)));
            }
            if (Minecraft.getInstance().screen != screen)
                throw new IllegalStateException("IPN screen changed during refresh");
            owner = screen;
            buttons = List.copyOf(next);
            actions = List.copyOf(replacements);
            ExternalInventoryButtonManager.get().refreshCurrentScreen();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            disable(error);
        }
    }

    private static Iterable<?> roots(Object value) {
        // IPN clears its roots outside containers and while a new screen is initializing.
        if (value == null) return Collections.emptyList();
        if (value instanceof Iterable<?> widgets) return widgets;
        throw new IllegalStateException("Unsupported IPN roots");
    }

    private static void collect(Object widget, List<Button> out, Set<Object> visited,
                                int[] remaining, int depth, String path) throws ReflectiveOperationException {
        // Count edges too: repeated references must not bypass the traversal limit.
        if (--remaining[0] < 0 || depth > 64)
            throw new IllegalStateException("IPN widget tree exceeds traversal limit");
        if (widget == null || !visited.add(widget)) return;
        if (!Boolean.TRUE.equals(call(widget, "getVisible")) || !Boolean.TRUE.equals(call(widget, "getActive"))) return;
        if (("settings".equals(path) || "gui_editor".equals(path)) && sortButton.isInstance(widget) && Boolean.FALSE.equals(call(widget, "getUnderManagement"))) {
            Object holder = call(widget, "getTexture");
            Object point = call(widget, "getTexturePt");
            Object id = call(holder, "getId");
            if (!(id instanceof ResourceLocation texture))
                throw new IllegalStateException("Unsupported IPN texture identifier");
            Object label = call(widget, "getTooltipText");
            Object key = call(widget, "getId");
            if (!(key instanceof String actionId) || !(label instanceof String text))
                throw new IllegalStateException("Unsupported IPN action identity");
            // Only the two configuration owners may contribute hosted buttons.
            out.add(new Button(widget, path, text.isEmpty() ? actionId : text, texture,
                    (int) call(point, "getX"), (int) call(point, "getY"),
                    (int) call(widget, "getWidth"), (int) call(widget, "getHeight")));
        }
        Object children = call(widget, "getChildren");
        if (!(children instanceof Iterable<?> entries)) throw new IllegalStateException("Unsupported IPN children");
        String childRole = configurationRole(widget.getClass().getName());
        for (Object child : entries) collect(child, out, visited, remaining, depth + 1, childRole);
    }

    private static String configurationRole(String ownerClass) {
        if ("org.anti_ad.mc.ipnext.gui.inject.base.SettingsWidget".equals(ownerClass)) return "settings";
        if ("org.anti_ad.mc.ipnext.gui.inject.EditorWidget".equals(ownerClass)) return "gui_editor";
        return "";
    }

    private static Object call(Object target, String name) throws ReflectiveOperationException {
        return target.getClass().getMethod(name).invoke(target);
    }

    private static void activate(Screen screen, Button button) {
        if (!ready || screen == null || Minecraft.getInstance().screen != screen || owner != screen) return;
        update();
        if (!buttons.contains(button) || !suppress(button.widget)) return;
        try {
            if (Boolean.TRUE.equals(call(button.widget, "getVisible"))
                    && Boolean.TRUE.equals(call(button.widget, "getActive"))
                    && Boolean.FALSE.equals(call(button.widget, "getUnderManagement")))
                onClick.invoke(button.widget, 0);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            disable(error);
        }
    }

    private static void disable(Throwable error) {
        ready = false;
        owner = null;
        buttons = List.of();
        actions = List.of();
        ExternalInventoryButtonManager.get().refreshCurrentScreen();
        LOGGER.warn("IPN button bridge disabled; native buttons remain active", error);
    }

    public static boolean suppress(Object widget) {
        if (!ready || !IpnContract.hooksApplied() || owner == null || owner != Minecraft.getInstance().screen
                || buttons.stream().noneMatch(button -> button.widget == widget)
                || !ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE)) return false;
        try {
            return Boolean.TRUE.equals(call(widget, "getVisible"))
                    && Boolean.TRUE.equals(call(widget, "getActive"))
                    && Boolean.FALSE.equals(call(widget, "getUnderManagement"));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            disable(error);
            return false;
        }
    }

    private record Button(Object widget, String id, String label, ResourceLocation texture,
                          int x, int y, int width, int height) {
    }
}
