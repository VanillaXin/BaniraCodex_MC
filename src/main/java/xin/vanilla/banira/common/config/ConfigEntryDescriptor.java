package xin.vanilla.banira.common.config;

import lombok.Builder;
import lombok.Getter;
import net.neoforged.neoforge.common.ModConfigSpec;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
     * 写入配置文件的注释行（TOML）；{@link ConfigTooltipGuiKind#TRANSLATION_KEY} 时为空列表
     */
    private final List<String> tooltip;

    /**
     * 配置编辑器悬浮提示的展示方式
     */
    @Builder.Default
    private final ConfigTooltipGuiKind tooltipGuiKind = ConfigTooltipGuiKind.MULTILINE_LITERAL;

    /**
     * {@link ConfigTooltipGuiKind#TRANSLATION_KEY} 时的模组翻译键
     */
    @Builder.Default
    private final String tooltipTranslationKey = "";

    /**
     * {@link ConfigTooltipGuiKind#LOCALIZED_STATIC} 时的硬编码文案（小写语言代码，如 {@code zh_cn} → 全文，可含换行）
     */
    @Builder.Default
    private final Map<String, String> tooltipLocalizedByLang = Collections.emptyMap();

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
    private final ModConfigSpec.ConfigValue<?> configValue;

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
        STRING_LIST,
        INTEGER_LIST,
        LONG_LIST,
        DOUBLE_LIST,
        BOOLEAN_LIST,
        ENUM_LIST
    }

    /**
     * 是否为「列表」类配置（多元素 TOML 数组 / 逗号分隔网络编码）。
     */
    public static boolean isListValueType(ConfigValueType t) {
        if (t == null) {
            return false;
        }
        switch (t) {
            case STRING_LIST:
            case INTEGER_LIST:
            case LONG_LIST:
            case DOUBLE_LIST:
            case BOOLEAN_LIST:
            case ENUM_LIST:
                return true;
            default:
                return false;
        }
    }

    public boolean isListType() {
        return isListValueType(valueType);
    }

    /**
     * 与 {@link xin.vanilla.banira.common.config.annotation.ConfigEntry.Gui.Tooltip} 三种写法对应
     */
    public enum ConfigTooltipGuiKind {
        /**
         * {@code translationKey}：不写文件注释，GUI 用翻译键
         */
        TRANSLATION_KEY,
        /**
         * 各语言字段：文件多行注释，GUI 用 {@link xin.vanilla.banira.common.util.Translator#pickLocalizedMapValue(String, java.util.Map)}
         */
        LOCALIZED_STATIC,
        /**
         * {@code value[]}：文件与 GUI 均为同一组文本行（GUI 多行合并）
         */
        MULTILINE_LITERAL,
        ;
    }
}
