package xin.vanilla.banira.common.util;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.NonNull;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.api.ICommandNotify;
import xin.vanilla.banira.common.api.IVirtualPermissionType;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumMCColor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Supplier;

public final class CommandUtils {
    private CommandUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();


    public static boolean checkModStatus(CommandContext<CommandSourceStack> context, Supplier<Boolean> modDisabled) {
        if (modDisabled.get()) {
            CommandSourceStack source = context.getSource();
            Entity entity = source.getEntity();
            if (entity instanceof ServerPlayer player) {
                MessageUtils.sendMessage(player, BaniraComponent.get().trans(EnumI18nType.WORD, "mod_disabled"));
            }
        }
        return modDisabled.get();
    }

    public static String getLanguage(CommandSourceStack source) {
        String lang = Translator.getServerLanguage();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
            try {
                lang = Translator.getPlayerLanguage(source.getPlayerOrException());
            } catch (Exception ignored) {
            }
        }
        return lang;
    }

    /**
     * 判断是否拥有某个虚拟指令权限
     *
     * @param source 指令来源实体
     * @param type   指令类型
     */
    public static boolean hasVirtualPermission(Entity source, IVirtualPermissionType type) {
        if (!(source instanceof Player player)) {
            return false;
        }
        return VirtualPermissionManager.getRawVirtualPermission(player).contains(type.modId() + ":" + type.id());
    }

    /**
     * 是否拥有指定完整虚拟权限键（{@code modId:id}）
     */
    public static boolean hasVirtualPermission(Player player, String fullPermissionKey) {
        if (player == null || fullPermissionKey == null || fullPermissionKey.isEmpty()) {
            return false;
        }
        return VirtualPermissionManager.getRawVirtualPermission(player).contains(fullPermissionKey);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommand(@NonNull ServerPlayer player, @NonNull String command, int permission, boolean suppressedOutput) {
        boolean result = false;
        try {
            MinecraftServer server = player.getServer();
            CommandSourceStack commandSourceStack = player.createCommandSourceStack();
            if (permission > 0) {
                commandSourceStack = commandSourceStack.withPermission(permission);
            }
            if (suppressedOutput) {
                commandSourceStack = commandSourceStack.withSuppressedOutput();
            }
            if (server != null) {
                result = server.getCommands().performPrefixedCommand(commandSourceStack, command) > 0;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
        }
        return result;
    }

    /**
     * 执行指令
     */
    public static boolean executeCommand(@NonNull ServerPlayer player, @NonNull String command) {
        return executeCommand(player, command, 0, false);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommandNoOutput(@NonNull ServerPlayer player, @NonNull String command) {
        return executeCommandNoOutput(player, command, 0);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommandNoOutput(@NonNull ServerPlayer player, @NonNull String command, int permission) {
        return executeCommand(player, command, permission, true);
    }

    /**
     * 刷新玩家权限
     */
    public static void refreshPermission(@NonNull ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            server = BaniraCodex.serverInstance().key();
        }
        server.getPlayerList().sendPlayerPermissionLevel(player);
    }


    // region 指令参数相关

    public static void addSuggestion(SuggestionsBuilder suggestion, String input, String suggest) {
        if (suggest.contains(input) || StringUtils.isNullOrEmpty(input)) {
            suggestion.suggest(suggest);
        }
    }

    public static String getStringEmpty(CommandContext<?> context, String name) {
        return getStringDefault(context, name, "");
    }

    public static String getStringDefault(CommandContext<?> context, String name, String defaultValue) {
        String result;
        try {
            result = StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static String getStringEx(CommandContext<?> context, String name, String defaultValue) {
        String result;
        try {
            result = String.valueOf(context.getArgument(name, Object.class));
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static String replaceResourcePath(String s) {
        if (StringUtils.isNullOrEmpty(s)) return "";
        return s.substring(s.indexOf(":") + 1);
    }

    public static int getIntDefault(CommandContext<?> context, String name, int defaultValue) {
        int result;
        try {
            result = IntegerArgumentType.getInteger(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static long getLongDefault(CommandContext<?> context, String name, long defaultValue) {
        long result;
        try {
            result = LongArgumentType.getLong(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static boolean getBooleanDefault(CommandContext<?> context, String name, boolean defaultValue) {
        boolean result;
        try {
            result = BoolArgumentType.getBool(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static ServerLevel getDimensionDefault(CommandContext<CommandSourceStack> context, String name, ServerLevel defaultValue) {
        ServerLevel result;
        try {
            result = DimensionArgument.getDimension(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException e) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * 尝试获取布尔参数，若无则返回 null
     */
    @Nullable
    public static Boolean getBooleanOptional(CommandContext<?> context, String name) {
        try {
            return BoolArgumentType.getBool(context, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * 尝试获取 Double 参数，若无则返回 null
     */
    @Nullable
    public static Double getDoubleOptional(CommandContext<?> context, String name) {
        try {
            return DoubleArgumentType.getDouble(context, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * 尝试获取玩家参数，若无则返回执行者自身（若执行者为玩家），否则抛出 CommandSyntaxException
     */
    public static ServerPlayer getPlayerOrSelf(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        try {
            return EntityArgument.getPlayer(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            CommandSourceStack source = context.getSource();
            if (source.getEntity() instanceof ServerPlayer player) {
                return player;
            }
            throw CommandSourceStack.ERROR_NOT_PLAYER.create();
        }
    }

    /**
     * 尝试获取玩家参数，若无则返回 null
     */
    @Nullable
    public static ServerPlayer getPlayerOptional(CommandContext<CommandSourceStack> context, String name) {
        try {
            return EntityArgument.getPlayer(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return null;
        }
    }

    /**
     * 尝试获取玩家列表参数，若无则返回 fallback
     */
    public static Collection<ServerPlayer> getPlayersOptional(CommandContext<CommandSourceStack> context, String name, Collection<ServerPlayer> fallback) {
        try {
            return EntityArgument.getPlayers(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return fallback;
        }
    }

    /**
     * 尝试获取维度参数，若无则返回 defaultValue 对应的 RegistryKey
     */
    public static ResourceKey<Level> getDimensionKeyDefault(CommandContext<CommandSourceStack> context, String name, ResourceKey<Level> defaultValue) {
        try {
            return DimensionArgument.getDimension(context, name).dimension();
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return defaultValue;
        }
    }

    /**
     * 若为第一次使用指令则进行提示
     */
    public static void notifyHelp(CommandContext<CommandSourceStack> context, ICommandNotify playerData, Component modName, String command) {
        CommandSourceStack source = context.getSource();
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player) {
            if (!playerData.isNotified()) {
                Component button = BaniraComponent.get().literal(command)
                        .color(EnumMCColor.AQUA.getColor())
                        .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, BaniraComponent.get().literal(command)
                                .toVanilla())
                        );
                MessageUtils.sendMessage(player, BaniraComponent.get().trans(EnumI18nType.FORMAT, "notify_help", modName, button));
                playerData.setNotified(true);
            }
        }
    }

    // endregion 指令参数相关



}
