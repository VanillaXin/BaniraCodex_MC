package xin.vanilla.banira.client.data;

import lombok.Data;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.common.util.EnvironmentUtils;
import xin.vanilla.banira.internal.config.ClientConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 统一主题配置（数值可由资源包 themes/&lt;season&gt;.json 覆盖）
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
     * 警告色
     */
    private int warning;
    /**
     * 成功色
     */
    private int success;
    /**
     * 信息色
     */
    private int info;

    /**
     * 列表项文字色覆盖
     */
    private int listItemTextOverride;

    /**
     * 输入框背景覆盖（0 表示使用默认浅色底）
     */
    private int inputBgOverride;

    /**
     * 输入框错误背景覆盖（0 表示使用默认浅红底）
     */
    private int inputBgErrorOverride;

    /**
     * 悬浮提示是否使用纹理绘制，默认 true。AUTO 模式时生效。
     */
    private boolean tooltipUseTexture = true;
    // endregion

    public boolean tooltipUseTexture() {
        return tooltipUseTexture;
    }

    public BaniraColorConfig tooltipUseTexture(boolean v) {
        this.tooltipUseTexture = v;
        return this;
    }

    // region 深色界面上的悬停对比度（避免按钮/列表悬停底与浅色字贴太近）

    private static double srgbChannelToLinear(int channel) {
        double x = (channel & 0xFF) / 255.0;
        return x <= 0.04045 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4);
    }

    private static double relativeLuminanceRgb24(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0.2126 * srgbChannelToLinear(r) + 0.7152 * srgbChannelToLinear(g) + 0.0722 * srgbChannelToLinear(b);
    }

    /**
     * 根据表面亮度判断是否为深色 GUI（夜间主题等），用于悬停/焦点底色推导。
     */
    private boolean surfaceReadsAsDarkUi() {
        return relativeLuminanceRgb24(bgSurface & 0xFFFFFF) < 0.2;
    }

    private static int lerpRgbOpaque(int baseArgb, int targetRgb24, float t) {
        int br = (baseArgb >> 16) & 0xFF, bg = (baseArgb >> 8) & 0xFF, bb = baseArgb & 0xFF;
        int tr = (targetRgb24 >> 16) & 0xFF, tg = (targetRgb24 >> 8) & 0xFF, tb = targetRgb24 & 0xFF;
        int r = (int) (br + (tr - br) * t + 0.5f);
        int g = (int) (bg + (tg - bg) * t + 0.5f);
        int bl = (int) (bb + (tb - bb) * t + 0.5f);
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        bl = Math.min(255, Math.max(0, bl));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private int buttonHoverFillFromAccent(int accentSample) {
        return lerpRgbOpaque(0xFF000000 | (bgSurface & 0xFFFFFF), accentSample & 0xFFFFFF, 0.30f);
    }

    private int popupRowHoverFillFromAccent(int accentSample) {
        return lerpRgbOpaque(0xFF000000 | (bgQuaternary & 0xFFFFFF), accentSample & 0xFFFFFF, 0.40f);
    }

    // endregion

    // region 组件颜色（由语义色推导，可自定义覆盖）
    public int panelBg() {
        return bgSurface;
    }

    public int buttonBg() {
        return bgSurface;
    }

    public int buttonBgHover() {
        if (surfaceReadsAsDarkUi()) {
            return buttonHoverFillFromAccent(accentHover);
        }
        return accentHover;
    }

    public int buttonBgFocused() {
        if (surfaceReadsAsDarkUi()) {
            return buttonHoverFillFromAccent(accentFocused);
        }
        return accentFocused;
    }

    public int buttonBgPressed() {
        if (surfaceReadsAsDarkUi()) {
            return lerpRgbOpaque(0xFF000000 | (bgSurface & 0xFFFFFF), accentPressed & 0xFFFFFF, 0.42f);
        }
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
        if (surfaceReadsAsDarkUi()) {
            return 0xFFF5F5F7;
        }
        return textPrimary;
    }

    public int buttonTextFocused() {
        if (surfaceReadsAsDarkUi()) {
            return 0xFFF5F5F7;
        }
        return textPrimary;
    }

    public int buttonTextPressed() {
        return 0xE0FFFFFF;
    }

    public int buttonTextDisabled() {
        return textDisabled;
    }

    /**
     * 按钮左键长按进度「已填充」区域：需与作为轨道的 {@link #buttonBgHover()} 明显区分
     */
    public int buttonLongPressProgressFill() {
        int a = accent & 0xFFFFFF;
        if (surfaceReadsAsDarkUi()) {
            return lerpRgbOpaque(0xFF000000 | (bgSurface & 0xFFFFFF), a, 0.62f);
        }
        return 0xFF000000 | a;
    }

    /**
     * 按钮预置线条图标（加减号、箭头等，不含关闭红叉）默认态描边色；深色界面用浅色以免与深底融在一起。
     */
    public int buttonPresetIconColor() {
        if (surfaceReadsAsDarkUi()) {
            return textPrimary;
        }
        return 0xFF333333;
    }

    public int buttonPresetIconHoverColor() {
        if (surfaceReadsAsDarkUi()) {
            return 0xFFF5F5F7;
        }
        return 0xFF555555;
    }

    public int buttonPresetIconFocusedColor() {
        if (surfaceReadsAsDarkUi()) {
            return 0xFFF0F0F5;
        }
        return 0xFF444444;
    }

    public int buttonPresetIconPressedColor() {
        if (surfaceReadsAsDarkUi()) {
            return 0xFFD8D8E0;
        }
        return 0xFF222222;
    }

    public int buttonPresetIconDisabledColor() {
        if (surfaceReadsAsDarkUi()) {
            return textDisabled;
        }
        return 0xFFAAAAAA;
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
        return inputBgOverride != 0 ? inputBgOverride : 0xFFFFFFFF;
    }

    public int inputBgError() {
        return inputBgErrorOverride != 0 ? inputBgErrorOverride : 0xFFFFEBEE;
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

    // region 状态语义色（背景/边框/文字分开，便于输入校验、通知、徽标等复用）
    public int statusErrorBg() {
        return inputBgError();
    }

    public int statusErrorBorder() {
        return error();
    }

    public int statusErrorText() {
        return error();
    }

    public int statusWarningBg() {
        return bgTertiary();
    }

    public int statusWarningBorder() {
        return warning();
    }

    public int statusWarningText() {
        return warning();
    }

    public int statusSuccessBg() {
        return bgSecondary();
    }

    public int statusSuccessBorder() {
        return success();
    }

    public int statusSuccessText() {
        return success();
    }

    public int statusInfoBg() {
        return bgSecondary();
    }

    public int statusInfoBorder() {
        return info();
    }

    public int statusInfoText() {
        return info();
    }
    // endregion

    /**
     * 按语义 token 获取颜色。组件和页面绘制应走这里，避免绑定具体字段或派生方法。
     */
    public int color(@Nonnull BaniraColorToken token) {
        switch (token) {
            case ACCENT: return accent();
            case ACCENT_HOVER: return accentHover();
            case ACCENT_FOCUSED: return accentFocused();
            case ACCENT_PRESSED: return accentPressed();
            case BG_PRIMARY: return bgPrimary();
            case BG_SECONDARY: return bgSecondary();
            case BG_SURFACE: return bgSurface();
            case BG_TERTIARY: return bgTertiary();
            case BG_QUATERNARY: return bgQuaternary();
            case BG_DISABLED: return bgDisabled();
            case TEXT_PRIMARY: return textPrimary();
            case TEXT_SECONDARY: return textSecondary();
            case TEXT_HINT: return textHint();
            case TEXT_DISABLED: return textDisabled();
            case BORDER: return border();
            case BORDER_HOVER: return borderHover();
            case BORDER_FOCUSED: return borderFocused();
            case BORDER_DISABLED: return borderDisabled();
            case STATUS_ERROR: return error();
            case STATUS_WARNING: return warning();
            case STATUS_SUCCESS: return success();
            case STATUS_INFO: return info();
            case STATUS_ERROR_BG: return statusErrorBg();
            case STATUS_ERROR_BORDER: return statusErrorBorder();
            case STATUS_ERROR_TEXT: return statusErrorText();
            case STATUS_WARNING_BG: return statusWarningBg();
            case STATUS_WARNING_BORDER: return statusWarningBorder();
            case STATUS_WARNING_TEXT: return statusWarningText();
            case STATUS_SUCCESS_BG: return statusSuccessBg();
            case STATUS_SUCCESS_BORDER: return statusSuccessBorder();
            case STATUS_SUCCESS_TEXT: return statusSuccessText();
            case STATUS_INFO_BG: return statusInfoBg();
            case STATUS_INFO_BORDER: return statusInfoBorder();
            case STATUS_INFO_TEXT: return statusInfoText();
            case PANEL_BG: return panelBg();
            case LIST_ITEM_TEXT: return listItemText();
            case BUTTON_BG: return buttonBg();
            case BUTTON_BG_HOVER: return buttonBgHover();
            case BUTTON_BG_FOCUSED: return buttonBgFocused();
            case BUTTON_BG_PRESSED: return buttonBgPressed();
            case BUTTON_BG_DISABLED: return buttonBgDisabled();
            case BUTTON_BORDER: return buttonBorder();
            case BUTTON_BORDER_HOVER: return buttonBorderHover();
            case BUTTON_BORDER_FOCUSED: return buttonBorderFocused();
            case BUTTON_BORDER_PRESSED: return buttonBorderPressed();
            case BUTTON_BORDER_DISABLED: return buttonBorderDisabled();
            case BUTTON_TEXT: return buttonText();
            case BUTTON_TEXT_HOVER: return buttonTextHover();
            case BUTTON_TEXT_FOCUSED: return buttonTextFocused();
            case BUTTON_TEXT_PRESSED: return buttonTextPressed();
            case BUTTON_TEXT_DISABLED: return buttonTextDisabled();
            case BUTTON_ICON: return buttonPresetIconColor();
            case BUTTON_ICON_HOVER: return buttonPresetIconHoverColor();
            case BUTTON_ICON_FOCUSED: return buttonPresetIconFocusedColor();
            case BUTTON_ICON_PRESSED: return buttonPresetIconPressedColor();
            case BUTTON_ICON_DISABLED: return buttonPresetIconDisabledColor();
            case BUTTON_LONG_PRESS_PROGRESS_FILL: return buttonLongPressProgressFill();
            case INPUT_BG: return inputBg();
            case INPUT_BG_ERROR: return inputBgError();
            case INPUT_TEXT: return inputText();
            case INPUT_TEXT_UNEDITABLE: return inputTextUneditable();
            case INPUT_HINT: return inputHint();
            case INPUT_CURSOR: return inputCursor();
            case INPUT_BORDER: return inputBorder();
            case INPUT_BORDER_FOCUSED: return inputBorderFocused();
            case INPUT_BORDER_DISABLED: return inputBorderDisabled();
            case POPUP_BG: return popupBg();
            case POPUP_BORDER: return popupBorder();
            case POPUP_ITEM_TEXT: return popupItemText();
            case POPUP_ITEM_TEXT_SELECTED: return popupItemTextSelected();
            case POPUP_ITEM_HOVER: return popupItemHover();
            case POPUP_ITEM_SELECTED: return popupItemSelected();
            case POPUP_ITEM_SELECTED_BORDER: return popupItemSelectedBorder();
            case SCROLLBAR_BG: return scrollbarBg();
            case SCROLLBAR_THUMB: return scrollbarThumb();
            case SCROLLBAR_THUMB_HOVER: return scrollbarThumbHover();
            case NOTIFICATION_NORMAL_BG: return notificationNormalBg();
            case NOTIFICATION_NORMAL_BORDER: return notificationNormalBorder();
            case NOTIFICATION_NORMAL_TEXT: return notificationNormalText();
            case NOTIFICATION_WARNING_BG: return notificationWarningBg();
            case NOTIFICATION_WARNING_BORDER: return notificationWarningBorder();
            case NOTIFICATION_WARNING_TEXT: return notificationWarningText();
            case NOTIFICATION_ERROR_BG: return notificationErrorBg();
            case NOTIFICATION_ERROR_BORDER: return notificationErrorBorder();
            case NOTIFICATION_ERROR_TEXT: return notificationErrorText();
            case NOTIFICATION_SUCCESS_BG: return notificationSuccessBg();
            case NOTIFICATION_SUCCESS_BORDER: return notificationSuccessBorder();
            case NOTIFICATION_SUCCESS_TEXT: return notificationSuccessText();
            case CURSOR_LIGHT_MAIN: return cursorLightMain();
            case CURSOR_LIGHT_SUB: return cursorLightSub();
            case CURSOR_LIGHT_PRESSED: return cursorLightPressed();
            case CURSOR_DARK_MAIN: return cursorDarkMain();
            case CURSOR_DARK_SUB: return cursorDarkSub();
            case CURSOR_DARK_PRESSED: return cursorDarkPressed();
            default: throw new IllegalArgumentException("Unsupported Banira color token: " + token);
        }
    }

    /**
     * 为组件字段默认值提供语义取色，避免组件直接依赖具体主题字段。
     */
    public static int colorForSeason(@Nullable EnumSeason season, @Nonnull BaniraColorToken token) {
        return forSeason(season).color(token);
    }

    // region 通知条（按语义类型，由客户端根据当前主题计算）

    public int notificationNormalBg() {
        return popupBg();
    }

    public int notificationNormalBorder() {
        return popupBorder();
    }

    public int notificationNormalText() {
        return textPrimary();
    }

    public int notificationWarningBg() {
        return statusWarningBg();
    }

    public int notificationWarningBorder() {
        return statusWarningBorder();
    }

    public int notificationWarningText() {
        return statusWarningText();
    }

    public int notificationErrorBg() {
        return statusErrorBg();
    }

    public int notificationErrorBorder() {
        return statusErrorBorder();
    }

    public int notificationErrorText() {
        return statusErrorText();
    }

    public int notificationSuccessBg() {
        return statusSuccessBg();
    }

    public int notificationSuccessBorder() {
        return statusSuccessBorder();
    }

    public int notificationSuccessText() {
        return statusSuccessText();
    }
    // endregion

    /**
     * 弹出项悬停背景色
     */
    public int popupItemHover() {
        if (surfaceReadsAsDarkUi()) {
            return popupRowHoverFillFromAccent(accentHover);
        }
        return (accentHover & 0x00FFFFFF) | 0xCC000000;
    }

    /**
     * 弹出项已选中背景色
     */
    public int popupItemSelected() {
        if (surfaceReadsAsDarkUi()) {
            return popupRowHoverFillFromAccent(accentFocused);
        }
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

    // region 解析与加载

    /**
     * 将界面季节（含 AUTO、null）解析为具体春夏秋冬：显式季节优先，否则客户端配置的 guiThemeStyle，再否则日历季节。
     */
    @Nonnull
    public static EnumSeason resolveEffectiveSeason(@Nullable EnumSeason screenSeason) {
        if (screenSeason != null && screenSeason != EnumSeason.AUTO) {
            return screenSeason;
        }
        EnumSeason cfg = ClientConfig.get().guiThemeStyle();
        if (cfg != null && cfg != EnumSeason.AUTO) {
            return cfg;
        }
        return DateUtils.getSeason();
    }

    /**
     * 按「已解析的」具体季节取主题（资源包 + 内置回退）
     */
    @Nonnull
    public static BaniraColorConfig themeForConcreteSeason(@Nonnull EnumSeason concreteSeason) {
        if (EnvironmentUtils.isClient()) {
            return BaniraColorThemeLoader.get().resolve(concreteSeason);
        }
        return builtinForConcreteSeason(concreteSeason);
    }

    /**
     * 按界面季节取主题；AUTO/null 时经 {@link #resolveEffectiveSeason(EnumSeason)} 再加载。
     */
    @Nonnull
    public static BaniraColorConfig forSeason(@Nullable EnumSeason season) {
        return themeForConcreteSeason(resolveEffectiveSeason(season));
    }

    /**
     * 春/夏/秋/冬：与 {@link #themeForConcreteSeason(EnumSeason)} 相同（含资源包）
     */
    public static BaniraColorConfig spring() {
        return themeForConcreteSeason(EnumSeason.SPRING);
    }

    public static BaniraColorConfig summer() {
        return themeForConcreteSeason(EnumSeason.SUMMER);
    }

    public static BaniraColorConfig autumn() {
        return themeForConcreteSeason(EnumSeason.AUTUMN);
    }

    public static BaniraColorConfig winter() {
        return themeForConcreteSeason(EnumSeason.WINTER);
    }

    /**
     * 内置配色（资源缺失或解析失败时由 {@link BaniraColorThemeLoader} 使用）
     */
    static BaniraColorConfig builtinForConcreteSeason(@Nonnull EnumSeason season) {
        switch (season) {
            case SPRING:
                return builtinSpring();
            case SUMMER:
                return builtinSummer();
            case AUTUMN:
                return builtinAutumn();
            case WINTER:
            default:
                return builtinWinter();
        }
    }

    /**
     * 四季夜间内置配色（资源包可通过对应季节 JSON 的 {@code night} 对象覆盖）
     */
    public static BaniraColorConfig builtinNightForConcreteSeason(@Nonnull EnumSeason season) {
        switch (season) {
            case SPRING:
                return builtinNightSpring();
            case SUMMER:
                return builtinNightSummer();
            case AUTUMN:
                return builtinNightAutumn();
            case WINTER:
            default:
                return builtinNightWinter();
        }
    }

    /**
     * 春 - 樱花粉：白底微粉，层次由浅到深
     */
    private static BaniraColorConfig builtinSpring() {
        BaniraColorConfig c = new BaniraColorConfig()
                .accent(0xFFD96A88).accentHover(0xFFF1B4C5).accentFocused(0xFFE889A2).accentPressed(0xFFC95A76)
                .bgPrimary(0xFFFFFDFB).bgSecondary(0xFFF7FBF4).bgSurface(0xFFFFF6F8).bgTertiary(0xFFF0F5EC).bgQuaternary(0xFFF7E9EF).bgDisabled(0xFFF0E9EC)
                .textPrimary(0xFF5C3B48).textSecondary(0xFF6F7661).textHint(0xFFA9919C).textDisabled(0xFFAAA4A7)
                .border(0xFFD8B6C2).borderHover(0xFFC77D92).borderFocused(0xFF9CBF7A).borderDisabled(0xFFD8D2D4)
                .error(0xFFC63D4F).warning(0xFFC7862F).success(0xFF5D9A68).info(0xFF4F86C6)
                .cursorLightPressedOverride(0xFFA94866);
        c.listItemTextOverride(0xFF4E3340);
        return c;
    }

    /**
     * 夏 - 清新绿：白底微绿，清爽通透
     */
    private static BaniraColorConfig builtinSummer() {
        return new BaniraColorConfig()
                .accent(0xFF2F9D8F).accentHover(0xFF87D9C9).accentFocused(0xFF47B7A7).accentPressed(0xFF238477)
                .bgPrimary(0xFFF8FEFB).bgSecondary(0xFFF0FAF7).bgSurface(0xFFF8FBF2).bgTertiary(0xFFE6F3EE).bgQuaternary(0xFFEEF2DF).bgDisabled(0xFFEAF0EA)
                .textPrimary(0xFF244B45).textSecondary(0xFF52746C).textHint(0xFF8FAFA6).textDisabled(0xFF98A8A3)
                .border(0xFFA8CCC3).borderHover(0xFF2F9D8F).borderFocused(0xFFD6A642).borderDisabled(0xFFD2E0DC)
                .error(0xFFC94A4A).warning(0xFFD9922E).success(0xFF3C9B64).info(0xFF2E86C1)
                .cursorLightPressedOverride(0xFF1F766C);
    }

    /**
     * 秋 - 活力橙：白底暖橙，层次分明
     */
    private static BaniraColorConfig builtinAutumn() {
        return new BaniraColorConfig()
                .accent(0xFFC76D2E).accentHover(0xFFE6B17E).accentFocused(0xFF7F9B6D).accentPressed(0xFFA85828)
                .bgPrimary(0xFFFFFBF5).bgSecondary(0xFFF8F1E6).bgSurface(0xFFF4F5EA).bgTertiary(0xFFEDE6DA).bgQuaternary(0xFFE5EAD8).bgDisabled(0xFFF0EAE2)
                .textPrimary(0xFF4E4034).textSecondary(0xFF706555).textHint(0xFFA28F7A).textDisabled(0xFFA9A096)
                .border(0xFFC9B59F).borderHover(0xFFC76D2E).borderFocused(0xFF7F9B6D).borderDisabled(0xFFDAD2C8)
                .error(0xFFB84A45).warning(0xFFC9802F).success(0xFF6E8D56).info(0xFF557FA6)
                .cursorLightPressedOverride(0xFF9A4A22);
    }

    /**
     * 冬 - 宝石蓝：白底冰蓝，清冽通透
     */
    private static BaniraColorConfig builtinWinter() {
        return new BaniraColorConfig()
                .accent(0xFF4E8FD8).accentHover(0xFFA7C9F2).accentFocused(0xFF7A6ED0).accentPressed(0xFF346DAF)
                .bgPrimary(0xFFF9FCFF).bgSecondary(0xFFF3F8FC).bgSurface(0xFFF6F5FB).bgTertiary(0xFFE8F0F7).bgQuaternary(0xFFEDEAF6).bgDisabled(0xFFECEFF4)
                .textPrimary(0xFF30445D).textSecondary(0xFF61748C).textHint(0xFF96A8BC).textDisabled(0xFFA2AAB5)
                .border(0xFFB9CBE0).borderHover(0xFF4E8FD8).borderFocused(0xFF7A6ED0).borderDisabled(0xFFD4DCE6)
                .error(0xFFB9445D).warning(0xFFB6782D).success(0xFF4E9270).info(0xFF4E8FD8)
                .cursorLightPressedOverride(0xFF275F9D);
    }

    private static BaniraColorConfig builtinNightSpring() {
        BaniraColorConfig c = new BaniraColorConfig()
                .accent(0xFFF0A8C0).accentHover(0xFFFFC8D8).accentFocused(0xFFA8D08A).accentPressed(0xFFE090B0)
                .bgPrimary(0xFF181519).bgSecondary(0xFF201B20).bgSurface(0xFF28232A).bgTertiary(0xFF2F3030).bgQuaternary(0xFF382E36).bgDisabled(0xFF282428)
                .textPrimary(0xFFF2DDE5).textSecondary(0xFFC8B7C0).textHint(0xFF9A8E96).textDisabled(0xFF746D72)
                .border(0xFF66505D).borderHover(0xFFF0A8C0).borderFocused(0xFFA8D08A).borderDisabled(0xFF474047)
                .error(0xFFFF7088).warning(0xFFF0B560).success(0xFF8BCB8D).info(0xFF80B8F0)
                .cursorLightMainOverride(0xFFFFD0E0).cursorDarkMainOverride(0xFFF0A8C0)
                .cursorLightPressedOverride(0xFFFFA8C8).cursorDarkPressedOverride(0xFFE080A0);
        c.listItemTextOverride(0xFFFFE8F0);
        c.inputBgOverride(0xFF2E262C);
        c.inputBgErrorOverride(0xFF3A2830);
        return c;
    }

    private static BaniraColorConfig builtinNightSummer() {
        return new BaniraColorConfig()
                .accent(0xFF63D4C1).accentHover(0xFFB4EFE4).accentFocused(0xFFE0C86A).accentPressed(0xFF45B5A5)
                .bgPrimary(0xFF111918).bgSecondary(0xFF17211F).bgSurface(0xFF1E2927).bgTertiary(0xFF263330).bgQuaternary(0xFF30362A).bgDisabled(0xFF222928)
                .textPrimary(0xFFE2F4EF).textSecondary(0xFFAED1C8).textHint(0xFF7E9E96).textDisabled(0xFF60746F)
                .border(0xFF436A64).borderHover(0xFF63D4C1).borderFocused(0xFFE0C86A).borderDisabled(0xFF374341)
                .error(0xFFFF7480).warning(0xFFF0B860).success(0xFF7FD992).info(0xFF78B8F0)
                .cursorLightMainOverride(0xFFE8FFFA).cursorDarkMainOverride(0xFF63D4C1)
                .cursorLightPressedOverride(0xFFBDEFE6).cursorDarkPressedOverride(0xFF38A99B)
                .listItemTextOverride(0xFFDFF8F2)
                .inputBgOverride(0xFF222E2B)
                .inputBgErrorOverride(0xFF33272A);
    }

    private static BaniraColorConfig builtinNightAutumn() {
        return new BaniraColorConfig()
                .accent(0xFFFFA060).accentHover(0xFFFFC79A).accentFocused(0xFFA7C080).accentPressed(0xFFE58042)
                .bgPrimary(0xFF181512).bgSecondary(0xFF211B17).bgSurface(0xFF29231E).bgTertiary(0xFF312B25).bgQuaternary(0xFF333828).bgDisabled(0xFF27231F)
                .textPrimary(0xFFF1E0CE).textSecondary(0xFFCBBBA3).textHint(0xFF978A7A).textDisabled(0xFF70685F)
                .border(0xFF675746).borderHover(0xFFFFA060).borderFocused(0xFFA7C080).borderDisabled(0xFF403830)
                .error(0xFFFF7474).warning(0xFFFFB45C).success(0xFFA7C080).info(0xFF82A7D8)
                .cursorLightMainOverride(0xFFFFF0D8).cursorDarkMainOverride(0xFFFFA868)
                .cursorLightPressedOverride(0xFFFFD0A0).cursorDarkPressedOverride(0xFFE07830)
                .listItemTextOverride(0xFFFFE8D8)
                .inputBgOverride(0xFF2E2820)
                .inputBgErrorOverride(0xFF382820);
    }

    private static BaniraColorConfig builtinNightWinter() {
        return new BaniraColorConfig()
                .accent(0xFF7AB8F2).accentHover(0xFFB8D8FA).accentFocused(0xFFA8A0F0).accentPressed(0xFF4F91D0)
                .bgPrimary(0xFF11161D).bgSecondary(0xFF171D24).bgSurface(0xFF1F242E).bgTertiary(0xFF272D37).bgQuaternary(0xFF302F3D).bgDisabled(0xFF21262D)
                .textPrimary(0xFFDDEBFA).textSecondary(0xFFADC2D8).textHint(0xFF7E98B4).textDisabled(0xFF5D7186)
                .border(0xFF475D73).borderHover(0xFF7AB8F2).borderFocused(0xFFA8A0F0).borderDisabled(0xFF394451)
                .error(0xFFFF7088).warning(0xFFE8B86A).success(0xFF80CFA0).info(0xFF7AB8F2)
                .cursorLightMainOverride(0xFFEAF7FF).cursorDarkMainOverride(0xFF7AB8F2)
                .cursorLightPressedOverride(0xFFC5DFFF).cursorDarkPressedOverride(0xFF4D84C8)
                .listItemTextOverride(0xFFE4F0FF)
                .inputBgOverride(0xFF222B35)
                .inputBgErrorOverride(0xFF302832);
    }
    // endregion
}
