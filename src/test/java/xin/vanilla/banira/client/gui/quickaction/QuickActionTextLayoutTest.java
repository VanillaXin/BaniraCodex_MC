package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.function.ToIntFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class QuickActionTextLayoutTest {
    @Test
    public void truncatesWithVisibleEllipsisAndWrapsWithoutDroppingContent() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("xin.vanilla.banira.client.gui.quickaction.QuickActionTextLayout");
        } catch (ClassNotFoundException exception) {
            fail("QuickActionTextLayout is missing");
            return;
        }
        Method ellipsize = type.getDeclaredMethod(
                "ellipsize", String.class, int.class, ToIntFunction.class);
        Method wrap = type.getDeclaredMethod(
                "wrap", String.class, int.class, ToIntFunction.class);
        ellipsize.setAccessible(true);
        wrap.setAccessible(true);
        ToIntFunction<String> width = String::length;

        assertEquals("abcd...", ellipsize.invoke(null, "abcdefghij", 7, width));
        assertEquals("short", ellipsize.invoke(null, "short", 7, width));
        String wrapped = (String) wrap.invoke(null, "abcdefghij", 4, width);
        assertEquals("abcd\nefgh\nij", wrapped);
        assertEquals("abcdefghij", wrapped.replace("\n", ""));
    }
}
