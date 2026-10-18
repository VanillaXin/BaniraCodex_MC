package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.notification.NotificationRegionLayout.Rect;
import xin.vanilla.banira.common.enums.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class UiNotificationAdmissionTest {
    private Notification item() throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe"); Field field = type.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Notification n = (Notification) type.getMethod("allocateInstance", Class.class).invoke(field.get(null), Notification.class);
        Field animation = Notification.class.getDeclaredField("animationState"); animation.setAccessible(true);
        Constructor<?> constructor = animation.getType().getDeclaredConstructor(); constructor.setAccessible(true); animation.set(n, constructor.newInstance());
        n.cachedWidth(100); n.cachedHeight(30); n.padding(4); n.richDrawLines(new ArrayList<>());
        n.animation(EnumMoveType.FADE_IN); n.position(EnumPosition.TOP_LEFT); n.animationTime(100); n.durationTime(1000); n.scheduledTime(0);
        return n;
    }
    private boolean accepts(Notification n, long now) throws Exception {
        Method method = Arrays.stream(Notification.class.getMethods()).filter(m -> m.getName().equals("acceptsInput")).findFirst().orElse(null);
        assertNotNull("Exit animations must not retain interactive hit targets", method); return (boolean) method.invoke(n, now);
    }
    @Test public void admissionAdvancesOriginalAnimationAndHitGeometryWithoutGraphics() throws Exception {
        Notification n = item(); Rect target = new Rect(10, 20, 100, 30), region = new Rect(0, 0, 300, 200);
        assertNotNull(n.prepareInRegion(target, region, 1000, 9));
        assertNotNull(n.prepareInRegion(target, region, 1200, 9));
        assertEquals(1200, n.lastRenderTime()); assertEquals(10, n.hitX(), 0); assertEquals(20, n.hitY(), 0);
        assertTrue(n.containsPoint(15, 25)); assertTrue(n.isCloseHit(98, 32)); assertTrue(accepts(n, 1200));
        n.dismissAnimated(1300); assertFalse(accepts(n, 1300));
        assertNotNull(n.prepareInRegion(target, region, 1350, 9));
        assertNull(n.prepareInRegion(target, region, 1401, 9)); assertTrue(n.finished());
    }
    @Test public void scheduledItemsDoNotStartBeforeAdmissionAndNaturalExitRejectsClick() throws Exception {
        Notification n = item(); n.scheduledTime(2000); Rect target = new Rect(10, 20, 100, 30), region = new Rect(0, 0, 300, 200);
        assertNull(n.prepareInRegion(target, region, 1999, 9)); assertEquals(0, n.lastRenderTime());
        n.prepareInRegion(target, region, 2000, 9); assertTrue(accepts(n, 2200)); assertFalse(accepts(n, 3100));
        n.advance(3201); assertTrue(n.finished());
    }
}
