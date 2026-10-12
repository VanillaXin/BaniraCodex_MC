package xin.vanilla.banira.internal.forge.compat.minimap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.InfoDisplayManager;
import xaero.hud.minimap.info.codec.InfoDisplayCommonStateCodecs;
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Loaded only by guarded Xaero mixins. */
public final class XaeroNotificationInfo {
    private static final Set<InfoDisplayManager> MANAGERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<ResourceLocation, InfoDisplay<Boolean>> DISPLAYS = new HashMap<>();
    private static boolean listening;

    private XaeroNotificationInfo() {
    }

    private static String nativeId(ResourceLocation id) {
        return id.toString().equals("banira_codex:unread") ? "banira_unread" : "banira_line_" + id;
    }

    private static synchronized InfoDisplay<Boolean> display(MinimapLine line) {
        InfoDisplay<Boolean> existing = DISPLAYS.get(line.id());
        if (existing != null) return existing;
        InfoDisplay<Boolean> result = InfoDisplay.Builder.<Boolean>begin()
                .setId(nativeId(line.id()))
                .setName(line.displayName())
                .setDefaultState(true)
                .setCodec(InfoDisplayCommonStateCodecs.BOOLEAN)
                .setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
                .setCompiler((display, compiler, session, width, pos) -> {
                    if (!Boolean.TRUE.equals(display.getState())) return;
                    for (MinimapLineSnapshot row : NotificationMinimapBridge.rows(EnumNotificationHudHost.XAERO)) {
                        if (!row.id().equals(line.id())) continue;
                        net.minecraft.util.text.ITextComponent text = row.text();
                        if (Minecraft.getInstance().font.width(text) <= width) compiler.addLine(text);
                        else compiler.addWords(text.getString());
                        break;
                    }
                }).build();
        DISPLAYS.put(line.id(), result);
        return result;
    }

    public static void append(Consumer<Object> destination) {
        for (MinimapLine line : NotificationMinimapBridge.registrations()) destination.accept(display(line));
    }

    public static void attach(Object target) {
        InfoDisplayManager manager = (InfoDisplayManager) target;
        synchronized (MANAGERS) {
            MANAGERS.add(manager);
        }
        synchronized (XaeroNotificationInfo.class) {
            if (!listening) {
                listening = true;
                MinimapLineRegistry.shared().onRegistration(line -> Minecraft.getInstance().execute(() -> {
                    ArrayList<InfoDisplayManager> managers;
                    synchronized (MANAGERS) {
                        managers = new ArrayList<>(MANAGERS);
                    }
                    for (InfoDisplayManager current : managers) add(current, line);
                }));
            }
        }
        for (MinimapLine line : NotificationMinimapBridge.registrations()) add(manager, line);
    }

    private static void add(InfoDisplayManager manager, MinimapLine line) {
        if (manager.get(nativeId(line.id())) != null) return;
        manager.add(display(line));
        manager.applyLocalConfig();
    }
}
