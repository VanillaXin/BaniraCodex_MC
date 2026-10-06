package xin.vanilla.banira.internal.forge.compat.minimap;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.InfoSlotDisplayEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import journeymap.api.v2.common.event.MinimapEventRegistry;
import net.minecraft.network.chat.Component;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.client.notification.NotificationUnreadHud;
import net.minecraft.client.Minecraft;
import journeymap.client.ui.UIManager;

/** JourneyMap discovers this class only when its optional client API is installed. */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapNotificationPlugin implements IClientPlugin {
    private static final String KEY = "word.banira_codex.notification_unread";
    private boolean previousVisible;
    private EnumNotificationHudHost previousPreference;
    private EnumNotificationHudHost previousOwner;

    @Override
    public String getModId() {
        return "banira_codex";
    }

    @Override
    public void initialize(IClientAPI api) {
        ClientEventRegistry.INFO_SLOT_REGISTRY_EVENT.subscribe(getModId(), event ->
                event.register(getModId(), Component.translatable(KEY), 100,
                        () -> UIManager.INSTANCE.isMiniMapEnabled()
                                ? NotificationMinimapBridge.text(EnumNotificationHudHost.JOURNEYMAP)
                                : Component.empty()));
        MinimapEventRegistry.INFO_SLOT_DISPLAY_EVENT.subscribe(getModId(), event -> {
            EnumNotificationHudHost preference = ClientConfig.get().notificationHud().host();
            if (preference == EnumNotificationHudHost.AUTO || preference == EnumNotificationHudHost.JOURNEYMAP) {
                event.addLast(KEY, InfoSlotDisplayEvent.Position.Bottom);
            }
        });
        BaniraClientEvents.Client.onClientTick(event -> refreshLayout());
    }

    private void refreshLayout() {
        if (Minecraft.getInstance().level == null) {
            previousPreference = previousOwner = null;
            return;
        }
        boolean visible = NotificationUnreadHud.isVisible();
        EnumNotificationHudHost preference = ClientConfig.get().notificationHud().host();
        EnumNotificationHudHost owner = NotificationMinimapBridge.selectedHost();
        if (visible == previousVisible && preference == previousPreference && owner == previousOwner) return;
        previousVisible = visible;
        previousPreference = preference;
        previousOwner = owner;
        // JourneyMap removes empty slots from its layout until the next layout rebuild.
        if (UIManager.INSTANCE.getMinimap() != null) UIManager.INSTANCE.getMinimap().updateDisplayVars(true);
    }
}
