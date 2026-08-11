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
import xin.vanilla.banira.api.permission.BaniraVirtualPermissionRegistry;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumOperationType;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.common.util.VirtualPermissionManager;
import xin.vanilla.banira.internal.config.CommonConfig;

import java.util.*;
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
        String rawRules = CommandUtils.getStringDefault(context, "rules", "");
        Optional<LinkedHashSet<String>> resolvedRules = resolvePermissionKeys(rawRules);
        if (!resolvedRules.isPresent()) {
            MessageUtils.sendMessage(source, false, BaniraComponent.get().trans(
                    EnumI18nType.FORMAT, "unknown_virtual_permission", rawRules));
            return 0;
        }
        Set<String> rules = resolvedRules.get();
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
            if (type == EnumOperationType.CLEAR) {
                VirtualPermissionManager.clearVirtualPermission(target);
            } else if (type != EnumOperationType.GET && type != EnumOperationType.LIST) {
                VirtualPermissionManager.modifyVirtualPermissions(target, type, rules);
            }
            Set<String> permissions = VirtualPermissionManager.getRawVirtualPermission(target);
            String permissionsStr = VirtualPermissionManager.buildRawPermissionsString(permissions);
            MessageUtils.sendNotification(target, BaniraComponent.get().trans(EnumI18nType.FORMAT,
                    "player_virtual_op", target.getDisplayName().getString(), permissionsStr),
                    NotificationTypeKeys.COMMAND_FEEDBACK);
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                if (!target.getStringUUID().equalsIgnoreCase(player.getStringUUID())) {
                    MessageUtils.sendNotification(player, BaniraComponent.get().trans(EnumI18nType.FORMAT,
                            "player_virtual_op", target.getDisplayName().getString(), permissionsStr),
                            NotificationTypeKeys.COMMAND_FEEDBACK);
                }
            } else {
                final String langFinal = language;
                final String targetDisplayName = target.getDisplayName().getString();
                source.sendSuccess(() -> BaniraComponent.get().trans(EnumI18nType.FORMAT, "player_virtual_op", targetDisplayName, permissionsStr).languageCode(langFinal).toChat(langFinal), true);
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
        BaniraVirtualPermissionRegistry.all().stream()
                .filter(BaniraVirtualPermission::op)
                .filter(permission -> Arrays.stream(split)
                        .noneMatch(in -> in.equalsIgnoreCase(permission.key())))
                .filter(permission -> current.isEmpty()
                        || permission.key().toLowerCase(Locale.ROOT).contains(current.toLowerCase(Locale.ROOT)))
                .forEach(permission -> builder.suggest(permission.key()));
        return builder.buildFuture();
    }

    static Optional<LinkedHashSet<String>> resolvePermissionKeys(String input) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (input == null || input.trim().isEmpty()) {
            return Optional.of(result);
        }
        for (String token : input.replace(' ', ',').split(",")) {
            String key = token.trim();
            if (key.isEmpty()) continue;
            Optional<BaniraVirtualPermission> permission = BaniraVirtualPermissionRegistry.find(key);
            if (!permission.isPresent()) return Optional.empty();
            result.add(permission.get().key());
        }
        return Optional.of(result);
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
