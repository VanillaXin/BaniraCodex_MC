package xin.vanilla.banira.common.data;

import com.google.gson.JsonObject;
import org.junit.Test;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ComponentNetworkTranslationTest {

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
}
