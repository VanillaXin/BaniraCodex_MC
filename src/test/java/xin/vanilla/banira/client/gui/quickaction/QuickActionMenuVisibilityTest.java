package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuickActionMenuVisibilityTest {
    @Test
    public void hiddenMenuRowsRoundTripSeparatelyFromHiddenIcons() {
        JsonObject json = new JsonObject();
        JsonArray hiddenMenuItems = new JsonArray();
        hiddenMenuItems.add("entry:test:menu");
        json.add("hiddenMenuItemIds", hiddenMenuItems);
        JsonArray hiddenIcons = new JsonArray();
        hiddenIcons.add("test:icon");
        json.add("hiddenIconIds", hiddenIcons);
        JsonArray userSlots = new JsonArray();
        userSlots.add("test:icon");
        json.add("userSlotGrid", userSlots);

        QuickActionLayout restored = new QuickActionLayout();
        restored.fromJson(json);
        JsonObject roundTrip = restored.toJson();

        assertTrue(roundTrip.getAsJsonArray("hiddenMenuItemIds").toString()
                .contains("entry:test:menu"));
        assertTrue(restored.hiddenIconIds().contains("test:icon"));
        assertFalse(restored.hiddenIconIds().contains("entry:test:menu"));
    }
}
