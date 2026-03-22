package xin.vanilla.banira.internal.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.common.enums.EnumSeason;

/**
 * 客户端专用配置（Forge CLIENT）
 * <p>
 * 运行时通过 {@link #get()} 返回的 {@link RootView} 读写 {@link ConfigHolder}（路径由代理按字段名推导）。
 */
@Config(name = "banira_codex-client", type = ModConfig.Type.CLIENT)
public class ClientConfig implements ConfigData {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "GUI 主题样式：AUTO 与界面「自动」一致时按日历季节；可固定为春夏秋冬之一以覆盖日历。",
            en_us = "GUI theme style: with screen season on Auto, uses calendar season unless you pick a fixed season here.")
    private EnumSeason guiThemeStyle = EnumSeason.AUTO;

    public ClientConfig() {
    }

    public static RootView get() {
        return ClientConfigAccess.root(ForgeConfigAdapter.getHolder(ClientConfig.class));
    }

    // region 运行时视图接口

    public interface RootView {
        EnumSeason guiThemeStyle();

        RootView guiThemeStyle(EnumSeason value);

        ConfigHolder holder();
    }

    // endregion 运行时视图接口
}
