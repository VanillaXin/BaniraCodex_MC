package xin.vanilla.banira.internal.fabric.platform;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Fabric 1.18.2 资源包与模组容器语言资源适配。 */
public final class FabricBaniraResourceService {
    private static final Logger LOGGER = LogManager.getLogger();

    private FabricBaniraResourceService() {
    }

    public static Map<String, JsonObject> modLanguageFiles(String modId) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        if (modId == null || modId.trim().isEmpty()) return result;

        // Translator 可能早于客户端资源管理器创建，先读取模组容器中的基础语言。
        collectModContainerLanguageFiles(modId, result);
        ResourceManager manager = activeResourceManager();
        if (manager != null) {
            Predicate<String> jsonLang = path -> path.endsWith(".json");
            manager.listPacks().forEach(pack -> collectLanguageFiles(pack, modId, jsonLang, result));
        }
        return result;
    }

    private static void collectModContainerLanguageFiles(String modId, Map<String, JsonObject> result) {
        FabricLoader.getInstance().getModContainer(modId)
                .flatMap(container -> container.findPath("assets/" + modId + "/lang"))
                .filter(Files::isDirectory)
                .ifPresent(path -> collectLanguageDirectory(path, result));
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
            mergeLanguage(languageCode, JsonUtils.parseObject(reader), result);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to load language file {}: {}", path, throwable.getMessage());
        }
    }

    private static ResourceManager activeResourceManager() {
        try {
            return BaniraServerRuntime.isRunning()
                    ? BaniraServerRuntime.resourceManager()
                    : BaniraClientRuntime.resourceManager();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void collectLanguageFiles(PackResources pack, String modId, Predicate<String> predicate,
                                             Map<String, JsonObject> result) {
        try {
            Collection<ResourceLocation> locations = pack.getResources(
                    PackType.CLIENT_RESOURCES, modId, "lang", Integer.MAX_VALUE, predicate);
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
            mergeLanguage(languageCode(location), JsonUtils.parseObject(reader), result);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to load language file {}: {}", location, throwable.getMessage());
        }
    }

    private static void mergeLanguage(String languageCode, JsonObject json, Map<String, JsonObject> result) {
        JsonObject existing = result.get(languageCode);
        if (existing == null) result.put(languageCode, json);
        else JsonUtils.mergeInPlace(existing, json);
    }

    private static String languageCode(ResourceLocation location) {
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.substring(0, name.length() - 5).toLowerCase();
    }
}
