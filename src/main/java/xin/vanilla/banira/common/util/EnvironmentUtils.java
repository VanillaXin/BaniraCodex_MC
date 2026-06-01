package xin.vanilla.banira.common.util;

import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * Loader-neutral runtime environment helpers.
 */
public final class EnvironmentUtils {
    private EnvironmentUtils() {
    }

    public enum PhysicalSide {
        CLIENT,
        DEDICATED_SERVER,
        UNKNOWN
    }

    public static PhysicalSide side() {
        if (!BaniraPlatforms.isInstalled()) {
            return PhysicalSide.UNKNOWN;
        }
        if (BaniraPlatforms.get().isClient()) {
            return PhysicalSide.CLIENT;
        }
        if (BaniraPlatforms.get().isDedicatedServer()) {
            return PhysicalSide.DEDICATED_SERVER;
        }
        return PhysicalSide.UNKNOWN;
    }

    public static boolean isClient() {
        return BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient();
    }

    public static boolean isDedicatedServer() {
        return BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isDedicatedServer();
    }

    public static boolean isProduction() {
        return BaniraPlatforms.isInstalled() && !BaniraPlatforms.get().isDevelopment();
    }

    public static boolean isDevelopment() {
        return BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isDevelopment();
    }
}
