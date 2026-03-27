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
import net.minecraftforge.common.ForgeConfigSpec;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
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

    public static void configKeySuggestion(Class<?> configClazz, SuggestionsBuilder builder, String configKey) {
        if (configKey == null) configKey = "";
        configKey = configKey.trim();
        boolean isEmpty = configKey.isEmpty();

        Map<String, ForgeConfigSpec.ConfigValue<?>> map = buildConfigKeyMap(configClazz);
        if (CollectionUtils.isNullOrEmpty(map)) return;

        String lowerInput = configKey.toLowerCase(Locale.ROOT);

        if (isEmpty) {
            for (String key : map.keySet()) {
                builder.suggest(key);
            }
            return;
        }

        if (configKey.indexOf('.') >= 0) {
            String[] inputParts = lowerInput.split("\\.");
            int prefixSegments = inputParts.length - 1;
            String lastInputPart = inputParts[inputParts.length - 1];

            for (String key : map.keySet()) {
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
                if (!prefixMatches) continue;

                String lastKeyPart = keyParts[keyParts.length - 1];
                if (lastKeyPart.contains(lastInputPart)) {
                    builder.suggest(key);
                }
            }
        } else {
            for (String key : map.keySet()) {
                if (key.toLowerCase(Locale.ROOT).contains(lowerInput)) {
                    builder.suggest(key);
                }
            }
        }

    }

    @SuppressWarnings("rawtypes")
    public static void configValueSuggestion(Class<?> configClazz, SuggestionsBuilder builder, String configKey) {
        ForgeConfigSpec.ConfigValue<?> cv = findConfigValueByKey(configClazz, configKey);
        if (cv == null) return;
        else builder.suggest(String.valueOf(cv.get()));

        ForgeConfigSpec.ValueSpec vs = getValueSpec(cv);
        if (vs != null) {
            builder.suggest(String.valueOf(vs.getDefault()));
        }

        Class<?> type = getConfigValueType(cv);
        if (type == Boolean.class || type == boolean.class) {
            builder.suggest("true").suggest("false");
        } else if (Enum.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumClass = (Class<? extends Enum>) type;
            for (Object c : enumClass.getEnumConstants()) {
                builder.suggest(((Enum<?>) c).name());
            }
        } else if (Comparable.class.isAssignableFrom(type) && vs != null && vs.getRange() != null) {
            Class<?> rangeClass = FieldUtils.getClass(vs.getRange());
            for (String fieldName : FieldUtils.getPrivateFieldNames(rangeClass, Comparable.class, false, false, true)) {
                builder.suggest(String.valueOf(FieldUtils.getPrivateFieldValue(rangeClass, vs.getRange(), fieldName)));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int executeModifyConfig(Class<?> configClazz, CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String lang = getLanguage(source);
        String configKey = StringArgumentType.getString(context, "configKey");
        String configValue = StringArgumentType.getString(context, "configValue");

        ForgeConfigSpec.ConfigValue<?> cv = findConfigValueByKey(configClazz, configKey);
        if (cv == null) {
            Component component = BaniraComponent.get().trans(EnumI18nType.FORMAT, "config_key_absent", configKey);
            source.sendFailure(component.toChat(lang));
            return 0;
        }

        Class<?> type = getConfigValueType(cv);
        Object parsed;
        try {
            parsed = parseStringToType(configValue, type);
        } catch (Exception e) {
            LOGGER.error(e);
            Component component = BaniraComponent.get().trans(EnumI18nType.FORMAT, "config_value_parse_error", configValue, e.getMessage());
            source.sendFailure(component.toChat(lang));
            return 0;
        }

        if (validateConfigValueWithSpec(cv, parsed)) {
            ((ForgeConfigSpec.ConfigValue) cv).set(parsed);
        } else {
            Component component = BaniraComponent.get().trans(EnumI18nType.FORMAT, "config_value_set_error", configKey, configValue);
            source.sendFailure(component.toChat(lang));
            return 0;
        }

        tryApplyServerConfigBake(configClazz);

        Component component = BaniraComponent.get().trans(EnumI18nType.FORMAT, "config_value_set_success", configKey, parsed);
        source.sendSuccess(component.toChat(lang), true);

        return 1;
    }


    private static final Map<Class<?>, List<Field>> allConfigValueFieldsCache = new HashMap<>();

    private static List<Field> getAllConfigValueFields(Class<?> configClazz) {
        return allConfigValueFieldsCache.computeIfAbsent(configClazz, (k) -> {
            List<Field> out = new ArrayList<>();
            for (Field f : k.getDeclaredFields()) {
                try {
                    if (ForgeConfigSpec.ConfigValue.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        out.add(f);
                    }
                } catch (Throwable ignored) {
                }
            }
            return out;
        });
    }

    public static final Map<Class<?>, Map<String, ForgeConfigSpec.ConfigValue<?>>> configKeyMapCache = new HashMap<>();

    private static Map<String, ForgeConfigSpec.ConfigValue<?>> buildConfigKeyMap(Class<?> configClazz) {
        return configKeyMapCache.computeIfAbsent(configClazz, (k) -> {
            Map<String, ForgeConfigSpec.ConfigValue<?>> map = new LinkedHashMap<>();
            for (Field f : getAllConfigValueFields(k)) {
                try {
                    Object raw = f.get(null);
                    if (raw instanceof ForgeConfigSpec.ConfigValue) {
                        ForgeConfigSpec.ConfigValue<?> cv = (ForgeConfigSpec.ConfigValue<?>) raw;
                        String path = getConfigValuePath(cv);
                        if (path != null) {
                            map.put(path, cv);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            return map;
        });
    }

    private static String getConfigValuePath(ForgeConfigSpec.ConfigValue<?> cv) {
        return cv.getPath().stream().map(String::valueOf).collect(Collectors.joining("."));
    }

    private static ForgeConfigSpec.ConfigValue<?> findConfigValueByKey(Class<?> configClazz, String key) {
        if (key == null) return null;
        Map<String, ForgeConfigSpec.ConfigValue<?>> map = buildConfigKeyMap(configClazz);

        if (map.containsKey(key)) return map.get(key);

        List<String> matches = map.keySet().stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).contains(key.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        if (matches.size() == 1) return map.get(matches.get(0));

        return null;
    }

    private static Class<?> getConfigValueType(ForgeConfigSpec.ConfigValue<?> cv) {
        try {
            Object cur = cv.get();
            if (cur != null) return cur.getClass();
        } catch (Throwable ignored) {
        }
        return Object.class;
    }

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

    @SuppressWarnings("all")
    private static void tryApplyServerConfigBake(Class<?> configClazz) {
        try {
            Method m = configClazz.getDeclaredMethod("bake");
            if (m != null) {
                m.setAccessible(true);
                m.invoke(null);
                return;
            }
        } catch (Throwable ignored) {
        }

        String[] candidate = new String[]{"save", "sync", "write", "apply"};
        for (String name : candidate) {
            try {
                Method m = configClazz.getDeclaredMethod(name);
                if (m != null) {
                    m.setAccessible(true);
                    m.invoke(null);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static final Map<String, ForgeConfigSpec.ValueSpec> valueSpecCache = new HashMap<>();

    public static ForgeConfigSpec.ValueSpec getValueSpec(ForgeConfigSpec.ConfigValue<?> cv) {
        return valueSpecCache.computeIfAbsent(getConfigValuePath(cv), (k) -> {
            try {
                ForgeConfigSpec spec = null;
                for (String candidate : FieldUtils.getPrivateFieldNames(ForgeConfigSpec.ConfigValue.class, ForgeConfigSpec.class)) {
                    Object value = FieldUtils.getPrivateFieldValue(ForgeConfigSpec.ConfigValue.class, cv, candidate);
                    if (value != null) {
                        spec = (ForgeConfigSpec) value;
                        break;
                    }
                }
                if (spec != null) {
                    return spec.getSpec().get(cv.getPath());
                }
            } catch (Throwable ignored) {
            }
            return null;
        });
    }

    public static boolean validateConfigValueWithSpec(ForgeConfigSpec.ConfigValue<?> cv, Object parsedValue) {
        if (cv == null) return false;
        ForgeConfigSpec.ValueSpec vs = getValueSpec(cv);
        return vs != null && vs.test(parsedValue);
    }

    // endregion config modifier

}
