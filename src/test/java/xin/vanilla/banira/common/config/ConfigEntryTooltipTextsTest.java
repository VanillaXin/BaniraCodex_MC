package xin.vanilla.banira.common.config;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigTooltipGuiKind;
import xin.vanilla.banira.BaniraLang;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigEntryTooltipTextsTest {
    static {
        BaniraPlatforms.install(new TestBaniraPlatform().mod(Banira.MOD_ID, BaniraLang.class));
    }

    @Test
    public void languageResourceOverridesLocalizedAnnotation() {
        ConfigEntryDescriptor descriptor = ConfigEntryDescriptor.builder()
                .path("notificationHud.mode")
                .tooltipGuiKind(ConfigTooltipGuiKind.LOCALIZED_STATIC)
                .tooltipLocalizedByLang(Collections.singletonMap("en_us", "Outdated description"))
                .build();
        assertEquals("Display mode", text(descriptor));
    }

    @Test
    public void explicitResourceKeyOverridesLiteralAnnotation() {
        ConfigEntryDescriptor descriptor = ConfigEntryDescriptor.builder()
                .path("other")
                .tooltipTranslationKey("config_editor_save_tooltip")
                .tooltip(Collections.singletonList("Outdated description"))
                .build();
        assertEquals("Save changes to the local config file", text(descriptor));
    }

    @Test
    public void missingResourceRetainsLocalizedAnnotation() {
        ConfigEntryDescriptor descriptor = ConfigEntryDescriptor.builder()
                .path("missing")
                .tooltipGuiKind(ConfigTooltipGuiKind.LOCALIZED_STATIC)
                .tooltipLocalizedByLang(Collections.singletonMap("en_us", "Annotation fallback"))
                .build();
        assertEquals("Annotation fallback", text(descriptor));
    }

    @Test
    public void descriptionCanComeEntirelyFromLanguageResource() {
        ConfigEntryDescriptor descriptor = ConfigEntryDescriptor.builder().path("notificationHud.mode").build();
        assertTrue(ConfigEntryTooltipTexts.hasGuiTooltip(descriptor, Banira.MOD_ID));
        assertEquals("Display mode", text(descriptor));
    }

    private String text(ConfigEntryDescriptor descriptor) {
        return ConfigEntryTooltipTexts.guiTooltipComponent(descriptor, Banira.MOD_ID)
                .getString("en_us", true, true);
    }
}
