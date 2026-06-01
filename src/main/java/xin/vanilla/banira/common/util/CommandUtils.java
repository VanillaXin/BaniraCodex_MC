package xin.vanilla.banira.common.util;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.NonNull;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.api.ICommandNotify;
import xin.vanilla.banira.common.api.IVirtualPermissionType;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigListSpecHelper;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumMCColor;

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

    private static final Logger LOGGER = LogManager.getLogger();


    public static boolean checkModStatus(CommandContext<CommandSource> context, Supplier<Boolean> modDisabled) {
        if (modDisabled.get()) {
            CommandSource source = context.getSource();
            Entity entity = source.getEntity();
            if (entity instanceof ServerPlayerEntity) {
                MessageUtils.sendMessage((ServerPlayerEntity) entity, BaniraComponent.get().trans(EnumI18nType.WORD, "mod_disabled"));
            }
        }
        return modDisabled.get();
    }

    public static String getLanguage(CommandSource source) {
        String lang = Translator.getServerLanguage();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
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
        if (!(source instanceof PlayerEntity)) {
            return false;
        }
        PlayerEntity player = (PlayerEntity) source;
        return VirtualPermissionManager.getRawVirtualPermission(player).contains(type.modId() + ":" + type.id());
    }

    /**
     * 是否拥有指定完整虚拟权限键（{@code modId:id}）
     */
    public static boolean hasVirtualPermission(PlayerEntity player, String fullPermissionKey) {
        if (player == null || fullPermissionKey == null || fullPermissionKey.isEmpty()) {
            return false;
        }
        return VirtualPermissionManager.getRawVirtualPermission(player).contains(fullPermissionKey);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommand(@NonNull ServerPlayerEntity player, @NonNull String command, int permission, boolean suppressedOutput) {
        boolean result = false;
        try {
            MinecraftServer server = player.getServer();
            CommandSource commandSourceStack = player.createCommandSourceStack();
            if (permission > 0) {
                commandSourceStack = commandSourceStack.withPermission(permission);
            }
            if (suppressedOutput) {
                commandSourceStack = commandSourceStack.withSuppressedOutput();
            }
            result = server.getCommands().performCommand(commandSourceStack, command) > 0;
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
        }
        return result;
    }

    /**
     * 执行指令
     */
    public static boolean executeCommand(@NonNull ServerPlayerEntity player, @NonNull String command) {
        return executeCommand(player, command, 0, false);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommandNoOutput(@NonNull ServerPlayerEntity player, @NonNull String command) {
        return executeCommandNoOutput(player, command, 0);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommandNoOutput(@NonNull ServerPlayerEntity player, @NonNull String command, int permission) {
        return executeCommand(player, command, permission, true);
    }

    /**
     * 刷新玩家权限
     */
    public static void refreshPermission(@NonNull ServerPlayerEntity player) {
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

    public static ServerWorld getDimensionDefault(CommandContext<CommandSource> context, String name, ServerWorld defaultValue) {
        ServerWorld result;
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
    public static ServerPlayerEntity getPlayerOrSelf(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        try {
            return EntityArgument.getPlayer(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            CommandSource source = context.getSource();
            if (source.getEntity() instanceof ServerPlayerEntity) {
                return source.getPlayerOrException();
            }
            throw CommandSource.ERROR_NOT_PLAYER.create();
        }
    }

    /**
     * 尝试获取玩家参数，若无则返回 null
     */
    @Nullable
    public static ServerPlayerEntity getPlayerOptional(CommandContext<CommandSource> context, String name) {
        try {
            return EntityArgument.getPlayer(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return null;
        }
    }

    /**
     * 尝试获取玩家列表参数，若无则返回 fallback
     */
    public static Collection<ServerPlayerEntity> getPlayersOptional(CommandContext<CommandSource> context, String name, Collection<ServerPlayerEntity> fallback) {
        try {
            return EntityArgument.getPlayers(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return fallback;
        }
    }

    /**
     * 尝试获取维度参数，若无则返回 defaultValue 对应的 RegistryKey
     */
    public static RegistryKey<World> getDimensionKeyDefault(CommandContext<CommandSource> context, String name, RegistryKey<World> defaultValue) {
        try {
            return DimensionArgument.getDimension(context, name).dimension();
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return defaultValue;
        }
    }

    /**
     * 若为第一次使用指令则进行提示
     */
    public static void notifyHelp(CommandContext<CommandSource> context, ICommandNotify playerData, Component modName, String command) {
        CommandSource source = context.getSource();
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
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

    public static void configKeySuggestion(ConfigHolder holder, SuggestionsBuilder builder, String configKey) {
        if (holder == null || holder.getValuePaths().isEmpty()) {
            return;
        }
        if (configKey == null) {
            configKey = "";
        }
        configKey = configKey.trim();
        boolean isEmpty = configKey.isEmpty();
        String lowerInput = configKey.toLowerCase(Locale.ROOT);

        if (isEmpty) {
            for (String key : holder.getValuePaths()) {
                builder.suggest(key);
            }
            return;
        }

        if (configKey.indexOf('.') >= 0) {
            String[] inputParts = lowerInput.split("\\.");
            int prefixSegments = inputParts.length - 1;
            String lastInputPart = inputParts[inputParts.length - 1];

            for (String key : holder.getValuePaths()) {
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
            for (String key : holder.getValuePaths()) {
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
        builder.suggest(String.valueOf(holder.get(path)));
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

    public static int executeModifyConfig(ConfigHolder holder, CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
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
        if (holder.getValuePaths().contains(key)) {
            return key;
        }
        List<String> matches = holder.getValuePaths().stream()
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
