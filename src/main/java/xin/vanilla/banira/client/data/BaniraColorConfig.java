package xin.vanilla.banira.client.data;

import lombok.Data;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.DateUtils;

import javax.annotation.Nullable;

/**
 * 统一主题配置
 */
@Data
@Accessors(chain = true, fluent = true)
public final class BaniraColorConfig {

    // region 语义化颜色（强调色系）
    /**
     * 强调色 - 主色，用于焦点、高亮等
     */
    private int accent;
    /**
     * 强调色-悬停
     */
    private int accentHover;
    /**
     * 强调色-焦点
     */
    private int accentFocused;
    /**
     * 强调色-按下
     */
    private int accentPressed;
    // endregion

    // region 背景色（层次由浅到深，避免复杂界面时颜色融合）
    /**
     * 主背景 - 最浅
     */
    private int bgPrimary;
    /**
     * 次背景
     */
    private int bgSecondary;
    /**
     * 表面色（面板、卡片等）
     */
    private int bgSurface;
    /**
     * 三级背景（滚动槽、分隔区等）
     */
    private int bgTertiary;
    /**
     * 四级背景（深色区域、悬浮层底层）
     */
    private int bgQuaternary;
    /**
     * 禁用背景
     */
    private int bgDisabled;
    // endregion

    // region 文字色
    /**
     * 主文字
     */
    private int textPrimary;
    /**
     * 次文字
     */
    private int textSecondary;
    /**
     * 提示/占位符
     */
    private int textHint;
    /**
     * 禁用文字
     */
    private int textDisabled;
    // endregion

    // region 边框色
    private int border;
    private int borderHover;
    private int borderFocused;
    private int borderDisabled;
    // endregion

    // region 其他
    /**
     * 错误色
     */
    private int error;

    /**
     * 列表项文字色覆盖
     */
    private int listItemTextOverride;
    // endregion

    // region 组件颜色（由语义色推导，可自定义覆盖）
    public int panelBg() {
        return bgSurface;
    }

    public int buttonBg() {
        return bgSurface;
    }

    public int buttonBgHover() {
        return accentHover;
    }

    public int buttonBgFocused() {
        return accentFocused;
    }

    public int buttonBgPressed() {
        return accentPressed;
    }

    public int buttonBgDisabled() {
        return bgDisabled;
    }

    public int buttonBorder() {
        return border;
    }

    public int buttonBorderHover() {
        return borderHover;
    }

    public int buttonBorderFocused() {
        return accent;
    }

    public int buttonBorderPressed() {
        return accentPressed;
    }

    public int buttonBorderDisabled() {
        return borderDisabled;
    }

    public int buttonText() {
        return textPrimary;
    }

    public int buttonTextHover() {
        return textPrimary;
    }

    public int buttonTextFocused() {
        return textPrimary;
    }

    public int buttonTextPressed() {
        return 0xE0FFFFFF;
    }

    public int buttonTextDisabled() {
        return textDisabled;
    }

    /**
     * 列表项文字色，保证在选项背景上有足够对比度（春季等浅色主题使用更深色）
     */
    public int listItemText() {
        return listItemTextOverride != 0 ? listItemTextOverride : textPrimary;
    }

    /**
     * 弹出项/下拉项未选中时的文字色，使用强调色提升可读性
     */
    public int popupItemText() {
        return accent;
    }

    /**
     * 弹出项/下拉项选中或悬停时的文字色，使用浅色保证在深色背景上可读
     */
    public int popupItemTextSelected() {
        return 0xE0FFFFFF;
    }

    public int inputText() {
        return textPrimary;
    }

    public int inputBg() {
        return 0xFFFFFFFF;
    }

    public int inputBgError() {
        return 0xFFFFEBEE;
    }

    public int inputTextUneditable() {
        return textSecondary;
    }

    public int inputHint() {
        return textHint;
    }

    public int inputCursor() {
        return accent;
    }

    public int inputBorder() {
        return border;
    }

    public int inputBorderFocused() {
        return accent;
    }

    public int inputBorderDisabled() {
        return borderDisabled;
    }

    public int scrollbarBg() {
        return bgTertiary;
    }

    /**
     * 滑块颜色，使用 accentFocused 以与槽背景 bgTertiary 形成对比
     */
    public int scrollbarThumb() {
        return accentFocused;
    }

    public int scrollbarThumbHover() {
        return accent;
    }

    public int popupBg() {
        return bgQuaternary;
    }

    public int popupBorder() {
        return borderHover;
    }

    /**
     * 弹出项悬停背景色
     */
    public int popupItemHover() {
        return (accentHover & 0x00FFFFFF) | 0xCC000000;
    }

    /**
     * 弹出项已选中背景色
     */
    public int popupItemSelected() {
        return (accentFocused & 0x00FFFFFF) | 0xAA000000;
    }

    /**
     * 多选时选中项左侧指示条颜色
     */
    public int popupItemSelectedBorder() {
        return accent;
    }

    /**
     * 鼠标光标 - 亮色背景下的主色（可覆盖，0 时使用 accent）
     */
    private int cursorLightMainOverride;

    public int cursorLightMain() {
        return cursorLightMainOverride != 0 ? cursorLightMainOverride : accent;
    }

    /**
     * 鼠标光标 - 亮色背景下的辅色
     */
    public int cursorLightSub() {
        return accentHover;
    }

    /**
     * 鼠标光标 - 暗色背景下的主色
     */
    private int cursorDarkMainOverride;

    /**
     * 鼠标光标 - 暗色背景下的辅色
     */
    public int cursorDarkSub() {
        return 0xE0FFFFFF;
    }

    /**
     * 鼠标光标 - 亮色背景下按键按下时的颜色
     */
    private int cursorLightPressedOverride;
    /**
     * 鼠标光标 - 暗色背景下按键按下时的颜色
     */
    private int cursorDarkPressedOverride;

    /**
     * 鼠标光标 - 暗色背景下的主色
     */
    public int cursorDarkMain() {
        return cursorDarkMainOverride != 0 ? cursorDarkMainOverride : accentHover;
    }

    /**
     * 鼠标光标 - 亮色背景下按键按下时的颜色
     */
    public int cursorLightPressed() {
        return cursorLightPressedOverride != 0 ? cursorLightPressedOverride : accentPressed;
    }

    /**
     * 鼠标光标 - 暗色背景下按键按下时的颜色
     */
    public int cursorDarkPressed() {
        return cursorDarkPressedOverride != 0 ? cursorDarkPressedOverride : accentFocused;
    }
    // endregion

    // region 季节预设

    /**
     * 春 - 樱花粉：白底微粉，层次由浅到深
     */
    public static BaniraColorConfig spring() {
        BaniraColorConfig c = new BaniraColorConfig()
                .accent(0xFFE8A0B0).accentHover(0xFFF5D0DC).accentFocused(0xFFF0B8C8).accentPressed(0xFFE090A8)
                .bgPrimary(0xFFFFFCFD).bgSecondary(0xFFFFF5F8).bgSurface(0xFFFFF0F5).bgTertiary(0xFFFFE8EF).bgQuaternary(0xFFFFE0E8).bgDisabled(0xFFF5EEF0)
                .textPrimary(0xFF8B4A5A).textSecondary(0xFFA67B8A).textHint(0xFFC8A8B5).textDisabled(0xFFB0B0B0)
                .border(0xFFF0B8C8).borderHover(0xFFE8A0B0).borderFocused(0xFFE090A0).borderDisabled(0xFFD8D0D2)
                .error(0xFFB00020)
                .cursorLightPressedOverride(0xFFB05068);
        c.listItemTextOverride(0xFF6B3A4A);
        return c;
    }

    /**
     * 夏 - 清新绿：白底微绿，清爽通透
     */
    public static BaniraColorConfig summer() {
        return new BaniraColorConfig()
                .accent(0xFF5BA85B).accentHover(0xFFA8D8A8).accentFocused(0xFF88C888).accentPressed(0xFF4A9A4A)
                .bgPrimary(0xFFFFFCFD).bgSecondary(0xFFF5FFF5).bgSurface(0xFFF0FFF0).bgTertiary(0xFFE8F5E8).bgQuaternary(0xFFE0F0E0).bgDisabled(0xFFEEF5EE)
                .textPrimary(0xFF2D5A2D).textSecondary(0xFF5A7B5A).textHint(0xFF98B898).textDisabled(0xFFA0B0A0)
                .border(0xFF88C888).borderHover(0xFF5BA85B).borderFocused(0xFF3D8A3D).borderDisabled(0xFFD0E0D0)
                .error(0xFFB00020)
                .cursorLightPressedOverride(0xFF2D7A2D);
    }

    /**
     * 秋 - 活力橙：白底暖橙，层次分明
     */
    public static BaniraColorConfig autumn() {
        return new BaniraColorConfig()
                .accent(0xFFE88A38).accentHover(0xFFF5C8A0).accentFocused(0xFFF0B070).accentPressed(0xFFD87A28)
                .bgPrimary(0xFFFFFCF8).bgSecondary(0xFFFFF5EB).bgSurface(0xFFFFF0E0).bgTertiary(0xFFFFE8D0).bgQuaternary(0xFFFFE0C0).bgDisabled(0xFFF5EDE5)
                .textPrimary(0xFF8B5A2D).textSecondary(0xFFA67B5A).textHint(0xFFC8A888).textDisabled(0xFFB0A898)
                .border(0xFFF0B070).borderHover(0xFFE88A38).borderFocused(0xFFD87A28).borderDisabled(0xFFE0D8D0)
                .error(0xFFB00020)
                .cursorLightPressedOverride(0xFFC05018);
    }

    /**
     * 冬 - 宝石蓝：白底冰蓝，清冽通透
     */
    public static BaniraColorConfig winter() {
        return new BaniraColorConfig()
                .accent(0xFF3D7AB8).accentHover(0xFFA0C8E8).accentFocused(0xFF78B0D8).accentPressed(0xFF2D6AA8)
                .bgPrimary(0xFFFFFCFD).bgSecondary(0xFFF5FAFF).bgSurface(0xFFF0F8FF).bgTertiary(0xFFE8F0F8).bgQuaternary(0xFFE0E8F5).bgDisabled(0xFFEEF2F8)
                .textPrimary(0xFF4A6B8B).textSecondary(0xFF6A8BA8).textHint(0xFFA8C0D8).textDisabled(0xFFB0C0D0)
                .border(0xFF78B0D8).borderHover(0xFF3D7AB8).borderFocused(0xFF2D6AA8).borderDisabled(0xFFD0D8E5)
                .error(0xFFB00020)
                .cursorLightPressedOverride(0xFF1D4A88);
    }

    public static BaniraColorConfig forSeason(@Nullable EnumSeason season) {
        if (season == null || season == EnumSeason.AUTO) {
            return forSeason(DateUtils.getSeason());
        }
        switch (season) {
            case SPRING:
                return spring();
            case SUMMER:
                return summer();
            case AUTUMN:
                return autumn();
            case WINTER:
                return winter();
            default:
                return winter();
        }
    }
    // endregion
}
