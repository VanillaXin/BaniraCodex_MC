package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import static org.junit.Assert.*;

public class UiSharedControlsTest {
    @Test public void colorPickerConvertsNativeArgbToCssRgba() throws Exception {
        Class<?> bridge = UiSessionHandoffTest.type("internal.client.ui.UiColorBridge");
        assertEquals("#11223380", bridge.getMethod("toCss", String.class).invoke(null, "#80112233"));
    }
    @Test public void nativeAndDomKeyCaptureShareTheSameStateMachine() throws Exception {
        boolean press = false, release = false, clear = false;
        try (InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/client/gui/widget/KeyCaptureInputWidget.class")) {
            ClassNode node = new ClassNode(); new ClassReader(input).accept(node, 0);
            for (MethodNode method : node.methods) for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!call.owner.equals("xin/vanilla/banira/internal/client/ui/UiKeyCapture")) continue;
                press |= call.name.equals("press"); release |= call.name.equals("release"); clear |= call.name.equals("clear");
            }
        }
        assertTrue("Native key widget must share capture semantics", press && release && clear);
    }

    @Test public void keySequenceRequiresAllReleasesAndCanBeCancelled() {
        UiKeyCapture capture = new UiKeyCapture();
        assertNull(capture.press(341)); assertNull(capture.press(75));
        assertNull(capture.release(75));
        assertEquals(java.util.Arrays.asList("LeftControl", "K"), capture.release(341));
        capture.press(341); capture.clear();
        capture.press(65);
        assertEquals(java.util.Collections.singletonList("A"), capture.release(65));
        assertEquals(java.util.Collections.emptyList(), capture.press(259));
    }
}
