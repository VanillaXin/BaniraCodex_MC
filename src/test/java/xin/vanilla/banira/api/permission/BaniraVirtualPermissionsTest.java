package xin.vanilla.banira.api.permission;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class BaniraVirtualPermissionsTest {

    @Test
    public void buildsStablePermissionKey() {
        assertEquals("child_mod:OPEN_PANEL", BaniraVirtualPermissions.key(SamplePermission.OPEN_PANEL));
        assertEquals("child_mod:OPEN_PANEL", SamplePermission.OPEN_PANEL.key());
    }

    @Test
    public void keysIgnoreDisabledPermissionsAndKeepOrder() {
        Set<String> keys = BaniraVirtualPermissions.keys(
                SamplePermission.OPEN_PANEL,
                SamplePermission.DISABLED,
                SamplePermission.EDIT_CONFIG
        );

        assertEquals(new LinkedHashSet<>(Arrays.asList("child_mod:OPEN_PANEL", "child_mod:EDIT_CONFIG")), keys);
    }

    private enum SamplePermission implements BaniraVirtualPermission {
        OPEN_PANEL(true, 10),
        DISABLED(false, 20),
        EDIT_CONFIG(true, 30);

        private final boolean op;
        private final int sort;

        SamplePermission(boolean op, int sort) {
            this.op = op;
            this.sort = sort;
        }

        @Override
        public String modId() {
            return "child_mod";
        }

        @Override
        public String id() {
            return name();
        }

        @Override
        public boolean op() {
            return op;
        }

        @Override
        public int sort() {
            return sort;
        }
    }
}
