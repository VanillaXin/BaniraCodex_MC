package xin.vanilla.banira.internal.fabric.platform;

import com.google.gson.JsonObject;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** 1.16 资源包遍历适配，供服务端语言解析使用。 */
public final class FabricBaniraResourceService {
    private static final Logger LOGGER = LogManager.getLogger();

    private FabricBaniraResourceService() { }

    public static Map<String, JsonObject> modLanguageFiles(String modId) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        if (modId == null || modId.trim().isEmpty()) return result;
        ResourceManager manager = activeResourceManager();
        if (manager == null) return result;
        Predicate<String> jsonLang = path -> path.endsWith(".json");
        try {
            ((ResourceManagerAccessor) manager).banira$packs().forEach(pack -> collectLanguageFiles(pack, modId, jsonLang, result));
        } catch (Exception e) {
            LOGGER.debug("Failed to list lang from resource packs", e);
        }
        return result;
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
            JsonObject existing = result.get(languageCode);
            if (existing == null) result.put(languageCode, json); else JsonUtils.mergeInPlace(existing, json);
        } catch (Throwable t) {
            LOGGER.debug("Failed to load language file: {}", t.getMessage());
        }
    }

    private static String languageCode(ResourceLocation location) {
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.replace(".json", "").toLowerCase();
    }
}
