package xin.vanilla.banira.common.data;

import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.BaniraLang;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.notification.NotificationBudget;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.List;

import static org.junit.Assert.*;

public class ComponentNativeRegistryTransportTest {
    @BeforeClass
    public static void bootstrap() throws Exception {
        xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.bootstrapGame();
        BaniraPlatforms.install(new TestBaniraPlatform().mod(Banira.MOD_ID, BaniraLang.class));
    }

    @Test
    public void registryBackedHoverDoesNotRequireGlobalRuntimeForChatPreparation() {
        Context context = context();
        var original = net.minecraft.network.chat.Component.literal("reward").withStyle(Style.EMPTY.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(context.item))));
        var result = BaniraComponent.get().object(original).toChat("zh_cn");
        ItemStack prepared = result.getStyle().getHoverEvent().getValue(HoverEvent.Action.SHOW_ITEM).getItemStack();
        assertEquals(2, prepared.getEnchantments().getLevel(context.enchantment));
        assertEquals(context.item.getComponentsPatch(), prepared.getComponentsPatch());
        assertNotSame(context.item, prepared);
    }

    @Test
    public void explicitRegistryTransportKeepsEnchantmentsAndLocalizesOnlyItemText() {
        Context context = context();
        context.item.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.translatable("word.banira_codex.enabled"));
        context.item.set(DataComponents.LORE, new ItemLore(List.of(
                net.minecraft.network.chat.Component.translatable("word.banira_codex.disabled"),
                net.minecraft.network.chat.Component.translatable("item.minecraft.diamond"))));
        CompoundTag opaque = new CompoundTag();
        opaque.putString("translate", "opaque-item-data");
        context.item.set(DataComponents.CUSTOM_DATA, CustomData.of(opaque));
        var original = net.minecraft.network.chat.Component.translatable("format.banira_codex.config_editor_sync_server_ok",
                net.minecraft.network.chat.Component.translatable("item.minecraft.diamond"))
                .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(context.item))));
        var ops = context.registries.createSerializationContext(JsonOps.INSTANCE);
        var before = ComponentSerialization.CODEC.encodeStart(ops, original).getOrThrow();
        NotificationBudget.Payload payload = NotificationBudget.prepare(BaniraComponent.get().object(original), "zh_cn", context.registries);
        var message = payload.vanillaMessage();
        assertTrue(message.getString().startsWith("服务端已应用并保存 "));
        ItemStack prepared = message.getStyle().getHoverEvent().getValue(HoverEvent.Action.SHOW_ITEM).getItemStack();
        assertEquals(2, prepared.getEnchantments().getLevel(context.enchantment));
        assertEquals("已启用", prepared.get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("已禁用", prepared.get(DataComponents.LORE).lines().getFirst().getString());
        String json = ComponentSerialization.CODEC.encodeStart(ops, message).getOrThrow().toString();
        assertTrue(json.contains("item.minecraft.diamond"));
        assertFalse(json.contains("word.banira_codex"));
        assertEquals(opaque, prepared.get(DataComponents.CUSTOM_DATA).copyTag());
        assertEquals(before, ComponentSerialization.CODEC.encodeStart(ops, original).getOrThrow());
        assertTrue(payload.componentJson().contains("item.minecraft.diamond"));
        assertFalse(payload.componentJson().contains("word.banira_codex"));
    }

    @Test
    public void dynamicSeparatorsArePreparedWhileTheirSelectorsAndNbtSourcesRemainIntact() {
        var selector = net.minecraft.network.chat.Component.Serializer.fromJson(
                "{\"selector\":\"@a\",\"separator\":{\"translate\":\"word.banira_codex.enabled\"}}", RegistryAccess.EMPTY);
        var selected = (SelectorContents) BaniraComponent.get().object(selector).toChat("zh_cn").getContents();
        assertEquals("@a", selected.getPattern());
        assertEquals("已启用", selected.getSeparator().orElseThrow().getString());
        var nbt = net.minecraft.network.chat.Component.Serializer.fromJson(
                "{\"nbt\":\"foo\",\"storage\":\"minecraft:test\",\"interpret\":true,\"separator\":{\"translate\":\"word.banira_codex.disabled\"}}", RegistryAccess.EMPTY);
        var prepared = (NbtContents) BaniraComponent.get().object(nbt).toChat("zh_cn").getContents();
        assertEquals("foo", prepared.getNbtPath());
        assertTrue(prepared.isInterpreting());
        assertEquals(((NbtContents) nbt.getContents()).getDataSource(), prepared.getDataSource());
        assertEquals("已禁用", prepared.getSeparator().orElseThrow().getString());
    }

    private static Context context() {
        MappedRegistry<Enchantment> enchantments = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("banira_codex", "native_transport_test");
        var definition = Enchantment.definition(HolderSet.direct(Items.DIAMOND_SWORD.builtInRegistryHolder()),
                1, 3, Enchantment.constantCost(1), Enchantment.constantCost(20), 1, EquipmentSlotGroup.MAINHAND);
        Holder<Enchantment> enchantment = Registry.registerForHolder(enchantments, id, Enchantment.enchantment(definition).build(id));
        RegistryAccess registries = new RegistryAccess.ImmutableRegistryAccess(List.of(BuiltInRegistries.ITEM, enchantments)).freeze();
        ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
        item.enchant(enchantment, 2);
        return new Context(registries, item, enchantment);
    }

    @Test
    public void missingNativeTemplatesKeepNullAndPrimitiveArgumentsReadable() {
        var styled = net.minecraft.network.chat.Component.translatableWithFallback(
                "absent_optional.styled", "%s %s", "player", 7).withStyle(Style.EMPTY.withBold(true).withItalic(true));
        var prepared = BaniraComponent.get().object(styled).toChat("zh_cn");
        assertEquals("player 7", prepared.getString());
        prepared.visit((style, text) -> {
            assertTrue(style.isBold());
            assertTrue(style.isItalic());
            return java.util.Optional.empty();
        }, Style.EMPTY);
        var percent = net.minecraft.network.chat.Component.translatableWithFallback("absent_optional.percent", "100%%");
        assertEquals("100%", BaniraComponent.get().object(percent).toChat("zh_cn").getString());
        var nativeMessage = net.minecraft.network.chat.Component.translatableWithFallback(
                "absent_optional.reward", "%s %s %s", null, 7, true);
        assertEquals("null 7 true", BaniraComponent.get().object(nativeMessage).toChat("zh_cn").getString());
    }

    private record Context(RegistryAccess registries, ItemStack item, Holder<Enchantment> enchantment) {
    }
}
