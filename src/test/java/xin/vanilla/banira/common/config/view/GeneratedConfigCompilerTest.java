package xin.vanilla.banira.common.config.view;

import lombok.Data;
import org.junit.Test;
import org.spongepowered.asm.mixin.Mixin;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class GeneratedConfigCompilerTest {
    @Config(name = "compiler", generateView = true, viewUnbound = Config.UnboundAccess.DEFAULTS)
    @Data
    public static class CompilerConfig {
        @ConfigEntry.CollapsibleObject
        private Group group = new Group();

        @Data
        public static class Group {
            private int count = 3;
            private List<String> rules = Arrays.asList("a,b", "c");
        }
    }

    @Mixin(value = ConfigHolder.class, remap = false)
    public abstract static class CompilerMixin { }

    @Test
    public void generatedViewsCompileWithLombokAndMixinAndUseRealBinding() {
        AtomicReference<BaniraConfigHandle> current = new AtomicReference<>();
        BaniraPlatforms.install(new TestBaniraPlatform().configService(new BaniraConfigService() {
            public <T> void register(Class<T> type, String modId) { throw new UnsupportedOperationException(); }
            public <T> T view(Class<?> type, Class<T> view) { throw new UnsupportedOperationException(); }
            public BaniraConfigHandle handle(Class<?> type) {
                assertEquals(CompilerConfig.class, type);
                return current.get();
            }
        }));
        CompilerConfigView root = CompilerConfigView.get();
        CompilerConfigView.GroupView group = root.group();
        assertSame(root, CompilerConfigView.get());
        assertEquals(3, group.count());
        assertEquals(Arrays.asList("a,b", "c"), group.rules());
        assertEquals(3, new CompilerConfig().getGroup().getCount());

        ConfigViewBindingTest.RecordingHandle handle = ConfigViewBindingTest.counter("group.count", 8)
                .add("group.rules", List.class, Arrays.asList("x,y"), Arrays.asList("a,b","c"));
        current.set(handle);
        assertEquals(8, group.count());
        assertSame(group, group.count(9));
        assertEquals(Integer.valueOf(9), handle.get("group.count"));
        assertEquals(0, handle.saves);
        group.rules().clear();
        assertEquals(Arrays.asList("x,y"), group.rules());
        current.set(null);
        assertEquals(3, group.count());
    }
}
