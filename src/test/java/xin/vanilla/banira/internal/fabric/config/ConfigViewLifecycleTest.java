package xin.vanilla.banira.internal.fabric.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.internal.config.*;
import xin.vanilla.banira.platform.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static org.junit.Assert.*;

public class ConfigViewLifecycleTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void retainedCommonCategoryFollowsFileReloadAndRebinding() throws Exception { verify(false); }
    @Test public void retainedClientFollowsFileReloadAndRebinding() throws Exception { verify(true); }

    private void verify(boolean client) throws Exception {
        Path directory = temporary.newFolder().toPath();
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(directory)
                .configService(FabricBaniraConfigService.INSTANCE));
        Class<?> type = client ? ClientConfig.class : CommonConfig.class;
        String key = client ? "notificationLogMaxEntries" : "help.helpInfoNumPerPage";
        String fileName = client ? "banira_codex-client.toml" : "banira_codex-common.toml";
        FabricConfigAdapter.register(type, "banira_codex");
        ConfigHolder original = FabricConfigAdapter.getHolder(type);
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
        int initial = read.getAsInt();
        Path file = directory.resolve(fileName);
        for (int i = 0; i < 20; i++) {
            FabricConfigValueStore external = new FabricConfigValueStore(file, original.getDescriptors());
            external.set(key, 30 + i);
            external.save();
            FabricConfigAdapter.register(type, "banira_codex");
            assertNotSame(original, FabricConfigAdapter.getHolder(type));
            assertEquals(30 + i, read.getAsInt());
            assertEquals(initial, ((Number) original.get(key)).intValue());
        }
        write.accept(61);
        ConfigHolder latest = FabricConfigAdapter.getHolder(type);
        latest.save();
        assertEquals(61, ((Number) new FabricConfigValueStore(file, latest.getDescriptors()).get(key)).intValue());
        BaniraPlatforms.install(new TestBaniraPlatform().configService(NoopConfigService.INSTANCE));
        assertEquals(initial, read.getAsInt());
        write.accept(77);
        assertEquals(61, ((Number) latest.get(key)).intValue());
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(directory)
                .configService(FabricBaniraConfigService.INSTANCE));
        assertEquals(61, read.getAsInt());
    }

    @Config(name = "typed-fixture", generateView = true, viewUnbound = Config.UnboundAccess.DEFAULTS)
    public static class TypedFixture {
        @ConfigEntry.Gui.CollapsibleObject private Group group = new Group();
        public static class Group {
            private List<String> rules = Arrays.asList("a,b", "c");
            @ConfigEntry.BoundedDouble(min = 0, max = 10) private double rate = 0.002;
            private long count = 9000000000L;
            private Mode mode = Mode.FIRST;
            private String title = "";
        }
    }
    public enum Mode { FIRST, SECOND }

    @Test public void realBackendPreservesTypedValuesAndListSnapshotsAcrossRestart() throws Exception {
        Path directory = temporary.newFolder().toPath();
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(directory)
                .configService(FabricBaniraConfigService.INSTANCE));
        FabricConfigAdapter.register(TypedFixture.class, "test");
        TypedFixtureView root = TypedFixtureView.get();
        TypedFixtureView.GroupView retained = root.group();
        List<String> values = new ArrayList<>(Arrays.asList("tick, clazz -> tick >= 5", "a,b"));
        retained.rules(values).rate(0.125).count(9100000000L).mode(Mode.SECOND);
        String title = String.join("", Collections.nCopies(500, "long, # \"quoted\"\n"));
        retained.title(title);
        values.clear();
        retained.rules().clear();
        assertEquals(2, retained.rules().size());
        Path file = directory.resolve("typed-fixture.toml");
        byte[] before = Files.readAllBytes(file);
        assertTrue(new String(before, StandardCharsets.UTF_8).contains("0.125"));
        root.handle().save();
        FabricConfigAdapter.register(TypedFixture.class, "test");
        assertEquals(Arrays.asList("tick, clazz -> tick >= 5", "a,b"), retained.rules());
        assertEquals(0.125, retained.rate(), 0.0);
        assertEquals(9100000000L, retained.count());
        assertEquals(Mode.SECOND, retained.mode());
        assertEquals(title, retained.title());
        try { retained.rate(-1); fail("invalid value accepted"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(0.125, retained.rate(), 0.0);
    }
}
