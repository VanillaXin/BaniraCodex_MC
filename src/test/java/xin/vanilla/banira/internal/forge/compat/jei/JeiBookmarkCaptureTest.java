package xin.vanilla.banira.internal.forge.compat.jei;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JeiBookmarkCaptureTest {
    @Test
    public void unsupportedReplacementPreservesNativeButtonAfterSuccessfulCapture() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Class<?> drawable = Class.forName("mezz.jei.api.gui.drawable.IDrawable", false, loader);
        Method capture = JeiCompatibility.class.getMethod("capture", Object.class, drawable, drawable);
        Object valid = new JeiBookmarkClickTest.ModernButton();
        capture.invoke(null, valid, null, null);
        assertTrue(JeiCompatibility.isManagedBookmark(valid));
        Object unsupported = new JeiBookmarkClickTest.UnsupportedButton();
        capture.invoke(null, unsupported, null, null);
        assertFalse(JeiCompatibility.isManagedBookmark(valid));
        assertFalse(JeiCompatibility.isManagedBookmark(unsupported));
        assertFalse(JeiCompatibility.shouldBlockNativeClick(unsupported));
        assertFalse(JeiCompatibility.shouldSuppress(valid));
        Class<?> provider = Class.forName(JeiCompatibility.class.getName() + "$BookmarkProvider");
        Constructor<?> constructor = provider.getDeclaredConstructor();
        constructor.setAccessible(true);
        Method actions = provider.getMethod("actions",
                Class.forName("net.minecraft.client.gui.screens.Screen", false, loader));
        actions.setAccessible(true);
        assertTrue(((List<?>) actions.invoke(constructor.newInstance(), new Object[]{null})).isEmpty());
    }
}
