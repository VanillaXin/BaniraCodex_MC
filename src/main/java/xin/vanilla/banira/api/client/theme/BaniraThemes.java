package xin.vanilla.banira.api.client.theme;

import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.DateUtils;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 子 Mod 主题偏好注册入口。
 * <p>
 * 配置仍由子 Mod 自己保存；这里仅保存动态读取配置的函数，确保玩家修改后无需重启界面系统。
 */
public final class BaniraThemes {
    private static final Map<String, Supplier<BaniraThemeMode>> MOD_THEME_MODES =
            new ConcurrentHashMap<>();

    private BaniraThemes() {
    }

    public static void register(@Nonnull String modId,
                                @Nonnull Supplier<BaniraThemeMode> modeSupplier) {
        MOD_THEME_MODES.put(normalizeModId(modId), modeSupplier);
    }

    public static boolean unregister(@Nonnull String modId) {
        return MOD_THEME_MODES.remove(normalizeModId(modId)) != null;
    }

    @Nonnull
    public static BaniraThemeMode modeFor(@Nonnull String modId) {
        Supplier<BaniraThemeMode> supplier = MOD_THEME_MODES.get(normalizeModId(modId));
        if (supplier == null) {
            return BaniraThemeMode.FOLLOW_BANIRA;
        }
        BaniraThemeMode mode = supplier.get();
        return mode != null ? mode : BaniraThemeMode.FOLLOW_BANIRA;
    }

    /**
     * 返回可直接赋给 {@link BaniraScreen#season(EnumSeason)} 的季节值。
     */
    @Nonnull
    public static EnumSeason seasonFor(@Nonnull String modId) {
        return seasonFor(modeFor(modId));
    }

    @Nonnull
    public static EnumSeason seasonFor(@Nonnull BaniraThemeMode mode) {
        switch (mode) {
            case AUTO:
                // 使用具体季节可绕过 Banira 的固定全局主题设置。
                return DateUtils.getSeason();
            case SPRING:
                return EnumSeason.SPRING;
            case SUMMER:
                return EnumSeason.SUMMER;
            case AUTUMN:
                return EnumSeason.AUTUMN;
            case WINTER:
                return EnumSeason.WINTER;
            case FOLLOW_BANIRA:
            default:
                return EnumSeason.AUTO;
        }
    }

    public static <T extends BaniraScreen> T apply(@Nonnull T screen,
                                                   @Nonnull String modId) {
        screen.season(seasonFor(modId));
        return screen;
    }

    private static String normalizeModId(String modId) {
        String normalized = modId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        return normalized;
    }
}
