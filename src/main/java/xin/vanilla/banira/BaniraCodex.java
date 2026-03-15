package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.config.CustomConfig;
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
     * 语言管理器（直接使用 BaniraLang.INSTANCE 亦可）
     */
    @Getter
    private final static BaniraLang languager = BaniraLang.INSTANCE;

    /**
     * 玩家数据管理器
     */
    public static final PlayerDataManager playerDataManager = PlayerDataManager.getOrCreateInstance(() ->
                    serverInstance().key().getWorldPath(FolderName.PLAYER_DATA_DIR)
            , MODID
            , StringUtils.reverseBySeparatorElegant(ARTIFACT_ID, ".")
    );

    public BaniraCodex() {
        // 注册事件总线
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
    @SubscribeEvent
    public void onCommonSetup(final FMLCommonSetupEvent event) {
        CustomConfig.loadCustomConfig(false);
    }

    private void registerBaniraEvent() {
        BaniraEventBus.registerServerStarting(server ->
                serverInstance().key(server).value(true)
        );
        BaniraEventBus.registerServerStarting(server ->
                playerDataManager.clearCache()
        );
        BaniraEventBus.registerServerStarting(server ->
                AdvancementUtils.clearAdvancementData()
        );
        BaniraEventBus.registerServerStopping(server ->
                serverInstance().value(false)
        );
        BaniraEventBus.registerPlayerSave(player ->
                playerDataManager.saveToDisk(PlayerUtils.getPlayerUUID(player))
        );
        BaniraEventBus.registerPlayerLoggedOut(player -> {
            if (player instanceof ServerPlayerEntity) {
                PlayerUtils.removePlayerDataStatus(player);
            }
        });

        if (FMLEnvironment.dist.isClient()) {
            BaniraEventBus.registerClientPlayerLoggedIn(player ->
                    PacketUtils.sendPacketToServer(NetworkInit.HANDLER::getChannel, new ModLoadedToBoth(MODID))
            );
            BaniraEventBus.registerClientPlayerLoggedOut(player ->
                    AdvancementUtils.clearAdvancementData()
            );
            BaniraEventBus.registerClientGuiChanged(LogoModifier::modifyLogo);
            BaniraEventBus.registerClientTextureReload(TextureUtils::resourceReloadEvent);
        }
    }

}
