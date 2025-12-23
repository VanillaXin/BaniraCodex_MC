package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.api.ResourceFactory;
import xin.vanilla.banira.common.data.KeyValue;
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

    @Getter
    private final static ResourceFactory resourceFactory = () -> MODID;

    /**
     * 服务端实例
     */
    @Getter
    private final static KeyValue<MinecraftServer, Boolean> serverInstance = new KeyValue<>(null, false);

    /**
     * 语言管理器
     */
    @Getter
    private final static LanguageHelper languager = LanguageHelper.init(MODID);

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
    public void onCommonSetup(final FMLCommonSetupEvent event) {
        CustomConfig.loadCustomConfig(false);
    }

    private void registerBaniraEvent() {
        BaniraEventBus.registerServerStarting(server ->
                serverInstance().setKey(server).setValue(true)
        );
        BaniraEventBus.registerServerStarting(server ->
                playerDataManager.clearCache()
        );
        BaniraEventBus.registerServerStarting(server ->
                AdvancementUtils.clearAdvancementData()
        );
        BaniraEventBus.registerServerStopping(server ->
                serverInstance().setValue(false)
        );
        BaniraEventBus.registerPlayerSave(player ->
                playerDataManager.saveToDisk(PlayerUtils.getPlayerUUID(player))
        );

        if (FMLEnvironment.dist.isClient()) {
            BaniraEventBus.registerPlayerLoggedOut(player ->
                    AdvancementUtils.clearAdvancementData()
            );
            BaniraEventBus.registerClientGuiChanged(LogoModifier::modifyLogo);
            BaniraEventBus.registerClientTextureReload(TextureUtils::resourceReloadEvent);
        }
    }

}
