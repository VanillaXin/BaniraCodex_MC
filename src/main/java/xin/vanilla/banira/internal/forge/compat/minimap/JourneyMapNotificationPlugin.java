package xin.vanilla.banira.internal.forge.compat.minimap;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.InfoSlotDisplayEvent;
import journeymap.api.v2.client.event.RegistryEvent.InfoSlotRegistryEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import journeymap.api.v2.common.event.MinimapEventRegistry;
import journeymap.client.event.handlers.ScreenEventHandler;
import journeymap.client.ui.UIManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineLayout;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapNotificationPlugin implements IClientPlugin {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();
    private static final java.lang.reflect.Method NATIVE_SCREEN_RULE = nativeScreenRule();
    private boolean nativeScreenRuleFailed;
    private InfoSlotRegistryEvent registry;
    private final java.util.Set<net.minecraft.resources.ResourceLocation> registered = new java.util.HashSet<>();
    private boolean previousAllowed;
    private EnumNotificationHudHost previousAutomaticOwner;
    private long previousRevision = -1;
    private java.util.List<net.minecraft.resources.ResourceLocation> previousRequested = java.util.Collections.emptyList();

    @Override
    public String getModId() {
        return "banira_codex";
    }

    private static String key(MinimapLine line) {
        return line.displayName().getContents() instanceof TranslatableContents
                ? ((TranslatableContents) line.displayName().getContents()).getKey()
                : line.displayName().getString();
    }

    @Override
    public void initialize(IClientAPI api) {
        NotificationMinimapBridge.registrations();
        ClientEventRegistry.INFO_SLOT_REGISTRY_EVENT.subscribe(getModId(), event -> {
            registry = event;
            registered.clear();
            for (MinimapLine line : NotificationMinimapBridge.registrations()) register(line);
        });
        MinimapLineRegistry.shared().onRegistration(line -> Minecraft.getInstance().execute(() -> register(line)));
        MinimapEventRegistry.INFO_SLOT_DISPLAY_EVENT.subscribe(getModId(), event ->
                appendDisplaySlots(NotificationMinimapBridge.registrations(),
                        key -> event.addLast(key, InfoSlotDisplayEvent.Position.Bottom)));
        BaniraClientEvents.Client.onClientTick(event -> refreshLayout());
    }

    private static void appendDisplaySlots(java.util.List<MinimapLine> lines,
                                           java.util.function.Consumer<String> append) {
        for (MinimapLine line : lines) {
            EnumNotificationHudHost preference = MinimapLineRegistry.shared().hostPreference(line);
            if (preference == EnumNotificationHudHost.AUTO || preference == EnumNotificationHudHost.JOURNEYMAP)
                append.accept(key(line));
        }
    }

    private void register(MinimapLine line) {
        if (registry == null || !registered.add(line.id())) return;
        registry.register(getModId(), line.displayName(), 100, () -> {
            if (!UIManager.INSTANCE.isMiniMapEnabled()
                    || !journeymap.client.JourneymapClient.getInstance().getStateHandler().isMinimapEnabled()
                    || !nativeScreenVisible())
                return Component.empty();
            for (MinimapLineSnapshot row : NotificationMinimapBridge.rows(EnumNotificationHudHost.JOURNEYMAP))
                if (row.id().equals(line.id())) return xin.vanilla.banira.internal.client.ui.MinimapUiRows.tag(
                        EnumNotificationHudHost.JOURNEYMAP, line.id(), row.text());
            return Component.empty();
        });
        previousRevision = -1;
    }

    private boolean nativeScreenVisible() {
        journeymap.client.JourneymapClient client = journeymap.client.JourneymapClient.getInstance();
        if (!client.enabled() || !Boolean.TRUE.equals(client.isMapping())) return false;
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        boolean chat = screen instanceof ChatScreen;
        boolean behindScreens = client.getCoreProperties().allowMiniMapBehindScreens.get();
        boolean nativeAllowed = screen == null || chat
                || behindScreens && nativeAllowsScreen(screen);
        return MinimapLineLayout.allowsNativeJourneyScreen(screen != null, chat, minecraft.options.hideGui,
                behindScreens, nativeAllowed);
    }

    private static java.lang.reflect.Method nativeScreenRule() {
        try {
            java.lang.reflect.Method method = ScreenEventHandler.class.getDeclaredMethod("allowableDrawScreens", Screen.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            LOGGER.warn("JourneyMap screen visibility rule is unavailable", failure);
            return null;
        }
    }

    private boolean nativeAllowsScreen(Screen screen) {
        if (NATIVE_SCREEN_RULE == null || nativeScreenRuleFailed) return false;
        try {
            return Boolean.TRUE.equals(NATIVE_SCREEN_RULE.invoke(ScreenEventHandler.getInstance(), screen));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            nativeScreenRuleFailed = true;
            LOGGER.warn("JourneyMap screen visibility rule failed", failure);
            return false;
        }
    }

    private void refreshLayout() {
        if (Minecraft.getInstance().level == null) {
            previousRevision = -1;
            previousAutomaticOwner = null;
            previousRequested = java.util.Collections.emptyList();
            return;
        }
        boolean allowed = UIManager.INSTANCE.isMiniMapEnabled()
                && journeymap.client.JourneymapClient.getInstance().getStateHandler().isMinimapEnabled()
                && nativeScreenVisible();
        java.util.List<net.minecraft.resources.ResourceLocation> requested =
                MinimapLineRegistry.shared().requestedIds(EnumNotificationHudHost.JOURNEYMAP);
        EnumNotificationHudHost automaticOwner = MinimapLineRegistry.shared()
                .selectedHost(EnumNotificationHudHost.AUTO, System.nanoTime());
        long revision = MinimapLineRegistry.shared().layoutRevision();
        if (allowed == previousAllowed && automaticOwner == previousAutomaticOwner
                && revision == previousRevision && requested.equals(previousRequested)) return;
        previousAllowed = allowed;
        previousAutomaticOwner = automaticOwner;
        previousRevision = revision;
        previousRequested = requested;
        // Empty native slots need a layout rebuild when their content becomes available.
        if (UIManager.INSTANCE.getMinimap() != null) UIManager.INSTANCE.getMinimap().updateDisplayVars(true);
    }
}
