package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class QuickActionStartupLayoutTest {
    private QuickActionOverlay freshOverlay() throws Exception {
        java.lang.reflect.Constructor<QuickActionOverlay> constructor = QuickActionOverlay.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void assertNoPendingSave(QuickActionOverlay overlay) throws Exception {
        try {
            Field pending = QuickActionOverlay.class.getDeclaredField("pendingLayoutJson");
            pending.setAccessible(true);
            assertNull("Startup must not enqueue a file write", ((AtomicReference<?>) pending.get(overlay)).get());
        } catch (NoSuchFieldException legacySave) {
            Field pending = QuickActionOverlay.class.getDeclaredField("savePending");
            pending.setAccessible(true);
            assertFalse("Startup must not mark the unread layout for saving", pending.getBoolean(overlay));
        }
    }

    @Test
    public void partialRegistrationsCannotPruneOrSaveAnUnreadLayout() throws Exception {
        QuickActionOverlay overlay = freshOverlay();
        JsonObject saved = new JsonObject();
        JsonArray grid = new JsonArray();
        grid.add("");
        grid.add("later:entry");
        JsonArray hidden = new JsonArray();
        hidden.add("later:entry");
        JsonArray menu = new JsonArray();
        menu.add("entry:later:entry");
        saved.add("userSlotGrid", grid);
        saved.add("hiddenIconIds", hidden);
        saved.add("hiddenMenuItemIds", menu);
        saved.addProperty("anchorX", 0.37);
        saved.addProperty("cellSize", 22);
        overlay.layout().fromJson(saved);
        JsonObject before = overlay.layout().toJson();
        for (int i = 0; i < 4; i++) overlay.onRegistryChanged();
        assertEquals(before, overlay.layout().toJson());
        assertNoPendingSave(overlay);
        assertTrue(overlay.layout().hiddenIconIds().contains("later:entry"));
        assertTrue(overlay.layout().hiddenMenuItemIds().contains("entry:later:entry"));
    }

    @Test
    public void aNewOverlayHasNoPendingWrite() throws Exception {
        assertNoPendingSave(freshOverlay());
    }

    @Test
    public void freshStartupDoesNotScheduleDefaultLayoutReplacement() throws Exception {
        QuickActionOverlay overlay = freshOverlay();
        overlay.onRegistryChanged();
        assertNoPendingSave(overlay);
    }
}
