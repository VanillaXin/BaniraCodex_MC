package xin.vanilla.banira.api.client;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraLogoService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class BaniraLogosTest {

    @Test
    public void registerDelegatesToLoaderBackedLogoRegistry() {
        RecordingLogoService service = new RecordingLogoService();
        BaniraPlatforms.install(new TestBaniraPlatform().logoService(service));

        BaniraLogos.register("demo", () -> "logo.png");

        assertEquals("demo", service.modId);
        assertEquals("logo.png", service.logoFileSupplier.get());
    }

    private static final class RecordingLogoService implements BaniraLogoService {
        private String modId;
        private Supplier<String> logoFileSupplier;

        @Override
        public void register(String modId, Supplier<String> logoFileSupplier) {
            this.modId = modId;
            this.logoFileSupplier = logoFileSupplier;
        }

        @Override
        public void register(Function<String, String> logoFileFunction) {
        }
    }
}
