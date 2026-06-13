package xin.vanilla.banira.internal.forge.platform;

import com.google.gson.JsonObject;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.mixin.accessors.ResourceManagerAccessor;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Forge 1.16.5 resource-pack traversal. Resource manager internals change often by version.
 */
public final class ForgeBaniraResourceService {
    private static final Logger LOGGER = LogManager.getLogger();

    private ForgeBaniraResourceService() {
    }

    public static Map<String, JsonObject> modLanguageFiles(String modId) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        if (modId == null || modId.trim().isEmpty()) return result;
        IResourceManager manager = activeResourceManager();
        if (manager == null) return result;

        Predicate<String> jsonLang = path -> path.endsWith(".json");
        try {
            ((ResourceManagerAccessor) manager).banira$packs().forEach(pack -> collectLanguageFiles(pack, modId, jsonLang, result));
        } catch (Exception e) {
            LOGGER.debug("Failed to list lang from resource packs:", e);
        }
        return result;
    }

    private static IResourceManager activeResourceManager() {
        if (BaniraCodex.serverInstance().key() != null) {
            return BaniraCodex.serverInstance().key().getDataPackRegistries().getResourceManager();
        }
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            return (IResourceManager) minecraftClass.getMethod("getResourceManager").invoke(minecraft);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void collectLanguageFiles(IResourcePack pack, String modId, Predicate<String> predicate, Map<String, JsonObject> result) {
        try {
            Collection<ResourceLocation> locations = pack.getResources(ResourcePackType.CLIENT_RESOURCES, modId, "lang", Integer.MAX_VALUE, predicate);
            for (ResourceLocation location : locations) {
                loadLanguage(ResourcePackType.CLIENT_RESOURCES, pack, location, result);
                loadLanguage(ResourcePackType.SERVER_DATA, pack, location, result);
            }
        } catch (Exception e) {
            LOGGER.trace("Failed to list lang from pack {}: {}", pack.getName(), e.getMessage());
        }
    }

    private static void loadLanguage(ResourcePackType packType, IResourcePack pack, ResourceLocation location, Map<String, JsonObject> result) {
        try (InputStreamReader reader = new InputStreamReader(pack.getResource(packType, location), StandardCharsets.UTF_8)) {
            String languageCode = languageCode(location);
            JsonObject json = JsonUtils.parseObject(reader);
            JsonObject existing = result.get(languageCode);
            if (existing == null) {
                result.put(languageCode, json);
            } else {
                JsonUtils.mergeInPlace(existing, json);
            }
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
