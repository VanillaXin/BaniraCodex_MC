package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.NotificationTypeConfigScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.client.gui.quickaction.CustomQuickActionManager;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.internal.config.ClientConfig;

import java.util.function.Consumer;

public final class BaniraCodexClientBootstrap {
    private BaniraCodexClientBootstrap() {
    }

    public static void init() {
        BaniraClientEventHub.ModLifecycle.onClientSetup(event -> {
            LogoModifier.register(BaniraCodex.MODID, () -> Math.random() > 0.5 ? "logo_.png" : "logo.png");

            ResourceLocation texture = Identifier.id().create("gui/quick_icon.png");
            Component label = BaniraComponent.get().transClient("key.banira_codex.categories");
            Consumer<QuickActionContext> action = ctx ->
                    Minecraft.getInstance().setScreen(
                            new CodexNavigationScreen(new CodexNavigationScreen.Args().parentScreen(ctx.currentScreen()))
                    );
            QuickActionRegistry.get().registerListOnly(BaniraCodex.MODID + ":quick_codex_navigation", texture, label, action);
            CustomQuickActionManager customActions = CustomQuickActionManager.get();
            customActions.registerScreen(BaniraCodex.MODID + ":navigation",
                    parent -> new CodexNavigationScreen(new CodexNavigationScreen.Args().parentScreen((net.minecraft.client.gui.screens.Screen) parent)));
            customActions.registerScreen(BaniraCodex.MODID + ":notification_log",
                    parent -> new NotificationLogScreen(new NotificationLogScreen.Args().parentScreen((net.minecraft.client.gui.screens.Screen) parent)));
            customActions.registerScreen(BaniraCodex.MODID + ":notification_types",
                    parent -> new NotificationTypeConfigScreen(new NotificationTypeConfigScreen.Args().parentScreen((net.minecraft.client.gui.screens.Screen) parent)));
            customActions.reload();
            ClientConfig.get().holder().onSaved(changed ->
                    ExternalInventoryButtonManager.get().refreshCurrentScreen());
            ClientConfig.get().holder().onReloaded(changed ->
                    ExternalInventoryButtonManager.get().refreshCurrentScreen());
        });
        BaniraClientEventHub.Client.onKeyPressedPre(CustomQuickActionManager.get()::onKeyPressed);
        BaniraClientEventHub.Client.onGuiChanged(event ->
                ExternalInventoryButtonManager.get().refreshCurrentScreen());
    }
}
