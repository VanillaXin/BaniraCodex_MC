package xin.vanilla.banira.internal.fabric.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigCategoryTitleSpec;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class FabricConfigAdapterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void proxyResolvesNestedCategoryPaths() {
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(temporaryFolder.getRoot().toPath()));
        FabricConfigAdapter.register(NestedConfig.class, "test_mod");

        NestedConfigView view = FabricConfigAdapter.view(NestedConfig.class, NestedConfigView.class);

        SectionView section = view.section();
        assertEquals(2, section.count());
        assertSame(section, section.count(4));
        assertEquals(4, section.count());
        assertEquals(4, (int) view.holder().get("section.count"));
    }

    @Test
    public void registrationPreservesTooltipCategoryAndPermissionMetadata() {
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(temporaryFolder.getRoot().toPath()));
        FabricConfigAdapter.register(MetadataConfig.class, "test_mod");

        ConfigHolder holder = FabricConfigAdapter.getHolder(MetadataConfig.class);
        ConfigEntryDescriptor translated = holder.getDescriptor("section.translated");
        assertEquals(ConfigEntryDescriptor.ConfigTooltipGuiKind.TRANSLATION_KEY,
                translated.getTooltipGuiKind());
        assertEquals("test.config.translated", translated.getTooltipTranslationKey());
        assertEquals(ConfigEntry.EditPermissionPolicy.FIELD_OVERRIDE, translated.getEditPermissionPolicy());
        assertEquals(Integer.valueOf(3), translated.getFieldEditPermissionLevel());
        assertEquals("test:edit", translated.getFieldEditVirtualPermissionKey());

        ConfigEntryDescriptor localized = holder.getDescriptor("section.localized");
        assertEquals(ConfigEntryDescriptor.ConfigTooltipGuiKind.LOCALIZED_STATIC,
                localized.getTooltipGuiKind());
        assertEquals("中文说明", localized.getTooltipLocalizedByLang().get("zh_cn"));
        assertEquals("English help", localized.getTooltipLocalizedByLang().get("en_us"));

        ConfigEntryDescriptor literal = holder.getDescriptor("section.literal");
        assertEquals(ConfigEntryDescriptor.ConfigTooltipGuiKind.MULTILINE_LITERAL,
                literal.getTooltipGuiKind());
        assertEquals(2, literal.getTooltip().size());
        assertNull(literal.getFieldEditPermissionLevel());

        ConfigCategoryTitleSpec category = holder.getCategoryTitleSpec("section");
        assertEquals(ConfigCategoryTitleSpec.Kind.LOCALIZED_STATIC, category.getKind());
        assertEquals("配置分类", category.getLocalizedByLang().get("zh_cn"));
    }

    @Config(name = "fabric-adapter-test", type = ConfigScope.COMMON)
    public static class NestedConfig implements NestedConfigView, ConfigData {
        @ConfigEntry.Gui.CollapsibleObject
        private Section section = new Section();

        @Override
        public SectionView section() {
            return null;
        }

        @Override
        public ConfigHolder holder() {
            return null;
        }
    }

    public interface NestedConfigView {
        SectionView section();

        ConfigHolder holder();
    }

    public interface SectionView {
        int count();

        SectionView count(int value);
    }

    public static class Section {
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        private int count = 2;
    }

    @Config(name = "fabric-adapter-metadata-test", type = ConfigScope.COMMON)
    public static class MetadataConfig implements ConfigData {
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "配置分类", en_us = "Config section")
        private MetadataSection section = new MetadataSection();
    }

    public static class MetadataSection {
        @ConfigEntry.Gui.Tooltip(translationKey = "test.config.translated")
        @ConfigEntry.RequiresEditPermission(
                policy = ConfigEntry.EditPermissionPolicy.FIELD_OVERRIDE,
                permissionLevel = 3,
                virtualPermissionKey = "test:edit")
        private int translated = 1;

        @ConfigEntry.Gui.Tooltip(zh_cn = "中文说明", en_us = "English help")
        private boolean localized = true;

        @ConfigEntry.Gui.Tooltip({"第一行", "second line"})
        private String literal = "value";
    }
}
