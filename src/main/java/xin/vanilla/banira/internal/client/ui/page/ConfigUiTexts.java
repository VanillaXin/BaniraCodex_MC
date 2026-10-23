package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.config.*;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.Translator;

final class ConfigUiTexts {
    private ConfigUiTexts() { }

    static Component title(ConfigHolder holder, ConfigEntryDescriptor descriptor) {
        var translator = Translator.of(holder.getModId());
        String key = "config." + descriptor.getPath();
        String text = translator.translate(EnumI18nType.WORD, key);
        if (text.equals(translator.getKey(EnumI18nType.WORD, key))) {
            text = ConfigEntryTooltipTexts.guiTooltipComponent(descriptor, holder.getModId()).getString(Translator.getClientLanguage());
        }
        if (text.isBlank()) text = descriptor.getDisplayName();
        int newline = text.indexOf('\n');
        return BaniraComponent.get().literal(newline < 0 ? text : text.substring(0, newline));
    }

    static Component tooltip(ConfigHolder holder, ConfigEntryDescriptor descriptor) {
        String text = ConfigEntryTooltipTexts.guiTooltipComponent(descriptor, holder.getModId()).getString(Translator.getClientLanguage());
        StringBuilder result = new StringBuilder(text).append('\n').append(descriptor.getPath()).append('\n')
                .append(word("config_editor_default")).append(": ").append(descriptor.getDefaultValue());
        if (descriptor.getMinValue() != null && descriptor.getMaxValue() != null) {
            result.append('\n').append(word("config_editor_range")).append(": ")
                    .append(descriptor.getMinValue()).append(" .. ").append(descriptor.getMaxValue());
        }
        return BaniraComponent.get().literal(result.toString());
    }

    static Component heading(ConfigHolder holder) {
        var translator = Translator.of(holder.getModId());
        String name = translator.translate(EnumI18nType.WORD, "mod_name");
        if (name.equals(translator.getKey(EnumI18nType.WORD, "mod_name"))) name = holder.getModId();
        return BaniraComponent.get().literal(name + " / " + word("config_editor_title"));
    }

    static String word(String key) {
        return BaniraComponent.get().transClientAuto(key).getString(Translator.getClientLanguage());
    }
}
