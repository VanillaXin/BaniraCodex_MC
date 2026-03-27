package xin.vanilla.banira.common.util;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;

import java.util.Locale;

/**
 * 枚举描述的 i18n 辅助：按物理分发选择客户端或服务端翻译。
 */
public final class EnumDescriptionHelper {

    private EnumDescriptionHelper() {
    }

    /**
     * 与 {@link #describeKey(Enum)} 对应的翻译组件。
     */
    public static Component transAutoDesc(String key) {
        if (EnvironmentUtils.isClient()) {
            return BaniraComponent.get().transClientAuto(key);
        }
        return BaniraComponent.get().transAuto(key);
    }

    /**
     * 根据枚举类型与常量名生成语言键：{@code enum_&lt;类名蛇形&gt;_&lt;常量小写&gt;}，
     * 类名会去掉前缀 {@code Enum}（若有）。
     */
    public static String describeKey(Enum<?> constant) {
        String sn = constant.getDeclaringClass().getSimpleName();
        if (sn.startsWith("Enum")) {
            sn = sn.substring(4);
        }
        String snake = StringUtils.toSnakeCase(sn);
        return "enum_" + snake + "_" + constant.name().toLowerCase(Locale.ROOT);
    }

    /**
     * 默认实现：{@code transAutoDesc(describeKey(constant))}。
     */
    public static Component describeEnum(Enum<?> constant) {
        return transAutoDesc(describeKey(constant));
    }
}
