package xin.vanilla.banira.internal.forge.util;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ForgeInternalFieldAccessTest {
    @Test
    public void writesFinalObjectFieldForLoaderMetadataPatches() throws Exception {
        Target target = new Target();

        assertTrue(ForgeInternalFieldAccess.writeObjectField(Target.class, target, "logoFile", "patched.png"));

        Field field = Target.class.getDeclaredField("logoFile");
        field.setAccessible(true);
        assertEquals("patched.png", field.get(target));
    }

    private static final class Target {
        private final String logoFile = "default.png";
    }
}
