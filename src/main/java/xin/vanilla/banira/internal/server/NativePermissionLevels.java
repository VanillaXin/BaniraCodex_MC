package xin.vanilla.banira.internal.server;

import net.minecraft.server.permissions.*;

public final class NativePermissionLevels {
    private NativePermissionLevels() {
    }

    public static boolean has(PermissionSet permissions, int level) {
        if (level <= 0) return true;
        Permission permission = switch (level) {
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            case 4 -> Permissions.COMMANDS_OWNER;
            default -> null;
        };
        return permission != null && permissions != null && permissions.hasPermission(permission);
    }

    public static PermissionSet forLevel(int level) {
        if (level < 0 || level > 4) throw new IllegalArgumentException("Invalid command permission level: " + level);
        return LevelBasedPermissionSet.forLevel(PermissionLevel.byId(level));
    }
}
