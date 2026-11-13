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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class IpnCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOURCE = "inventoryprofilesnext";
    private static boolean ready;
    private static Object handler;
    private static Method currentWidgets;
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
        sortButton.getMethod("onClick", int.class);
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
                return ready && screen == owner ? actions : List.of();
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
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            if (screen != null && roots instanceof Iterable<?> widgets) {
                int rootIndex = 0;
                for (Object root : widgets) {
                    String path = "root" + rootIndex++;
                    if (call(root, "getScreen") == screen) collect(root, next, visited, path);
                }
            }
            if (owner == screen && next.equals(buttons)) return;
            List<ExternalInventoryAction> replacements = new ArrayList<>();
            Set<String> ids = new java.util.HashSet<>();
            for (Button button : next) {
                if (!ids.add(button.id)) throw new IllegalStateException("Duplicate IPN action " + button.id);
                Texture texture = Texture.of(button.texture).u0(button.x).v0(button.y)
                        .uWidth(button.width).vHeight(button.height);
                replacements.add(new ExternalInventoryAction(button.id, BaniraComponent.get().literal(button.label),
                        QuickIcon.resource(texture), context -> activate(screen, button)));
            }
            owner = screen;
            buttons = List.copyOf(next);
            actions = List.copyOf(replacements);
            ExternalInventoryButtonManager.get().refreshCurrentScreen();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            ready = false;
            buttons = List.of();
            actions = List.of();
            ExternalInventoryButtonManager.get().refreshCurrentScreen();
            LOGGER.warn("IPN button bridge disabled; native buttons remain active", error);
        }
    }

    private static void collect(Object widget, List<Button> out, Set<Object> visited, String path) throws ReflectiveOperationException {
        if (widget == null || !visited.add(widget)) return;
        if (visited.size() > 512) throw new IllegalStateException("IPN widget tree exceeds traversal limit");
        if (!Boolean.TRUE.equals(call(widget, "getVisible"))) return;
        if (("settings".equals(path) || "gui_editor".equals(path)) && sortButton.isInstance(widget) && Boolean.TRUE.equals(call(widget, "getActive"))
                && !Boolean.TRUE.equals(call(widget, "getUnderManagement"))) {
            Object holder = call(widget, "getTexture");
            Object point = call(widget, "getTexturePt");
            Object id = call(holder, "getId");
            if (!(id instanceof ResourceLocation texture))
                throw new IllegalStateException("Unsupported IPN texture identifier");
            String label = String.valueOf(call(widget, "getTooltipText"));
            String key = String.valueOf(call(widget, "getId"));
            // Only the two configuration owners may contribute hosted buttons.
            out.add(new Button(widget, path, label.isEmpty() ? key : label, texture,
                    (int) call(point, "getX"), (int) call(point, "getY"),
                    (int) call(widget, "getWidth"), (int) call(widget, "getHeight")));
        }
        Object children = call(widget, "getChildren");
        if (children instanceof Iterable<?> entries) {
            String childRole = configurationRole(widget.getClass().getName());
            for (Object child : entries) collect(child, out, visited, childRole);
        }
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
        if (!ready || Minecraft.getInstance().screen != screen || owner != screen || !buttons.contains(button)) return;
        try {
            if (Boolean.TRUE.equals(call(button.widget, "getVisible")) && Boolean.TRUE.equals(call(button.widget, "getActive")))
                sortButton.getMethod("onClick", int.class).invoke(button.widget, 0);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            ready = false;
            ExternalInventoryButtonManager.get().refreshCurrentScreen();
            LOGGER.warn("IPN action failed; native buttons restored", error);
        }
    }

    public static boolean suppress(Object widget) {
        return ready && owner == Minecraft.getInstance().screen
                && buttons.stream().anyMatch(button -> button.widget == widget)
                && ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE);
    }

    private record Button(Object widget, String id, String label, ResourceLocation texture,
                          int x, int y, int width, int height) {
    }
}
