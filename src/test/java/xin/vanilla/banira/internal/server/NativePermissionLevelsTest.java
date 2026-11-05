package xin.vanilla.banira.internal.server;

import net.minecraft.server.permissions.PermissionSet;
import org.junit.Test;

import static org.junit.Assert.*;

public class NativePermissionLevelsTest {
    @Test
    public void commandLevelsRetainEveryThreshold() {
        for (int granted = 0; granted <= 4; granted++) {
            PermissionSet permissions = NativePermissionLevels.forLevel(granted);
            for (int required = 0; required <= 4; required++)
                assertEquals("granted=" + granted + ",required=" + required,
                        granted >= required, NativePermissionLevels.has(permissions, required));
        }
    }

    @Test
    public void invalidThresholdCannotEscalateToOwner() {
        assertFalse(NativePermissionLevels.has(PermissionSet.ALL_PERMISSIONS, 5));
        assertFalse(NativePermissionLevels.has(PermissionSet.ALL_PERMISSIONS, Integer.MAX_VALUE));
        assertFalse(NativePermissionLevels.has(null, 1));
        assertTrue(NativePermissionLevels.has(PermissionSet.NO_PERMISSIONS, 0));
        assertThrows(IllegalArgumentException.class, () -> NativePermissionLevels.forLevel(5));
        assertThrows(IllegalArgumentException.class, () -> NativePermissionLevels.forLevel(-1));
    }
}
