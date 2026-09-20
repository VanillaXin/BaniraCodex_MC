package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraftforge.common.ForgeConfigSpec;
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
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void retainedCommonCategoryFollowsReloadReplacementAndUnload() throws Exception { verify(false); }
    @Test public void retainedClientRootFollowsReloadReplacementAndUnload() throws Exception { verify(true); }

    private void verify(boolean client) throws Exception {
        Class<?> type = client ? ClientConfig.class : CommonConfig.class;
        String key = client ? "notificationLogMaxEntries" : "help.helpInfoNumPerPage";
        AtomicReference<BaniraConfigHandle> current = new AtomicReference<>();
        BaniraPlatforms.install(new TestBaniraPlatform().configService(new BaniraConfigService() {
            public <T> void register(Class<T> config, String modId) { throw new UnsupportedOperationException(); }
            public <T> T view(Class<?> config, Class<T> view) { throw new UnsupportedOperationException(); }
            public BaniraConfigHandle handle(Class<?> config) { return config == type ? current.get() : null; }
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
            first.backend.acceptReload();
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
        final ForgeConfigBackend backend;
        final CommentedFileConfig file;

        Fixture(Class<?> type, Path path) throws Exception {
            this.path = path;
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            List<ConfigEntryDescriptor> descriptors = new ArrayList<>();
            Map<String, ForgeConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
            Map<String, String> tooltips = new LinkedHashMap<>();
            Map<String, ConfigCategoryTitleSpec> titles = new LinkedHashMap<>();
            Method scan = ForgeConfigAdapter.class.getDeclaredMethod("buildFromClass", ForgeConfigSpec.Builder.class,
                    Class.class, String.class, List.class, Map.class, Map.class, Map.class);
            scan.setAccessible(true);
            scan.invoke(null, builder, type, "", descriptors, values, tooltips, titles);
            ForgeConfigSpec spec = builder.build();
            backend = new ForgeConfigBackend(spec, values);
            Config annotation = type.getAnnotation(Config.class);
            holder = ConfigHolder.create("fixture", annotation.name(), annotation.type(), backend, descriptors, tooltips, titles);
            backend.setHolder(holder);
            file = backend.wrap(CommentedFileConfig.builder(path).sync().build());
            file.load();
            spec.setConfig(file);
            holder.acceptInitialExternalLoad();
        }

        public void close() { file.close(); }
    }
}
