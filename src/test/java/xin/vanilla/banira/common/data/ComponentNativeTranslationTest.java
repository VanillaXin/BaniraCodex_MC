package xin.vanilla.banira.common.data;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.BaniraLang;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ComponentNativeTranslationTest {

    static {
        BaniraPlatforms.install(new TestBaniraPlatform().mod(Banira.MOD_ID, BaniraLang.class));
    }

    @Test
    public void vanillaKeyStaysTranslatableUnderModScope() {
        Component value = BaniraComponent.get().trans("item.minecraft.diamond").languageCode("zh_cn");
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(value.toVanilla("zh_cn")));
        assertFalse(Component.serialize(value).has("translationFallback"));
    }

    @Test
    public void explicitMinecraftScopeDoesNotFreezeEnglishOrLegacyFallback() {
        Component value = new ScopedComponent("minecraft").trans("gui.done")
                .translationFallback("Done").languageCode("zh_cn");
        assertEquals(Arrays.asList("gui.done"), keys(value.toVanilla("zh_cn")));
        assertFalse(Component.serialize(value).has("translationFallback"));
    }

    @Test
    public void unscopedKeyRetainsItsNativeArguments() {
        Component value = Component.trans("commands.give.success.single", new Object[]{
                net.minecraft.network.chat.Component.translatable("item.minecraft.diamond"), "Player"});
        assertEquals(Arrays.asList("commands.give.success.single", "item.minecraft.diamond"), keys(value.toVanilla("zh_cn")));
    }

    @Test
    public void modTemplateKeepsNativeArgumentAndTranslatesOnlyModText() {
        Component value = BaniraComponent.get().transAuto("config_editor_sync_server_ok",
                        net.minecraft.network.chat.Component.translatable("item.minecraft.diamond"))
                .languageCode("zh_cn");
        net.minecraft.network.chat.Component result = value.toVanilla("zh_cn");
        assertTrue(result.getString().startsWith("服务端已应用并保存 "));
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(result));
    }

    @Test
    public void plainTemplateKeepsNativeArgument() {
        Component value = BaniraComponent.get().literal("Reward: %s").appendArg(
                net.minecraft.network.chat.Component.translatable("item.minecraft.apple"));
        assertEquals(Arrays.asList("item.minecraft.apple"), keys(value.toVanilla("zh_cn")));
    }

    @Test
    public void indexedRepeatedArgumentsAndEscapedPercentDoNotConsumeSlots() {
        Component value = BaniraComponent.get().literal("%2$s / %1$s / %1$s / %% / %s").appendArg(
                net.minecraft.network.chat.Component.translatable("item.minecraft.apple"),
                net.minecraft.network.chat.Component.translatable("item.minecraft.diamond"));
        net.minecraft.network.chat.Component result = value.toVanilla("zh_cn");
        assertEquals(Arrays.asList("item.minecraft.diamond", "item.minecraft.apple", "item.minecraft.apple", "item.minecraft.apple"), keys(result));
        assertTrue(result.getString().contains(" / % / "));
    }

    @Test
    public void missingArgumentsRemainVisibleWithoutThrowing() {
        assertEquals("%2$s / %s", BaniraComponent.get().literal("%2$s / %s").toVanilla("zh_cn").getString());
    }

    @Test
    public void nativeAppendDoesNotStringifyOrMutateSource() {
        MutableComponent source = net.minecraft.network.chat.Component.translatable("item.minecraft.apple");
        Component value = BaniraComponent.get().literal("Reward: ").append(source);
        net.minecraft.network.chat.Component result = value.toVanilla("zh_cn");
        assertEquals(Arrays.asList("item.minecraft.apple"), keys(result));
        assertTrue(source.getSiblings().isEmpty());
        assertFalse(source == result.getSiblings().get(0));
    }

    @Test
    public void nativeArgumentKeepsStyleAndClickAction() {
        MutableComponent source = net.minecraft.network.chat.Component.translatable("item.minecraft.diamond")
                .withStyle(style -> style.withBold(true).withColor(0x336699)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.SuggestCommand("/give")));
        Component value = BaniraComponent.get().literal("%s").appendArg(source);
        net.minecraft.network.chat.Component nativePart = value.toVanilla("zh_cn").getSiblings().get(1);
        assertTrue(nativePart.getStyle().isBold());
        assertEquals(0x336699, nativePart.getStyle().getColor().getValue());
        assertEquals("/give", ((net.minecraft.network.chat.ClickEvent.SuggestCommand) nativePart.getStyle().getClickEvent()).command());
    }

    @Test
    public void networkRoundTripKeepsNativeSubtreeAndModFallback() {
        Component value = BaniraComponent.get().transAuto("config_editor_sync_server_ok",
                net.minecraft.network.chat.Component.translatable("item.minecraft.diamond")).languageCode("zh_cn");
        JsonObject json = Component.serialize(value);
        assertEquals("服务端已应用并保存 %s 项配置", json.get("translationFallback").getAsString());
        Component decoded = Component.deserialize(json);
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(decoded.toVanilla("zh_cn")));
    }

    @Test
    public void nativeTransConversionKeepsOriginalInsteadOfDroppingIt() {
        Component value = BaniraComponent.get().object(net.minecraft.network.chat.Component.translatable("gui.done"));
        assertEquals(Arrays.asList("gui.done"), keys(value.toVanillaTrans()));
    }

    @Test
    public void absentModStillUsesAvailableServerTemplate() {
        Component value = new ScopedComponent("unavailable_component_test")
                .trans(EnumI18nType.FORMAT, "reward", net.minecraft.network.chat.Component.translatable("item.minecraft.apple"))
                .translationFallback("奖励：%s");
        assertEquals(Arrays.asList("item.minecraft.apple"), keys(value.toVanilla("zh_cn")));
        assertTrue(value.toVanilla("zh_cn").getString().startsWith("奖励："));
    }

    @Test
    public void templateColorAndResetReachNativeArgumentsWithoutFlattening() {
        Component value = BaniraComponent.get().literal("\u00a7c%s \u00a7r%s").appendArg(
                net.minecraft.network.chat.Component.translatable("item.minecraft.apple"),
                net.minecraft.network.chat.Component.translatable("item.minecraft.diamond"));
        List<net.minecraft.network.chat.Component> parts = value.toVanilla("zh_cn").getSiblings().stream()
                .filter(part -> part.getContents() instanceof TranslatableContents).collect(java.util.stream.Collectors.toList());
        assertEquals(2, parts.size());
        assertEquals(0xff5555, parts.get(0).getStyle().getColor().getValue());
        assertNull(parts.get(1).getStyle().getColor());
        assertEquals(Arrays.asList("item.minecraft.apple", "item.minecraft.diamond"), keys(value.toVanilla("zh_cn")));
    }

    @Test
    public void plainLiteralKeepsItsCompactNativeShape() {
        net.minecraft.network.chat.Component value = BaniraComponent.get().literal("plain").toVanilla("zh_cn");
        assertEquals("plain", value.getString());
        assertTrue(value.getSiblings().isEmpty());
    }

    @Test
    public void nativeNamesAreNeverFlattenedForPrintfDecoration() {
        Component value = BaniraComponent.get().literal("%20s").appendArg(
                net.minecraft.network.chat.Component.translatable("item.minecraft.diamond"));
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(value.toVanilla("zh_cn")));
    }

    @Test
    public void plainNativeArgumentsStillHonorPrintfCaseAndWidth() {
        Component value = BaniraComponent.get().literal("%8S").appendArg(net.minecraft.network.chat.Component.literal("value"));
        assertEquals("   VALUE", value.toVanilla("zh_cn").getString());
    }

    @Test
    public void formattedNativeLiteralKeepsItsStyleAndClickAction() {
        net.minecraft.network.chat.Component original = net.minecraft.network.chat.Component.literal("value")
                .withStyle(style -> style.withBold(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.SuggestCommand("/value")));
        net.minecraft.network.chat.Component result = BaniraComponent.get().literal("%8S").appendArg(original).toVanilla("zh_cn");
        assertEquals("   VALUE", result.getString());
        net.minecraft.network.chat.Component formatted = result.getSiblings().stream()
                .filter(part -> "   VALUE".equals(part.getString())).findFirst().orElseThrow(AssertionError::new);
        assertTrue(formatted.getStyle().isBold());
        assertEquals("/value", ((net.minecraft.network.chat.ClickEvent.SuggestCommand) formatted.getStyle().getClickEvent()).command());
        assertEquals("value", original.getString());
    }

    @Test
    public void nativeLiteralWithAppendedTranslationsCannotBeFlattenedByPrintf() {
        Component argument = BaniraComponent.get().object(net.minecraft.network.chat.Component.literal("base"))
                .append(net.minecraft.network.chat.Component.translatable("item.minecraft.diamond"));
        net.minecraft.network.chat.Component result = BaniraComponent.get().literal("%8S").appendArg(argument).toVanilla("zh_cn");
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(result));
        assertTrue(result.getString().startsWith("base"));
    }

    @Test
    public void typedClickEventsKeepTheExistingWrapperContract() {
        List<net.minecraft.network.chat.ClickEvent> events = Arrays.asList(
                new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create("https://example.com/reward")),
                new net.minecraft.network.chat.ClickEvent.OpenFile("reward.txt"),
                new net.minecraft.network.chat.ClickEvent.RunCommand("/reward"),
                new net.minecraft.network.chat.ClickEvent.SuggestCommand("/reward player"),
                new net.minecraft.network.chat.ClickEvent.ChangePage(3),
                new net.minecraft.network.chat.ClickEvent.CopyToClipboard("reward-data"));
        for (net.minecraft.network.chat.ClickEvent event : events) {
            Component value = BaniraComponent.get().literal("reward").clickEvent(event);
            JsonObject json = Component.serialize(value);
            assertEquals(event.action().getSerializedName(), json.getAsJsonObject("clickEvent").get("action").getAsString());
            assertTrue(json.getAsJsonObject("clickEvent").has("value"));
            assertEquals(event, Component.deserialize(json).clickEvent());
            assertEquals(event, value.toChat("zh_cn").getStyle().getClickEvent());
        }
    }

    private static List<String> keys(net.minecraft.network.chat.Component component) {
        List<String> result = new ArrayList<>();
        if (component.getContents() instanceof TranslatableContents) {
            TranslatableContents contents = (TranslatableContents) component.getContents();
            result.add(contents.getKey());
            for (Object arg : contents.getArgs()) {
                if (arg instanceof net.minecraft.network.chat.Component) {
                    result.addAll(keys((net.minecraft.network.chat.Component) arg));
                }
            }
        }
        for (net.minecraft.network.chat.Component sibling : component.getSiblings()) {
            result.addAll(keys(sibling));
        }
        return result;
    }
}
