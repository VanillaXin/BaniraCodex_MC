package xin.vanilla.banira.common.util;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EntityUtils {
    private EntityUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation UNKNOWN_ENTITY = Identifier.id().create("unknown_entity");

    /**
     * 所有实体类型缓存
     */
    private static volatile List<EntityType<?>> allEntityTypesCache = Collections.emptyList();

    /**
     * 反序列化缓存：String -> EntityType
     */
    private static final ConcurrentMap<String, EntityType<?>> deserializeCache = new ConcurrentHashMap<>();

    // region 获取实体注册信息

    /**
     * 获取实体类型的注册 ID
     *
     * @param entityType 实体类型
     */
    @Nullable
    public static ResourceLocation getEntityRegistry(EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }
        String id = Banira.platform().registryService().entityTypeKey(entityType);
        return id != null ? ResourceLocation.tryParse(id) : null;
    }

    /**
     * 获取实体类型的注册 ID 字符串
     *
     * @param entityType 实体类型
     */
    public static String getEntityRegistryString(EntityType<?> entityType) {
        ResourceLocation registryName = getEntityRegistry(entityType);
        return registryName != null ? registryName.toString() : UNKNOWN_ENTITY.toString();
    }

    /**
     * 获取实体的注册 ID（即其 {@link EntityType} 的注册 ID）
     *
     * @param entity 实体
     */
    @Nullable
    public static ResourceLocation getEntityRegistry(Entity entity) {
        if (entity == null) {
            return null;
        }
        return getEntityRegistry(entity.getType());
    }

    /**
     * 获取实体的注册 ID 字符串
     *
     * @param entity 实体
     */
    public static String getEntityRegistryString(Entity entity) {
        if (entity == null) {
            return UNKNOWN_ENTITY.toString();
        }
        return getEntityRegistryString(entity.getType());
    }

    // endregion

    // region 名称与翻译键

    /**
     * 获取实体类型名称翻译键
     *
     * @param entityType 实体类型
     */
    public static String getEntityNameKey(EntityType<?> entityType) {
        if (entityType == null) {
            return "";
        }
        return entityType.getDescriptionId();
    }

    /**
     * 获取实体名称翻译键
     *
     * @param entity 实体
     */
    public static String getEntityNameKey(Entity entity) {
        if (entity == null) {
            return "";
        }
        return getEntityNameKey(entity.getType());
    }

    /**
     * 获取实体类型显示名称
     *
     * @param entityType 实体类型
     */
    public static Component getEntityHoverName(EntityType<?> entityType) {
        if (entityType == null) {
            return BaniraComponent.get().empty();
        }
        String key = getEntityNameKey(entityType);
        return BaniraComponent.get().object(new TranslatableComponent(key));
    }

    /**
     * 获取实体显示名称
     *
     * @param entity 实体
     */
    public static Component getEntityHoverName(Entity entity) {
        if (entity == null) {
            return BaniraComponent.get().empty();
        }
        return getEntityHoverName(entity.getType());
    }

    /**
     * 获取实体类型显示名称字符串
     *
     * @param entityType 实体类型
     */
    public static String getEntityHoverNameString(EntityType<?> entityType) {
        if (entityType == null) {
            return "";
        }
        String key = getEntityNameKey(entityType);
        return new TranslatableComponent(key).getString();
    }

    /**
     * 获取实体显示名称字符串
     *
     * @param entity 实体
     */
    public static String getEntityHoverNameString(Entity entity) {
        if (entity == null) {
            return "";
        }
        return getEntityHoverNameString(entity.getType());
    }

    // endregion

    // region 比较与空判断

    /**
     * 两 {@link EntityType} 是否为同一注册项
     */
    public static boolean areEntityTypesEqual(@Nullable EntityType<?> a, @Nullable EntityType<?> b) {
        if (a == null || b == null) {
            return false;
        }
        return a == b;
    }

    public static boolean isUnknownEntity(Entity entity) {
        return entity == null || getEntityRegistryString(entity).equals(UNKNOWN_ENTITY.toString());
    }

    public static boolean isUnknownEntity(EntityType<?> type) {
        return type == null || getEntityRegistryString(type).equals(UNKNOWN_ENTITY.toString());
    }

    public static boolean isUnknownEntity(ResourceLocation entityId) {
        return entityId == null || entityId.toString().equals(UNKNOWN_ENTITY.toString());
    }

    public static boolean isUnknownEntity(String entityId) {
        return StringUtils.isNullOrEmpty(entityId) || entityId.equals(UNKNOWN_ENTITY.toString());
    }

    // endregion

    // region 序列化与反序列化

    /**
     * 将实体类型序列化为注册 ID 字符串（与原版 summon 等命令中的实体 ID 一致，如 {@code minecraft:zombie}）。
     */
    public static String serializeEntityType(EntityType<?> entityType) {
        if (entityType == null) {
            return "";
        }
        ResourceLocation rl = getEntityRegistry(entityType);
        return rl != null ? rl.toString() : "";
    }

    /**
     * 从注册 ID 字符串反序列化为 {@link EntityType}；无效时返回 {@code null}。
     */
    @Nullable
    public static EntityType<?> deserializeEntityType(String entityTypeString) {
        if (StringUtils.isNullOrEmptyEx(entityTypeString)) {
            return null;
        }
        String key = entityTypeString.trim();
        EntityType<?> cached = deserializeCache.get(key);
        if (cached != null) {
            return cached;
        }
        EntityType<?> resolved = getEntityTypeFromRegistry(key);
        if (resolved != null) {
            deserializeCache.putIfAbsent(key, resolved);
        }
        return resolved;
    }

    @Nullable
    public static EntityType<?> getEntityTypeFromRegistry(String entityId) {
        ResourceLocation location = Identifier.id().parse(entityId);
        return getEntityTypeFromRegistry(location);
    }

    @Nullable
    public static EntityType<?> getEntityTypeFromRegistry(@Nullable ResourceLocation location) {
        if (location == null) {
            return null;
        }
        try {
            Object entityType = Banira.platform().registryService().entityType(location.toString());
            return entityType instanceof EntityType ? (EntityType<?>) entityType : null;
        } catch (Exception e) {
            LOGGER.debug("Failed to find entity type by registry name: {}", location, e);
            return null;
        }
    }

    // endregion

    // region 所有实体

    public static List<Entity> getAllEntities() {
        List<Entity> entities = new ArrayList<>();
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            server.getAllLevels().forEach(level ->
                    level.getEntities().getAll().forEach(entities::add)
            );
        }
        return entities;
    }

    public static List<Entity> getAllEntities(ServerLevel level) {
        List<Entity> entities = new ArrayList<>();
        level.getAllEntities().forEach(entities::add);
        return entities;
    }

    /**
     * 清除实体类型相关缓存
     */
    public static void clearCache() {
        allEntityTypesCache = Collections.emptyList();
        deserializeCache.clear();
    }

    /**
     * 获取当前注册表中所有实体类型的快照列表
     */
    public static List<EntityType<?>> getAllEntityTypes() {
        if (allEntityTypesCache.isEmpty()) {
            synchronized (EntityUtils.class) {
                if (allEntityTypesCache.isEmpty()) {
                    Map<ResourceLocation, EntityType<?>> byId = new LinkedHashMap<>();
                    for (Object value : Banira.platform().registryService().entityTypes()) {
                        if (!(value instanceof EntityType)) continue;
                        EntityType<?> entityType = (EntityType<?>) value;
                        ResourceLocation rl = getEntityRegistry(entityType);
                        if (rl == null) rl = UNKNOWN_ENTITY;
                        byId.putIfAbsent(rl, entityType);
                    }
                    allEntityTypesCache = List.copyOf(byId.values());
                }
            }
        }
        return new ArrayList<>(allEntityTypesCache);
    }

    // endregion
}
