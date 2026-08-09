package xin.vanilla.banira.internal.resource;

import com.google.gson.JsonObject;
import xin.vanilla.banira.internal.fabric.platform.FabricBaniraResourceService;

import java.util.Map;

/** 内部资源访问门面；不同加载器和版本只替换其后方实现。 */
public final class BaniraResourceAccess {
    private BaniraResourceAccess() {
    }

    public static Map<String, JsonObject> modLanguageFiles(String modId) {
        return FabricBaniraResourceService.modLanguageFiles(modId);
    }
}
