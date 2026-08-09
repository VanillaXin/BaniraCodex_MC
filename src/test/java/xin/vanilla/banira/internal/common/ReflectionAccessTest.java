package xin.vanilla.banira.internal.common;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

public class ReflectionAccessTest {

    @Test
    public void readsAndWritesPrivateFieldWithoutUnsafe() {
        Sample sample = new Sample();

        assertEquals("old", ReflectionAccess.fieldValue(Sample.class, sample, "value"));

        assertTrue(ReflectionAccess.setFieldValue(Sample.class, sample, "value", "new"));
        assertEquals("new", ReflectionAccess.fieldValue(sample, "value", String.class));
    }

    @Test
    public void findsPrivateFieldsByType() {
        List<String> names = ReflectionAccess.privateFieldNames(Sample.class, String.class);

        assertTrue(names.contains("value"));
    }

    @Test
    public void convertsArgumentsWhenFindingMethod() throws Exception {
        ReflectionAccess.MethodMatchResult result = ReflectionAccess.findMethodWithTypeConversion(
                Sample.class,
                "join",
                new Object[]{2.0D, "x"}
        );

        assertNotNull(result.method);
        Method method = result.method;
        assertEquals("2:x", method.invoke(new Sample(), result.args));
    }

    private static final class Sample {
        private String value = "old";

        @SuppressWarnings("unused")
        private String join(int count, String suffix) {
            return count + ":" + suffix;
        }
    }
}
