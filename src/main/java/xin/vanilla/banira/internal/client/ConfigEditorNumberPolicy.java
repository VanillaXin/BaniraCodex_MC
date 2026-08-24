package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.common.config.ConfigEntryDescriptor;

/**
 * 配置编辑器的数值精度策略。
 */
final class ConfigEditorNumberPolicy {

    private ConfigEditorNumberPolicy() {
    }

    static double stepFor(ConfigEntryDescriptor.ConfigValueType type, double min, double max, int decimalPlaces) {
        if (type != ConfigEntryDescriptor.ConfigValueType.DOUBLE) {
            return 1D;
        }
        int precision = Math.max(0, decimalPlaces);
        return 1D / Math.pow(10D, precision);
    }
}
