package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.command.BaniraCommand;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.config.TestConfig;
import xin.vanilla.banira.internal.network.NetworkInit;

@Mod(BaniraCodex.MODID)
@Accessors(fluent = true)
public class BaniraCodex {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "banira_codex";
    public static final String ARTIFACT_ID = "xin.vanilla";

    /**
     * 服务端实例
     */
    @Getter
    private final static KeyValue<MinecraftServer, Boolean> serverInstance = new KeyValue<>(null, false);

    /**
     * 玩家数据管理器
     */
    public static final PlayerDataManager playerDataManager = PlayerDataManager.getOrCreateInstance(() ->
                    serverInstance().key().getWorldPath(FolderName.PLAYER_DATA_DIR)
            , MODID
            , StringUtils.reverseBySeparatorElegant(ARTIFACT_ID, ".")
    );

    public BaniraCodex() {
        // 配置必须在 CONFIG 加载阶段之前注册，故放在构造函数
        ForgeConfigAdapter.register(CommonConfig.class, MODID);
        ForgeConfigAdapter.register(TestConfig.class, MODID);

        // 注册Mod生命周期事件
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        // 注册游戏事件总线
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(BaniraScheduler.class);
        MinecraftForge.EVENT_BUS.register(BaniraEventBus.class);
        // 注册网络通道
        NetworkInit.register();

        registerBaniraEvent();
    }

    /**
     * 公共设置阶段事件
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        CustomConfig.loadCustomConfig(false);
    }

    /**
     * 注册指令
     */
    @SubscribeEvent
    public void onRegisterCommands(final RegisterCommandsEvent event) {
        BaniraCommand.register(event.getDispatcher());
    }

    private void registerBaniraEvent() {
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
            if (player instanceof ServerPlayerEntity) {
                PlayerUtils.removePlayerDataStatus(player);
            }
        });

        if (FMLEnvironment.dist.isClient()) {
            BaniraEventBus.Player.onClientLoggedIn(player ->
                    PacketUtils.sendPacketToServer(NetworkInit.HANDLER::getChannel, new ModLoadedToBoth(MODID))
            );
            BaniraEventBus.Player.onClientLoggedOut(player -> AdvancementUtils.clearAdvancementData());
            BaniraEventBus.Client.onGuiChanged(event -> LogoModifier.modifyLogo());
            BaniraEventBus.Client.onTextureReload(event -> {
                if (BaniraCodex.MODID.equals(event.getMap().location().getNamespace())) {
                    TextureUtils.resourceReloadEvent();
                }
            });
            BaniraEventBus.Client.onDrawScreenPost(event -> NotificationManager.get().render(event.getMatrixStack()));
            BaniraEventBus.Client.onRenderOverlayPost(event -> {
                if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && Minecraft.getInstance().screen == null) {
                    NotificationManager.get().render(event.getMatrixStack());
                }
            });
        }
    }

}
