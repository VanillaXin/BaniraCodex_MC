package xin.vanilla.banira.internal.compat.minimap;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
/** Loaded only by FTB's guarded client initialization hook. */
public final class FtbNotificationInfo implements MinimapInfoComponent {
    private static final ResourceLocation ID = new ResourceLocation("banira_codex", "unread_notifications");
    public static void register() {
        if (FTBChunksAPI.clientApi().getMinimapComponents().stream().noneMatch(c -> ID.equals(c.id())))
            FTBChunksAPI.clientApi().registerMinimapComponent(new FtbNotificationInfo());
    }
    @Override public ResourceLocation id() { return ID; }
    @Override public Component displayName() { return Component.translatable("word.banira_codex.notification_unread"); }
    @Override public boolean shouldRender(MinimapContext context) {
        return !NotificationMinimapBridge.text(EnumNotificationHudHost.FTB_CHUNKS).getString().isEmpty();
    }
    @Override public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        Component text = NotificationMinimapBridge.text(EnumNotificationHudHost.FTB_CHUNKS);
        if (!text.getString().isEmpty()) drawCenteredText(font, graphics, text, 0);
    }
}
