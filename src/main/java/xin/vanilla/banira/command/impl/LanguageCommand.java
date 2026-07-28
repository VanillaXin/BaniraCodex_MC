package xin.vanilla.banira.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.config.CustomConfig;

import java.util.concurrent.CompletableFuture;

/**
 * 语言切换指令实现
 */
public final class LanguageCommand {
    private LanguageCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.requireSourcePlayer(context.getSource());
        String language = StringArgumentType.getString(context, "language");
        Translator translator = (Translator) Translator.of(BaniraCodex.MODID);
        if (translator.getI18nFiles().contains(language)) {
            CustomConfig.setPlayerLanguage(PlayerUtils.getPlayerUUIDString(player), language);
            MessageUtils.sendNotification(player,
                    BaniraComponent.get().trans(EnumI18nType.FORMAT, "player_default_language", language),
                    NotificationTypeKeys.COMMAND_FEEDBACK);
        } else if ("server".equalsIgnoreCase(language) || "client".equalsIgnoreCase(language)) {
            CustomConfig.setPlayerLanguage(PlayerUtils.getPlayerUUIDString(player), language);
            MessageUtils.sendNotification(player,
                    BaniraComponent.get().trans(EnumI18nType.FORMAT, "player_default_language", language),
                    NotificationTypeKeys.COMMAND_FEEDBACK);
        } else {
            MessageUtils.sendNotification(player,
                    BaniraComponent.get().trans(EnumI18nType.FORMAT, "language_not_exist").color(0xFFFF0000),
                    NotificationTypeKeys.COMMAND_FEEDBACK);
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = CommandUtils.getLanguage(context.getSource());
        Translator translator = (Translator) Translator.of(BaniraCodex.MODID);
        Component clientTooltip = BaniraComponent.get().transLang(lang, EnumI18nType.WORD, "suggest_language_client");
        Component serverTooltip = BaniraComponent.get().transLang(lang, EnumI18nType.WORD, "suggest_language_server");
        builder.suggest("client", clientTooltip.toVanilla(lang));
        builder.suggest("server", serverTooltip.toVanilla(lang));
        translator.getI18nFiles().forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().command().commandLanguage())
                .then(Commands.argument("language", StringArgumentType.word())
                        .suggests(LanguageCommand::suggestion)
                        .executes(LanguageCommand::execute)
                );
    }
}
