package xin.vanilla.banira.internal.compat.minimap;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;
import xin.vanilla.banira.internal.client.minimap.MinimapTextLayout;

import java.util.Collections;
import java.util.List;

public final class FtbNotificationInfo implements MinimapInfoComponent {
    private static boolean listening;
    private final MinimapLine line;
    private List<Component> text = Collections.emptyList();

    private FtbNotificationInfo(MinimapLine line) {
        this.line = line;
    }

    public static synchronized void register() {
        NotificationMinimapBridge.registrations();
        if (listening) return;
        listening = true;
        MinimapLineRegistry.shared().onRegistration(line -> Minecraft.getInstance().execute(() -> {
            FtbNotificationInfo component = new FtbNotificationInfo(line);
            if (FTBChunksAPI.clientApi().getMinimapComponents().stream().noneMatch(c -> component.id().equals(c.id()))) {
                FTBChunksAPI.clientApi().registerMinimapComponent(component);
                FTBChunksClient.INSTANCE.setupComponents();
            }
        }));
    }

    @Override
    public ResourceLocation id() {
        return line.id().toString().equals("banira_codex:unread")
                ? ResourceLocation.fromNamespaceAndPath("banira_codex", "unread_notifications") : line.id();
    }

    @Override
    public Component displayName() {
        return line.displayName();
    }

    @Override
    public boolean shouldRender(MinimapContext context) {
        text = Collections.emptyList();
        for (MinimapLineSnapshot row : NotificationMinimapBridge.rows(EnumNotificationHudHost.FTB_CHUNKS)) {
            if (!row.id().equals(line.id())) continue;
            Minecraft mc = context.minecraft();
            float mapScale = FTBChunksClientConfig.MINIMAP_PROPORTIONAL.get()
                    ? mc.getWindow().getGuiScaledWidth() / 10f / 64f * FTBChunksClientConfig.MINIMAP_SCALE.get().floatValue()
                    : (float) (FTBChunksClientConfig.MINIMAP_SCALE.get() * 4 / mc.getWindow().getGuiScale());
            float fontScale = FTBChunksClientConfig.MINIMAP_FONT_SCALE.get().floatValue();
            int width = Math.max(1, (int) (Math.min(64 * mapScale, mc.getWindow().getGuiScaledWidth())
                    / Math.max(.01f, fontScale)));
            text = MinimapTextLayout.wrap(row.text(), mc.font.getSplitter(), width);
            break;
        }
        return !text.isEmpty();
    }

    @Override
    public int height(MinimapContext context) {
        return computeLineHeight(context.minecraft(), text.size()) + 1;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        for (int i = 0; i < text.size(); i++) drawCenteredText(font, graphics, text.get(i), i * (font.lineHeight + 2));
    }
}
