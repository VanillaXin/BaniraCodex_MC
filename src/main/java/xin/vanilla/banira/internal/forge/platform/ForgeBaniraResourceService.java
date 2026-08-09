package xin.vanilla.banira.internal.forge.platform;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.internal.common.ClientRuntimeBridge;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Forge 1.19.2 资源包与模组文件语言资源适配。 */
public final class ForgeBaniraResourceService {
    private static final Logger LOGGER = LogManager.getLogger();

    private ForgeBaniraResourceService() {
    }

    public static Map<String, JsonObject> modLanguageFiles(String modId) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        if (modId == null || modId.trim().isEmpty()) return result;
        ResourceManager manager = activeResourceManager();
        if (manager != null) {
            Predicate<ResourceLocation> jsonLang = location -> location.getPath().endsWith(".json");
            manager.listPacks().forEach(pack -> collectLanguageFiles(pack, modId, jsonLang, result));
        }
        collectRegisteredModLanguages(modId, result);
        return result;
    }

    /** 启动早期资源管理器尚未就绪时，直接从 Forge 已登记的模组文件读取语言。 */
    private static void collectRegisteredModLanguages(String modId, Map<String, JsonObject> result) {
        try {
            IModFileInfo modFile = ModList.get().getModFileById(modId);
            if (modFile == null) return;
            collectLanguageDirectory(modFile.getFile().findResource("assets/" + modId + "/lang"), result);
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
        try {
            return BaniraServerRuntime.isRunning()
                    ? BaniraServerRuntime.resourceManager()
                    : ClientRuntimeBridge.resourceManager();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void collectLanguageFiles(PackResources pack, String modId, Predicate<ResourceLocation> predicate,
                                             Map<String, JsonObject> result) {
        try {
            Collection<ResourceLocation> locations = pack.getResources(
                    PackType.CLIENT_RESOURCES, modId, "lang", predicate);
            for (ResourceLocation location : locations) {
                loadLanguage(PackType.CLIENT_RESOURCES, pack, location, result);
                loadLanguage(PackType.SERVER_DATA, pack, location, result);
            }
        } catch (Exception exception) {
            LOGGER.trace("Failed to list lang from pack {}: {}", pack.getName(), exception.getMessage());
        }
    }

    private static void loadLanguage(PackType packType, PackResources pack, ResourceLocation location,
                                     Map<String, JsonObject> result) {
        try (InputStreamReader reader = new InputStreamReader(pack.getResource(packType, location), StandardCharsets.UTF_8)) {
            String languageCode = languageCode(location);
            JsonObject json = JsonUtils.parseObject(reader);
            JsonObject existing = result.get(languageCode);
            if (existing == null) result.put(languageCode, json);
            else JsonUtils.mergeInPlace(existing, json);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to load language file {}: {}", location, throwable.getMessage());
        }
    }

    private static String languageCode(ResourceLocation location) {
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.substring(0, name.length() - 5).toLowerCase();
    }
}
