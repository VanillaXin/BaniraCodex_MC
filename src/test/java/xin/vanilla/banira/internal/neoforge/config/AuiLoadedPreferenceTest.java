package xin.vanilla.banira.internal.neoforge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.Test;
import xin.vanilla.banira.internal.client.ui.UiBackendController;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AuiLoadedPreferenceTest {
    @Test
    public void loadedPreferenceOverridesStartupDefaultAndSwitchesOnTick() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ModConfigSpec.BooleanValue useAui = builder.define("useAui", true);
        ModConfigSpec spec = builder.build();
        AtomicInteger probes = new AtomicInteger();
        UiBackendController controller = new UiBackendController(useAui::get, () -> {
            probes.incrementAndGet();
            return true;
        });
        controller.tick();
        assertEquals(0, probes.get());
        CommentedConfig loaded = CommentedConfig.inMemory();
        loaded.set("useAui", false);
        LoadedConfigFixture.accept(spec, loaded);
        assertFalse(controller.wantsAui("page"));
        assertEquals(0, probes.get());
        useAui.set(true);
        controller.requestSwitch(useAui.get());
        assertFalse(controller.wantsAui("page"));
        controller.tick();
        assertTrue(controller.wantsAui("page"));
        assertEquals(1, probes.get());
    }
}
