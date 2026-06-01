package xin.vanilla.banira.api;

import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * Stable entry point for dependent mods.
 * <p>
 * Loader-specific code should stay behind {@link BaniraPlatform}; callers should
 * prefer this facade over Forge/Fabric/NeoForge implementation classes.
 */
public final class Banira {
    private Banira() {
    }

    public static BaniraPlatform platform() {
        return BaniraPlatforms.get();
    }
}
