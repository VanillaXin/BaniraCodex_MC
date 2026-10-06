package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ForgeManagedModConfigTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void unloadStopsOnlyOwnedNativeWatchers() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        java.util.Map<String, ForgeConfigSpec.ConfigValue<?>> values = new java.util.LinkedHashMap<>();
        values.put("value", builder.define("value", 1));
        ForgeConfigSpec spec = builder.build();
        ForgeManagedModConfig mod = newModConfig(spec);
        ForgeConfigValueStore backend = new ForgeConfigValueStore(spec, values);
        java.util.Map<ModConfig, ForgeConfigValueStore> backends = registry("BACKEND_BY_CONFIG");
        Path path = temporary.newFile(mod.getFileName()).toPath();
        Files.writeString(path, "value = 1\n", StandardCharsets.UTF_8);
        com.electronwill.nightconfig.core.file.FileWatcher owned = new com.electronwill.nightconfig.core.file.FileWatcher();
        com.electronwill.nightconfig.core.file.FileWatcher borrowed = new com.electronwill.nightconfig.core.file.FileWatcher();
        backends.put(mod, backend);
        try (ForgeConfigFile file = (ForgeConfigFile) backend.wrap(CommentedFileConfig.of(path))) {
            file.load();
            ForgeConfigAdapter.watchOwned(owned, mod, path, () -> {
            });
            ForgeConfigAdapter.unwatch(mod, path);
            assertThrows("Removing the final file must stop our watcher executor", IllegalStateException.class,
                    () -> owned.addWatch(path, () -> {
                    }));
            ForgeConfigAdapter.watch(borrowed, mod, path, () -> {
            });
            ForgeConfigAdapter.unwatch(mod, path);
            borrowed.addWatch(path, () -> {
            });
        } finally {
            ForgeConfigAdapter.unwatch(mod, path);
            owned.stop();
            borrowed.stop();
            backends.remove(mod);
            untrack(mod);
        }
    }

    @Test
    public void missingFileReleasesItsWriterBeforeTransactionalInitialization() throws Exception {
        Path target = temporary.getRoot().toPath().resolve("initial/config.toml");
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        java.util.Map<String, ForgeConfigSpec.ConfigValue<?>> values = new java.util.LinkedHashMap<>();
        values.put("value", builder.define("value", 1));
        ForgeConfigSpec spec = builder.build();
        ForgeConfigValueStore backend = new ForgeConfigValueStore(spec, values);
        try (CommentedFileConfig config = backend.wrap(ForgeManagedModConfig.bootstrap(target,
                temporary.getRoot().toPath().resolve("absent.toml")))) {
            config.load();
            assertEquals(1, ((Number) config.get("value")).intValue());
            config.set("value", 2);
            assertEquals(2, ((Number) new com.electronwill.nightconfig.toml.TomlParser()
                    .parse(Files.readString(target, StandardCharsets.UTF_8)).get("value")).intValue());
        }
    }

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
            watcher.removeWatch(path);
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

    @Test
    public void unloadKeepsTheFileWritableForTheEventAndFinalSave() throws Exception {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.define("value", 1);
        ForgeConfigSpec spec = builder.build();
        ForgeManagedModConfig mod = newModConfig(spec);
        Path path = temporary.newFile(mod.getFileName()).toPath();
        Files.writeString(path, "value = 1\n", StandardCharsets.UTF_8);
        try (ForgeConfigFile file = new ForgeConfigFile(CommentedFileConfig.of(path), spec, candidate -> {
        })) {
            file.load();
            java.lang.reflect.Method setData = ModConfig.class.getDeclaredMethod("setConfigData", com.electronwill.nightconfig.core.CommentedConfig.class);
            setData.setAccessible(true);
            setData.invoke(mod, file);
            mod.getHandler().unload(path.getParent(), mod);
            // Forge dispatches Unloading before its final save and only then clears the spec.
            file.set("value", 2);
            mod.save();
            file.set("value", 3);
            mod.save();
            assertEquals(3, ((Number) file.get("value")).intValue());
            assertTrue(Files.readString(path).contains("value = 3"));
        } finally {
            untrack(mod);
        }
    }

    @Test
    public void replacingOldFilePreservesTheNewReaderGateAndReleasesOnlyItsOwnFile() throws Exception {
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
        try (ForgeConfigFile old = (ForgeConfigFile) backend.wrap(CommentedFileConfig.of(path))) {
            old.load();
            spec.setConfig(old);
            try (ForgeConfigFile next = (ForgeConfigFile) backend.wrap(CommentedFileConfig.of(path))) {
                next.load();
                java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
                ForgeConfigReloadGate gate = new ForgeConfigReloadGate(() -> true, calls::incrementAndGet);
                gates.put(mod, gate);
                com.electronwill.nightconfig.core.file.FileWatcher.defaultInstance().addWatch(path, () -> {
                });
                ForgeConfigAdapter.releaseReplacedFile(spec, old, next);
                gate.run();
                assertEquals("The new reader must keep receiving events", 1, calls.get());
                assertSame(gate, gates.get(mod));
                assertThrows(IllegalStateException.class, old::load);
                spec.setConfig(next);
                backend.set("value", 3);
                assertEquals(3, ((Number) next.get("value")).intValue());
                ForgeConfigAdapter.releaseReplacedFile(spec, next, next);
                next.load();
                ForgeConfigAdapter.releaseReplacedFile(new ForgeConfigSpec.Builder().build(), next, null);
                next.load();
                ForgeConfigAdapter.releaseReplacedFile(spec, next, null);
                assertThrows(IllegalStateException.class, next::load);
                assertFalse(gates.containsKey(mod));
                gate.run();
                assertEquals(1, calls.get());
            }
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
        IModFileInfo owningFile = (IModFileInfo) java.lang.reflect.Proxy.newProxyInstance(
                IModFileInfo.class.getClassLoader(), new Class<?>[]{IModFileInfo.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isClientSideOnly")) return false;
                    throw new UnsupportedOperationException("Unused owning-file fixture method: " + method.getName());
                });
        IModInfo info = (IModInfo) java.lang.reflect.Proxy.newProxyInstance(IModInfo.class.getClassLoader(),
                new Class<?>[]{IModInfo.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getModId") || method.getName().equals("getNamespace")) return id;
                    if (method.getName().equals("getOwningFile")) return owningFile;
                    if (method.getName().equals("getVersion"))
                        return new org.apache.maven.artifact.versioning.DefaultArtifactVersion("1");
                    if (method.getName().equals("getConfig")) return java.lang.reflect.Proxy.newProxyInstance(
                            IModInfo.class.getClassLoader(), new Class<?>[]{net.minecraftforge.forgespi.language.IConfigurable.class},
                            (nested, accessor, keys) -> java.util.Optional.empty());
                    return null;
                });
        ModContainer container = new ModContainer(info) {
            @Override
            public boolean matches(Object mod) {
                return mod == this;
            }

            @Override
            public Object getMod() {
                return this;
            }
        };
        return new ForgeManagedModConfig(ModConfig.Type.SERVER, spec, container, id + ".toml");
    }

    @Test
    public void missingFileUsesDefaultConfigWithoutChangingIt() throws Exception {
        Path defaults = temporary.newFile("defaults.toml").toPath();
        String original = "# template\nitems = [\"a,b\", \"c\"]\n";
        Files.write(defaults, original.getBytes(StandardCharsets.UTF_8));
        Path target = temporary.getRoot().toPath().resolve("nested/config.toml");
        try (CommentedFileConfig file = ForgeManagedModConfig.bootstrap(target, defaults)) {
            file.load();
            assertEquals(java.util.Arrays.asList("a,b", "c"), file.get("items"));
            assertEquals(original, new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        }
        assertEquals(original, new String(Files.readAllBytes(defaults), StandardCharsets.UTF_8));
    }

    @Test
    public void existingConfigWinsOverDefaultTemplate() throws Exception {
        Path target = temporary.newFile("existing.toml").toPath();
        Path defaults = temporary.newFile("template.toml").toPath();
        Files.write(target, "value = 3\n".getBytes(StandardCharsets.UTF_8));
        Files.write(defaults, "value = 5\n".getBytes(StandardCharsets.UTF_8));
        try (CommentedFileConfig file = ForgeManagedModConfig.bootstrap(target, defaults)) {
            file.load();
            assertEquals(3, ((Number) file.get("value")).intValue());
            file.set("value", 9);
        }
        assertEquals("value = 3\n", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test
    public void missingTemplateCreatesAnEmptyFileForTransactionalInitialization() {
        Path target = temporary.getRoot().toPath().resolve("new/config.toml");
        try (CommentedFileConfig file = ForgeManagedModConfig.bootstrap(target,
                temporary.getRoot().toPath().resolve("absent.toml"))) {
            file.load();
            assertTrue(Files.isRegularFile(target));
            assertTrue(file.isEmpty());
        }
    }
}
