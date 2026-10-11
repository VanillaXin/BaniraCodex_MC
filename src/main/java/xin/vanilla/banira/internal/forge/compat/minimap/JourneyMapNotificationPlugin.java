package xin.vanilla.banira.internal.forge.compat.minimap;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.InfoSlotDisplayEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import journeymap.api.v2.common.event.MinimapEventRegistry;
import journeymap.client.ui.UIManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapNotificationPlugin implements IClientPlugin {
    private boolean initialized;
    private boolean previousAllowed;
    private List<ResourceLocation> previousRequested = Collections.emptyList();
    private long previousRevision = -1;
    private EnumNotificationHudHost previousOwner;
    private Consumer<MinimapLine> registrar;
    private final Set<ResourceLocation> registeredIds = new HashSet<>();

    @Override
    public String getModId() {
        return "banira_codex";
    }

    private static String key(MinimapLine line) {
        ITextComponent name = line.displayName();
        return name instanceof TranslationTextComponent ? ((TranslationTextComponent) name).getKey() : name.getString();
    }

    private void registerLine(MinimapLine line) {
        if (registrar != null && registeredIds.add(line.id())) registrar.accept(line);
    }

    @Override
    public void initialize(IClientAPI api) {
        if (initialized) return;
        initialized = true;
        NotificationMinimapBridge.registrations();
        MinimapLineRegistry.shared().onRegistration(line -> Minecraft.getInstance().execute(() -> {
            registerLine(line);
            previousRevision = -1;
        }));
        ClientEventRegistry.INFO_SLOT_REGISTRY_EVENT.subscribe(getModId(), event -> {
            registrar = line -> event.register(line.id().getNamespace(), line.displayName(), 100, () -> {
                if (!allowed()) return StringTextComponent.EMPTY;
                for (MinimapLineSnapshot row : NotificationMinimapBridge.rows(EnumNotificationHudHost.JOURNEYMAP)) {
                    if (row.id().equals(line.id())) return row.text();
                }
                return StringTextComponent.EMPTY;
            });
            registeredIds.clear();
            for (MinimapLine line : NotificationMinimapBridge.registrations()) registerLine(line);
        });
        MinimapEventRegistry.INFO_SLOT_DISPLAY_EVENT.subscribe(getModId(), event -> {
            List<ResourceLocation> requested = MinimapLineRegistry.shared().requestedIds(EnumNotificationHudHost.JOURNEYMAP);
            for (MinimapLine line : NotificationMinimapBridge.registrations()) {
                if (requested.contains(line.id())) event.addLast(key(line), InfoSlotDisplayEvent.Position.Bottom);
            }
        });
        BaniraClientEvents.Client.onClientTick(event -> refreshLayout());
    }

    private static boolean allowed() {
        return UIManager.INSTANCE.isMiniMapEnabled()
                && journeymap.client.JourneymapClient.getInstance().getStateHandler().isMinimapEnabled();
    }

    private void refreshLayout() {
        if (Minecraft.getInstance().level == null) {
            previousRevision = -1;
            previousRequested = Collections.emptyList();
            previousOwner = null;
            return;
        }
        boolean allowed = allowed();
        MinimapLineRegistry registry = MinimapLineRegistry.shared();
        List<ResourceLocation> requested = registry.requestedIds(EnumNotificationHudHost.JOURNEYMAP);
        long revision = registry.layoutRevision();
        EnumNotificationHudHost owner = registry.selectedHost(EnumNotificationHudHost.AUTO, System.nanoTime());
        if (allowed == previousAllowed && requested.equals(previousRequested)
                && revision == previousRevision && owner == previousOwner) return;
        previousAllowed = allowed;
        previousRequested = requested;
        previousRevision = revision;
        previousOwner = owner;
        if (UIManager.INSTANCE.getMinimap() != null) UIManager.INSTANCE.getMinimap().updateDisplayVars(true);
    }
}
