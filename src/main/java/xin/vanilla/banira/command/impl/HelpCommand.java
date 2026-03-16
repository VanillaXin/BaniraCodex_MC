package xin.vanilla.banira.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.command.BaniraCommand;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.config.CommonConfig;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class HelpCommand {
    private HelpCommand() {
    }

    public static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String command;
        int page;
        try {
            command = StringArgumentType.getString(context, "command");
            page = NumberUtils.toInt(command);
        } catch (IllegalArgumentException ignored) {
            command = "";
            page = 1;
        }
        String lang = Translator.getPlayerLanguage(player);
        Component helpInfo;
        if (page > 0) {
            int helpNumPerPage = CommonConfig.get().help().helpInfoNumPerPage();
            int pages = (int) Math.ceil((double) BaniraCommand.HELP_MESSAGE.size() / helpNumPerPage);
            helpInfo = Component.literal(StringUtils.format("%1$s help [page] - %2$s/%3$s\n", BaniraCommand.getCommandPrefix(), page, pages));
            for (int i = 0; (page - 1) * helpNumPerPage + i < BaniraCommand.HELP_MESSAGE.size() && i < helpNumPerPage; i++) {
                KeyValue<String, EnumCommandType> keyValue = BaniraCommand.HELP_MESSAGE.get((page - 1) * helpNumPerPage + i);
                Component commandTips;
                if (keyValue.value().name().toLowerCase().contains("concise")) {
                    commandTips = Component.transLang(BaniraCodex.MODID, lang, EnumI18nType.FORMAT, "concise", keyValue.key());
                } else {
                    commandTips = Component.transLang(BaniraCodex.MODID, lang, EnumI18nType.FORMAT, keyValue.value().name().toLowerCase());
                }
                commandTips.color(EnumMCColor.GRAY.getColor());
                String com = "/" + keyValue.key();
                helpInfo.append(Component.literal(com)
                                .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.transLang(BaniraCodex.MODID, lang, EnumI18nType.FORMAT, "click_to_suggest").toVanilla(lang)))
                        )
                        .append(new Component(" -> ").color(EnumMCColor.YELLOW.getColor()))
                        .append(commandTips);
                if (i != Math.min((page - 1) * helpNumPerPage + helpNumPerPage, BaniraCommand.HELP_MESSAGE.size()) - 1) {
                    helpInfo.append("\n");
                }
            }
            if (pages > 1) {
                helpInfo.append("\n");
                Component prevButton = Component.literal("<<< ");
                if (page > 1) {
                    prevButton.color(EnumMCColor.AQUA.getColor())
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/%s help %d", BaniraCommand.getCommandPrefix(), page - 1)))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.transLang(BaniraCodex.MODID, lang, EnumI18nType.FORMAT, "previous_page").toVanilla(lang)));
                } else {
                    prevButton.color(EnumMCColor.DARK_AQUA.getColor());
                }
                helpInfo.append(prevButton);
                helpInfo.append(Component.literal(String.format(" %s/%s ",
                                StringUtils.padOptimizedLeft(page, String.valueOf(pages).length(), " "),
                                pages))
                        .color(EnumMCColor.WHITE.getColor()));
                Component nextButton = Component.literal(" >>>");
                if (page < pages) {
                    nextButton.color(EnumMCColor.AQUA.getColor())
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/%s help %d", BaniraCommand.getCommandPrefix(), page + 1)))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.transLang(BaniraCodex.MODID, lang, EnumI18nType.FORMAT, "next_page").toVanilla(lang)));
                } else {
                    nextButton.color(EnumMCColor.DARK_AQUA.getColor());
                }
                helpInfo.append(nextButton);
            }
        } else {
            try {
                EnumCommandType type = EnumCommandType.valueOf(command.toUpperCase());
                helpInfo = Component.empty();
                String com = "/" + BaniraCommand.getCommand(type);
                helpInfo.append(Component.literal(com)
                                .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.transLang(BaniraCodex.MODID, lang, EnumI18nType.FORMAT, "click_to_suggest").toVanilla(lang)))
                        )
                        .append("\n")
                        .append(Component.transLang(BaniraCodex.MODID, lang, EnumI18nType.FORMAT, command.toLowerCase() + "_detail").color(EnumMCColor.GRAY.getColor()));
            } catch (IllegalArgumentException e) {
                helpInfo = Component.trans(BaniraCodex.MODID, EnumI18nType.FORMAT, "command_not_found").color(0xFFFF0000);
            }
        }
        MessageUtils.sendMessage(player, helpInfo);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String input = CommandUtils.getStringEmpty(context, "command");
        boolean isInputEmpty = input == null || input.isEmpty();
        int totalPages = (int) Math.ceil((double) BaniraCommand.HELP_MESSAGE.size() / CommonConfig.get().help().helpInfoNumPerPage());
        for (int i = 0; i < totalPages && isInputEmpty; i++) {
            builder.suggest(i + 1);
        }
        for (EnumCommandType type : Arrays.stream(EnumCommandType.values())
                .filter(t -> t != EnumCommandType.HELP)
                .filter(t -> !t.ignore())
                .filter(t -> !t.name().toLowerCase().contains("concise"))
                .sorted(Comparator.comparingInt(EnumCommandType::sort))
                .collect(Collectors.toList())) {
            if (isInputEmpty || type.name().toLowerCase().contains(input.toLowerCase())) {
                builder.suggest(type.name());
            }
        }
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal("help")
                .executes(HelpCommand::execute)
                .then(Commands.argument("command", StringArgumentType.word())
                        .suggests(HelpCommand::suggestion)
                        .executes(HelpCommand::execute)
                );
    }
}
