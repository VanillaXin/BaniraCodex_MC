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
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.permission.BaniraVirtualPermission;
import xin.vanilla.banira.api.permission.BaniraVirtualPermissions;
import xin.vanilla.banira.common.api.ICommandNotify;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
     * 判断是否拥有某个虚拟权限
     *
     * @param source 指令来源实体
     * @param type   权限类型
     */
    public static boolean hasVirtualPermission(Entity source, BaniraVirtualPermission type) {
        if (!(source instanceof Player player)) {
            return false;
        }
        return VirtualPermissionManager.getRawVirtualPermission(player).contains(BaniraVirtualPermissions.key(type));
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
            result = server.getCommands().performCommand(server.getCommands().getDispatcher().parse(command, commandSourceStack), command) > 0;
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
            server = BaniraServerRuntime.server();
        }
        if (server == null) return;
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


    // region config modifier

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseStringToType(String parsedStr, Class<?> targetType) throws IllegalArgumentException {
        if (targetType == Boolean.class || targetType == boolean.class) {
            return StringUtils.stringToBoolean(parsedStr);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return NumberUtils.toInt(parsedStr);
        }
        if (targetType == Long.class || targetType == long.class) {
            return NumberUtils.toLong(parsedStr);
        }
        if (targetType == Double.class || targetType == double.class) {
            return NumberUtils.toDouble(parsedStr);
        }
        if (Enum.class.isAssignableFrom(targetType)) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) targetType;
            for (Object c : enumClass.getEnumConstants()) {
                if (c.toString().equalsIgnoreCase(parsedStr) || ((Enum<?>) c).name().equalsIgnoreCase(parsedStr)) {
                    return Enum.valueOf(enumClass, ((Enum<?>) c).name());
                }
            }
            throw new IllegalArgumentException("Unknown enum constant: " + parsedStr);
        }
        if (targetType == String.class) {
            return parsedStr;
        }
        if (List.class.isAssignableFrom(targetType)) {
            String[] parts = parsedStr.split(",");
            return Arrays.stream(parts).map(String::trim).collect(Collectors.toList());
        }
        return parsedStr;
    }


    public static void configKeySuggestion(ConfigHolder holder, SuggestionsBuilder builder, String configKey) {
        if (holder == null || CollectionUtils.isNullOrEmpty(holder.valuePaths())) {
            return;
        }
        if (configKey == null) {
            configKey = "";
        }
        configKey = configKey.trim();
        boolean isEmpty = configKey.isEmpty();
        String lowerInput = configKey.toLowerCase(Locale.ROOT);

        if (isEmpty) {
            for (String key : holder.valuePaths()) {
                builder.suggest(key);
            }
            return;
        }

        if (configKey.indexOf('.') >= 0) {
            String[] inputParts = lowerInput.split("\\.");
            int prefixSegments = inputParts.length - 1;
            String lastInputPart = inputParts[inputParts.length - 1];

            for (String key : holder.valuePaths()) {
                String lowerKey = key.toLowerCase(Locale.ROOT);
                String[] keyParts = lowerKey.split("\\.");
                if (keyParts.length < prefixSegments + 1) {
                    continue;
                }
                boolean prefixMatches = true;
                for (int i = 0; i < prefixSegments; i++) {
                    if (!keyParts[i].equals(inputParts[i])) {
                        prefixMatches = false;
                        break;
                    }
                }
                if (!prefixMatches) {
                    continue;
                }
                String lastKeyPart = keyParts[keyParts.length - 1];
                if (lastKeyPart.contains(lastInputPart)) {
                    builder.suggest(key);
                }
            }
        } else {
            for (String key : holder.valuePaths()) {
                if (key.toLowerCase(Locale.ROOT).contains(lowerInput)) {
                    builder.suggest(key);
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void configValueSuggestion(ConfigHolder holder, SuggestionsBuilder builder, String configKey) {
        if (holder == null) {
            return;
        }
        String path = holder.findValuePath(configKey);
        if (path == null) {
            return;
        }
        builder.suggest(String.valueOf(holder.get(path)));
        Object defaultValue = holder.defaultValue(path);
        if (defaultValue != null) {
            builder.suggest(String.valueOf(defaultValue));
        }
        Class<?> type = holder.valueClass(path);
        if (type == Boolean.class || type == boolean.class) {
            builder.suggest("true").suggest("false");
        } else if (Enum.class.isAssignableFrom(type)) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) type;
            for (Object c : enumClass.getEnumConstants()) {
                builder.suggest(((Enum<?>) c).name());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int executeModifyConfig(ConfigHolder holder, CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String configKey = StringArgumentType.getString(context, "configKey");
        String configValue = StringArgumentType.getString(context, "configValue");

        String path = holder.findValuePath(configKey);
        if (path == null) {
            MessageUtils.sendMessage(source, false, BaniraComponent.get().transAuto("config_key_absent", configKey));
            return 0;
        }

        Class<?> type = holder.valueClass(path);
        Object parsed;
        try {
            parsed = parseStringToType(configValue, type);
        } catch (Exception e) {
            MessageUtils.sendMessage(source, false, BaniraComponent.get().transAuto("config_value_parse_error", configValue, e.getMessage()));
            return 0;
        }

        if (!holder.setIfValid(path, parsed)) {
            MessageUtils.sendMessage(source, false, BaniraComponent.get().transAuto("config_value_set_error", configKey, configValue));
            return 0;
        }

        holder.save();
        MessageUtils.sendMessageWithAdmin(source, true, BaniraComponent.get().transAuto("config_value_set_success", path, parsed));
        return 1;
    }

    // endregion config modifier

}
