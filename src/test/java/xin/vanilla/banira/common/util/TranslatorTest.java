package xin.vanilla.banira.common.util;

import net.minecraftforge.fml.common.Mod;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.*;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class TranslatorTest {

    @Test
    public void annotationModIdWinsOverInstalledPlatform() {
        BaniraPlatforms.install(new WrongModIdPlatform());

        assertEquals("annotated_translation_test", new AnnotatedTranslator().getModId());
    }

    private static final class AnnotatedTranslator extends Translator {
        private AnnotatedTranslator() {
            super(AnnotatedMod.class);
        }
    }

    @Mod("annotated_translation_test")
    private static final class AnnotatedMod {
    }

    private static final class WrongModIdPlatform implements BaniraPlatform {
        @Override
        public String loaderType() {
            return "test";
        }

        @Override
        public String minecraftVersion() {
            return "0.0";
        }

        @Override
        public boolean isClient() {
            return false;
        }

        @Override
        public boolean isDedicatedServer() {
            return true;
        }

        @Override
        public boolean isDevelopment() {
            return true;
        }

        @Override
        public boolean isModLoaded(String modId) {
            return false;
        }

        @Override
        public String modDisplayName(String modId) {
            return modId;
        }

        @Override
        public String modIdFromMainClass(Class<?> modMainClass) {
            return "wrong_mod_id";
        }

        @Override
        public Class<?> modMainClass(String modId) {
            return AnnotatedMod.class;
        }

        @Override
        public Path configDir() {
            return Path.of("config");
        }

        @Override
        public BaniraConfigService configService() {
            return NoopConfigService.INSTANCE;
        }

        @Override
        public BaniraNetworkService networkService() {
            return null;
        }

        @Override
        public BaniraInputService inputService() {
            return null;
        }

        @Override
        public BaniraRenderService renderService() {
            return null;
        }
    }

    private enum NoopConfigService implements BaniraConfigService {
        INSTANCE;

        @Override
        public <T> void register(Class<T> configClass, String modId) {
        }

        @Override
        public <T> T get(Class<T> configClass) {
            throw new IllegalStateException("No config registered");
        }

        @Override
        public ConfigHolder holder(Class<?> configClass) {
            return null;
        }
    }
}
