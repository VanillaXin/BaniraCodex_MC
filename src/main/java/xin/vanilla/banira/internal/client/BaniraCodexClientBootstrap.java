package xin.vanilla.banira.internal.client;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.NotificationTypeConfigScreen;
import xin.vanilla.banira.client.gui.quickaction.CustomQuickActionManager;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;
import xin.vanilla.banira.internal.client.ui.UiBackendController;
import xin.vanilla.banira.internal.client.ui.UiSessionHost;
import xin.vanilla.banira.internal.client.ui.AuiUiBackend;
import xin.vanilla.banira.api.client.ui.BaniraUi;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract;

import java.util.function.Consumer;

/**
 * Banira 自身的客户端初始化，避免根入口直接引用具体 GUI 注册细节。
 */
public final class BaniraCodexClientBootstrap {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final UiBackendController UI_BACKEND = new UiBackendController(
            () -> ClientConfigView.get().useAui(),
            BaniraCodexClientBootstrap::probeAui);
    private static final AuiUiBackend AUI = new AuiUiBackend(BaniraCodexClientBootstrap::uiSessions);
    private static final UiSessionHost UI_SESSIONS = new UiSessionHost(UI_BACKEND, AUI::create,
            BaniraClientRuntime::presentUiScreen, BaniraClientRuntime::isScreen);
    private static boolean hadConnection;
    private static final xin.vanilla.banira.internal.client.ui.AuiHudHost UI_HUDS = new xin.vanilla.banira.internal.client.ui.AuiHudHost(UI_BACKEND, AUI::createHud);

    public static UiSessionHost uiSessions() { return UI_SESSIONS; }

    private static boolean probeAui() {
        ClassLoader loader = BaniraCodexClientBootstrap.class.getClassLoader();
        if (loader.getResource("com/sighs/apricityui/screen/ApricityScreen.class") == null) return false;
        AuiCapabilityContract.Result result = AuiCapabilityContract.inspect(loader);
        if (!result.compatible()) {
            LOGGER.warn("AUI capability mismatch; native interfaces remain active: {}", result.mismatch());
        }
        return result.compatible();
    }

    public static UiBackendController uiBackend() {
        return UI_BACKEND;
    }

    private BaniraCodexClientBootstrap() {
    }

    public static void init() {
        UI_SESSIONS.hudHost(UI_HUDS);
        xin.vanilla.banira.internal.client.ui.SharedHudHosts.install(UI_HUDS);
        xin.vanilla.banira.internal.client.ui.SharedHudHosts.register(xin.vanilla.banira.internal.client.ui.SharedHudHosts.NOTIFICATIONS,
                () -> BaniraClientRuntime.hasConnection());
        xin.vanilla.banira.internal.client.ui.SharedHudHosts.register(xin.vanilla.banira.internal.client.ui.SharedHudHosts.UNREAD,
                () -> xin.vanilla.banira.client.notification.NotificationUnreadHud.isVisible()
                        && xin.vanilla.banira.client.notification.NotificationMinimapBridge.selectedHost()
                        == xin.vanilla.banira.common.enums.EnumNotificationHudHost.STANDALONE);
        xin.vanilla.banira.internal.client.ui.SharedHudHosts.register(xin.vanilla.banira.internal.client.ui.SharedHudHosts.QUICK_ACTIONS,
                () -> xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay.isSupportedInventoryScreen(net.minecraft.client.Minecraft.getInstance().screen));
        xin.vanilla.banira.internal.client.ui.SharedHudHosts.register(xin.vanilla.banira.internal.client.ui.SharedHudHosts.POINTER,
                () -> net.minecraft.client.Minecraft.getInstance().screen instanceof xin.vanilla.banira.client.gui.BaniraScreen && ClientConfig.get().useCustomCursor());
        BaniraUi.install(new BaniraUi.Driver() {
            @Override public void screenRemoved(Object screen) { xin.vanilla.banira.internal.client.ui.CommonUiPages.removed(screen); }
            @Override public boolean closePage(Object screen, xin.vanilla.banira.api.client.ui.UiCloseReason reason) {
                return xin.vanilla.banira.internal.client.ui.CommonUiPages.close(screen, reason);
            }
            @Override public void open(UiPageSpec page, Object previousScreen) { UI_SESSIONS.open(page, previousScreen); }
            @Override public boolean usesAui(String surfaceId) { return UI_BACKEND.usesAui(surfaceId); }
            @Override public AutoCloseable registerHud(UiHudSpec hud) { return UI_SESSIONS.registerHud(hud); }
            @Override public boolean renderHud(String id, net.minecraft.client.gui.GuiGraphics graphics) {
                return UI_HUDS.renderManual(id, graphics);
            }
            @Override public xin.vanilla.banira.api.client.ui.UiContainerLayer container(UiPageSpec page, java.util.function.BooleanSupplier current) {
                return xin.vanilla.banira.internal.client.ui.UiContainerLayerHost.create(UI_BACKEND, page, current,
                        () -> AUI.createContainer(page));
            }
        });
        BaniraClientEventHub.ModLifecycle.onClientSetup(event -> {
            LogoModifier.register(Banira.MOD_ID, () -> Math.random() > 0.5 ? "logo_.png" : "logo.png");

            ResourceLocation texture = Identifier.id().create("gui/quick_icon.png");
            Component label = BaniraComponent.get().transClient("key.banira_codex.categories");
            Consumer<QuickActionContext> action = ctx ->
                    BaniraClientRuntime.setScreen(
                            new CodexNavigationScreen(new CodexNavigationScreen.Args().parentScreen(ctx.currentScreen()))
                    );
            QuickActionRegistry.get().registerListOnly(Banira.MOD_ID + ":quick_codex_navigation", texture, label, action);
            CustomQuickActionManager customActions = CustomQuickActionManager.get();
            customActions.registerScreen(Banira.MOD_ID + ":navigation",
                    parent -> new CodexNavigationScreen(new CodexNavigationScreen.Args().parentScreen((net.minecraft.client.gui.screens.Screen) parent)));
            customActions.registerScreen(Banira.MOD_ID + ":notification_log",
                    parent -> new NotificationLogScreen(new NotificationLogScreen.Args().parentScreen((net.minecraft.client.gui.screens.Screen) parent)));
            customActions.registerScreen(Banira.MOD_ID + ":notification_types",
                    parent -> new NotificationTypeConfigScreen(new NotificationTypeConfigScreen.Args().parentScreen((net.minecraft.client.gui.screens.Screen) parent)));
            customActions.reload();
            BaniraConfigs.holder(ClientConfig.class).onSaved(changed -> {
                UI_SESSIONS.switchBackend(ClientConfigView.get().useAui());
                ExternalInventoryButtonManager.get().refreshCurrentScreen();
            });
            BaniraConfigs.holder(ClientConfig.class).onReloaded(changed -> {
                UI_SESSIONS.switchBackend(ClientConfigView.get().useAui());
                UI_SESSIONS.reload();
                ExternalInventoryButtonManager.get().refreshCurrentScreen();
            });
            UI_BACKEND.requestSwitch(ClientConfigView.get().useAui());
        });
        BaniraClientEventHub.Client.onClientTick(event -> {
            if (event != BaniraClientTickEvent.END) return;
            boolean connected = BaniraClientRuntime.hasConnection();
            if (hadConnection && !connected) UI_SESSIONS.disconnect();
            hadConnection = connected;
            xin.vanilla.banira.internal.client.ui.CommonUiPages.tick();
            UI_SESSIONS.tick();
            UI_HUDS.tick(connected);
        });
        BaniraClientEventHub.Client.onTextureReload(event -> { AUI.reload(); UI_SESSIONS.reload(); });
        BaniraClientEventHub.Client.onKeyPressedPre(CustomQuickActionManager.get()::onKeyPressed);
        BaniraClientEventHub.Client.onGuiChanged(event ->
                ExternalInventoryButtonManager.get().refreshCurrentScreen());
    }
}
