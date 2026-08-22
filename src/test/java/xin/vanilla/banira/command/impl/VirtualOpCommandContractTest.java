package xin.vanilla.banira.command.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.permission.BaniraVirtualPermission;
import xin.vanilla.banira.api.permission.BaniraVirtualPermissionRegistry;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VirtualOpCommandContractTest {

    @Before
    @After
    public void resetRegistry() throws Exception {
        java.lang.reflect.Method clear = BaniraVirtualPermissionRegistry.class
                .getDeclaredMethod("clearForTests");
        clear.setAccessible(true);
        clear.invoke(null);
    }

    @Test
    public void resolvesRegisteredCompleteKeysAndRejectsUnknownKeys() {
        BaniraVirtualPermissionRegistry.register(permission("child", "reward.add.item", 0));
        BaniraVirtualPermissionRegistry.register(permission("other", "reward.add.coin", 1));

        assertEquals(new LinkedHashSet<>(Arrays.asList(
                        "child:reward.add.item", "other:reward.add.coin")),
                VirtualOpCommand.resolvePermissionKeys(
                        "CHILD:REWARD.ADD.ITEM,other:reward.add.coin").orElse(null));
        assertFalse(VirtualOpCommand.resolvePermissionKeys("child:missing").isPresent());
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
