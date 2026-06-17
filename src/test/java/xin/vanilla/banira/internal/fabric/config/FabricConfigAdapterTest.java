package xin.vanilla.banira.internal.fabric.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class FabricConfigAdapterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void proxyResolvesNestedCategoryPaths() {
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(temporaryFolder.getRoot().toPath()));
        FabricConfigAdapter.register(NestedConfig.class, "test_mod");

        Object raw = FabricConfigAdapter.get(NestedConfig.class);
        NestedConfigView view = (NestedConfigView) raw;

        SectionView section = view.section();
        assertEquals(2, section.count());
        assertSame(section, section.count(4));
        assertEquals(4, section.count());
        assertEquals(4, (int) view.holder().get("section.count"));
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
}
