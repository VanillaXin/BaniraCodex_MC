package xin.vanilla.banira.client.data;

import lombok.Data;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.platform.BaniraPlatforms;

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
        return bgTertiary();
    }

    public int notificationWarningBorder() {
        return accent();
    }

    public int notificationWarningText() {
        return textPrimary();
    }

    public int notificationErrorBg() {
        return inputBgError();
    }

    public int notificationErrorBorder() {
        return error();
    }

    public int notificationErrorText() {
        return error();
    }

    public int notificationSuccessBg() {
        return bgSecondary();
    }

    public int notificationSuccessBorder() {
        return accent();
    }

    public int notificationSuccessText() {
        return accent();
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
        if (BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient()) {
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
    private static BaniraColorConfig builtinSummer() {
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
    private static BaniraColorConfig builtinAutumn() {
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
    private static BaniraColorConfig builtinWinter() {
        return new BaniraColorConfig()
                .accent(0xFF3D7AB8).accentHover(0xFFA0C8E8).accentFocused(0xFF78B0D8).accentPressed(0xFF2D6AA8)
                .bgPrimary(0xFFFFFCFD).bgSecondary(0xFFF5FAFF).bgSurface(0xFFF0F8FF).bgTertiary(0xFFE8F0F8).bgQuaternary(0xFFE0E8F5).bgDisabled(0xFFEEF2F8)
                .textPrimary(0xFF4A6B8B).textSecondary(0xFF6A8BA8).textHint(0xFFA8C0D8).textDisabled(0xFFB0C0D0)
                .border(0xFF78B0D8).borderHover(0xFF3D7AB8).borderFocused(0xFF2D6AA8).borderDisabled(0xFFD0D8E5)
                .error(0xFFB00020)
                .cursorLightPressedOverride(0xFF1D4A88);
    }

    private static BaniraColorConfig builtinNightSpring() {
        BaniraColorConfig c = new BaniraColorConfig()
                .accent(0xFFF0A8C0).accentHover(0xFFFFC8D8).accentFocused(0xFFFFB0D0).accentPressed(0xFFE090B0)
                .bgPrimary(0xFF1A1418).bgSecondary(0xFF221A20).bgSurface(0xFF2A2228).bgTertiary(0xFF322A30).bgQuaternary(0xFF3A3238).bgDisabled(0xFF282228)
                .textPrimary(0xFFF0D8E0).textSecondary(0xFFD0B0C0).textHint(0xFF988898).textDisabled(0xFF706870)
                .border(0xFF705868).borderHover(0xFFF0A8C0).borderFocused(0xFFFFB0D0).borderDisabled(0xFF484048)
                .error(0xFFFF7088)
                .cursorLightMainOverride(0xFFFFD0E0).cursorDarkMainOverride(0xFFF0A8C0)
                .cursorLightPressedOverride(0xFFFFA8C8).cursorDarkPressedOverride(0xFFE080A0);
        c.listItemTextOverride(0xFFFFE8F0);
        c.inputBgOverride(0xFF2E262C);
        c.inputBgErrorOverride(0xFF3A2830);
        return c;
    }

    private static BaniraColorConfig builtinNightSummer() {
        return new BaniraColorConfig()
                .accent(0xFF7BC87C).accentHover(0xFFB8E8B8).accentFocused(0xFF98D898).accentPressed(0xFF5AB85A)
                .bgPrimary(0xFF141A16).bgSecondary(0xFF1A221C).bgSurface(0xFF202A22).bgTertiary(0xFF283228).bgQuaternary(0xFF303A30).bgDisabled(0xFF222822)
                .textPrimary(0xFFE0F0E0).textSecondary(0xFFB0D0B0).textHint(0xFF789878).textDisabled(0xFF587058)
                .border(0xFF486848).borderHover(0xFF7BC87C).borderFocused(0xFF98D898).borderDisabled(0xFF384038)
                .error(0xFFFF7088)
                .cursorLightMainOverride(0xFFE8FFE8).cursorDarkMainOverride(0xFF7BC87C)
                .cursorLightPressedOverride(0xFFC8F0C8).cursorDarkPressedOverride(0xFF58A858)
                .listItemTextOverride(0xFFD8F8D8)
                .inputBgOverride(0xFF242E26)
                .inputBgErrorOverride(0xFF2E3830);
    }

    private static BaniraColorConfig builtinNightAutumn() {
        return new BaniraColorConfig()
                .accent(0xFFFFA868).accentHover(0xFFFFD0A8).accentFocused(0xFFFFC090).accentPressed(0xFFE88840)
                .bgPrimary(0xFF1A1612).bgSecondary(0xFF221C18).bgSurface(0xFF2A241E).bgTertiary(0xFF322C26).bgQuaternary(0xFF3A342E).bgDisabled(0xFF282220)
                .textPrimary(0xFFF0E0D0).textSecondary(0xFFD0B898).textHint(0xFF988878).textDisabled(0xFF706860)
                .border(0xFF685848).borderHover(0xFFFFA868).borderFocused(0xFFFFC090).borderDisabled(0xFF403830)
                .error(0xFFFF7088)
                .cursorLightMainOverride(0xFFFFF0D8).cursorDarkMainOverride(0xFFFFA868)
                .cursorLightPressedOverride(0xFFFFD0A0).cursorDarkPressedOverride(0xFFE07830)
                .listItemTextOverride(0xFFFFE8D8)
                .inputBgOverride(0xFF2E2820)
                .inputBgErrorOverride(0xFF382820);
    }

    private static BaniraColorConfig builtinNightWinter() {
        return new BaniraColorConfig()
                .accent(0xFF68A8E8).accentHover(0xFFA8D0F8).accentFocused(0xFF88C0F0).accentPressed(0xFF4890D8)
                .bgPrimary(0xFF12161C).bgSecondary(0xFF181C22).bgSurface(0xFF1E222A).bgTertiary(0xFF262A32).bgQuaternary(0xFF2E323A).bgDisabled(0xFF202428)
                .textPrimary(0xFFD8E8F8).textSecondary(0xFFA8C0D8).textHint(0xFF7898B8).textDisabled(0xFF587088)
                .border(0xFF486078).borderHover(0xFF68A8E8).borderFocused(0xFF88C0F0).borderDisabled(0xFF384050)
                .error(0xFFFF7088)
                .cursorLightMainOverride(0xFFE8F8FF).cursorDarkMainOverride(0xFF68A8E8)
                .cursorLightPressedOverride(0xFFB8D8F8).cursorDarkPressedOverride(0xFF4080C8)
                .listItemTextOverride(0xFFE0F0FF)
                .inputBgOverride(0xFF202830)
                .inputBgErrorOverride(0xFF283038);
    }
    // endregion
}
