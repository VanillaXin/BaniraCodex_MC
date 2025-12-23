package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.api.ResourceFactory;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.common.util.LanguageHelper;
import xin.vanilla.banira.common.util.StringUtils;
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
        // 注册服务器启动和关闭事件
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

        // 注册事件
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(BaniraScheduler.class);

        // 注册网络通道
        NetworkInit.register();
    }

    /**
     * 公共设置阶段事件
     */
    public void onCommonSetup(final FMLCommonSetupEvent event) {
        CustomConfig.loadCustomConfig(false);
    }

    private void onServerStarting(final FMLServerStartingEvent event) {
        serverInstance().setKey(event.getServer()).setValue(true);
        playerDataManager.clearCache();
    }

    private void onServerStarted(final FMLServerStartedEvent event) {
    }

    private void onServerStopping(final FMLServerStoppingEvent event) {
        serverInstance().setValue(false);
    }

}
