package xin.vanilla.banira.internal.neoforge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigCategoryTitleSpec;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.config.CommonConfigView;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static org.junit.Assert.*;

public class ConfigViewLifecycleTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void retainedCommonCategoryFollowsReloadReplacementAndUnload() throws Exception {
        verify(false);
    }

    @Test
    public void retainedClientRootFollowsReloadReplacementAndUnload() throws Exception {
        verify(true);
    }

    @Test
    public void sealedNativeRecordMirrorsAtomicCommitsAndRejectsInvalidReloadBeforeUnload() throws Exception {
        net.neoforged.fml.loading.FMLPaths.loadAbsolutePaths(temporary.getRoot().toPath());
        try (Fixture fixture = new Fixture(CommonConfig.class, temporary.newFile("native.toml").toPath())) {
            String key = "help.helpInfoNumPerPage";
            xin.vanilla.banira.common.config.ConfigReadSnapshot before = fixture.holder.snapshotForRead(java.util.Collections.singleton(key));
            assertEquals(xin.vanilla.banira.common.config.ConfigCommitResult.APPLIED, fixture.holder.compareAndSetAll(
                    fixture.holder.snapshotForEdit(java.util.Collections.singleton(key)), java.util.Collections.singletonMap(key, 23),
                    xin.vanilla.banira.common.config.ConfigEditOrigin.API));
            assertEquals(23, ((Number) fixture.nativeFile.get(key)).intValue());
            fixture.nativeFile.save();
            assertEquals(23, ((Number) new TomlParser().parse(new String(Files.readAllBytes(fixture.path), StandardCharsets.UTF_8)).get(key)).intValue());
            assertEquals(10, ((Number) before.getValues().get(key)).intValue());
            CommentedConfig invalid = new TomlParser().parse(new String(Files.readAllBytes(fixture.path), StandardCharsets.UTF_8));
            invalid.set(key, "wrong type");
            byte[] external = TomlFormat.instance().createWriter().writeToString(invalid).getBytes(StandardCharsets.UTF_8);
            Files.write(fixture.path, external);
            assertThrows(com.electronwill.nightconfig.core.io.ParsingException.class, () -> fixture.managed.acceptConfig(fixture.loaded));
            assertEquals(23, ((Number) fixture.holder.get(key)).intValue());
            assertArrayEquals(external, Files.readAllBytes(fixture.path));
            fixture.managed.acceptConfig(null);
            assertThrows(IllegalStateException.class, () -> fixture.holder.snapshotForEdit(java.util.Collections.singleton(key)));
        }
    }

    private void verify(boolean client) throws Exception {
        net.neoforged.fml.loading.FMLPaths.loadAbsolutePaths(temporary.getRoot().toPath());
        Class<?> type = client ? ClientConfig.class : CommonConfig.class;
        String key = client ? "notificationLogMaxEntries" : "help.helpInfoNumPerPage";
        AtomicReference<BaniraConfigHandle> current = new AtomicReference<>();
        BaniraPlatforms.install(new TestBaniraPlatform().configService(new BaniraConfigService() {
            public <T> void register(Class<T> config, String modId) {
                throw new UnsupportedOperationException();
            }

            public <T> T view(Class<?> config, Class<T> view) {
                throw new UnsupportedOperationException();
            }

            public BaniraConfigHandle handle(Class<?> config) {
                return config == type ? current.get() : null;
            }
        }));
        try (Fixture first = new Fixture(type, temporary.newFile("first.toml").toPath());
             Fixture second = new Fixture(type, temporary.newFile("second.toml").toPath())) {
            current.set(first.holder);
            IntSupplier read;
            IntConsumer write;
            if (client) {
                ClientConfigView retained = ClientConfigView.get();
                read = retained::notificationLogMaxEntries;
                write = retained::notificationLogMaxEntries;
            } else {
                CommonConfigView.HelpView retained = CommonConfigView.get().help();
                read = retained::helpInfoNumPerPage;
                write = retained::helpInfoNumPerPage;
            }
            assertEquals(client ? 500 : 10, read.getAsInt());
            List<Integer> reloaded = new ArrayList<>();
            first.holder.onReloaded(paths -> reloaded.add(read.getAsInt()));
            CommentedConfig external = new TomlParser().parse(new String(Files.readAllBytes(first.path), StandardCharsets.UTF_8));
            external.set(key, 31);
            Files.write(first.path, TomlFormat.instance().createWriter().writeToString(external).getBytes(StandardCharsets.UTF_8));
            first.file.load();
            first.spec.afterReload();
            first.holder.acceptExternalReload();
            assertEquals(31, read.getAsInt());
            assertEquals(java.util.Collections.singletonList(31), reloaded);

            second.holder.set(key, 42);
            second.holder.save();
            current.set(second.holder);
            assertEquals(42, read.getAsInt());
            write.accept(43);
            assertEquals(31, ((Number) first.holder.get(key)).intValue());
            assertEquals(43, ((Number) second.holder.get(key)).intValue());
            second.holder.save();
            assertEquals(43, ((Number) new TomlParser().parse(new String(Files.readAllBytes(second.path), StandardCharsets.UTF_8)).get(key)).intValue());
            current.set(null);
            assertEquals(client ? 500 : 10, read.getAsInt());
            write.accept(44);
            assertEquals(43, ((Number) second.holder.get(key)).intValue());
            current.set(first.holder);
            assertEquals(31, read.getAsInt());
        } finally {
            current.set(null);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Path path;
        final ConfigHolder holder;
        final NeoForgeConfigValueStore backend;
        final CommentedFileConfig file;
        final CommentedFileConfig nativeFile;
        final ModConfigSpec spec;
        final NeoForgeManagedConfigSpec managed;
        final net.neoforged.fml.config.IConfigSpec.ILoadedConfig loaded;

        Fixture(Class<?> type, Path path) throws Exception {
            this.path = path;
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            List<ConfigEntryDescriptor> descriptors = new ArrayList<>();
            Map<String, ModConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
            Map<String, String> tooltips = new LinkedHashMap<>();
            Map<String, ConfigCategoryTitleSpec> titles = new LinkedHashMap<>();
            Method scan = NeoForgeConfigAdapter.class.getDeclaredMethod("buildFromClass", ModConfigSpec.Builder.class,
                    Class.class, String.class, List.class, Map.class, Map.class, Map.class);
            scan.setAccessible(true);
            scan.invoke(null, builder, type, "", descriptors, values, tooltips, titles);
            spec = builder.build();
            backend = new NeoForgeConfigValueStore(spec, values);
            Config annotation = type.getAnnotation(Config.class);
            holder = ConfigHolder.create("fixture", annotation.name(), annotation.type(), backend, descriptors, tooltips, titles);
            backend.setHolder(holder);
            nativeFile = CommentedFileConfig.builder(path).sync().build();
            nativeFile.load();
            spec.correct(nativeFile);
            nativeFile.save();
            managed = new NeoForgeManagedConfigSpec(spec, backend,
                    loaded -> LoadedConfigFixture.bind(spec, loaded));
            net.neoforged.neoforgespi.language.IModInfo info =
                    (net.neoforged.neoforgespi.language.IModInfo) java.lang.reflect.Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[]{net.neoforged.neoforgespi.language.IModInfo.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("getModId") || method.getName().equals("getNamespace"))
                                    return "fixture";
                                throw new UnsupportedOperationException(method.getName());
                            });
            net.neoforged.bus.api.IEventBus bus = net.neoforged.bus.api.BusBuilder.builder().build();
            net.neoforged.fml.ModContainer container = new net.neoforged.fml.ModContainer(info) {
                @Override
                public net.neoforged.bus.api.IEventBus getEventBus() {
                    return bus;
                }
            };
            java.lang.reflect.Constructor<net.neoforged.fml.config.ModConfig> constructor =
                    net.neoforged.fml.config.ModConfig.class.getDeclaredConstructor(
                            net.neoforged.fml.config.ModConfig.Type.class, net.neoforged.fml.config.IConfigSpec.class,
                            net.neoforged.fml.ModContainer.class, String.class, java.util.concurrent.locks.ReentrantLock.class);
            constructor.setAccessible(true);
            net.neoforged.fml.config.ModConfig config = constructor.newInstance(
                    net.neoforged.fml.config.ModConfig.Type.COMMON, managed, container, path.getFileName().toString(),
                    new java.util.concurrent.locks.ReentrantLock());
            Class<?> loadedType = Class.forName("net.neoforged.fml.config.LoadedConfig");
            java.lang.reflect.Constructor<?> loadedConstructor = loadedType.getDeclaredConstructor(
                    CommentedConfig.class, Path.class, net.neoforged.fml.config.ModConfig.class);
            loadedConstructor.setAccessible(true);
            loaded =
                    (net.neoforged.fml.config.IConfigSpec.ILoadedConfig) loadedConstructor.newInstance(nativeFile, path, config);
            java.lang.reflect.Field loadedField = net.neoforged.fml.config.ModConfig.class.getDeclaredField("loadedConfig");
            loadedField.setAccessible(true);
            loadedField.set(config, loaded);
            backend.bindModConfig(config);
            managed.validateSpec(config);
            managed.acceptConfig(loaded);
            file = backend.managedFile();
            holder.acceptInitialExternalLoad();
        }

        public void close() {
            file.close();
            nativeFile.close();
        }
    }
}
