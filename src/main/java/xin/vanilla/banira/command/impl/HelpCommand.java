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
import xin.vanilla.banira.BaniraComponent;
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
        ServerPlayerEntity player = CommandUtils.requireSourcePlayer(context.getSource());
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
            String headerTemplate = CommonConfig.get().help().helpHeader();
            if (headerTemplate == null || headerTemplate.isEmpty()) {
                headerTemplate = "-----==== Banira Codex Help (%d/%d) ====-----";
            }
            helpInfo = BaniraComponent.get().literal(String.format(headerTemplate, page, pages) + "\n");
            for (int i = 0; (page - 1) * helpNumPerPage + i < BaniraCommand.HELP_MESSAGE.size() && i < helpNumPerPage; i++) {
                KeyValue<String, EnumCommandType> keyValue = BaniraCommand.HELP_MESSAGE.get((page - 1) * helpNumPerPage + i);
                Component commandTips;
                if (keyValue.value().name().toLowerCase().contains("concise")) {
                    commandTips = BaniraComponent.get().transLang(lang, EnumI18nType.FORMAT, "concise",
                            BaniraCommand.getCommand(keyValue.value().replaceConcise()));
                } else {
                    commandTips = BaniraComponent.get().transLang(lang, EnumI18nType.WORD, keyValue.value().name().toLowerCase());
                }
                commandTips.color(EnumMCColor.GRAY.getColor());
                String com = "/" + keyValue.key();
                helpInfo.append(BaniraComponent.get().literal(com)
                                .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        BaniraComponent.get().transLang(lang, EnumI18nType.WORD, "click_to_suggest").toVanilla(lang)))
                        )
                        .append(BaniraComponent.get().literal(" -> ").color(EnumMCColor.YELLOW.getColor()))
                        .append(commandTips);
                if (i != Math.min((page - 1) * helpNumPerPage + helpNumPerPage, BaniraCommand.HELP_MESSAGE.size()) - 1) {
                    helpInfo.append("\n");
                }
            }
            if (pages > 1) {
                helpInfo.append("\n");
                Component prevButton = BaniraComponent.get().literal("<<< ");
                if (page > 1) {
                    prevButton.color(EnumMCColor.AQUA.getColor())
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/%s %s %d", BaniraCommand.getCommandPrefix(),
                                            CommonConfig.get().command().commandHelp(), page - 1)))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    BaniraComponent.get().transLang(lang, EnumI18nType.WORD, "previous_page").toVanilla(lang)));
                } else {
                    prevButton.color(EnumMCColor.DARK_AQUA.getColor());
                }
                helpInfo.append(prevButton);
                helpInfo.append(BaniraComponent.get().literal(String.format(" %s/%s ",
                                StringUtils.padOptimizedLeft(page, String.valueOf(pages).length(), " "),
                                pages))
                        .color(EnumMCColor.WHITE.getColor()));
                Component nextButton = BaniraComponent.get().literal(" >>>");
                if (page < pages) {
                    nextButton.color(EnumMCColor.AQUA.getColor())
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/%s %s %d", BaniraCommand.getCommandPrefix(),
                                            CommonConfig.get().command().commandHelp(), page + 1)))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    BaniraComponent.get().transLang(lang, EnumI18nType.WORD, "next_page").toVanilla(lang)));
                } else {
                    nextButton.color(EnumMCColor.DARK_AQUA.getColor());
                }
                helpInfo.append(nextButton);
            }
        } else {
            try {
                EnumCommandType type = EnumCommandType.valueOf(command.toUpperCase());
                helpInfo = BaniraComponent.get().empty();
                String com = "/" + BaniraCommand.getCommand(type);
                helpInfo.append(BaniraComponent.get().literal(com)
                                .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        BaniraComponent.get().transLang(lang, EnumI18nType.WORD, "click_to_suggest").toVanilla(lang)))
                        )
                        .append("\n")
                        .append(commandDetailLine(type.replaceConcise(), lang).color(EnumMCColor.GRAY.getColor()));
            } catch (IllegalArgumentException e) {
                helpInfo = BaniraComponent.get().trans(EnumI18nType.WORD, "command_not_found").color(0xFFFF0000);
            }
        }
        MessageUtils.sendMessage(player, helpInfo);
        return 1;
    }

    private static Component commandDetailLine(EnumCommandType baseType, String lang) {
        String prefix = BaniraCommand.getCommandPrefix();
        switch (baseType) {
            case HELP:
                return BaniraComponent.get().transLang(lang, EnumI18nType.FORMAT, "command_detail_help",
                        prefix, CommonConfig.get().command().commandHelp());
            case LANGUAGE: {
                String sub = CommonConfig.get().command().commandLanguage();
                if (sub == null || sub.isEmpty()) {
                    sub = "language";
                }
                return BaniraComponent.get().transLang(lang, EnumI18nType.FORMAT, "command_detail_language",
                        prefix, sub);
            }
            case VIRTUAL_OP: {
                String sub = CommonConfig.get().command().commandVirtualOp();
                if (sub == null || sub.isEmpty()) {
                    sub = "virtual_op";
                }
                return BaniraComponent.get().transLang(lang, EnumI18nType.FORMAT, "command_detail_virtual_op",
                        prefix, sub);
            }
            default:
                return BaniraComponent.get().transLang(lang, EnumI18nType.WORD, baseType.name().toLowerCase() + "_detail");
        }
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
        return Commands.literal(CommonConfig.get().command().commandHelp())
                .executes(HelpCommand::execute)
                .then(Commands.argument("command", StringArgumentType.word())
                        .suggests(HelpCommand::suggestion)
                        .executes(HelpCommand::execute)
                );
    }
}
