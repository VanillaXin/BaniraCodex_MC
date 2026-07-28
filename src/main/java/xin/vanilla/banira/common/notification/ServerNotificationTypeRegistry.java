package xin.vanilla.banira.common.notification;

import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;

import java.util.List;

/**
 * 服务端已登记的通知类型 id。依赖 mod 请优先使用
 * {@link xin.vanilla.banira.api.notification.BaniraNotificationTypes#register(String)}
 * 声明本 Mod 会发送的类型；
 * 此外 {@link xin.vanilla.banira.common.util.MessageUtils#sendNotification(net.minecraft.entity.player.ServerPlayerEntity, xin.vanilla.banira.common.data.Component, String)} 在发包时也会自动 {@link #ensureKnown}。
 * <p>
 * 可通过 {@link #register(String, EnumPosition, EnumMoveType)} 为类型指定默认位置与动画；未指定时使用 {@link EnumPosition#TOP_RIGHT} 与 {@link EnumMoveType#AUTO}。
 * <p>
 * 可选登记 {@link EnumNotificationTypeDisplayMode}，随 {@link xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient} 下发：
 * 客户端在本地 JSON 无该类型条目时采用（客户端模组显式 {@code NotificationTypeRegistry.register(id, mode)} 优先）。
 */
public final class ServerNotificationTypeRegistry {

    private static final ServerNotificationTypeRegistryState STATE = new ServerNotificationTypeRegistryState();

    static {
        registerInternal(NotificationTypeKeys.HELP, EnumPosition.TOP_CENTER, EnumMoveType.AUTO,
                EnumNotificationTypeDisplayMode.VANILLA_CHAT);
        registerInternal(NotificationTypeKeys.COMMAND_FEEDBACK, EnumPosition.TOP_CENTER, EnumMoveType.AUTO,
                EnumNotificationTypeDisplayMode.VANILLA_CHAT);
    }

    private ServerNotificationTypeRegistry() {
    }

    /**
     * 在服务端注册一种通知类型（默认位置 {@link EnumPosition#TOP_RIGHT}、动画 {@link EnumMoveType#AUTO}）
     */
    public static void register(String typeId) {
        registerInternal(typeId);
    }

    public static void registerInternal(String typeId) {
        STATE.register(typeId);
    }

    /**
     * 注册类型并指定：客户端配置文件无该类型条目时的建议展示方式（登录同步）
     */
    public static void register(String typeId, EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        registerInternal(typeId, clientDefaultDisplayIfAbsent);
    }

    public static void registerInternal(String typeId, EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        STATE.register(typeId, clientDefaultDisplayIfAbsent);
    }

    /**
     * 在服务端注册通知类型并指定默认布局（用于仅传类型 id 的 {@code sendNotification} 重载）
     */
    public static void register(String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation) {
        registerInternal(typeId, defaultPosition, defaultAnimation);
    }

    public static void registerInternal(String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation) {
        STATE.register(typeId, defaultPosition, defaultAnimation);
    }

    /**
     * 同时指定布局与客户端同步用默认展示方式
     */
    public static void register(String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation,
                                EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        registerInternal(typeId, defaultPosition, defaultAnimation, clientDefaultDisplayIfAbsent);
    }

    public static void registerInternal(String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation,
                                        EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        STATE.register(typeId, defaultPosition, defaultAnimation, clientDefaultDisplayIfAbsent);
    }

    public static void ensureKnown(String typeId) {
        STATE.ensureKnown(typeId);
    }

    /**
     * 注销通知类型声明。默认类型会被保留。
     */
    public static boolean unregister(String typeId) {
        return unregisterInternal(typeId);
    }

    public static boolean unregisterInternal(String typeId) {
        return STATE.unregister(typeId);
    }

    /**
     * 未单独 {@link #register(String, EnumPosition, EnumMoveType)} 时返回 {@link EnumPosition#TOP_RIGHT}
     */
    public static EnumPosition defaultPosition(String typeId) {
        return defaultPositionInternal(typeId);
    }

    public static EnumPosition defaultPositionInternal(String typeId) {
        return STATE.defaultLayout(typeId).position();
    }

    /**
     * 未单独注册布局时返回 {@link EnumMoveType#AUTO}
     */
    public static EnumMoveType defaultAnimation(String typeId) {
        return defaultAnimationInternal(typeId);
    }

    public static EnumMoveType defaultAnimationInternal(String typeId) {
        return STATE.defaultLayout(typeId).animation();
    }

    /**
     * 发往客户端的排序副本
     */
    public static List<String> sortedSnapshot() {
        return sortedSnapshotInternal();
    }

    public static List<String> sortedSnapshotInternal() {
        return STATE.sortedSnapshot();
    }

    /**
     * 构建登录同步条目（类型 id + 可选的客户端默认展示方式）
     */
    public static List<NotificationTypeSyncEntry> buildSyncEntries() {
        return STATE.buildSyncEntries();
    }

    /**
     * 某类型的默认位置与动画
     */
    @Value
    @Accessors(fluent = true)
    public static class TypeLayoutDefaults {
        EnumPosition position;
        EnumMoveType animation;
    }
}
