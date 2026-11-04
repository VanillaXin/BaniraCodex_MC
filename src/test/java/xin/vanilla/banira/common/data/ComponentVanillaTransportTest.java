package xin.vanilla.banira.common.data;

import com.google.gson.*;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.BaniraLang;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.notification.NotificationBudget;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ComponentVanillaTransportTest {
    static {
        BaniraPlatforms.install(new TestBaniraPlatform().mod(Banira.MOD_ID, BaniraLang.class));
    }

    @Test
    public void vanillaKeysComeFromMinecraftInsteadOfModLanguageTables() {
        for (String key : Arrays.asList("gui.done", "chat.type.text", "item.minecraft.diamond", "block.minecraft.stone"))
            assertTrue(key, VanillaComponentTranslations.isVanillaKey(key));
        assertFalse(VanillaComponentTranslations.isVanillaKey("word.banira_codex.enabled"));
        assertFalse(VanillaComponentTranslations.isVanillaKey("item.client_missing_mod.token"));
    }

    @Test
    public void unknownRawModKeyIsNotDelegatedToTheClient() throws Exception {
        net.minecraft.network.chat.Component message = BaniraComponent.get().trans("word.missing_server_mod.unknown").toChat("zh_cn");
        assertTrue(keys(json(message)).isEmpty());
        assertEquals("word.missing_server_mod.unknown", message.getString());
    }

    @Test
    public void nativeModRootIsResolvedOnTheServer() throws Exception {
        Component source = BaniraComponent.get().object(new TranslatableComponent(
                "word.banira_codex.config_editor_save_success")).languageCode("zh_cn");
        assertEquals("配置已保存", source.toChat("zh_cn").getString());
        assertTrue(keys(json(source.toChat("zh_cn"))).isEmpty());
    }

    @Test
    public void nativeModTemplateKeepsVanillaParameters() throws Exception {
        Component source = nativeReward();
        net.minecraft.network.chat.Component message = source.toChat("zh_cn");
        assertTrue(message.getString().startsWith("服务端已应用并保存 "));
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(json(message)));
    }

    @Test
    public void vanillaOuterTemplateCannotHideModTranslationsInItsParameters() throws Exception {
        Component source = BaniraComponent.get().object(new TranslatableComponent("chat.type.text",
                new TranslatableComponent("word.banira_codex.enabled"),
                new TranslatableComponent("item.minecraft.diamond"))).languageCode("zh_cn");
        net.minecraft.network.chat.Component message = source.toChat("zh_cn");
        assertTrue(message.getString().contains("已启用"));
        assertEquals(Arrays.asList("chat.type.text", "item.minecraft.diamond"), keys(json(message)));
    }

    @Test
    public void vanillaNotificationPayloadWorksWithoutAnyModLanguageOnTheReceiver() throws Exception {
        NotificationBudget.Payload payload = NotificationBudget.prepare(nativeReward(), "zh_cn");
        net.minecraft.network.chat.Component serverMessage = payload.vanillaMessage();
        byte[] bytes = json(serverMessage).toString().getBytes(StandardCharsets.UTF_8);
        net.minecraft.network.chat.Component received = decode(parse(new String(bytes, StandardCharsets.UTF_8)));
        assertEquals(serverMessage.getString(), received.getString());
        assertTrue(received.getString().startsWith("服务端已应用并保存 "));
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(json(received)));
    }

    @Test
    public void wrappedNotificationsKeepTheirOwnMetadataAndNativeVanillaArguments() throws Exception {
        Component source = BaniraComponent.get().transAuto("config_editor_sync_server_ok",
                new TranslatableComponent("item.minecraft.diamond")).languageCode("zh_cn");
        JsonObject data = parse(NotificationBudget.prepare(source, "zh_cn").componentJson()).getAsJsonObject();
        assertEquals("FORMAT", data.get("i18nType").getAsString());
        assertEquals("服务端已应用并保存 %s 项配置", data.get("translationFallback").getAsString());
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(json(Component.deserialize(data).toVanilla("zh_cn"))));
    }

    @Test
    public void wrappedOriginalDoesNotRequireTheClientToInstallTheOriginMod() throws Exception {
        JsonObject data = nativeReward().toJson();
        assertEquals("ORIGINAL", data.get("i18nType").getAsString());
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(data.get("original")));
        assertTrue(Component.deserialize(data).toVanilla("zh_cn").getString().startsWith("服务端已应用并保存 "));
    }

    @Test
    public void legacyTranslationExportAlsoBakesModTranslations() throws Exception {
        Component source = BaniraComponent.get().transAuto("config_editor_save_success").languageCode("zh_cn");
        assertEquals("配置已保存", source.toVanillaTrans().getString());
        assertTrue(keys(json(source.toVanillaTrans())).isEmpty());
    }

    @Test
    public void styleAndTextHoverSurviveServerTranslation() throws Exception {
        net.minecraft.network.chat.Component original = new TranslatableComponent(
                "word.banira_codex.enabled").withStyle(style -> style.withBold(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/test"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        new TranslatableComponent("word.banira_codex.disabled"))));
        net.minecraft.network.chat.Component message = BaniraComponent.get().object(original).languageCode("zh_cn").toChat("zh_cn");
        assertTrue(message.getStyle().isBold());
        assertEquals("/test", message.getStyle().getClickEvent().getValue());
        assertEquals("已禁用", message.getStyle().getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT).getString());
        assertTrue(keys(json(message)).isEmpty());
    }

    @Test
    public void inheritedStylesAreNotResetByTheServerTemplateFormatter() throws Exception {
        net.minecraft.network.chat.Component original = new TranslatableComponent(
                "format.banira_codex.config_editor_sync_server_ok",
                new TranslatableComponent("item.minecraft.diamond")).withStyle(style -> style.withBold(true));
        JsonObject value = json(BaniraComponent.get().object(original).languageCode("zh_cn").toVanillaTrans()).getAsJsonObject();
        assertTrue(value.get("bold").getAsBoolean());
        assertFalse(value.toString().contains("\"bold\":false"));
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(value));
    }

    @Test
    public void vanillaFallbackDoesNotModifyTheNativeSource() throws Exception {
        Component source = nativeReward();
        String before = json((net.minecraft.network.chat.Component) source.original()).toString();
        source.toChat("zh_cn");
        source.toJson();
        assertEquals(before, json((net.minecraft.network.chat.Component) source.original()).toString());
    }

    @Test
    public void unknownNativeModKeyUsesItsServerFallback() throws Exception {
        JsonElement value = prepare(parse("{\"translate\":\"unavailable.reward\",\"fallback\":\"Reward: %s\",\"with\":[{\"translate\":\"item.minecraft.diamond\"}]}"), true);
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(value));
        assertTrue(decode(value).getString().startsWith("Reward: "));
    }

    @Test
    public void unavailableNativeModWithoutFallbackBecomesVisiblePlainText() throws Exception {
        JsonElement value = prepare(parse("{\"translate\":\"unavailable.reward\"}"), true);
        assertTrue(keys(value).isEmpty());
        assertEquals("unavailable.reward", decode(value).getString());
    }

    @Test
    public void entityHoverNamesArePreparedWithoutTouchingIdentity() throws Exception {
        JsonElement value = prepare(parse("{\"text\":\"entity\",\"hoverEvent\":{\"action\":\"show_entity\",\"contents\":{\"type\":\"minecraft:pig\",\"id\":\"00000000-0000-0000-0000-000000000001\",\"name\":{\"translate\":\"word.banira_codex.enabled\"}}}}"), true);
        JsonObject info = value.getAsJsonObject().getAsJsonObject("hoverEvent").getAsJsonObject("contents");
        assertEquals("minecraft:pig", info.get("type").getAsString());
        assertEquals("00000000-0000-0000-0000-000000000001", info.get("id").getAsString());
        assertEquals("已启用", decode(info.get("name")).getString());
    }

    @Test
    public void itemDataIsNeverInterpretedAsAChatComponent() throws Exception {
        JsonElement input = parse("{\"text\":\"item\",\"hoverEvent\":{\"action\":\"show_item\",\"contents\":{\"id\":\"minecraft:stone\",\"components\":{\"minecraft:custom_data\":{\"translate\":\"opaque-data\",\"text\":\"unchanged\"}}}}}");
        assertEquals(input, prepare(input, true));
    }

    @Test
    public void typedItemNamesAndLoreAreTranslatedWithoutTouchingCustomData() throws Exception {
        JsonElement input = parse("{\"text\":\"item\",\"hoverEvent\":{\"action\":\"show_item\",\"contents\":{\"id\":\"minecraft:stone\",\"components\":{\"minecraft:custom_name\":{\"translate\":\"word.banira_codex.enabled\"},\"minecraft:lore\":[{\"translate\":\"item.minecraft.diamond\"}],\"minecraft:custom_data\":{\"translate\":\"opaque-data\"}}}}}");
        JsonObject components = prepare(input, true).getAsJsonObject().getAsJsonObject("hoverEvent").getAsJsonObject("contents").getAsJsonObject("components");
        assertEquals("已启用", decode(components.get("minecraft:custom_name")).getString());
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(components.get("minecraft:lore")));
        assertEquals("opaque-data", components.getAsJsonObject("minecraft:custom_data").get("translate").getAsString());
    }

    @Test
    public void legacyItemNameAndLoreAreHandledByTheNbtParser() throws Exception {
        String encoded = "{display:{Name:'{\"translate\":\"word.banira_codex.enabled\"}',Lore:['{\"translate\":\"item.minecraft.diamond\"}','{\"translate\":\"word.banira_codex.disabled\"}']},opaque:{translate:\"not-a-component\",value:7}}";
        net.minecraft.nbt.CompoundTag before = net.minecraft.nbt.TagParser.parseTag(encoded);
        net.minecraft.nbt.CompoundTag after = net.minecraft.nbt.TagParser.parseTag(Component.prepareItemHoverTag(encoded, "zh_cn", true));
        assertEquals(before.getCompound("opaque"), after.getCompound("opaque"));
        assertEquals("已启用", decode(parse(after.getCompound("display").getString("Name"))).getString());
        net.minecraft.nbt.ListTag lore = after.getCompound("display").getList("Lore", 8);
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(parse(lore.getString(0))));
        assertEquals("已禁用", decode(parse(lore.getString(1))).getString());
        String untouched = "{ display : { Name : '{\"translate\":\"item.minecraft.diamond\"}' }, data:7b }";
        assertEquals(untouched, Component.prepareItemHoverTag(untouched, "zh_cn", true));
        assertEquals("invalid{", Component.prepareItemHoverTag("invalid{", "zh_cn", true));
    }

    @Test
    public void unknownClientFontIsRemovedOnlyForVanillaTransport() throws Exception {
        JsonElement input = parse("{\"text\":\"readable\",\"font\":\"optional_client:font\",\"italic\":true}");
        assertFalse(prepare(input, true).getAsJsonObject().has("font"));
        assertEquals(input, prepare(input, false));
        assertTrue(prepare(input, true).getAsJsonObject().get("italic").getAsBoolean());
    }

    @Test
    public void vanillaDynamicContentsAndClickCommandsRemainUnchanged() throws Exception {
        for (String input : Arrays.asList("{\"score\":{\"name\":\"Player\",\"objective\":\"score\"}}",
                "{\"selector\":\"@a\",\"separator\":{\"text\":\", \"}}",
                "{\"keybind\":\"key.jump\"}",
                "{\"text\":\"click\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/tellraw @s {translate:opaque}\"}}")) {
            JsonElement value = parse(input);
            assertEquals(value, prepare(value, true));
        }
    }

    @Test
    public void nativeNoArgumentTemplatesDecodeEscapesWithoutChangingPlainLiterals() throws Exception {
        for (String[] sample : new String[][]{{"100%%", "100%"}, {"%%%%", "%%"}, {"100%", "100%"}, {"%% %s", "%% %s"}}) {
            JsonObject input = new JsonObject();
            input.addProperty("translate", "unavailable.percent");
            input.addProperty("fallback", sample[0]);
            assertEquals(sample[1], decode(prepare(input, true)).getString());
        }
        assertEquals("100%%", BaniraComponent.get().literal("100%%").toChat("zh_cn").getString());
    }

    private static Component nativeReward() {
        return BaniraComponent.get().object(new TranslatableComponent(
                "format.banira_codex.config_editor_sync_server_ok",
                new TranslatableComponent("item.minecraft.diamond"))).languageCode("zh_cn");
    }

    private static JsonElement json(net.minecraft.network.chat.Component component) throws Exception {
        Method method = Component.class.getDeclaredMethod("nativeComponentJson", net.minecraft.network.chat.Component.class);
        method.setAccessible(true);
        return (JsonElement) method.invoke(null, component);
    }

    private static net.minecraft.network.chat.Component decode(JsonElement json) throws Exception {
        Method method = Component.class.getDeclaredMethod("nativeComponentFromJson", JsonElement.class);
        method.setAccessible(true);
        return (net.minecraft.network.chat.Component) method.invoke(null, json);
    }

    private static JsonElement prepare(JsonElement json, boolean vanillaOnly) throws Exception {
        Method method = Component.class.getDeclaredMethod("prepareNativeJson", JsonElement.class, String.class, boolean.class);
        method.setAccessible(true);
        return (JsonElement) method.invoke(null, json, "zh_cn", vanillaOnly);
    }

    private static JsonElement parse(String json) {
        return new JsonParser().parse(json);
    }

    private static List<String> keys(JsonElement component) {
        List<String> result = new ArrayList<>();
        if (component.isJsonArray()) {
            for (JsonElement child : component.getAsJsonArray()) result.addAll(keys(child));
        } else if (component.isJsonObject()) {
            JsonObject object = component.getAsJsonObject();
            if (object.has("translate")) {
                String key = object.get("translate").getAsString();
                assertTrue("Non-vanilla key leaked: " + key, VanillaComponentTranslations.isVanillaKey(key));
                result.add(key);
            }
            for (String field : Arrays.asList("extra", "with", "separator"))
                if (object.has(field)) result.addAll(keys(object.get(field)));
            if (object.has("hoverEvent")) {
                JsonObject hover = object.getAsJsonObject("hoverEvent");
                if (hover.has("contents") && "show_text".equals(hover.get("action").getAsString())) result.addAll(keys(hover.get("contents")));
            }
        }
        return result;
    }
}
