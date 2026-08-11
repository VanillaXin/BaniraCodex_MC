package xin.vanilla.banira.common.util;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.NonNull;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.permission.BaniraVirtualPermission;
import xin.vanilla.banira.api.permission.BaniraVirtualPermissions;
import xin.vanilla.banira.common.api.ICommandNotify;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigListSpecHelper;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.internal.command.BaniraCommandAccess;
import xin.vanilla.banira.internal.server.BaniraServerAccess;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class CommandUtils {
    private CommandUtils() {
    }

    public static boolean checkModStatus(CommandContext<CommandSourceStack> context, Supplier<Boolean> modDisabled) {
        if (modDisabled.get()) {
            CommandSourceStack source = context.getSource();
            Entity entity = BaniraCommandAccess.sourceEntity(source);
            if (entity instanceof ServerPlayer) {
                MessageUtils.sendNotification((ServerPlayer) entity,
                        BaniraComponent.get().trans(EnumI18nType.WORD, "mod_disabled"),
                        NotificationTypeKeys.COMMAND_FEEDBACK);
            }
        }
        return modDisabled.get();
    }

    public static String getLanguage(CommandSourceStack source) {
        String lang = Translator.getServerLanguage();
        Entity entity = BaniraCommandAccess.sourceEntity(source);
        if (entity instanceof ServerPlayer) {
            try {
                lang = Translator.getPlayerLanguage(BaniraCommandAccess.sourcePlayer(source));
            } catch (Exception ignored) {
            }
        }
        return lang;
    }

    public static Entity getSourceEntity(CommandSourceStack source) {
        return BaniraCommandAccess.sourceEntity(source);
    }

    @Nullable
    public static ServerPlayer getSourcePlayer(CommandSourceStack source) {
        try {
            return BaniraCommandAccess.sourcePlayer(source);
        } catch (CommandSyntaxException ignored) {
            return null;
        }
    }

    public static ServerPlayer requireSourcePlayer(CommandSourceStack source) throws CommandSyntaxException {
        return BaniraCommandAccess.sourcePlayer(source);
    }

    public static boolean hasPermission(CommandSourceStack source, int permission) {
        return BaniraCommandAccess.hasPermission(source, permission);
    }

    public static boolean hasVirtualPermission(CommandSourceStack source, BaniraVirtualPermission type) {
        return hasVirtualPermission(getSourceEntity(source), type);
    }

    /**
     * 判断是否拥有某个虚拟指令权限
     *
     * @param source 指令来源实体
     * @param type   指令类型
     */
    public static boolean hasVirtualPermission(Entity source, BaniraVirtualPermission type) {
        if (!(source instanceof Player)) {
            return false;
        }
        Player player = (Player) source;
        return VirtualPermissionManager.getRawVirtualPermission(player).contains(BaniraVirtualPermissions.key(type));
    }

    /**
     * 对中立玩家句柄判断指定完整虚拟权限键（{@code modId:id}）。
     */
    public static boolean hasVirtualPermission(Object player, String fullPermissionKey) {
        return player instanceof Player && hasVirtualPermission((Player) player, fullPermissionKey);
    }

    /**
     * 是否拥有指定完整虚拟权限键（{@code modId:id}）。
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
        return BaniraPlatforms.isInstalled()
                && BaniraCommandAccess.executePlayerCommand(player, command, permission, suppressedOutput);
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
        if (BaniraPlatforms.isInstalled()) {
            BaniraServerAccess.refreshPlayerPermission(player);
        }
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
        try {
            return BaniraCommandAccess.dimension(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException e) {
            return defaultValue;
        }
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
        return BaniraCommandAccess.playerOrSelf(context, name);
    }

    /**
     * 尝试获取玩家参数，若无则返回 null
     */
    @Nullable
    public static ServerPlayer getPlayerOptional(CommandContext<CommandSourceStack> context, String name) {
        try {
            return BaniraCommandAccess.player(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return null;
        }
    }

    /**
     * 尝试获取玩家列表参数，若无则返回 fallback
     */
    public static Collection<ServerPlayer> getPlayersOptional(CommandContext<CommandSourceStack> context, String name, Collection<ServerPlayer> fallback) {
        try {
            return BaniraCommandAccess.players(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return fallback;
        }
    }

    /**
     * 尝试获取维度参数，若无则返回 defaultValue 对应的 ResourceKey
     */
    public static ResourceKey<Level> getDimensionKeyDefault(CommandContext<CommandSourceStack> context, String name, ResourceKey<Level> defaultValue) {
        try {
            return BaniraCommandAccess.dimensionKey(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return defaultValue;
        }
    }

    /**
     * 若为第一次使用指令则进行提示
     */
    public static void notifyHelp(CommandContext<CommandSourceStack> context, ICommandNotify playerData, Component modName, String command) {
        CommandSourceStack source = context.getSource();
        Entity entity = BaniraCommandAccess.sourceEntity(source);
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            if (!playerData.isNotified()) {
                Component button = BaniraComponent.get().literal(command)
                        .color(EnumMCColor.AQUA.getColor())
                        .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, BaniraComponent.get().literal(command)
                                .toVanilla())
                        );
                MessageUtils.sendNotification(player,
                        BaniraComponent.get().trans(EnumI18nType.FORMAT, "notify_help", modName, button),
                        NotificationTypeKeys.HELP);
                playerData.setNotified(true);
            }
        }
    }

    // endregion 指令参数相关


    // region config modifier

    public static void configKeySuggestion(ConfigHolder holder, SuggestionsBuilder builder, String configKey) {
        if (holder == null || holder.valuePaths().isEmpty()) {
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

    public static void configValueSuggestion(ConfigHolder holder, SuggestionsBuilder builder, String configKey) {
        if (holder == null) {
            return;
        }
        String path = findConfigPath(holder, configKey);
        if (path == null) {
            return;
        }
        ConfigEntryDescriptor desc = holder.getDescriptor(path);
        if (desc == null) {
            return;
        }
        Object currentValue = holder.get(path);
        builder.suggest(String.valueOf(currentValue));
        if (desc.getDefaultValue() != null) {
            builder.suggest(String.valueOf(desc.getDefaultValue()));
        }
        if (desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.BOOLEAN
                || desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.BOOLEAN_LIST) {
            builder.suggest("true").suggest("false");
        } else if ((desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.ENUM
                || desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.ENUM_LIST)
                && desc.getEnumClass() != null) {
            Class<? extends Enum<?>> enumClass = desc.getEnumClass();
            for (Object c : enumClass.getEnumConstants()) {
                builder.suggest(((Enum<?>) c).name());
            }
        }
    }

    public static int executeModifyConfig(ConfigHolder holder, CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String configKey = StringArgumentType.getString(context, "configKey");
        String configValue = StringArgumentType.getString(context, "configValue");

        String path = findConfigPath(holder, configKey);
        if (path == null) {
            MessageUtils.sendMessage(source, false, BaniraComponent.get().transAuto("config_key_absent", configKey));
            return 0;
        }
        ConfigEntryDescriptor desc = holder.getDescriptor(path);
        if (desc == null) {
            MessageUtils.sendMessage(source, false, BaniraComponent.get().transAuto("config_key_absent", configKey));
            return 0;
        }

        Object parsed;
        try {
            parsed = parseStringToDescriptor(configValue, desc);
        } catch (Exception e) {
            MessageUtils.sendMessage(source, false, BaniraComponent.get().transAuto("config_value_parse_error", configValue, e.getMessage()));
            return 0;
        }

        if (!validateConfigValue(desc, parsed)) {
            MessageUtils.sendMessage(source, false, BaniraComponent.get().transAuto("config_value_set_error", configKey, configValue));
            return 0;
        }
        holder.set(path, parsed);

        holder.save();
        MessageUtils.sendMessageWithAdmin(source, true, BaniraComponent.get().transAuto("config_value_set_success", path, parsed));
        return 1;
    }

    private static String findConfigPath(ConfigHolder holder, String key) {
        if (holder == null || key == null) {
            return null;
        }
        if (holder.valuePaths().contains(key)) {
            return key;
        }
        List<String> matches = holder.valuePaths().stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).contains(key.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        if (matches.size() == 1) {
            return matches.get(0);
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseStringToDescriptor(String value, ConfigEntryDescriptor desc) {
        if (desc.isListType()) {
            String[] parts = value.split(",");
            List<Object> parsed = new ArrayList<>(parts.length);
            for (String part : parts) {
                Object one = ConfigListSpecHelper.coerceListElement(part.trim(), desc.getValueType(), desc.getEnumClass(),
                        desc.getMinValue(), desc.getMaxValue(), desc.getDecimalPlaces());
                if (one == null) {
                    throw new IllegalArgumentException("Invalid list element: " + part.trim());
                }
                parsed.add(one);
            }
            return parsed;
        }
        switch (desc.getValueType()) {
            case BOOLEAN:
                return StringUtils.stringToBoolean(value);
            case INTEGER:
                return NumberUtils.toInt(value);
            case LONG:
                return NumberUtils.toLong(value);
            case DOUBLE:
                return NumberUtils.toDouble(value);
            case ENUM:
                Class<? extends Enum<?>> enumClass = desc.getEnumClass();
                if (enumClass == null) {
                    throw new IllegalArgumentException("Missing enum class");
                }
                for (Object c : enumClass.getEnumConstants()) {
                    Enum<?> e = (Enum<?>) c;
                    if (e.name().equalsIgnoreCase(value) || e.toString().equalsIgnoreCase(value)) {
                        return Enum.valueOf((Class) enumClass, e.name());
                    }
                }
                throw new IllegalArgumentException("Unknown enum constant: " + value);
            case STRING:
            default:
                return value;
        }
    }

    private static boolean validateConfigValue(ConfigEntryDescriptor desc, Object value) {
        if (desc == null) {
            return false;
        }
        if (desc.isListType()) {
            if (!(value instanceof List)) {
                return false;
            }
            for (Object one : (List<?>) value) {
                if (ConfigListSpecHelper.coerceListElement(one, desc.getValueType(), desc.getEnumClass(),
                        desc.getMinValue(), desc.getMaxValue(), desc.getDecimalPlaces()) == null) {
                    return false;
                }
            }
            return true;
        }
        switch (desc.getValueType()) {
            case INTEGER:
            case LONG:
            case DOUBLE:
                if (!(value instanceof Number)) {
                    return false;
                }
                double v = ((Number) value).doubleValue();
                return (desc.getMinValue() == null || v >= desc.getMinValue().doubleValue())
                        && (desc.getMaxValue() == null || v <= desc.getMaxValue().doubleValue());
            case BOOLEAN:
                return value instanceof Boolean;
            case ENUM:
                return value instanceof Enum && desc.getEnumClass() != null
                        && desc.getEnumClass().isAssignableFrom(value.getClass());
            case STRING:
            default:
                return value instanceof String;
        }
    }

    // endregion config modifier

}
