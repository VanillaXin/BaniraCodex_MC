package xin.vanilla.banira.internal.config;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PlayerPreferenceStateTest {
    private static final String UUID = "c169f21b-c9cd-4184-8354-bcbdf6fedc9d";
    private static final String OTHER = "6de0b1e2-830a-41ec-b432-ad74c5aad1ed";

    private Map<String, String> values() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("uuid", UUID);
        values.put("language", "zh_cn");
        values.put("notificationReceiveMode", "notification");
        return values;
    }

    @Test
    public void noDefaultValueIsTreatedAsAnAcknowledgement() {
        PlayerPreferenceState state = new PlayerPreferenceState();
        Object connection = new Object();
        assertNull(state.snapshot(UUID));
        assertTrue(state.bind(connection, UUID));
        assertNull(state.snapshot(UUID));
        assertTrue(state.apply(connection, UUID, values()));
        assertEquals("zh_cn", state.snapshot(UUID).get("language"));
    }

    @Test
    public void lateOldConnectionPacketCannotPopulateANewSession() {
        PlayerPreferenceState state = new PlayerPreferenceState();
        Object old = new Object(), current = new Object();
        state.bind(old, UUID);
        assertTrue(state.apply(old, UUID, values()));
        state.bind(current, UUID);
        assertNull(state.snapshot(UUID));
        assertFalse(state.apply(old, UUID, values()));
        assertNull(state.snapshot(UUID));
        assertTrue(state.apply(current, UUID, values()));
    }

    @Test
    public void wrongPlayerAndUnknownFieldsAreRejectedAtomically() {
        PlayerPreferenceState state = new PlayerPreferenceState();
        Object connection = new Object();
        state.bind(connection, UUID);
        Map<String, String> wrong = values();
        wrong.put("uuid", OTHER);
        assertFalse(state.apply(connection, UUID, wrong));
        assertFalse(state.apply(connection, OTHER, values()));
        wrong = values();
        wrong.put("admin", "true");
        assertFalse(state.apply(connection, UUID, wrong));
        assertNull(state.snapshot(UUID));
    }

    @Test
    public void invalidValuesDoNotOverwriteConfirmedPreferences() {
        PlayerPreferenceState state = new PlayerPreferenceState();
        Object connection = new Object();
        state.bind(connection, UUID);
        state.apply(connection, UUID, values());
        for (String language : new String[]{"", " ", "../config", null}) {
            Map<String, String> invalid = values();
            invalid.put("language", language);
            assertFalse(state.apply(connection, UUID, invalid));
        }
        Map<String, String> invalid = values();
        invalid.put("notificationReceiveMode", "unknown");
        assertFalse(state.apply(connection, UUID, invalid));
        assertEquals("zh_cn", state.snapshot(UUID).get("language"));
    }

    @Test
    public void confirmedValuesAreDefensivelyCopiedAndImmutable() {
        PlayerPreferenceState state = new PlayerPreferenceState();
        Object connection = new Object();
        state.bind(connection, UUID);
        Map<String, String> source = values();
        state.apply(connection, UUID, source);
        source.put("language", "en_us");
        assertEquals("zh_cn", state.snapshot(UUID).get("language"));
        try {
            state.snapshot(UUID).put("language", "en_us");
            fail("Confirmed values must be immutable");
        } catch (UnsupportedOperationException expected) {
            assertEquals("zh_cn", state.snapshot(UUID).get("language"));
        }
    }

    @Test
    public void disconnectRejectsAnyPreviouslyQueuedSnapshot() {
        PlayerPreferenceState state = new PlayerPreferenceState();
        Object connection = new Object();
        state.bind(connection, UUID);
        state.apply(connection, UUID, values());
        state.clear();
        assertNull(state.snapshot(UUID));
        assertFalse(state.apply(connection, UUID, values()));
    }

    @Test
    public void repeatedBindingPreservesConfirmationButPlayerChangesDoNot() {
        PlayerPreferenceState state = new PlayerPreferenceState();
        Object connection = new Object();
        state.bind(connection, UUID);
        state.apply(connection, UUID, values());
        assertFalse(state.bind(connection, UUID));
        assertNotNull(state.snapshot(UUID));
        assertTrue(state.bind(connection, OTHER));
        assertNull(state.snapshot(UUID));
        assertNull(state.snapshot(OTHER));
    }

    @Test
    public void invalidIdentityCannotCreateAReadyState() {
        PlayerPreferenceState state = new PlayerPreferenceState();
        Object connection = new Object();
        state.bind(connection, "1-1-1-1-1");
        assertFalse(state.apply(connection, "1-1-1-1-1", values()));
        state.bind(null, UUID);
        assertFalse(state.apply(null, UUID, values()));
        assertNull(state.snapshot(UUID));
    }
}
