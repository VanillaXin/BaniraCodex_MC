package xin.vanilla.banira.internal.client;

import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.common.data.Component;

import java.util.function.Consumer;

/**
 * Banira 自身的客户端初始化，避免根入口直接引用具体 GUI 注册细节。
 */
public final class BaniraCodexClientBootstrap {

    private BaniraCodexClientBootstrap() {
    }

    public static void init() {
        BaniraClientEventHub.ModLifecycle.onClientSetup(event -> {
            LogoModifier.register(BaniraCodex.MODID, () -> Math.random() > 0.5 ? "logo_.png" : "logo.png");

            ResourceLocation texture = Identifier.id().create("gui/quick_icon.png");
            Component label = BaniraComponent.get().transClient("key.banira_codex.categories");
            Consumer<QuickActionContext> action = ctx ->
                    BaniraClientRuntime.setScreen(
                            new CodexNavigationScreen(new CodexNavigationScreen.Args().parentScreen(ctx.currentScreen()))
                    );
            QuickActionRegistry.get().registerListOnly(BaniraCodex.MODID + ":quick_codex_navigation", texture, label, action);
        });
    }
}
