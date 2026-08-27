package xin.vanilla.banira.client.gui.widget;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 验证加载器或子 Mod 的后置绘制不会覆盖已开启的 Tooltip 帧。
 */
public class TooltipPopupFrameOwnershipTest {

    @After
    public void releaseFrame() throws Exception {
        flushSubmittedFrame();
    }

    @Test
    public void nestedLateFrameKeepsExistingOwner() throws Exception {
        Object screen = new Object();

        assertTrue(TooltipWidget.beginPopupFrameIfIdle(screen, 12, 18));
        assertFalse(TooltipWidget.beginPopupFrameIfIdle(screen, 24, 36));

        flushSubmittedFrame();
        assertTrue(TooltipWidget.beginPopupFrameIfIdle(screen, 24, 36));
    }

    private static void flushSubmittedFrame() throws Exception {
        for (Method method : TooltipWidget.class.getMethods()) {
            String parameterName = method.getParameterCount() == 1
                    ? method.getParameterTypes()[0].getSimpleName()
                    : "";
            if (method.getName().equals("flushSubmittedPopupFrame")
                    && method.getParameterCount() == 1
                    && (parameterName.equals("MatrixStack") || parameterName.equals("PoseStack"))) {
                method.invoke(null, new Object[]{null});
                return;
            }
        }
        throw new AssertionError("Tooltip submitted-frame flush method is missing");
    }
}
