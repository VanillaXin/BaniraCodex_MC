package xin.vanilla.banira.internal.compat.minimap;

import net.minecraft.network.chat.TranslatableComponent;
import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.codec.InfoDisplayCommonStateCodecs;
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.function.Consumer;

/** Loaded exclusively by Xaero's guarded information-display mixin. */
public final class XaeroNotificationInfo {
    private static final InfoDisplay<Boolean> DISPLAY = InfoDisplay.Builder.<Boolean>begin()
            .setId("banira_unread")
            .setName(new TranslatableComponent("word.banira_codex.notification_unread"))
            .setDefaultState(true)
            .setCodec(InfoDisplayCommonStateCodecs.BOOLEAN)
            .setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
            .setCompiler((display, compiler, session, width, pos) -> {
                if (Boolean.TRUE.equals(display.getState())) {
                    net.minecraft.network.chat.Component text = NotificationMinimapBridge.text(EnumNotificationHudHost.XAERO);
                    if (!text.getString().isEmpty()) {
                        if (net.minecraft.client.Minecraft.getInstance().font.width(text) <= width) compiler.addLine(text);
                        else compiler.addWords(text.getString());
                    }
                }
            }).build();

    private XaeroNotificationInfo() {
    }

    public static void append(Consumer<Object> destination) {
        destination.accept(DISPLAY);
    }
}
