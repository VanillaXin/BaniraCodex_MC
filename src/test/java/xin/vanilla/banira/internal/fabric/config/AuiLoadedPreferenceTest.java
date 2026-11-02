package xin.vanilla.banira.internal.fabric.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.internal.client.ui.UiBackendController;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.NoopConfigService;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AuiLoadedPreferenceTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void savedPreferenceOverridesDefaultAndRetainedViewFollowsReload() throws Exception {
        lombok.var directory = temporary.newFolder().toPath();
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(directory).configService(FabricBaniraConfigService.INSTANCE));
        try {
            FabricConfigAdapter.register(ClientConfig.class, "banira_codex");
            lombok.var holder = FabricConfigAdapter.getHolder(ClientConfig.class);
            lombok.var retained = ClientConfigView.get();
            lombok.var file = directory.resolve("banira_codex-client.toml");
            lombok.var external = new FabricConfigValueStore(file, holder.getDescriptors());
            external.set("useAui", false);
            external.save();
            FabricConfigAdapter.register(ClientConfig.class, "banira_codex");
            AtomicInteger probes = new AtomicInteger();
            lombok.var controller = new UiBackendController(retained::useAui, () -> {
                probes.incrementAndGet();
                return true;
            });
            controller.tick();
            assertFalse(controller.wantsAui("page"));
            assertEquals(0, probes.get());
            external.set("useAui", true);
            external.save();
            FabricConfigAdapter.register(ClientConfig.class, "banira_codex");
            controller.requestSwitch(retained.useAui());
            assertFalse(controller.wantsAui("page"));
            controller.tick();
            assertTrue(controller.wantsAui("page"));
            assertEquals(1, probes.get());
        } finally {
            BaniraPlatforms.install(new TestBaniraPlatform().configService(NoopConfigService.INSTANCE));
        }
    }
}
