package xin.vanilla.banira.internal.fabric.platform;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.common.ClientRuntimeBridge;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Fabric 1.19.2 资源管理器与模组容器语言资源适配。 */
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
            Predicate<ResourceLocation> jsonLang = location ->
                    modId.equals(location.getNamespace()) && location.getPath().endsWith(".json");
            manager.listResources("lang", jsonLang)
                    .forEach((location, resource) -> loadLanguage(location, resource, result));
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
        ResourceManager client = ClientRuntimeBridge.resourceManager();
        if (client != null) return client;
        return BaniraCodex.serverInstance().val()
                ? BaniraCodex.serverInstance().key().getResourceManager()
                : null;
    }

    private static void loadLanguage(ResourceLocation location, Resource resource,
                                     Map<String, JsonObject> result) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
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
