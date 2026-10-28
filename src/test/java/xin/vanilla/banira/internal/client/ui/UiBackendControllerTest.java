package xin.vanilla.banira.internal.client.ui;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Test;
import xin.vanilla.banira.internal.compat.aui.AuiCapabilityContractTest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;

public class UiBackendControllerTest {
    private Object controller(boolean preference, BooleanSupplier capability) throws Exception {
        return controller(() -> preference, capability);
    }

    private Object controller(BooleanSupplier preference, BooleanSupplier capability) throws Exception {
        try {
            return AuiCapabilityContractTest.required("xin.vanilla.banira.internal.client.ui.UiBackendController")
                    .getConstructor(BooleanSupplier.class, BooleanSupplier.class)
                    .newInstance(preference, capability);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            throw new AssertionError("Controller construction must not read unloaded config", failure.getCause());
        }
    }

    private Object call(Object controller, String name, Class<?>[] types, Object... args) throws Exception {
        return controller.getClass().getMethod(name, types).invoke(controller, args);
    }

    private boolean flag(Object controller, String name, String id) throws Exception {
        return (boolean) call(controller, name, new Class<?>[]{String.class}, id);
    }

    @Test
    public void constructionAndIdleLifecycleDoNotReadUnloadedConfig() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger probes = new AtomicInteger();
        Object controller = controller(() -> {
            reads.incrementAndGet();
            throw new IllegalStateException("Cannot get config value before config is loaded");
        }, () -> {
            probes.incrementAndGet();
            return true;
        });
        call(controller, "revision", new Class<?>[0]);
        call(controller, "tick", new Class<?>[0]);
        call(controller, "reload", new Class<?>[0]);
        assertFalse(flag(controller, "usesAui", "page"));
        assertEquals(0, reads.get());
        assertEquals(0, probes.get());
    }

    @Test
    public void firstDemandReadsLoadedPreferenceOnce() throws Exception {
        for (boolean enabled : new boolean[]{false, true}) {
            boolean[] loaded = {false};
            AtomicInteger reads = new AtomicInteger();
            AtomicInteger probes = new AtomicInteger();
            Object controller = controller(() -> {
                assertTrue("Config read before loading", loaded[0]);
                reads.incrementAndGet();
                return enabled;
            }, () -> {
                probes.incrementAndGet();
                return true;
            });
            loaded[0] = true;
            for (int i = 0; i < 5; i++) assertEquals(enabled, flag(controller, "wantsAui", "page"));
            assertEquals(1, reads.get());
            assertEquals(enabled ? 1 : 0, probes.get());
        }
    }

    @Test
    public void setupSwitchOverridesUnresolvedPreference() throws Exception {
        for (boolean enabled : new boolean[]{false, true}) {
            Object controller = controller(() -> {
                fail("Setup already supplied the loaded preference");
                return false;
            }, () -> true);
            call(controller, "requestSwitch", new Class<?>[]{boolean.class}, enabled);
            call(controller, "tick", new Class<?>[0]);
            assertEquals(enabled, flag(controller, "wantsAui", "page"));
            assertFalse(flag(controller, "usesAui", "page"));
        }
    }

    @Test
    public void lateConfigFailuresRemainVisibleAndCanRetryAfterLoading() throws Exception {
        boolean[] loaded = {false};
        Object controller = controller(() -> {
            if (!loaded[0]) throw new IllegalStateException("Config not loaded");
            return true;
        }, () -> true);
        try {
            flag(controller, "wantsAui", "page");
            fail("Unexpected config failures must not be masked as native fallback");
        } catch (java.lang.reflect.InvocationTargetException failure) {
            assertTrue(failure.getCause() instanceof IllegalStateException);
        }
        loaded[0] = true;
        assertTrue(flag(controller, "wantsAui", "page"));
    }

    @Test
    public void realForgeConfigUsesLoadedValueInsteadOfStartupDefault() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.BooleanValue useAui = builder.define("useAui", true);
        ForgeConfigSpec spec = builder.build();
        AtomicInteger probes = new AtomicInteger();
        Object controller = controller(useAui::get, () -> {
            probes.incrementAndGet();
            return true;
        });
        CommentedConfig loaded = CommentedConfig.inMemory();
        loaded.set("useAui", false);
        spec.setConfig(loaded);
        assertFalse(flag(controller, "wantsAui", "page"));
        assertEquals(0, probes.get());
        useAui.set(true);
        call(controller, "requestSwitch", new Class<?>[]{boolean.class}, useAui.get());
        call(controller, "tick", new Class<?>[0]);
        assertTrue(flag(controller, "wantsAui", "page"));
        assertEquals(1, probes.get());
    }

    @Test
    public void userFalseNeverProbesOrClaimsAui() throws Exception {
        AtomicInteger probes = new AtomicInteger();
        Object controller = controller(false, () -> {
            probes.incrementAndGet();
            return true;
        });
        assertFalse(flag(controller, "wantsAui", "page"));
        assertFalse(flag(controller, "usesAui", "page"));
        assertEquals(0, probes.get());
    }

    @Test
    public void capabilityIsCachedAndPreferenceDoesNotClaimOwnership() throws Exception {
        AtomicInteger probes = new AtomicInteger();
        Object controller = controller(true, () -> {
            probes.incrementAndGet();
            return true;
        });
        for (int i = 0; i < 100; i++) assertTrue(flag(controller, "wantsAui", "page"));
        assertFalse(flag(controller, "usesAui", "page"));
        call(controller, "attach", new Class<?>[]{String.class}, "page");
        assertTrue(flag(controller, "usesAui", "page"));
        assertEquals(1, probes.get());
    }

    @Test
    public void failedCapabilityKeepsNativeWithoutChangingPreference() throws Exception {
        Object controller = controller(true, () -> false);
        assertFalse(flag(controller, "wantsAui", "page"));
        assertFalse(flag(controller, "usesAui", "page"));
    }

    @Test
    public void switchingIsDeferredAndReenableReprobes() throws Exception {
        AtomicInteger probes = new AtomicInteger();
        Object controller = controller(true, () -> {
            probes.incrementAndGet();
            return true;
        });
        call(controller, "attach", new Class<?>[]{String.class}, "page");
        call(controller, "requestSwitch", new Class<?>[]{boolean.class}, false);
        assertTrue(flag(controller, "usesAui", "page"));
        call(controller, "tick", new Class<?>[0]);
        assertFalse(flag(controller, "usesAui", "page"));
        call(controller, "requestSwitch", new Class<?>[]{boolean.class}, true);
        call(controller, "tick", new Class<?>[0]);
        assertTrue(flag(controller, "wantsAui", "page"));
        assertFalse(flag(controller, "usesAui", "page"));
        assertEquals(2, probes.get());
    }

    @Test
    public void surfaceFailureIsIsolatedUntilExplicitReload() throws Exception {
        Object controller = controller(true, () -> true);
        call(controller, "attach", new Class<?>[]{String.class}, "page");
        call(controller, "isolate", new Class<?>[]{String.class}, "page");
        assertFalse(flag(controller, "wantsAui", "page"));
        assertFalse(flag(controller, "usesAui", "page"));
        assertTrue(flag(controller, "wantsAui", "hud"));
        call(controller, "reload", new Class<?>[0]);
        assertTrue(flag(controller, "wantsAui", "page"));
    }

    @Test
    public void recoverableProbeFailureIsCachedAsNative() throws Exception {
        AtomicInteger probes = new AtomicInteger();
        Object controller = controller(true, () -> {
            probes.incrementAndGet();
            throw new NoClassDefFoundError("optional vendor dependency");
        });
        try {
            for (int i = 0; i < 100; i++) assertFalse(flag(controller, "wantsAui", "page"));
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new AssertionError("Recoverable optional dependency failure must use native UI", e.getCause());
        }
        assertEquals(1, probes.get());
    }

    @Test
    public void fatalVmErrorsAreNotHidden() throws Exception {
        Object controller = controller(true, () -> {
            throw new OutOfMemoryError("test sentinel");
        });
        try {
            flag(controller, "wantsAui", "page");
            fail("Fatal error was hidden");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof OutOfMemoryError);
        }
    }
}
