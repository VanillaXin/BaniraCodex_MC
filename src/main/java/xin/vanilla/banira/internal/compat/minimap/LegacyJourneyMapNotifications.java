package xin.vanilla.banira.internal.compat.minimap;

import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.theme.ThemeLabelSource;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.client.notification.NotificationUnreadHud;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.config.ClientConfig;
import net.minecraft.client.Minecraft;
import java.util.Arrays;

/** Loaded only by JourneyMap's guarded, client-side layout mixin. */
public final class LegacyJourneyMapNotifications {
    private static final ThemeLabelSource.InfoSlot SLOT = ThemeLabelSource.create(
            "banira_codex", "banira_unread", 0, 1, () -> canUseMap()
                    ? NotificationMinimapBridge.text(EnumNotificationHudHost.JOURNEYMAP).getString() : "");
    private static boolean previousVisible;
    private static boolean previousActive;
    private static EnumNotificationHudHost previousPreference;
    private static EnumNotificationHudHost previousOwner;

    static {
        BaniraClientEvents.Client.onClientTick(event -> refreshLayout());
    }

    private LegacyJourneyMapNotifications() {
    }

    private static boolean canUseMap() {
        return NotificationUnreadHud.isVisible() && UIManager.INSTANCE.isMiniMapEnabled()
                && MiniMap.uiState().active;
    }

    public static ThemeLabelSource.InfoSlot[] append(ThemeLabelSource.InfoSlot[] slots) {
        EnumNotificationHudHost preference = ClientConfig.get().notificationHud().host();
        if ((preference != EnumNotificationHudHost.AUTO && preference != EnumNotificationHudHost.JOURNEYMAP)
                || !canUseMap()) return slots;
        for (ThemeLabelSource.InfoSlot slot : slots) if (slot == SLOT) return slots;
        ThemeLabelSource.InfoSlot[] result = Arrays.copyOf(slots, slots.length + 1);
        result[slots.length] = SLOT;
        return result;
    }

    private static void refreshLayout() {
        if (Minecraft.getInstance().level == null) {
            previousPreference = previousOwner = null;
            previousActive = false;
            return;
        }
        boolean visible = NotificationUnreadHud.isVisible();
        boolean active = UIManager.INSTANCE.isMiniMapEnabled() && MiniMap.uiState().active;
        EnumNotificationHudHost preference = ClientConfig.get().notificationHud().host();
        EnumNotificationHudHost owner = NotificationMinimapBridge.selectedHost();
        if (visible == previousVisible && active == previousActive
                && preference == previousPreference && owner == previousOwner) return;
        previousVisible = visible;
        previousActive = active;
        previousPreference = preference;
        previousOwner = owner;
        if (UIManager.INSTANCE.getMiniMap() != null) UIManager.INSTANCE.getMiniMap().updateDisplayVars(true);
    }
}
