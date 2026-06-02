package xin.vanilla.banira.common.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CommonUtilContractTest {
    @Test
    public void stringFormatSupportsIndexedAndSequentialPlaceholders() {
        assertEquals("b-a-a", StringUtils.format("%2$s-%1$s-%1$s", "a", "b"));
        assertEquals("hello-world", StringUtils.format("%1$s-%2$s", "hello", "world"));
        assertEquals("apple-banana", StringUtils.format("%s-%s", "apple", "banana"));
    }

    @Test
    public void safeExpressionEvaluatorHandlesBooleanClassAndMethodExpressions() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("team", "Red");
        vars.put("playerTeam", "Blue");
        vars.put("name", "Alex");
        vars.put("playerX", 100.0);
        vars.put("playerY", null);
        vars.put("b", true);
        vars.put("clazz", Number.class);
        vars.put("clazzString", "java.lang.Double");
        List<Number> list = new ArrayList<>();
        list.add(100.0);
        vars.put("list", list);

        assertTrue(new SafeExpressionEvaluator("team == 'Red' && playerTeam != 'Red'").evaluateBoolean(vars));
        assertTrue(new SafeExpressionEvaluator("((name == 'Alex' && team == 'Red') || playerTeam == 'Red')").evaluateBoolean(vars));
        assertFalse(new SafeExpressionEvaluator("playerX == null && playerY != null").evaluateBoolean(vars));
        assertFalse(new SafeExpressionEvaluator("!b").evaluateBoolean(vars));
        assertTrue(new SafeExpressionEvaluator("'100' == 100 && '123' > 100").evaluateBoolean(vars));
        assertTrue(new SafeExpressionEvaluator("clazzString :> clazz").evaluateBoolean(vars));
        assertFalse(new SafeExpressionEvaluator("clazzString <: clazz").evaluateBoolean(vars));
        assertTrue(new SafeExpressionEvaluator("clazz <: clazzString").evaluateBoolean(vars));
        assertTrue(new SafeExpressionEvaluator("list.contains(playerX)").evaluateBoolean(vars));
    }

    @Test
    public void randomStringUtilsHonorsRequestedSources() {
        assertTrue(RandomStringUtils.generate(10, RandomStringUtils.CharSource.DIGITS).matches("\\d{10}"));
        assertTrue(RandomStringUtils.generate(8, RandomStringUtils.CharSource.LETTERS).matches("[A-Za-z]{8}"));
        assertEquals(6, RandomStringUtils.generateFromCustom(6, "甲乙丙丁戊己庚辛壬癸").length());
    }

    @Test
    public void fieldUtilsSetsNonFinalPrivateFieldsOnly() {
        FieldTarget target = new FieldTarget();

        FieldUtils.setPrivateFieldValue(FieldTarget.class, target, "mutable", "changed");
        FieldUtils.setPrivateFieldValue(FieldTarget.class, target, "immutable", "changed");

        assertEquals("changed", FieldUtils.getPrivateFieldValue(FieldTarget.class, target, "mutable"));
        assertEquals("initial", FieldUtils.getPrivateFieldValue(FieldTarget.class, target, "immutable"));
    }

    private static final class FieldTarget {
        private String mutable = "initial";
        private final String immutable = "initial";
    }
}
