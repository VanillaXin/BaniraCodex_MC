package xin.vanilla.banira.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.permission.BaniraVirtualPermission;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumOperationType;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.common.util.VirtualPermissionManager;
import xin.vanilla.banira.internal.config.CommonConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 虚拟 OP 权限管理指令实现
 */
public final class VirtualOpCommand {
    private VirtualOpCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnumOperationType type = EnumOperationType.valueOfEx(StringArgumentType.getString(context, "operation"));
        if (type == null) {
            MessageUtils.sendMessage(source, false,
                    BaniraComponent.get().trans(EnumI18nType.WORD, "invalid_operation"));
            return 0;
        }
        if (!CommandUtils.hasVirtualPermission(source.getEntity(), EnumCommandType.VIRTUAL_OP)
                && (source.getEntity() == null || !source.hasPermission(CommonConfig.get().permission().virtualOpPermission()))) {
            MessageUtils.sendMessage(source, false,
                    BaniraComponent.get().trans(EnumI18nType.WORD, "command_disabled"));
            return 0;
        }
        EnumCommandType[] rules;
        try {
            rules = Arrays.stream(CommandUtils.getStringDefault(context, "rules", "").split(","))
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(EnumCommandType::valueOf)
                    .toArray(EnumCommandType[]::new);
        } catch (IllegalArgumentException ignored) {
            rules = new EnumCommandType[]{};
        }
        List<ServerPlayer> targetList = new ArrayList<>();
        try {
            targetList.addAll(EntityArgument.getPlayers(context, "player"));
        } catch (IllegalArgumentException ignored) {
        }
        String language = CommonConfig.get().language().defaultLanguage();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
            language = Translator.getPlayerLanguage(source.getPlayerOrException());
        }
        for (ServerPlayer target : targetList) {
            switch (type) {
                case ADD:
                    VirtualPermissionManager.addVirtualPermission(target, rules);
                    break;
                case SET:
                    VirtualPermissionManager.setVirtualPermission(target, rules);
                    break;
                case DEL:
                case REMOVE:
                    VirtualPermissionManager.delVirtualPermission(target, rules);
                    break;
                case CLEAR:
                    VirtualPermissionManager.clearVirtualPermission(target);
                    break;
                case GET:
                case LIST:
                    break;
                default:
                    break;
            }
            Set<EnumCommandType> permissions = VirtualPermissionManager.getVirtualPermission(target);
            String permissionsStr = VirtualPermissionManager.buildPermissionsString(permissions);
            String targetName = target.getDisplayName().getString();
            String languageFinal = language;
            String permissionsStrFinal = permissionsStr;
            MessageUtils.sendNotification(target, BaniraComponent.get().trans(EnumI18nType.FORMAT,
                    "player_virtual_op", targetName, permissionsStrFinal),
                    NotificationTypeKeys.COMMAND_FEEDBACK);
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                if (!target.getStringUUID().equalsIgnoreCase(player.getStringUUID())) {
                    MessageUtils.sendNotification(player, BaniraComponent.get().trans(EnumI18nType.FORMAT,
                            "player_virtual_op", targetName, permissionsStrFinal),
                            NotificationTypeKeys.COMMAND_FEEDBACK);
                }
            } else {
                source.sendSuccess(() -> BaniraComponent.get().trans(EnumI18nType.FORMAT, "player_virtual_op", targetName, permissionsStrFinal).languageCode(languageFinal).toChat(languageFinal), true);
            }
            CommandUtils.refreshPermission(target);
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> operationSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        builder.suggest(EnumOperationType.ADD.name().toLowerCase());
        builder.suggest(EnumOperationType.SET.name().toLowerCase());
        builder.suggest(EnumOperationType.DEL.name().toLowerCase());
        builder.suggest(EnumOperationType.CLEAR.name().toLowerCase());
        builder.suggest(EnumOperationType.GET.name().toLowerCase());
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> rulesSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String operation = StringArgumentType.getString(context, "operation");
        if (operation.equalsIgnoreCase(EnumOperationType.GET.name())
                || operation.equalsIgnoreCase(EnumOperationType.CLEAR.name())
                || operation.equalsIgnoreCase(EnumOperationType.LIST.name())) {
            return builder.buildFuture();
        }
        String input = CommandUtils.getStringEmpty(context, "rules").replace(" ", ",");
        String[] split = input.split(",");
        String current = input.endsWith(",") ? "" : split[split.length - 1];
        Arrays.stream(EnumCommandType.values())
                .filter(BaniraVirtualPermission::op)
                .filter(type -> Arrays.stream(split).noneMatch(in -> in.equalsIgnoreCase(type.name())))
                .filter(type -> (current == null || current.isEmpty()) || type.name().toLowerCase().contains(current.toLowerCase()))
                .forEach(type -> builder.suggest(type.name()));
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().command().commandVirtualOp())
                .requires(source -> (source.getEntity() != null && CommandUtils.hasVirtualPermission(source.getEntity(), EnumCommandType.VIRTUAL_OP))
                        || source.hasPermission(CommonConfig.get().permission().virtualOpPermission()))
                .then(Commands.argument("operation", StringArgumentType.word())
                        .suggests(VirtualOpCommand::operationSuggestion)
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(VirtualOpCommand::execute)
                                .then(Commands.argument("rules", StringArgumentType.greedyString())
                                        .suggests(VirtualOpCommand::rulesSuggestion)
                                        .executes(VirtualOpCommand::execute)
                                )
                        )
                );
    }
}
