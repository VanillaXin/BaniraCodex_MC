package xin.vanilla.banira.common.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.Arrays;
import java.util.Objects;

final class NativeComponentTranslations {
    private NativeComponentTranslations() {
    }

    static MutableComponent prepare(net.minecraft.network.chat.Component source, String language, boolean vanillaOnly) {
        var contents = source.getContents();
        MutableComponent result;
        if (contents instanceof TranslatableContents translated) {
            Object[] arguments = Arrays.stream(translated.getArgs()).map(argument ->
                    argument instanceof net.minecraft.network.chat.Component component
                            ? prepare(component, language, vanillaOnly) : argument).toArray();
            if (VanillaComponentTranslations.isVanillaKey(translated.getKey())) {
                result = MutableComponent.create(new TranslatableContents(translated.getKey(), translated.getFallback(), arguments));
            } else {
                String template = VanillaComponentTranslations.serverTemplate(translated.getKey(), language);
                if (template == null)
                    template = translated.getFallback() == null ? translated.getKey() : translated.getFallback();
                Component formatted = Component.literal(arguments.length == 0 ? VanillaComponentTranslations.noArgumentTemplate(template) : template);
                for (Object argument : arguments)
                    formatted.appendArg(Component.object(
                            argument instanceof net.minecraft.network.chat.Component ? argument
                                    : net.minecraft.network.chat.Component.literal(String.valueOf(argument))));
                result = formatted.toVanilla(language).copy().setStyle(Style.EMPTY);
            }
        } else if (vanillaOnly && contents instanceof KeybindContents keybind
                && !VanillaComponentTranslations.isVanillaKey(keybind.getName())) {
            String template = VanillaComponentTranslations.serverTemplate(keybind.getName(), language);
            result = net.minecraft.network.chat.Component.literal(template == null ? keybind.getName() : template);
        } else if (contents instanceof SelectorContents selector) {
            result = MutableComponent.create(new SelectorContents(selector.getPattern(),
                    selector.getSeparator().map(separator -> prepare(separator, language, vanillaOnly))));
        } else if (contents instanceof NbtContents nbt) {
            result = MutableComponent.create(new NbtContents(nbt.getNbtPath(), nbt.isInterpreting(),
                    nbt.getSeparator().map(separator -> prepare(separator, language, vanillaOnly)), nbt.getDataSource()));
        } else {
            result = MutableComponent.create(contents);
        }
        Style style = source.getStyle();
        if (vanillaOnly && !"minecraft".equals(style.getFont().getNamespace())) style = style.withFont(null);
        if (style.getHoverEvent() != null)
            style = style.withHoverEvent(prepareHover(style.getHoverEvent(), language, vanillaOnly));
        result.setStyle(result.getStyle().applyTo(style));
        for (net.minecraft.network.chat.Component child : source.getSiblings())
            result.append(prepare(child, language, vanillaOnly));
        return result;
    }

    static HoverEvent prepareHover(HoverEvent event, String language, boolean vanillaOnly) {
        if (event.getAction() == HoverEvent.Action.SHOW_TEXT) {
            return new HoverEvent(HoverEvent.Action.SHOW_TEXT, prepare(event.getValue(HoverEvent.Action.SHOW_TEXT), language, vanillaOnly));
        }
        if (event.getAction() == HoverEvent.Action.SHOW_ENTITY) {
            var info = event.getValue(HoverEvent.Action.SHOW_ENTITY);
            return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityTooltipInfo(info.type, info.id,
                    info.name.map(name -> prepare(name, language, vanillaOnly))));
        }
        if (event.getAction() == HoverEvent.Action.SHOW_ITEM) {
            // Copy the stack directly: registry-backed data must not require a global serialization context.
            ItemStack stack = event.getValue(HoverEvent.Action.SHOW_ITEM).getItemStack().copy();
            for (var type : Arrays.asList(DataComponents.CUSTOM_NAME, DataComponents.ITEM_NAME)) {
                var name = stack.get(type);
                if (name != null) {
                    var prepared = prepare(name, language, vanillaOnly);
                    if (!Objects.equals(name, prepared)) stack.set(type, prepared);
                }
            }
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                ItemLore prepared = new ItemLore(lore.lines().stream().<net.minecraft.network.chat.Component>map(line -> prepare(line, language, vanillaOnly)).toList(),
                        lore.styledLines().stream().<net.minecraft.network.chat.Component>map(line -> prepare(line, language, vanillaOnly)).toList());
                if (!lore.equals(prepared)) stack.set(DataComponents.LORE, prepared);
            }
            return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack));
        }
        return event;
    }
}
