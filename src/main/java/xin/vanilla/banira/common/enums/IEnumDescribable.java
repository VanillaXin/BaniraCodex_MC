package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.common.data.Component;

/**
 * 枚举项的可翻译描述，用于下拉等 UI 的悬浮提示等场景。
 */
public interface IEnumDescribable {

    /**
     * 该项的说明文本（通常为 i18n 组件）。
     */
    Component enumDescription();
}
