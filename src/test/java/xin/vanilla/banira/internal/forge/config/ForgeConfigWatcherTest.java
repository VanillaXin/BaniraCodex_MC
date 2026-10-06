package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.internal.mixin.injections.ForgeConfigWatcherMixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ForgeConfigWatcherTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void managedWatcherDoesNotInvalidateTheCacheAgainOutsideTheFileMonitor() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.ConfigValue<Integer> value = builder.define("value", 1);
        ForgeConfigSpec spec = builder.build();
        Path path = temporary.newFile("managed.toml").toPath();
        Files.write(path, "value = 1\n".getBytes(StandardCharsets.UTF_8));
        try (ForgeConfigFile file = new ForgeConfigFile(CommentedFileConfig.of(path), spec, candidate -> {
        })) {
            file.load();
            spec.setConfig(file);
            assertEquals(Integer.valueOf(1), value.get());
            Files.write(path, "value = 2\n".getBytes(StandardCharsets.UTF_8));
            file.load();
            assertNull("Load must invalidate the old cache", cached(value));
            assertEquals(Integer.valueOf(2), value.get());
            assertFalse(Thread.holdsLock(file));
            reload(file, spec);
            assertEquals("The native watcher must not clear the newly primed cache again", 2, cached(value));
        }
    }

    @Test
    public void unmanagedWatcherStillInvalidatesItsCache() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.ConfigValue<Integer> value = builder.define("value", 1);
        ForgeConfigSpec spec = builder.build();
        try (CommentedFileConfig file = CommentedFileConfig.of(temporary.newFile("ordinary.toml"))) {
            file.set("value", 1);
            spec.setConfig(file);
            assertEquals(Integer.valueOf(1), value.get());
            file.set("value", 2);
            reload(file, spec);
            assertNull(cached(value));
            assertEquals(Integer.valueOf(2), value.get());
        }
    }

    @Test
    public void aManagedFileCannotSuppressAnotherSpecsInvalidation() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.ConfigValue<Integer> value = builder.define("value", 1);
        ForgeConfigSpec spec = builder.build();
        com.electronwill.nightconfig.core.CommentedConfig data =
                com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        data.set("value", 1);
        spec.setConfig(data);
        assertEquals(Integer.valueOf(1), value.get());
        Path path = temporary.newFile("foreign.toml").toPath();
        try (ForgeConfigFile file = new ForgeConfigFile(CommentedFileConfig.of(path),
                new ForgeConfigSpec.Builder().build(), candidate -> {
        })) {
            reload(file, spec);
            assertNull(cached(value));
        }
    }

    private static Object cached(ForgeConfigSpec.ConfigValue<?> value) throws Exception {
        Field field = ForgeConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        field.setAccessible(true);
        return field.get(value);
    }

    private static void reload(CommentedFileConfig file, ForgeConfigSpec spec) throws Exception {
        ForgeConfigWatcherMixin watcher = new ForgeConfigWatcherMixin() {
        };
        Field field = ForgeConfigWatcherMixin.class.getDeclaredField("commentedFileConfig");
        field.setAccessible(true);
        field.set(watcher, file);
        Method method = ForgeConfigWatcherMixin.class.getDeclaredMethod("banira$watcherReload", ForgeConfigSpec.class);
        method.setAccessible(true);
        method.invoke(watcher, spec);
    }
}
