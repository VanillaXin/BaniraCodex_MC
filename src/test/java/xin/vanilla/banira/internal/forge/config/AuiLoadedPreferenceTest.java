package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Test;
import xin.vanilla.banira.internal.client.ui.UiBackendController;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AuiLoadedPreferenceTest {
    @Test
    public void loadedPreferenceOverridesStartupDefaultAndSwitchesOnTick() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.BooleanValue useAui = builder.define("useAui", true);
        ForgeConfigSpec spec = builder.build();
        AtomicInteger probes = new AtomicInteger();
        UiBackendController controller = new UiBackendController(useAui::get, () -> {
            probes.incrementAndGet();
            return true;
        });
        controller.tick();
        assertEquals(0, probes.get());
        CommentedConfig loaded = CommentedConfig.inMemory();
        loaded.set("useAui", false);
        spec.setConfig(loaded);
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
