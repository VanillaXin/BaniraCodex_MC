package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class QuickActionStartupLayoutTest {
    @Test
    public void partialStartupRegistrationsCannotPruneOrSaveAnUnreadLayout() throws Exception {
        var constructor = QuickActionOverlay.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var overlay = constructor.newInstance();
        JsonObject saved = new JsonObject();
        JsonArray grid = new JsonArray();
        grid.add("later:entry");
        saved.add("userSlotGrid", grid);
        JsonArray hidden = new JsonArray();
        hidden.add("later:entry");
        saved.add("hiddenIconIds", hidden);
        JsonArray menu = new JsonArray();
        menu.add("entry:later:entry");
        saved.add("hiddenMenuItemIds", menu);
        overlay.layout().fromJson(saved);
        JsonObject before = overlay.layout().toJson();
        var pendingField = QuickActionOverlay.class.getDeclaredField("pendingLayoutJson");
        pendingField.setAccessible(true);
        var pending = (AtomicReference<?>) pendingField.get(overlay);
        for (int i = 0; i < 4; i++) overlay.onRegistryChanged();
        assertEquals(before, overlay.layout().toJson());
        assertNull("Startup must not enqueue a default file write", pending.get());
        assertTrue(overlay.layout().hiddenIconIds().contains("later:entry"));
        assertTrue(overlay.layout().hiddenMenuItemIds().contains("entry:later:entry"));
    }
}
