package xin.vanilla.banira.internal.neoforge.platform;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * NeoForge 资源包与模组文件语言资源适配。
 */
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

    /**
     * 启动早期资源管理器尚未就绪时，直接从 NeoForge 已登记的模组文件读取语言。
     */
    private static void collectRegisteredModLanguages(String modId, Map<String, JsonObject> result) {
        try {
            IModFileInfo modFile = ModList.get().getModFileById(modId);
            if (modFile == null) return;
            collectModFileLanguages(modFile.getFile().getContents(), modId, result);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to list lang from registered mod file {}: {}", modId, throwable.getMessage());
        }
    }

    static void collectModFileLanguages(JarContents contents, String modId, Map<String, JsonObject> result) {
        String prefix = "assets/" + modId + "/lang/";
        Map<String, JarResource> languages = new TreeMap<>();
        contents.visitContent(prefix, (path, resource) -> {
            if (!path.startsWith(prefix) || path.indexOf('/', prefix.length()) >= 0) return;
            if (path.toLowerCase(Locale.ROOT).endsWith(".json")) languages.putIfAbsent(path, resource.retain());
        });
        languages.forEach((path, resource) -> loadBaseLanguage(path, resource, result));
    }

    private static void loadBaseLanguage(String path, JarResource resource, Map<String, JsonObject> result) {
        try (var reader = resource.bufferedReader(StandardCharsets.UTF_8)) {
            String languageCode = languageCode(path);
            JsonObject base = JsonUtils.parseObject(reader);
            JsonObject override = result.get(languageCode);
            if (override != null) JsonUtils.mergeInPlace(base, override);
            result.put(languageCode, base);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to load language file {}: {}", path, throwable.getMessage());
        }
    }

    private static ResourceManager activeResourceManager() {
        if (BaniraServerRuntime.isRunning()) {
            return BaniraServerRuntime.server().getResourceManager();
        }
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            return (ResourceManager) minecraftClass.getMethod("getResourceManager").invoke(minecraft);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void loadLanguage(IoSupplier<InputStream> supplier, Identifier location,
                                     Map<String, JsonObject> result) {
        try (InputStreamReader reader = new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)) {
            String languageCode = languageCode(location.getPath());
            JsonObject json = JsonUtils.parseObject(reader);
            JsonObject existing = result.get(languageCode);
            if (existing == null) result.put(languageCode, json);
            else JsonUtils.mergeInPlace(existing, json);
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to load language file {}: {}", location, throwable.getMessage());
        }
    }

    private static String languageCode(String path) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.substring(0, name.length() - 5).toLowerCase(Locale.ROOT);
    }
}
