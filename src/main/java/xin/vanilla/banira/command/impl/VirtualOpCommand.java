package xin.vanilla.banira.command.impl;

import xin.vanilla.banira.BaniraComponent;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.api.IVirtualPermissionType;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumOperationType;
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

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        EnumOperationType type = EnumOperationType.fromString(StringArgumentType.getString(context, "operation"));
        if (type == null) {
            String lang = CommandUtils.getLanguage(source);
            source.sendFailure(BaniraComponent.get().trans(EnumI18nType.WORD, "invalid_operation").languageCode(lang).toChat(lang));
            return 0;
        }
        if (!CommandUtils.hasVirtualPermission(source.getEntity(), EnumCommandType.VIRTUAL_OP)
                && (source.getEntity() == null || !source.hasPermission(CommonConfig.get().permission().virtualOpPermission()))) {
            String lang = CommandUtils.getLanguage(source);
            source.sendFailure(BaniraComponent.get().trans(EnumI18nType.WORD, "command_disabled").languageCode(lang).toChat(lang));
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
        List<ServerPlayerEntity> targetList = new ArrayList<>();
        try {
            targetList.addAll(EntityArgument.getPlayers(context, "player"));
        } catch (IllegalArgumentException ignored) {
        }
        String language = CommonConfig.get().language().defaultLanguage();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
            language = Translator.getPlayerLanguage(source.getPlayerOrException());
        }
        for (ServerPlayerEntity target : targetList) {
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
            MessageUtils.sendMessage(target, BaniraComponent.get().trans(EnumI18nType.FORMAT, "player_virtual_op", target.getDisplayName().getString(), permissionsStr));
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = source.getPlayerOrException();
                if (!target.getStringUUID().equalsIgnoreCase(player.getStringUUID())) {
                    MessageUtils.sendMessage(player, BaniraComponent.get().trans(EnumI18nType.FORMAT, "player_virtual_op", target.getDisplayName().getString(), permissionsStr));
                }
            } else {
                source.sendSuccess(BaniraComponent.get().trans(EnumI18nType.FORMAT, "player_virtual_op", target.getDisplayName().getString(), permissionsStr).languageCode(language).toChat(language), true);
            }
            source.getServer().getPlayerList().sendPlayerPermissionLevel(target);
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> operationSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        builder.suggest(EnumOperationType.ADD.name().toLowerCase());
        builder.suggest(EnumOperationType.SET.name().toLowerCase());
        builder.suggest(EnumOperationType.DEL.name().toLowerCase());
        builder.suggest(EnumOperationType.CLEAR.name().toLowerCase());
        builder.suggest(EnumOperationType.GET.name().toLowerCase());
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> rulesSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
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
                .filter(IVirtualPermissionType::op)
                .filter(type -> Arrays.stream(split).noneMatch(in -> in.equalsIgnoreCase(type.name())))
                .filter(type -> (current == null || current.isEmpty()) || type.name().toLowerCase().contains(current.toLowerCase()))
                .forEach(type -> builder.suggest(type.name()));
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
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
