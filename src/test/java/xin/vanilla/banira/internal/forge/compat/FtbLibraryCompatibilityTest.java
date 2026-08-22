package xin.vanilla.banira.internal.forge.compat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FtbLibraryCompatibilityTest {
    @Test
    public void clearingReservedAreaResetsItsDimensions() throws Exception {
        Class<?> rectangleClass = Class.forName("net.minecraft.client.renderer.Rectangle2d");
        Object occupied = rectangleClass.getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(4, 8, 32, 48);
        Class<?> groupClass = Class.forName(
                "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton");
        java.lang.reflect.Field area = groupClass.getField("lastDrawnArea");
        area.set(null, occupied);

        Class.forName("xin.vanilla.banira.internal.forge.compat.ftblibrary.FtbLibraryCompatibility")
                .getMethod("clearReservedArea").invoke(null);

        Object cleared = area.get(null);
        assertEquals(0, rectangleClass.getMethod("getWidth").invoke(cleared));
        assertEquals(0, rectangleClass.getMethod("getHeight").invoke(cleared));
    }
}
