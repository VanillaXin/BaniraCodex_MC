package xin.vanilla.banira.common.data;

import com.google.gson.JsonObject;
import org.junit.Test;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.platform.*;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ComponentNetworkTranslationTest {

    static {
        BaniraPlatforms.install(new TranslationTestPlatform());
    }

    @Test
    public void serverBoundNotificationKeepsTranslationMetadataAndLanguage() {
        Component component = BaniraComponent.get()
                .transAuto("config_editor_sync_server_ok", 3)
                .languageCode("zh_cn");

        JsonObject json = Component.serialize(component);

        assertEquals("config_editor_sync_server_ok", json.get("text").getAsString());
        assertEquals(EnumI18nType.FORMAT.name(), json.get("i18nType").getAsString());
        assertEquals(BaniraCodex.MODID, json.get("modId").getAsString());
        assertEquals("zh_cn", json.get("languageCode").getAsString());

        Component decoded = Component.deserialize(json);
        assertEquals("config_editor_sync_server_ok", decoded.text());
        assertEquals(EnumI18nType.FORMAT, decoded.i18nType());
        assertEquals(BaniraCodex.MODID, decoded.modId());
        assertFalse(decoded.isLanguageCodeEmpty());
        assertEquals("zh_cn", decoded.languageCodeOrDefault());
        assertEquals(1, decoded.getArgs().size());
        assertEquals("3", decoded.getArgs().get(0).text());
    }

    @Test
    public void configEditorNotificationKeysResolveToText() {
        assertEquals("配置已保存", BaniraComponent.get()
                .transAuto("config_editor_save_success")
                .languageCode("zh_cn")
                .getString("zh_cn", true, true));
        assertEquals("服务端已应用并保存 3 项配置", BaniraComponent.get()
                .transAuto("config_editor_sync_server_ok", 3)
                .languageCode("zh_cn")
                .getString("zh_cn", true, true));
        String clientText = BaniraComponent.get()
                .transClientAuto("config_editor_save_success")
                .getString("zh_cn", true, true);
        assertFalse(clientText.contains("config_editor_save_success"));
    }

    private static final class TranslationTestPlatform implements BaniraPlatform {
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
            return BaniraCodex.MODID.equals(modId);
        }

        @Override
        public String modDisplayName(String modId) {
            return modId;
        }

        @Override
        public String modIdFromMainClass(Class<?> modMainClass) {
            return modMainClass == BaniraCodex.class ? BaniraCodex.MODID : "test";
        }

        @Override
        public Class<?> modMainClass(String modId) {
            return BaniraCodex.MODID.equals(modId) ? BaniraCodex.class : TranslationTestPlatform.class;
        }

        @Override
        public Path configDir() {
            return Path.of("config");
        }

        @Override
        public BaniraConfigService configService() {
            return null;
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
}
