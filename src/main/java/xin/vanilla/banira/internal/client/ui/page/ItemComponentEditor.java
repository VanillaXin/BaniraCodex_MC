package xin.vanilla.banira.internal.client.ui.page;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import xin.vanilla.banira.common.util.ItemUtils;

final class ItemComponentEditor {
    private ItemComponentEditor() {
    }

    static String serialize(ItemStack stack) {
        return serialize(stack, Minecraft.getInstance().level.registryAccess());
    }

    static String serialize(ItemStack stack, HolderLookup.Provider registries) {
        String id = ItemUtils.getItemRegistryString(stack);
        String input = new ItemInput(stack.getItemHolder(), stack.getComponentsPatch())
                .serialize(registries);
        String components = input.substring(id.length());
        return components.isEmpty() ? "[]" : components;
    }

    static ItemStack parse(ItemStack original, String text) throws CommandSyntaxException {
        return parse(original, text, Minecraft.getInstance().level.registryAccess());
    }

    static ItemStack parse(ItemStack original, String text, HolderLookup.Provider registries) throws CommandSyntaxException {
        if (text.stripLeading().startsWith("{")) {
            ItemStack result = original.copy();
            var tag = TagParser.parseTag(text);
            if (tag.isEmpty()) result.remove(DataComponents.CUSTOM_DATA);
            else result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return result;
        }
        StringReader reader = new StringReader(ItemUtils.getItemRegistryString(original) + text);
        ItemParser.ItemResult parsed = new ItemParser(registries).parse(reader);
        reader.skipWhitespace();
        if (reader.canRead())
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
        return new ItemInput(parsed.item(), parsed.components()).createItemStack(original.getCount(), false);
    }
}
