package xin.vanilla.banira.command.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.permission.BaniraVirtualPermission;
import xin.vanilla.banira.api.permission.BaniraVirtualPermissionRegistry;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void commandSourceUsesRegistryInsteadOfBaniraEnumParsing() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin",
                "vanilla", "banira", "command", "impl", "VirtualOpCommand.java")), StandardCharsets.UTF_8);

        assertFalse(source.contains("EnumCommandType::valueOf"));
        assertFalse(source.contains("EnumCommandType.values()"));
        assertTrue(source.contains("BaniraVirtualPermissionRegistry.all()"));
        assertTrue(source.contains("resolvePermissionKeys"));
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
