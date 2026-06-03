package xin.vanilla.banira.platform.resource;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Loader/version-neutral access to mod resource-pack data used by common utilities.
 */
public interface BaniraResourceService {
    Map<String, JsonObject> modLanguageFiles(String modId);
}
