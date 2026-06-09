package xin.vanilla.banira.internal.forge.util;

import org.junit.Test;
import xin.vanilla.banira.internal.common.ReflectionAccess;

import static org.junit.Assert.*;

public class ForgeInternalFieldAccessTest {

    @Test
    public void writesNonRecordPrivateField() {
        MutableHolder holder = new MutableHolder();

        assertTrue(ForgeInternalFieldAccess.setObjectField(MutableHolder.class, holder, "value", "changed"));

        assertEquals("changed", ReflectionAccess.fieldValue(MutableHolder.class, holder, "value"));
    }

    @Test
    public void refusesRecordFieldWrite() {
        RecordHolder holder = new RecordHolder("old");

        assertFalse(ForgeInternalFieldAccess.setObjectField(RecordHolder.class, holder, "value", "changed"));

        assertEquals("old", holder.value());
    }

    private static final class MutableHolder {
        private String value = "old";
    }

    private record RecordHolder(String value) {
    }
}
