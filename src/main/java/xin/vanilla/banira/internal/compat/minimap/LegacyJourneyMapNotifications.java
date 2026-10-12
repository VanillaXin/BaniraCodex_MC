package xin.vanilla.banira.internal.compat.minimap;

import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.theme.ThemeLabelSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import xin.vanilla.banira.internal.client.minimap.MinimapLineLayout;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LegacyJourneyMapNotifications {
    private static final Map<ResourceLocation, ThemeLabelSource.InfoSlot> SLOTS = new HashMap<>();
    private static boolean previousActive;
    private static List<ResourceLocation> previousRequested = Collections.emptyList();
    private static long previousRevision = -1;
    private static EnumNotificationHudHost previousOwner;

    static {
        BaniraClientEvents.Client.onClientTick(event -> refreshLayout());
    }

    private LegacyJourneyMapNotifications() {
    }

    private static boolean canUseMap() {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null
                || UIManager.INSTANCE == null) return false;
        Minecraft mc = Minecraft.getInstance();
        return MinimapLineLayout.allowsLegacyJourneyScreen(mc.screen != null, mc.screen instanceof ChatScreen)
                && UIManager.INSTANCE.isMiniMapEnabled() && MiniMap.uiState().active;
    }

    private static synchronized ThemeLabelSource.InfoSlot slot(MinimapLine line) {
        ThemeLabelSource.InfoSlot existing = SLOTS.get(line.id());
        if (existing != null) return existing;
        String id = line.id().toString().equals("banira_codex:unread") ? "banira_unread" : "banira_line_" + line.id();
        ThemeLabelSource.InfoSlot result = ThemeLabelSource.create(
                line.id().getNamespace(), id, 0, 1, () -> {
                    if (!canUseMap()) return "";
                    for (MinimapLineSnapshot row : NotificationMinimapBridge.rows(EnumNotificationHudHost.JOURNEYMAP)) {
                        if (row.id().equals(line.id())) return row.text().getString();
                    }
                    return "";
                });
        SLOTS.put(line.id(), result);
        return result;
    }

    public static ThemeLabelSource.InfoSlot[] append(ThemeLabelSource.InfoSlot[] slots) {
        if (!canUseMap()) return slots;
        List<ResourceLocation> requested = MinimapLineRegistry.shared().requestedIds(EnumNotificationHudHost.JOURNEYMAP);
        List<ThemeLabelSource.InfoSlot> result = new ArrayList<>(Arrays.asList(slots));
        for (MinimapLine line : NotificationMinimapBridge.registrations()) {
            if (!requested.contains(line.id())) continue;
            ThemeLabelSource.InfoSlot slot = slot(line);
            if (!result.contains(slot)) result.add(slot);
        }
        return result.toArray(new ThemeLabelSource.InfoSlot[0]);
    }

    private static void refreshLayout() {
        if (Minecraft.getInstance().level == null) {
            previousRevision = -1;
            previousRequested = Collections.emptyList();
            previousOwner = null;
            previousActive = false;
            return;
        }
        NotificationMinimapBridge.registrations();
        boolean active = canUseMap();
        MinimapLineRegistry registry = MinimapLineRegistry.shared();
        List<ResourceLocation> requested = registry.requestedIds(EnumNotificationHudHost.JOURNEYMAP);
        long revision = registry.layoutRevision();
        EnumNotificationHudHost owner = registry.selectedHost(EnumNotificationHudHost.AUTO, System.nanoTime());
        if (active == previousActive && requested.equals(previousRequested)
                && revision == previousRevision && owner == previousOwner) return;
        previousActive = active;
        previousRequested = requested;
        previousRevision = revision;
        previousOwner = owner;
        if (UIManager.INSTANCE.getMiniMap() != null) UIManager.INSTANCE.getMiniMap().updateDisplayVars(true);
    }
}
