package xin.vanilla.banira.api.permission;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPermissionService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaniraPermissionsTest {

    @Test
    public void acceptsVanillaOrVirtualPermissionAndRejectsWhenBothFail() {
        Object player = new Object();
        MutablePermissionService service = new MutablePermissionService();
        BaniraPlatforms.install(new TestBaniraPlatform().permissionService(service));

        service.vanilla = true;
        assertTrue(BaniraPermissions.has(player, 2, "example:reward.add.coin"));

        service.vanilla = false;
        service.virtual = true;
        assertTrue(BaniraPermissions.has(player, 2, "example:reward.add.coin"));

        service.virtual = false;
        assertFalse(BaniraPermissions.has(player, 2, "example:reward.add.coin"));
        assertFalse(BaniraPermissions.has(player, 2, ""));
    }

    private static final class MutablePermissionService implements BaniraPermissionService {
        private boolean vanilla;
        private boolean virtual;

        @Override
        public boolean hasVanillaPermission(Object player, int permissionLevel) {
            return vanilla;
        }

        @Override
        public boolean hasVirtualPermission(Object player, String permissionKey) {
            return virtual;
        }
    }
}
