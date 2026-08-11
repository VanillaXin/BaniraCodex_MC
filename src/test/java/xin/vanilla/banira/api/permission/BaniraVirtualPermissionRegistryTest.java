package xin.vanilla.banira.api.permission;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class BaniraVirtualPermissionRegistryTest {

    @Before
    @After
    public void resetRegistry() {
        BaniraVirtualPermissionRegistry.clearForTests();
    }

    @Test
    public void registersAndFindsCompleteKeysCaseInsensitively() {
        BaniraVirtualPermission permission = permission("example", "reward.add.coin", 20);

        assertSame(permission, BaniraVirtualPermissionRegistry.register(permission));
        assertSame(permission, BaniraVirtualPermissionRegistry.find("EXAMPLE:REWARD.ADD.COIN").orElse(null));
    }

    @Test
    public void ordersPermissionsBySortThenCanonicalKey() {
        BaniraVirtualPermissionRegistry.register(permission("zeta", "late", 20));
        BaniraVirtualPermissionRegistry.register(permission("beta", "same", 10));
        BaniraVirtualPermissionRegistry.register(permission("alpha", "same", 10));

        List<String> keys = BaniraVirtualPermissionRegistry.all().stream()
                .map(BaniraVirtualPermission::key)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("alpha:same", "beta:same", "zeta:late"), keys);
    }

    @Test
    public void rejectsDuplicateOrInvalidPermissions() {
        BaniraVirtualPermissionRegistry.register(permission("example", "reward.add.coin", 0));

        assertThrows(IllegalStateException.class, () ->
                BaniraVirtualPermissionRegistry.register(permission("EXAMPLE", "REWARD.ADD.COIN", 1)));
        assertThrows(IllegalArgumentException.class, () ->
                BaniraVirtualPermissionRegistry.register(permission("", "missing_namespace", 1)));
        assertThrows(IllegalArgumentException.class, () ->
                BaniraVirtualPermissionRegistry.register(permission("example", "", 1)));
    }

    private static BaniraVirtualPermission permission(String modId, String id, int sort) {
        return new BaniraVirtualPermission() {
            @Override
            public String modId() {
                return modId;
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public boolean op() {
                return true;
            }

            @Override
            public int sort() {
                return sort;
            }
        };
    }
}
