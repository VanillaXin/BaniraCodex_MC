package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.forgespi.language.IModInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ForgeWatcherCacheTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void watcherReloadInvalidatesCachesOnlyWhileHoldingTheFileMonitor() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        java.util.Map<String, ForgeConfigSpec.ConfigValue<?>> values = new java.util.LinkedHashMap<>();
        values.put("value", builder.define("value", 1));
        ForgeConfigSpec spec = builder.build();
        ForgeManagedModConfig mod = newModConfig(spec);
        ForgeConfigValueStore backend = new ForgeConfigValueStore(spec, values);
        java.util.Map<ModConfig, ForgeConfigValueStore> backends = registry("BACKEND_BY_CONFIG");
        java.util.Map<ModConfig, ForgeConfigReloadGate> gates = registry("RELOAD_GATES");
        Path path = temporary.newFile(mod.getFileName()).toPath();
        Files.writeString(path, "value = 1\n", StandardCharsets.UTF_8);
        backends.put(mod, backend);
        try (ForgeConfigFile file = (ForgeConfigFile) backend.wrap(CommentedFileConfig.of(path))) {
            file.load();
            spec.setConfig(file);
            assertEquals(1, ((Number) backend.get("value")).intValue());
            IConfigSpec<?> observed = (IConfigSpec<?>) java.lang.reflect.Proxy.newProxyInstance(
                    IConfigSpec.class.getClassLoader(), new Class<?>[]{IConfigSpec.class}, (proxy, method, args) -> {
                        if (method.getName().equals("afterReload")) {
                            assertTrue("Watcher cache invalidation must hold the managed file monitor",
                                    Thread.holdsLock(file));
                        }
                        try {
                            return method.invoke(spec, args);
                        } catch (java.lang.reflect.InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    });
            java.lang.reflect.Field specField = ModConfig.class.getDeclaredField("spec");
            specField.setAccessible(true);
            specField.set(mod, observed);
            java.util.concurrent.atomic.AtomicInteger reloads = new java.util.concurrent.atomic.AtomicInteger();
            com.electronwill.nightconfig.core.file.FileWatcher watcher =
                    com.electronwill.nightconfig.core.file.FileWatcher.defaultInstance();
            ForgeConfigAdapter.watch(watcher, mod, path,
                    () -> ForgeManagedModConfig.reload(mod, file, reloads::incrementAndGet));
            ForgeConfigReloadGate registered = gates.get(mod);
            assertNotNull(registered);
            // Drive the registered gate directly so OS event timing cannot decide this regression.
            watcher.addWatch(path, () -> { });
            ForgeConfigWatch.remove(path);
            Files.writeString(path, "value = 2\n", StandardCharsets.UTF_8);
            assertFalse(Thread.holdsLock(file));
            registered.run();
            assertEquals("The managed load must invalidate the primed real Forge cache", 2,
                    ((Number) backend.get("value")).intValue());
            assertEquals(1, reloads.get());
            registered.run();
            assertEquals("An unchanged file must not dispatch again", 1, reloads.get());
        } finally {
            ForgeConfigAdapter.unwatch(mod, path);
            backends.remove(mod);
            untrack(mod);
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> java.util.Map<ModConfig, V> registry(String name) throws Exception {
        java.lang.reflect.Field field = ForgeConfigAdapter.class.getDeclaredField(name);
        field.setAccessible(true);
        return (java.util.Map<ModConfig, V>) field.get(null);
    }

    private static void untrack(ModConfig mod) {
        ConfigTracker.INSTANCE.fileMap().remove(mod.getFileName(), mod);
        ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER).remove(mod);
    }

    private ForgeManagedModConfig newModConfig(ForgeConfigSpec spec) {
        net.minecraftforge.fml.loading.FMLPaths.loadAbsolutePaths(temporary.getRoot().toPath());
        net.minecraftforge.fml.loading.FMLConfig.load();
        String id = "test_" + java.util.UUID.randomUUID().toString().replace("-", "");
        IModInfo info = (IModInfo) java.lang.reflect.Proxy.newProxyInstance(IModInfo.class.getClassLoader(),
                new Class<?>[]{IModInfo.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getModId") || method.getName().equals("getNamespace")) return id;
                    if (method.getName().equals("getVersion")) return new org.apache.maven.artifact.versioning.DefaultArtifactVersion("1");
                    if (method.getName().equals("getConfig")) return java.lang.reflect.Proxy.newProxyInstance(
                            IModInfo.class.getClassLoader(), new Class<?>[]{net.minecraftforge.forgespi.language.IConfigurable.class},
                            (nested, accessor, keys) -> java.util.Optional.empty());
                    return null;
                });
        ModContainer container = new ModContainer(info) {
            @Override public boolean matches(Object mod) { return mod == this; }
            @Override public Object getMod() { return this; }
        };
        return new ForgeManagedModConfig(ModConfig.Type.SERVER, spec, container, id + ".toml");
    }

}
