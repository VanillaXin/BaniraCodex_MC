package xin.vanilla.banira.api.client.ui;

import net.minecraft.world.effect.MobEffectInstance;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.EffectUtils;
import java.util.Objects;

public final class UiGameElement {
    public enum Kind { OPTION, EFFECT, DIAGNOSTIC, PRESET, SPRITE }
    private final Kind kind;
    private final DropdownOption option;
    private final MobEffectInstance effect;
    private final String text;
    private final int fontSize, pressed;
    private final boolean wrap;
    private final EnumSeason season;
    private UiGameElement(Kind kind, DropdownOption option, MobEffectInstance effect, String text, int fontSize, boolean wrap, EnumSeason season, int pressed) {
        this.kind = kind; this.option = option; this.effect = effect; this.text = text; this.fontSize = fontSize; this.wrap = wrap; this.season = season; this.pressed = pressed;
    }
    public static UiGameElement option(DropdownOption option) { return new UiGameElement(Kind.OPTION, Objects.requireNonNull(option), null, "", 9, false, EnumSeason.AUTO, -1); }
    public static UiGameElement sprite(xin.vanilla.banira.client.data.Texture texture) {
        return new UiGameElement(Kind.SPRITE, new DropdownOption("", Objects.requireNonNull(texture), null), null, "", 9, false, EnumSeason.AUTO, -1);
    }
    public static UiGameElement effect(MobEffectInstance effect) { return new UiGameElement(Kind.EFFECT, null, EffectUtils.copyEffectInstance(Objects.requireNonNull(effect)), "", 9, false, EnumSeason.AUTO, -1); }
    public static UiGameElement preset(xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle style) { return new UiGameElement(Kind.PRESET, new DropdownOption(Objects.requireNonNull(style).name()), null, "", 9, false, EnumSeason.AUTO, -1); }
    public xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle preset() { return xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle.valueOf(option.value()); }
    public static UiGameElement diagnostic(String text, int fontSize, boolean wrap, EnumSeason season, int pressed) {
        return new UiGameElement(Kind.DIAGNOSTIC, null, null, Objects.requireNonNull(text).substring(0, Math.min(4096, text.length())), Math.max(4, Math.min(32, fontSize)), wrap, Objects.requireNonNull(season), pressed == 0 || pressed == 1 ? pressed : -1);
    }
    public Kind kind() { return kind; }
    public DropdownOption option() { return option; }
    public MobEffectInstance effect() { return effect == null ? null : EffectUtils.copyEffectInstance(effect); }
    public String text() { return text; }
    public int fontSize() { return fontSize; }
    public boolean wrap() { return wrap; }
    public EnumSeason season() { return season; }
    public int pressed() { return pressed; }
}
