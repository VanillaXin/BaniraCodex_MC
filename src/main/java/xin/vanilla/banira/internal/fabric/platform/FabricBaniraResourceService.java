package xin.vanilla.banira.internal.fabric.platform;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.mixin.accessors.ResourceManagerAccessor;
import xin.vanilla.banira.internal.mixin.accessors.MinecraftServerAccessor;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** 1.16 资源包遍历适配，供服务端语言解析使用。 */
public final class FabricBaniraResourceService {
    private static final Logger LOGGER = LogManager.getLogger();

    private FabricBaniraResourceService() { }

    public static Map<String, JsonObject> modLanguageFiles(String modId) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        if (modId == null || modId.trim().isEmpty()) return result;

        // Translator 可能早于客户端资源管理器创建，先从 mod 容器读取基础语言文件。
        collectModContainerLanguageFiles(modId, result);
        Predicate<String> jsonLang = path -> path.endsWith(".json");
        ResourceManager manager = activeResourceManager();
        if (manager != null) {
            try {
                ((ResourceManagerAccessor) manager).banira$packs()
                        .forEach(pack -> collectLanguageFiles(pack, modId, jsonLang, result));
            } catch (Exception e) {
                LOGGER.debug("Failed to list lang from resource packs", e);
            }
        }
        return result;
    }

    private static void collectModContainerLanguageFiles(String modId, Map<String, JsonObject> result) {
        FabricLoader.getInstance().getModContainer(modId)
                .flatMap(container -> container.findPath("assets/" + modId + "/lang"))
                .filter(Files::isDirectory)
                .ifPresent(path -> {
                    try (Stream<Path> files = Files.list(path)) {
                        files.filter(Files::isRegularFile)
                                .filter(file -> file.getFileName().toString().endsWith(".json"))
                                .sorted()
                                .forEach(file -> loadLanguage(file, result));
                    } catch (Exception e) {
                        LOGGER.debug("Failed to list bundled lang for mod {}", modId, e);
                    }
                });
    }

    private static void loadLanguage(Path file, Map<String, JsonObject> result) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            String name = file.getFileName().toString();
            mergeLanguage(name.substring(0, name.length() - ".json".length()).toLowerCase(),
                    JsonUtils.parseObject(reader), result);
        } catch (Exception e) {
            LOGGER.debug("Failed to load bundled language file {}", file, e);
        }
    }

    private static ResourceManager activeResourceManager() {
        if (BaniraCodex.serverInstance().key() != null) {
            return ((MinecraftServerAccessor) BaniraCodex.serverInstance().key()).banira$resources().getResourceManager();
        }
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            return (ResourceManager) minecraftClass.getMethod("getResourceManager").invoke(minecraft);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void collectLanguageFiles(PackResources pack, String modId, Predicate<String> predicate, Map<String, JsonObject> result) {
        try {
            Collection<ResourceLocation> locations = pack.getResources(PackType.CLIENT_RESOURCES, modId, "lang", Integer.MAX_VALUE, predicate);
            for (ResourceLocation location : locations) {
                loadLanguage(PackType.CLIENT_RESOURCES, pack, location, result);
                loadLanguage(PackType.SERVER_DATA, pack, location, result);
            }
        } catch (Exception e) {
            LOGGER.trace("Failed to list lang from pack {}: {}", pack.getName(), e.getMessage());
        }
    }

    private static void loadLanguage(PackType packType, PackResources pack, ResourceLocation location, Map<String, JsonObject> result) {
        try (InputStreamReader reader = new InputStreamReader(pack.getResource(packType, location), StandardCharsets.UTF_8)) {
            String languageCode = languageCode(location);
            JsonObject json = JsonUtils.parseObject(reader);
            mergeLanguage(languageCode, json, result);
        } catch (Throwable t) {
            LOGGER.debug("Failed to load language file: {}", t.getMessage());
        }
    }

    private static void mergeLanguage(String languageCode, JsonObject json, Map<String, JsonObject> result) {
        JsonObject existing = result.get(languageCode);
        if (existing == null) result.put(languageCode, json); else JsonUtils.mergeInPlace(existing, json);
    }

    private static String languageCode(ResourceLocation location) {
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.replace(".json", "").toLowerCase();
    }
}
