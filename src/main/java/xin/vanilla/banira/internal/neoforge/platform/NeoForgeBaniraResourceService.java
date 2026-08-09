package xin.vanilla.banira.internal.neoforge.platform;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.util.JsonUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/** NeoForge 1.21.1 资源包与模组文件语言资源适配。 */
public final class NeoForgeBaniraResourceService {
    private static final Logger LOGGER = LogManager.getLogger();

    private NeoForgeBaniraResourceService() {
    }

    public static Map<String, JsonObject> modLanguageFiles(String modId) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        if (modId == null || modId.trim().isEmpty()) return result;
        ResourceManager manager = activeResourceManager();
        if (manager != null) {
            manager.listPacks().forEach(pack -> {
                try {
                    pack.listResources(PackType.CLIENT_RESOURCES, modId, "lang", (location, supplier) -> {
                        if (location.getPath().endsWith(".json")) loadLanguage(supplier, location, result);
                    });
                } catch (Exception exception) {
                    LOGGER.trace("Failed to list lang from pack {}: {}", pack.packId(), exception.getMessage());
                }
            });
        }
        collectRegisteredModLanguages(modId, result);
        return result;
    }

    /** 启动早期资源管理器尚未就绪时，直接从 NeoForge 已登记的模组文件读取语言。 */
    private static void collectRegisteredModLanguages(String modId, Map<String, JsonObject> result) {
        try {
            IModInfo modInfo = ModList.get().getModContainerById(modId)
                    .map(container -> container.getModInfo())
                    .orElse(null);
            if (modInfo == null) return;
            Path languageDirectory = modInfo.getOwningFile().getFile()
                    .findResource("assets/" + modId + "/lang");
            collectLanguageDirectory(languageDirectory, result);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to list lang from registered mod file {}: {}", modId, throwable.getMessage());
        }
    }

    static void collectLanguageDirectory(Path directory, Map<String, JsonObject> result) {
        if (directory == null || result == null || !Files.isDirectory(directory)) return;
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .forEach(path -> loadLanguage(path, result));
        } catch (Exception exception) {
            LOGGER.debug("Failed to list lang directory {}: {}", directory, exception.getMessage());
        }
    }

    private static void loadLanguage(Path path, Map<String, JsonObject> result) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            String fileName = path.getFileName().toString();
            String languageCode = fileName.substring(0, fileName.length() - 5).toLowerCase();
            JsonObject base = JsonUtils.parseObject(reader);
            JsonObject override = result.get(languageCode);
            if (override != null) JsonUtils.mergeInPlace(base, override);
            result.put(languageCode, base);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to load language file {}: {}", path, throwable.getMessage());
        }
    }

    private static ResourceManager activeResourceManager() {
        if (BaniraCodex.serverInstance().val()) {
            return BaniraCodex.serverInstance().key().getResourceManager();
        }
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            return (ResourceManager) minecraftClass.getMethod("getResourceManager").invoke(minecraft);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void loadLanguage(IoSupplier<InputStream> supplier, ResourceLocation location,
                                     Map<String, JsonObject> result) {
        try (InputStreamReader reader = new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)) {
            String path = location.getPath();
            int slash = path.lastIndexOf('/');
            String fileName = slash >= 0 ? path.substring(slash + 1) : path;
            String languageCode = fileName.substring(0, fileName.length() - 5).toLowerCase();
            JsonObject json = JsonUtils.parseObject(reader);
            JsonObject existing = result.get(languageCode);
            if (existing == null) result.put(languageCode, json);
            else JsonUtils.mergeInPlace(existing, json);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to load language file {}: {}", location, throwable.getMessage());
        }
    }
}
