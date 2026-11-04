package xin.vanilla.banira.common.data;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
                new TranslatableComponent("item.minecraft.diamond"), "Player"});
        assertEquals(Arrays.asList("commands.give.success.single", "item.minecraft.diamond"), keys(value.toVanilla("zh_cn")));
    }

    @Test
    public void modTemplateKeepsNativeArgumentAndTranslatesOnlyModText() {
        Component value = BaniraComponent.get().transAuto("config_editor_sync_server_ok",
                new TranslatableComponent("item.minecraft.diamond"))
                .languageCode("zh_cn");
        net.minecraft.network.chat.Component result = value.toVanilla("zh_cn");
        assertTrue(result.getString().startsWith("服务端已应用并保存 "));
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(result));
    }

    @Test
    public void plainTemplateKeepsNativeArgument() {
        Component value = BaniraComponent.get().literal("Reward: %s").appendArg(
                new TranslatableComponent("item.minecraft.apple"));
        assertEquals(Arrays.asList("item.minecraft.apple"), keys(value.toVanilla("zh_cn")));
    }

    @Test
    public void indexedRepeatedArgumentsAndEscapedPercentDoNotConsumeSlots() {
        Component value = BaniraComponent.get().literal("%2$s / %1$s / %1$s / %% / %s").appendArg(
                new TranslatableComponent("item.minecraft.apple"),
                new TranslatableComponent("item.minecraft.diamond"));
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
        MutableComponent source = new TranslatableComponent("item.minecraft.apple");
        Component value = BaniraComponent.get().literal("Reward: ").append(source);
        net.minecraft.network.chat.Component result = value.toVanilla("zh_cn");
        assertEquals(Arrays.asList("item.minecraft.apple"), keys(result));
        assertTrue(source.getSiblings().isEmpty());
        assertFalse(source == result.getSiblings().get(0));
    }

    @Test
    public void nativeArgumentKeepsStyleAndClickAction() {
        MutableComponent source = new TranslatableComponent("item.minecraft.diamond")
                .withStyle(style -> style.withBold(true).withColor(net.minecraft.network.chat.TextColor.fromRgb(0x336699))
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/give")));
        Component value = BaniraComponent.get().literal("%s").appendArg(source);
        net.minecraft.network.chat.Component nativePart = value.toVanilla("zh_cn").getSiblings().get(1);
        assertTrue(nativePart.getStyle().isBold());
        assertEquals(0x336699, nativePart.getStyle().getColor().getValue());
        assertEquals("/give", nativePart.getStyle().getClickEvent().getValue());
    }

    @Test
    public void networkRoundTripKeepsNativeSubtreeAndModFallback() {
        Component value = BaniraComponent.get().transAuto("config_editor_sync_server_ok",
                new TranslatableComponent("item.minecraft.diamond")).languageCode("zh_cn");
        JsonObject json = Component.serialize(value);
        assertEquals("服务端已应用并保存 %s 项配置", json.get("translationFallback").getAsString());
        Component decoded = Component.deserialize(json);
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(decoded.toVanilla("zh_cn")));
    }

    @Test
    public void nativeTransConversionKeepsOriginalInsteadOfDroppingIt() {
        Component value = BaniraComponent.get().object(new TranslatableComponent("gui.done"));
        assertEquals(Arrays.asList("gui.done"), keys(value.toVanillaTrans()));
    }

    @Test
    public void absentModStillUsesAvailableServerTemplate() {
        Component value = new ScopedComponent("unavailable_component_test")
                .trans(EnumI18nType.FORMAT, "reward", new TranslatableComponent("item.minecraft.apple"))
                .translationFallback("奖励：%s");
        assertEquals(Arrays.asList("item.minecraft.apple"), keys(value.toVanilla("zh_cn")));
        assertTrue(value.toVanilla("zh_cn").getString().startsWith("奖励："));
    }

    @Test
    public void templateColorAndResetReachNativeArgumentsWithoutFlattening() {
        Component value = BaniraComponent.get().literal("\u00a7c%s \u00a7r%s").appendArg(
                new TranslatableComponent("item.minecraft.apple"),
                new TranslatableComponent("item.minecraft.diamond"));
        List<net.minecraft.network.chat.Component> parts = value.toVanilla("zh_cn").getSiblings().stream()
                .filter(part -> part instanceof TranslatableComponent).collect(java.util.stream.Collectors.toList());
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
                new TranslatableComponent("item.minecraft.diamond"));
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(value.toVanilla("zh_cn")));
    }

    @Test
    public void plainNativeArgumentsStillHonorPrintfCaseAndWidth() {
        Component value = BaniraComponent.get().literal("%8S").appendArg(new TextComponent("value"));
        assertEquals("   VALUE", value.toVanilla("zh_cn").getString());
    }

    @Test
    public void formattedNativeLiteralKeepsItsStyleAndClickAction() {
        net.minecraft.network.chat.Component original = new TextComponent("value")
                .withStyle(style -> style.withBold(true).withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/value")));
        net.minecraft.network.chat.Component result = BaniraComponent.get().literal("%8S").appendArg(original).toVanilla("zh_cn");
        assertEquals("   VALUE", result.getString());
        net.minecraft.network.chat.Component formatted = result.getSiblings().stream()
                .filter(part -> "   VALUE".equals(part.getString())).findFirst().orElseThrow(AssertionError::new);
        assertTrue(formatted.getStyle().isBold());
        assertEquals("/value", formatted.getStyle().getClickEvent().getValue());
        assertEquals("value", original.getString());
    }

    @Test
    public void nativeLiteralWithAppendedTranslationsCannotBeFlattenedByPrintf() {
        Component argument = BaniraComponent.get().object(new TextComponent("base"))
                .append(new TranslatableComponent("item.minecraft.diamond"));
        net.minecraft.network.chat.Component result = BaniraComponent.get().literal("%8S").appendArg(argument).toVanilla("zh_cn");
        assertEquals(Arrays.asList("item.minecraft.diamond"), keys(result));
        assertTrue(result.getString().startsWith("base"));
    }

    private static List<String> keys(net.minecraft.network.chat.Component component) {
        List<String> result = new ArrayList<>();
        if (component instanceof TranslatableComponent) {
            TranslatableComponent contents = (TranslatableComponent) component;
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
