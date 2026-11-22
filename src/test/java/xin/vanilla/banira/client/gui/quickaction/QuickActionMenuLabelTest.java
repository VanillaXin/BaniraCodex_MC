package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class QuickActionMenuLabelTest {
    private static final String ENTRY = "entry:ipn:config";
    private static final String ITEM = "item:ipn:config#gui";

    @Test
    public void blankNamesFollowTheCurrentDefaultTranslation() {
        QuickActionLayout layout = new QuickActionLayout();
        assertEquals("Settings", layout.menuItemLabel(ENTRY, "Settings"));
        layout.setMenuItemLabel(ENTRY, "  IPN  ");
        assertEquals("IPN", layout.menuItemLabel(ENTRY, "Settings"));
        layout.setMenuItemLabel(ENTRY, " \t\n ");
        assertEquals("", layout.menuItemLabel(ENTRY));
        assertEquals("Configuration", layout.menuItemLabel(ENTRY, "Configuration"));
        layout.setMenuItemLabel(ENTRY, "Short");
        layout.setMenuItemLabel(ENTRY, null);
        assertEquals("Settings", layout.menuItemLabel(ENTRY, "Settings"));
    }

    @Test
    public void namesRoundTripWithoutChangingOrderOrVisibility() {
        QuickActionLayout layout = new QuickActionLayout();
        layout.syncMenuItemOrder(Arrays.asList(ENTRY, "entry:other"));
        layout.hiddenMenuItemIds().add(ITEM);
        layout.syncIconBarWithRegistry(Collections.singleton("ipn:config"));
        layout.hiddenIconIds().add("ipn:config");
        layout.setMenuItemLabel(ENTRY, "IPN");
        layout.setMenuItemLabel(ITEM, "GUI");
        QuickActionLayout restored = new QuickActionLayout();
        restored.fromJson(layout.toJson());
        assertEquals("IPN", restored.menuItemLabel(ENTRY));
        assertEquals("GUI", restored.menuItemLabel(ITEM));
        assertEquals(layout.menuItemOrder(), restored.menuItemOrder());
        assertEquals(layout.hiddenMenuItemIds(), restored.hiddenMenuItemIds());
        assertEquals(layout.hiddenIconIds(), restored.hiddenIconIds());
    }

    @Test
    public void editedTargetSurvivesReorderingAndTemporaryRegistryAbsence() {
        QuickActionLayout layout = new QuickActionLayout();
        layout.syncMenuItemOrder(Arrays.asList(ENTRY, "entry:other"));
        QuickActionMenuEditSession session = new QuickActionMenuEditSession(ENTRY);
        assertTrue(session.move(layout, 1));
        assertTrue(session.toggleVisibility(layout));
        layout.syncMenuItemOrder(Collections.singletonList("entry:other"));
        session.rename(layout, "IPN");
        layout.syncMenuItemOrder(Arrays.asList("entry:other", ENTRY));
        assertEquals("IPN", layout.menuItemLabel(ENTRY));
        assertEquals("Other", layout.menuItemLabel("entry:other", "Other"));
        assertTrue(session.isHidden(layout));
    }

    @Test
    public void legacyAndMalformedPreferencesDoNotLeakPreviousNames() {
        QuickActionLayout layout = new QuickActionLayout();
        layout.setMenuItemLabel(ENTRY, "Old");
        layout.fromJson(new JsonObject());
        assertEquals("", layout.menuItemLabel(ENTRY));
        JsonObject saved = new JsonObject();
        JsonObject labels = new JsonObject();
        labels.add("null", JsonNull.INSTANCE);
        labels.add("object", new JsonObject());
        labels.add("array", new JsonArray());
        labels.addProperty("number", 42);
        labels.addProperty("boolean", true);
        labels.addProperty("", "Ignored");
        labels.addProperty(ITEM, "GUI");
        saved.add("menuItemLabels", labels);
        layout.fromJson(saved);
        assertEquals(1, layout.toJson().getAsJsonObject("menuItemLabels").entrySet().size());
        assertEquals("GUI", layout.menuItemLabel(ITEM));
        saved.add("menuItemLabels", new JsonArray());
        layout.fromJson(saved);
        assertEquals("", layout.menuItemLabel(ITEM));
    }

    @Test
    public void serializedNamesAreIndependentSnapshots() {
        QuickActionLayout layout = new QuickActionLayout();
        layout.setMenuItemLabel(ENTRY, "IPN");
        JsonObject snapshot = layout.toJson();
        snapshot.getAsJsonObject("menuItemLabels").addProperty(ENTRY, "Changed");
        assertEquals("IPN", layout.menuItemLabel(ENTRY));
        QuickActionLayout restored = new QuickActionLayout();
        restored.fromJson(snapshot);
        snapshot.getAsJsonObject("menuItemLabels").addProperty(ENTRY, "Again");
        assertEquals("Changed", restored.menuItemLabel(ENTRY));
    }

    @Test
    public void namesAreBoundedWithoutSplittingUnicodeAndControlCharactersAreRemoved() {
        QuickActionLayout layout = new QuickActionLayout();
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 100; i++) longName.appendCodePoint(0x1f338);
        layout.setMenuItemLabel(ENTRY, longName.toString());
        String name = layout.menuItemLabel(ENTRY);
        assertEquals(QuickActionLayout.MAX_MENU_LABEL_LENGTH, name.codePointCount(0, name.length()));
        assertTrue(Character.isLowSurrogate(name.charAt(name.length() - 1)));
        layout.setMenuItemLabel(ENTRY, " A\nB\tC\u0000 ");
        assertEquals("ABC", layout.menuItemLabel(ENTRY));
        layout.setMenuItemLabel(null, "Ignored");
        assertEquals(1, layout.toJson().getAsJsonObject("menuItemLabels").entrySet().size());
    }
}
