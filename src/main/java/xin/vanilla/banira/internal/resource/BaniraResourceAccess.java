package xin.vanilla.banira.internal.resource;

import com.google.gson.JsonObject;
import xin.vanilla.banira.internal.forge.platform.ForgeBaniraResourceService;

import java.util.Map;

/**
 * 内部资源访问门面；不同加载器/版本只需要替换这里后面的实现。
 */
public final class BaniraResourceAccess {
    private BaniraResourceAccess() {
    }

    public static Map<String, JsonObject> modLanguageFiles(String modId) {
        return ForgeBaniraResourceService.modLanguageFiles(modId);
    }
}
