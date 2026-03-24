package xin.vanilla.banira.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.common.enums.EnumSeason;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 客户端全局主题管理器
 */
@OnlyIn(Dist.CLIENT)
public final class ClientThemeManager {

    @Nullable
    private static BaniraColorConfig defaultTheme;
    @Nullable
    private static EnumSeason defaultSeason;

    private ClientThemeManager() {
    }

    /**
     * 设置全局默认主题，非空时优先于 defaultSeason
     */
    public static void setDefaultTheme(@Nullable BaniraColorConfig theme) {
        defaultTheme = theme;
    }

    /**
     * 设置全局默认季节，theme 为空时生效
     */
    public static void setDefaultSeason(@Nullable EnumSeason season) {
        defaultSeason = season;
    }

    /**
     * 获取当前有效主题
     */
    @Nonnull
    public static BaniraColorConfig getEffectiveTheme() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen instanceof BaniraScreen) {
            return ((BaniraScreen) screen).getEffectiveTheme();
        }
        if (defaultTheme != null) {
            return defaultTheme;
        }
        return BaniraColorConfig.forSeason(defaultSeason);
    }
}
