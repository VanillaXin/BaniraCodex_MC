package xin.vanilla.banira.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import xin.vanilla.banira.common.api.IVirtualPermissionType;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.common.enums.EnumOperationType;
import xin.vanilla.banira.internal.config.CustomConfig;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class VirtualPermissionManager {

    /**
     * 服务端虚拟权限表
     * uuid -> Set<permissionKey>
     * <p>
     * permissionKey {@code modId + ":" + id}
     */
    private static final Map<String, Set<String>> OP_MAP = deserialize();

    /**
     * 客户端缓存虚拟权限表
     * uuid -> Set<permissionKey>
     * <p>
     * permissionKey {@code modId + ":" + id}
     */
    private static final Map<String, Set<String>> OP_MAP_CLIENT = deserializeClient();

    /**
     * 添加权限（合并原有权限）
     */
    public static void addVirtualPermission(PlayerEntity player, EnumCommandType... types) {
        modifyPermissions(player.getStringUUID(), EnumOperationType.ADD, toKeys(types));
    }

    /**
     * 添加虚拟权限。
     */
    public static void addVirtualPermission(PlayerEntity player, IVirtualPermissionType... types) {
        modifyPermissions(player.getStringUUID(), EnumOperationType.ADD, toKeys(types));
    }

    /**
     * 设置权限（覆盖原有权限）
     */
    public static void setVirtualPermission(PlayerEntity player, EnumCommandType... types) {
        modifyPermissions(player.getStringUUID(), EnumOperationType.SET, toKeys(types));
    }

    /**
     * 设置虚拟权限（覆盖原有权限）
     */
    public static void setVirtualPermission(PlayerEntity player, IVirtualPermissionType... types) {
        modifyPermissions(player.getStringUUID(), EnumOperationType.SET, toKeys(types));
    }

    /**
     * 删除权限
     */
    public static void delVirtualPermission(PlayerEntity player, EnumCommandType... types) {
        modifyPermissions(player.getStringUUID(), EnumOperationType.REMOVE, toKeys(types));
    }

    /**
     * 删除虚拟权限
     */
    public static void delVirtualPermission(PlayerEntity player, IVirtualPermissionType... types) {
        modifyPermissions(player.getStringUUID(), EnumOperationType.REMOVE, toKeys(types));
    }

    /**
     * 清空所有权限
     */
    public static void clearVirtualPermission(PlayerEntity player) {
        modifyPermissions(player.getStringUUID(), EnumOperationType.CLEAR, Collections.emptySet());
    }

    /**
     * 获取当前权限列表
     */
    public static Set<EnumCommandType> getVirtualPermission(PlayerEntity player) {
        Set<String> raw = player.isLocalPlayer()
                ? getExistingPermissionsClient(player.getStringUUID())
                : getExistingPermissions(player.getStringUUID());
        return mapToEnum(raw, EnumCommandType.class);
    }

    /**
     * 获取某个 Mod 自己的虚拟权限列表
     *
     * @param player    玩家
     * @param enumClass 该 Mod 自己的指令枚举（需实现 {@link IVirtualPermissionType}）
     */
    public static <T extends Enum<T> & IVirtualPermissionType> Set<T> getVirtualPermission(PlayerEntity player, Class<T> enumClass) {
        Set<String> raw = player.isLocalPlayer()
                ? getExistingPermissionsClient(player.getStringUUID())
                : getExistingPermissions(player.getStringUUID());
        return mapToEnum(raw, enumClass);
    }

    /**
     * 返回所有 Mod 的原始权限键（uuid -> Set&lt;modId:id&gt;）。
     */
    public static Set<String> getRawVirtualPermission(PlayerEntity player) {
        return player.isLocalPlayer()
                ? getExistingPermissionsClient(player.getStringUUID())
                : getExistingPermissions(player.getStringUUID());
    }

    /**
     * 根据操作类型修改指定玩家的权限集合
     */
    private static void modifyPermissions(String stringUUID, EnumOperationType operation, Set<String> inputKeys) {
        Set<String> newTypes = processOperation(getExistingPermissions(stringUUID), inputKeys, operation);
        updateRuleList(stringUUID, newTypes);
    }

    /**
     * 查找现有规则（服务端）
     */
    private static Set<String> getExistingPermissions(String uuid) {
        return new LinkedHashSet<>(OP_MAP.getOrDefault(uuid, new LinkedHashSet<>()));
    }

    /**
     * 查找现有规则（客户端缓存）
     */
    private static Set<String> getExistingPermissionsClient(String uuid) {
        return new LinkedHashSet<>(OP_MAP_CLIENT.getOrDefault(uuid, new LinkedHashSet<>()));
    }

    /**
     * 处理权限操作
     */
    private static Set<String> processOperation(Set<String> existing, Set<String> input, EnumOperationType operation) {
        Set<String> result = new LinkedHashSet<>(existing);
        switch (operation) {
            case ADD:
                result.addAll(input);
                break;
            case SET:
                result.clear();
                result.addAll(input);
                break;
            case DEL:
            case REMOVE:
                input.forEach(result::remove);
                break;
            case CLEAR:
                result.clear();
                break;
        }
        return result;
    }

    /**
     * 更新规则列表
     */
    private static void updateRuleList(String stringUUID, Set<String> types) {
        OP_MAP.putAll(deserialize());
        OP_MAP.put(stringUUID, types);
        CustomConfig.setVirtualPermission(serialize());
    }

    private static JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        OP_MAP.forEach((uuid, types) -> {
            JsonArray jsonArray = new JsonArray();
            types.forEach(jsonArray::add);
            jsonObject.add(uuid, jsonArray);
        });
        return jsonObject;
    }

    private static Map<String, Set<String>> deserialize(JsonObject jsonObject) {
        Map<String, Set<String>> map = new HashMap<>();
        jsonObject.entrySet().forEach(entry -> {
            Set<String> types = new LinkedHashSet<>();
            entry.getValue().getAsJsonArray().forEach(jsonElement -> types.add(jsonElement.getAsString()));
            map.put(entry.getKey(), types);
        });
        return map;
    }

    private static Map<String, Set<String>> deserialize() {
        Map<String, Set<String>> result;
        try {
            result = deserialize(CustomConfig.getVirtualPermission());
        } catch (Exception e) {
            result = new HashMap<>();
        }
        return result;
    }

    public static void reloadClient() {
        OP_MAP_CLIENT.clear();
        OP_MAP_CLIENT.putAll(deserializeClient());
    }

    private static Map<String, Set<String>> deserializeClient() {
        Map<String, Set<String>> result;
        try {
            result = deserialize(CustomConfig.getVirtualPermissionClient());
        } catch (Exception e) {
            result = new HashMap<>();
        }
        return result;
    }

    // region 辅助方法

    private static String toKey(IVirtualPermissionType type) {
        return type.modId() + ":" + type.id();
    }

    private static Set<String> toKeys(IVirtualPermissionType... types) {
        return Arrays.stream(types)
                .filter(Objects::nonNull)
                .filter(IVirtualPermissionType::op)
                .map(VirtualPermissionManager::toKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <T extends Enum<T> & IVirtualPermissionType> Set<T> mapToEnum(Set<String> raw, Class<T> enumClass) {
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        Map<String, T> index = Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(VirtualPermissionManager::toKey, e -> e, (a, b) -> a));
        return raw.stream()
                .map(index::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(IVirtualPermissionType::sort))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // endregion 辅助方法

}
