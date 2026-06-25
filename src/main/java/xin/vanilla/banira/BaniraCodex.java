package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.event.BaniraClientForgeEventHandler;
import xin.vanilla.banira.client.event.BaniraClientModSetup;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.command.BaniraCommand;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.network.NetworkInit;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod(BaniraCodex.MODID)
@Accessors(fluent = true)
public class BaniraCodex {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "banira_codex";
    @Deprecated
    public static final String ARTIFACT_ID = "xin.vanilla";

    /**
     * 数据与配置使用的根目录名
     */
    public static final String VANILLA_XIN = "vanilla.xin";

    public static final LevelResource BANIRA_DIR = new LevelResource(VANILLA_XIN);

    /**
     * Banira世界数据路径
     */
    public static final Supplier<Path> BANIRA_WORLD_DATA_PATH = () -> serverInstance().key().getWorldPath(BANIRA_DIR);

    /**
     * 玩家数据目录路径
     */
    public static final Supplier<Path> BANIRA_PLAYER_DATA_PATH = () -> serverInstance().key().getWorldPath(BANIRA_DIR).resolve("playerdata");

    /**
     * Banira配置目录路径
     */
    public static final Supplier<Path> BANIRA_CONFIG_PATH = () -> FMLPaths.CONFIGDIR.get().resolve(VANILLA_XIN);

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

    public BaniraCodex(IEventBus modEventBus, ModContainer modContainer) {
        // 配置必须在 CONFIG 加载阶段之前注册
        ForgeConfigAdapter.register(CommonConfig.class, MODID);
        ForgeConfigAdapter.register(ClientConfig.class, MODID);
        // ForgeConfigAdapter.register(xin.vanilla.banira.internal.config.TestConfig.class, MODID);

        modEventBus.addListener(BaniraEventBus::dispatchModCommonSetup);

        // 注册游戏事件总线
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(BaniraScheduler.class);
        NeoForge.EVENT_BUS.register(BaniraEventBus.class);
        // 注册网络通道
        NetworkInit.register(modEventBus);

        registerBaniraEvent(modEventBus);
    }

    /**
     * 注册指令
     */
    @SubscribeEvent
    public void onRegisterCommands(final RegisterCommandsEvent event) {
        BaniraCommand.register(event.getDispatcher());
    }

    private void registerBaniraEvent(IEventBus modEventBus) {
        // 通用事件
        BaniraEventBus.ModLifecycle.onCommonSetup(event -> {
            CustomConfig.loadCustomConfig(false);
            ModLoadedPresence.register(MODID);
        });

        // 服务器事件
        BaniraEventBus.Server.onStarting(server -> serverInstance().key(server).value(true));
        BaniraEventBus.Server.onStarting(server -> playerDataManager.clearCache());
        BaniraEventBus.Server.onStarting(server -> AdvancementUtils.clearAdvancementData());
        BaniraEventBus.Server.onStopping(server -> serverInstance().value(false));

        final int CONFIG_SAVE_INTERVAL_TICKS = 6000;
        BaniraEventBus.Server.onTick(event -> {
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
            ClientProxy.init(modEventBus);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClientProxy {
        public static void init(IEventBus modEventBus) {
            BaniraClientModSetup.register(modEventBus);
            BaniraClientForgeEventHandler.register(NeoForge.EVENT_BUS);
            InputStateManager.register(NeoForge.EVENT_BUS);
            NotificationLogScreen.register(NeoForge.EVENT_BUS);

            BaniraClientEventHub.ModLifecycle.onClientSetup(event -> {
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
