package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.command.BaniraCommand;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.network.NetworkInit;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(fluent = true)
public class BaniraCodex implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "banira_codex";
    @Deprecated
    public static final String ARTIFACT_ID = "xin.vanilla";

    /**
     * 数据与配置使用的根目录名
     */
    public static final String VANILLA_XIN = "vanilla.xin";

    /**
     * Banira世界数据路径
     */
    public static final Supplier<Path> BANIRA_WORLD_DATA_PATH = () -> serverInstance().key().getWorldPath(LevelResource.ROOT).resolve(VANILLA_XIN);

    /**
     * 玩家数据目录路径
     */
    public static final Supplier<Path> BANIRA_PLAYER_DATA_PATH = () -> serverInstance().key().getWorldPath(LevelResource.ROOT).resolve(VANILLA_XIN).resolve("playerdata");

    /**
     * Banira配置目录路径
     */
    public static final Supplier<Path> BANIRA_CONFIG_PATH = () -> FabricLoader.getInstance().getConfigDir().resolve(VANILLA_XIN);

    /**
     * 服务端实例
     */
    @Getter
    private final static KeyValue<MinecraftServer, Boolean> serverInstance = new KeyValue<>(null, false);

    /**
     * 玩家数据管理器
     */
    public static final PlayerDataManager playerDataManager = PlayerDataManager.getOrCreateInstance(
            BANIRA_PLAYER_DATA_PATH,
            () -> serverInstance().key().getWorldPath(LevelResource.PLAYER_DATA_DIR),
            MODID,
            "",
            StringUtils.reverseBySeparatorElegant(ARTIFACT_ID, ".")
    );

    @Override
    public void onInitialize() {
        CommonConfig.init();
        registerFabricEvents();
        // 注册网络通道
        NetworkInit.register();
        registerBaniraEvent();
    }

    private void registerFabricEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(BaniraEventBus::dispatchServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(BaniraEventBus::dispatchServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(BaniraEventBus::dispatchServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BaniraScheduler.onServerTick(server);
            BaniraEventBus.dispatchServerTick(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BaniraEventBus.dispatchPlayerLoggedIn(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BaniraEventBus.dispatchPlayerLoggedOut(handler.player));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BaniraCommand.register(dispatcher));
    }

    private void registerBaniraEvent() {
        // 通用事件
        CustomConfig.loadCustomConfig(false);
        ModLoadedPresence.register(MODID);

        // 服务器事件
        BaniraEventBus.Server.onStarting(server -> serverInstance().key(server).value(true));
        BaniraEventBus.Server.onStarting(server -> playerDataManager.clearCache());
        BaniraEventBus.Server.onStarting(server -> AdvancementUtils.clearAdvancementData());
        BaniraEventBus.Server.onStopping(server -> serverInstance().value(false));

        final int CONFIG_SAVE_INTERVAL_TICKS = 6000;
        BaniraEventBus.Server.onTick(tickServer -> {
            MinecraftServer server = serverInstance().key();
            if (server == null) return;
            if (server.getTickCount() % CONFIG_SAVE_INTERVAL_TICKS == 0) {
                if (!CustomConfig.loadCustomConfig(true)) {
                    CustomConfig.saveCustomConfig();
                }
            }
        });
        BaniraEventBus.Save.onWorldSave(CustomConfig::saveCustomConfig);

        // 玩家事件
        BaniraEventBus.Save.onPlayerSave(player ->
                playerDataManager.saveToDisk(PlayerUtils.getPlayerUUID(player))
        );
        BaniraEventBus.Player.onLoggedOut(player -> {
            if (player instanceof ServerPlayer) {
                PlayerUtils.removeRemoteClientDataStatus(player);
            }
        });
        BaniraEventBus.Player.onLoggedIn(player -> {
            if (player instanceof ServerPlayer sp) {
                PacketUtils.sendPacketToPlayer(new NotificationTypesSyncToClient(ServerNotificationTypeRegistry.buildSyncEntries()), sp);
            }
        });

        if (EnvironmentUtils.isClient()) {
            ClientProxy.init();
        }
    }

    public static class ClientProxy {
        public static void init() {
            BaniraClientEventHub.ModLifecycle.onClientSetup(() -> {
                LogoModifier.register(MODID, () -> Math.random() > 0.5 ? "logo_.png" : "logo.png");

                ResourceLocation texture = Identifier.id().create("gui/quick_icon.png");
                Component label = BaniraComponent.get().transClient("key.banira_codex.categories");
                Consumer<QuickActionContext> action = ctx ->
                        Minecraft.getInstance().setScreen(
                                new CodexNavigationScreen(new CodexNavigationScreen.Args().parentScreen(ctx.currentScreen()))
                        );
                QuickActionRegistry.get().registerListOnly(MODID + ":quick_codex_navigation", texture, label, action);
            });
        }
    }

}
