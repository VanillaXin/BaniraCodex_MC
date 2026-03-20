package xin.vanilla.banira.common.config;

import lombok.Builder;
import lombok.Getter;
import net.minecraftforge.common.ForgeConfigSpec;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

import java.util.List;

/**
 * 配置项描述符，用于 GUI 渲染与验证
 */
@Getter
@Builder
public class ConfigEntryDescriptor {

    /**
     * 配置路径（如 "base.helpHeader"）
     */
    private final String path;

    /**
     * 显示名称（用于 GUI 标签）
     */
    private final String displayName;

    /**
     * 工具提示
     */
    private final List<String> tooltip;

    /**
     * 值类型
     */
    private final ConfigValueType valueType;

    /**
     * 默认值
     */
    private final Object defaultValue;

    /**
     * 最小值（数值类型）
     */
    private final Number minValue;

    /**
     * 最大值（数值类型）
     */
    private final Number maxValue;

    /**
     * 小数位数（DOUBLE 类型时有效，默认 2）
     */
    @Builder.Default
    private final int decimalPlaces = 2;

    /**
     * 枚举类（枚举类型时）
     */
    private final Class<? extends Enum<?>> enumClass;

    /**
     * 关联的 ConfigValue
     */
    private final ForgeConfigSpec.ConfigValue<?> configValue;

    /**
     * 修改该条目（服务端同步）时的权限策略
     */
    @Builder.Default
    private final ConfigEntry.EditPermissionPolicy editPermissionPolicy = ConfigEntry.EditPermissionPolicy.INHERIT;

    /**
     * {@link ConfigEntry.EditPermissionPolicy#FIELD_OVERRIDE} 时的权限等级
     */
    private final Integer fieldEditPermissionLevel;

    /**
     * {@link ConfigEntry.EditPermissionPolicy#FIELD_OVERRIDE} 时的虚拟权限完整键，可为 null 表示沿用全局虚拟键
     */
    private final String fieldEditVirtualPermissionKey;

    public enum ConfigValueType {
        STRING,
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        ENUM,
        STRING_LIST
    }
}
