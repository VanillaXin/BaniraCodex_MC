package xin.vanilla.banira.common.util;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.internal.config.CommonConfig;

import javax.annotation.Nullable;

/**
 * 配置编辑器将修改同步至服务端时的权限判定
 */
public final class ConfigEditPermission {
    private ConfigEditPermission() {
    }

    /**
     * 是否允许从服务端拉取/同步该配置（整份配置的入口权限，使用全局「修改服务端配置」设置）。
     */
    public static boolean canAccessServerConfigEditor(ServerPlayer player) {
        return canModifyEntry(player, null);
    }

    /**
     * 是否允许修改服务端上的该配置项（含未知路径时仅按全局设置判断）。
     */
    public static boolean canModifyEntry(ServerPlayer player, @Nullable ConfigEntryDescriptor desc) {
        int level = CommonConfig.get().permission().editServerConfigPermission();
        String virtualKey = CommonConfig.get().permission().editServerConfigVirtualPermissionKey();
        if (desc != null && desc.getEditPermissionPolicy() == ConfigEntry.EditPermissionPolicy.FIELD_OVERRIDE) {
            Integer fieldLevel = desc.getFieldEditPermissionLevel();
            if (fieldLevel != null) {
                level = fieldLevel;
            }
            String fieldKey = desc.getFieldEditVirtualPermissionKey();
            if (fieldKey != null && !fieldKey.isEmpty()) {
                virtualKey = fieldKey;
            }
        }
        if (player.createCommandSourceStack().hasPermission(level)) {
            return true;
        }
        return virtualKey != null && !virtualKey.isEmpty()
                && CommandUtils.hasVirtualPermission(player, virtualKey);
    }
}
