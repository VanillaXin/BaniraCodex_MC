package xin.vanilla.banira.common.data;

import com.google.gson.JsonObject;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.BaniraLang;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ComponentNetworkTranslationTest {

    static {
        BaniraPlatforms.install(new TestBaniraPlatform()
                .mod(Banira.MOD_ID, BaniraLang.class));
    }

    @Test
    public void serverBoundNotificationKeepsTranslationMetadataAndLanguage() {
        Component component = BaniraComponent.get()
                .transAuto("config_editor_sync_server_ok", 3)
                .languageCode("zh_cn");

        JsonObject json = Component.serialize(component);

        assertEquals("config_editor_sync_server_ok", json.get("text").getAsString());
        assertEquals(EnumI18nType.FORMAT.name(), json.get("i18nType").getAsString());
        assertEquals(Banira.MOD_ID, json.get("modId").getAsString());
        assertEquals("zh_cn", json.get("languageCode").getAsString());

        Component decoded = Component.deserialize(json);
        assertEquals("config_editor_sync_server_ok", decoded.text());
        assertEquals(EnumI18nType.FORMAT, decoded.i18nType());
        assertEquals(Banira.MOD_ID, decoded.modId());
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
}
