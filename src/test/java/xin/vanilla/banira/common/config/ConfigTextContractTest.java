package xin.vanilla.banira.common.config;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigTooltipGuiKind;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigTextContractTest {

    @Test
    public void tooltipTranslationKeyKeepsModScopedComponentMetadata() {
        ConfigEntryDescriptor desc = descriptorBuilder()
                .tooltipGuiKind(ConfigTooltipGuiKind.TRANSLATION_KEY)
                .tooltipTranslationKey("tooltip.entry")
                .build();

        Component component = ConfigEntryTooltipTexts.guiTooltipComponent(desc, "example_mod");

        assertTrue(ConfigEntryTooltipTexts.hasGuiTooltip(desc));
        assertEquals("tooltip.entry", component.text());
        assertEquals(EnumI18nType.WORD, component.i18nType());
        assertEquals("example_mod", component.modId());
    }

    @Test
    public void tooltipLocalizedAndLiteralTextProduceLiteralComponents() {
        ConfigEntryDescriptor localized = descriptorBuilder()
                .tooltipGuiKind(ConfigTooltipGuiKind.LOCALIZED_STATIC)
                .tooltipLocalizedByLang(Map.of("zh_cn", "本地化提示"))
                .build();
        ConfigEntryDescriptor literal = descriptorBuilder()
                .tooltip(List.of("line 1", "line 2"))
                .build();

        assertEquals("本地化提示", ConfigEntryTooltipTexts.guiTooltipComponent(localized, "example_mod").text());
        assertEquals("line 1\nline 2", ConfigEntryTooltipTexts.guiTooltipComponent(literal, "example_mod").text());
    }

    @Test
    public void emptyTooltipMetadataIsNotReportedAsGuiTooltip() {
        assertFalse(ConfigEntryTooltipTexts.hasGuiTooltip(descriptorBuilder()
                .tooltipGuiKind(ConfigTooltipGuiKind.TRANSLATION_KEY)
                .tooltipTranslationKey("")
                .build()));
        assertFalse(ConfigEntryTooltipTexts.hasGuiTooltip(descriptorBuilder()
                .tooltipGuiKind(ConfigTooltipGuiKind.LOCALIZED_STATIC)
                .tooltipLocalizedByLang(Map.of("zh_cn", ""))
                .build()));
        assertFalse(ConfigEntryTooltipTexts.hasGuiTooltip(descriptorBuilder()
                .tooltip(List.of())
                .build()));
    }

    @Test
    public void categoryTitleTranslationKeyKeepsModScopedComponentMetadata() {
        Component component = ConfigCategoryTitleTexts.categoryTitleComponent(
                ConfigCategoryTitleSpec.translationKey("category.main"), "example_mod", "fallback");

        assertEquals("category.main", component.text());
        assertEquals(EnumI18nType.WORD, component.i18nType());
        assertEquals("example_mod", component.modId());
    }

    @Test
    public void categoryTitleUsesLocalizedLiteralAndFallbacks() {
        assertEquals("分类标题", ConfigCategoryTitleTexts.categoryTitleComponent(
                ConfigCategoryTitleSpec.localized(Map.of("zh_cn", "分类标题")), "example_mod", "fallback").text());
        assertEquals("Literal Title", ConfigCategoryTitleTexts.categoryTitleComponent(
                ConfigCategoryTitleSpec.literal("Literal Title"), "example_mod", "fallback").text());
        assertEquals("fallback", ConfigCategoryTitleTexts.categoryTitleComponent(
                ConfigCategoryTitleSpec.translationKey(""), "example_mod", "fallback").text());
        assertEquals("fallback", ConfigCategoryTitleTexts.categoryTitleComponent(
                null, "example_mod", "fallback").text());
    }

    private static ConfigEntryDescriptor.ConfigEntryDescriptorBuilder descriptorBuilder() {
        return ConfigEntryDescriptor.builder()
                .path("entry")
                .displayName("Entry")
                .tooltip(List.of())
                .valueType(ConfigValueType.STRING)
                .defaultValue("");
    }
}
