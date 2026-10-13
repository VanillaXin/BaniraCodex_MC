package xin.vanilla.banira.client.minimap;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class MinimapLineLayoutTest {
    private float[] positions(float anchor, boolean above, float screenHeight, float... heights) throws Exception {
        Class<?> type;
        try {
            type = Class.forName("xin.vanilla.banira.internal.client.minimap.MinimapLineLayout");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Missing stacked minimap layout", e);
        }
        Method method = type.getMethod("positions", float.class, float[].class, float.class, boolean.class);
        return (float[]) method.invoke(null, anchor, heights, screenHeight, above);
    }

    @Test
    public void topAnchorUsesMeasuredWrappedLineHeights() throws Exception {
        assertArrayEquals(new float[]{100, 112}, positions(100, false, 480, 10, 20), .001f);
    }

    @Test
    public void bottomAnchorReservesTheWholeStackAboveTheMap() throws Exception {
        assertArrayEquals(new float[]{68, 80}, positions(100, true, 480, 10, 20), .001f);
    }

    @Test
    public void bottomClampingMovesBothRowsTogether() throws Exception {
        assertArrayEquals(new float[]{448, 460}, positions(470, false, 480, 10, 20), .001f);
    }

    @Test
    public void topClampingDoesNotCollapseTwoRowsOntoEachOther() throws Exception {
        assertArrayEquals(new float[]{0, 12}, positions(1, true, 480, 10, 20), .001f);
    }

    @Test
    public void emptyStackDoesNotReserveBlankSpace() throws Exception {
        assertArrayEquals(new float[0], positions(50, false, 480), .001f);
    }
    private boolean allowsLegacyJourneyScreen(boolean open, boolean chat) throws Exception {
        java.lang.reflect.Method method = null;
        for (java.lang.reflect.Method candidate : Class.forName("xin.vanilla.banira.internal.client.minimap.MinimapLineLayout").getMethods())
            if (candidate.getName().equals("allowsLegacyJourneyScreen")) method = candidate;
        assertNotNull("Native JourneyMap5 screen visibility policy", method);
        return (Boolean) method.invoke(null, open, chat);
    }

    @org.junit.Test
    public void legacyJourneyHidesUnderInventoryAndPauseButKeepsChat() throws Exception {
        assertTrue(allowsLegacyJourneyScreen(false, false));
        assertTrue(allowsLegacyJourneyScreen(true, true));
        assertFalse(allowsLegacyJourneyScreen(true, false));
    }
}
