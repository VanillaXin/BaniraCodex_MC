package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.internal.common.BaniraCodexRuntime;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.internal.forge.ForgeBaniraCodexEntry;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.nio.file.Path;
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
    public static final Supplier<Path> BANIRA_CONFIG_PATH = () -> BaniraPlatforms.get().configDir().resolve(VANILLA_XIN);

    /**
     * 服务端实例
     */
    @Getter
    private final static KeyValue<MinecraftServer, Boolean> serverInstance = BaniraServerRuntime.serverInstance();

    /**
     * 玩家数据管理器
     */
    public static final PlayerDataManager playerDataManager = BaniraServerRuntime.playerDataManager();

    public BaniraCodex() {
        ForgeBaniraCodexEntry.bootstrap();
        BaniraCodexRuntime.bootstrap();
    }

}
