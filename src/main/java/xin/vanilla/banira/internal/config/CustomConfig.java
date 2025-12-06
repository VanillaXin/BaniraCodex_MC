package xin.vanilla.banira.internal.config;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.util.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CustomConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String FILE_NAME = "common_config.json";

    @Getter
    private static JsonObject customConfig = new JsonObject();

    @Getter
    @Setter
    private static JsonObject clientConfig = new JsonObject();

    @Getter
    @Setter
    private static boolean dirty = false;

    private static JsonObject defaultConfig() {
        JsonObject config = new JsonObject();

        JsonUtils.setJsonObject(config, "player", new JsonObject());
        JsonObject server = new JsonObject();
        JsonUtils.setJsonObject(server, "virtual_permission", new JsonObject());
        JsonUtils.setInt(server, "help_num_per_page", 10);
        JsonUtils.setInt(server, "virtual_op_permission", 4);
        JsonUtils.setString(server, "default_language", "en_us");
        JsonUtils.setJsonObject(config, "server", server);

        return config;
    }

    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(Arrays.stream(BaniraCodex.ARTIFACT_ID.split("\\."))
                        .sorted().collect(Collectors.joining("."))
                );
    }

    /**
     * 加载 JSON 数据
     *
     * @param notDirty 是否仅在数据不为脏时读取
     */
    public static void loadCustomConfig(boolean notDirty) {
        File dir = getConfigDirectory().toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, FILE_NAME);
        if (file.exists()) {
            if (!notDirty || !isDirty()) {
                try {
                    customConfig = JsonUtils.PRETTY_GSON.fromJson(new String(Files.readAllBytes(Paths.get(file.getPath()))), JsonObject.class);
                    LOGGER.debug("Loaded custom common config.");
                } catch (Exception e) {
                    LOGGER.error("Error loading custom common config: ", e);
                }
            }
        } else {
            // 如果文件不存在，初始化默认值
            customConfig = defaultConfig();
            setDirty(true);
        }
    }

    /**
     * 保存 JSON 数据
     */
    public static void saveCustomConfig() {
        long timeout = 10;
        new Thread(() -> {
            if (!isDirty()) return;
            File dir = getConfigDirectory().toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, FILE_NAME);
            try (RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
                 FileChannel channel = accessFile.getChannel()) {
                FileLock lock = null;
                long startTime = System.currentTimeMillis();
                while (lock == null) {
                    try {
                        lock = channel.tryLock();
                    } catch (Exception e) {
                        if (System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(timeout)) {
                            throw new RuntimeException("Failed to acquire file lock within timeout.", e);
                        }
                        Thread.sleep(100);
                    }
                    if (!isDirty()) {
                        return;
                    }
                }
                try {
                    // 清空旧内容
                    accessFile.setLength(0);
                    accessFile.write(JsonUtils.PRETTY_GSON.toJson(customConfig).getBytes(StandardCharsets.UTF_8));
                    setDirty(false);
                    LOGGER.debug("Saved custom common config.");
                } catch (Exception e) {
                    LOGGER.error("Error saving custom common config: ", e);
                } finally {
                    if (lock.isValid()) {
                        try {
                            lock.release();
                        } catch (IOException e) {
                            LOGGER.warn("Failed to release file lock: ", e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error saving custom common config (outer): ", e);
            }
        }).start();
    }

    public static int getHelpNumPerPage() {
        return JsonUtils.getInt(customConfig, "server.help_num_per_page", 10);
    }

    public static void setHelpNumPerPage(int page) {
        JsonUtils.setInt(customConfig, "server.help_num_per_page", page);
        setDirty(true);
    }

    public static int getVirtualOpPermission() {
        return JsonUtils.getInt(customConfig, "server.virtual_op_permission", 4);
    }

    public static void setVirtualOpPermission(int permission) {
        JsonUtils.setInt(customConfig, "server.virtual_op_permission", permission);
    }

    public static String getDefaultLanguage() {
        return JsonUtils.getString(customConfig, "server.default_language", "en_us");
    }

    public static void setDefaultLanguage(String language) {
        JsonUtils.setString(customConfig, "server.default_language", language);
    }

    public static String getPlayerLanguage(String uuid) {
        return JsonUtils.getString(customConfig, String.format("player.%s.language", uuid), "client");
    }

    public static String getPlayerLanguageClient(String uuid) {
        return JsonUtils.getString(clientConfig, String.format("player.%s.language", uuid), "client");
    }

    public static void setPlayerLanguage(String uuid, String language) {
        JsonUtils.setString(customConfig, String.format("player.%s.language", uuid), language);
        setDirty(true);
    }

    public static JsonObject getVirtualPermission() {
        return JsonUtils.getJsonObject(customConfig, "server.virtual_permission", new JsonObject());
    }

    public static JsonObject getVirtualPermissionClient() {
        return JsonUtils.getJsonObject(clientConfig, "server.virtual_permission", new JsonObject());
    }

    public static void setVirtualPermission(JsonObject virtualPermission) {
        JsonUtils.setJsonObject(customConfig, "server.virtual_permission", virtualPermission);
        setDirty(true);
    }
}
