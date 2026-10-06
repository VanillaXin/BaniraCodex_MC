package xin.vanilla.banira.internal.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.client.data.NotificationLogEntry;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class NotificationLogStoreTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void legacyHistoryLoadsAsRead() throws Exception {
        Path file = temporary.newFile().toPath();
        Files.write(file, "{\"entries\":[{\"id\":1}]}".getBytes(StandardCharsets.UTF_8));
        List<NotificationLogEntry> loaded = NotificationLogStore.load(file, 10);
        assertEquals(1, loaded.size());
        assertTrue(loaded.get(0).read());
    }

    @Test
    public void readStateAndRichTextRoundTripWithoutMutation() throws Exception {
        Path file = temporary.newFolder().toPath().resolve("history.json");
        String text = "{\"text\":\"\u672a\u8bfb,\u5185\u5bb9\",\"color\":\"gold\"}";
        NotificationLogEntry first = new NotificationLogEntry().id(1).componentJson(text).read(false);
        NotificationLogStore.save(file, Arrays.asList(first, new NotificationLogEntry().id(2).read(true)));
        List<NotificationLogEntry> loaded = NotificationLogStore.load(file, 10);
        assertEquals(2, loaded.size());
        assertFalse(loaded.get(0).read());
        assertTrue(loaded.get(1).read());
        assertEquals(text, loaded.get(0).componentJson());
    }

    @Test
    public void queuedSaveCapturesStateAndFlushKeepsLatestSubmission() throws Exception {
        Path file = temporary.newFolder().toPath().resolve("history.json");
        try (NotificationLogWriter writer = new NotificationLogWriter(file)) {
            NotificationLogEntry entry = new NotificationLogEntry().id(1).read(false);
            writer.submit(Collections.singletonList(entry));
            entry.read(true);
            writer.flush();
            assertFalse(NotificationLogStore.load(file, 10).get(0).read());
            for (int i = 0; i < 100; i++) {
                writer.submit(Collections.singletonList(new NotificationLogEntry().id(i).read(i == 99)));
            }
            writer.flush();
            assertEquals(99, NotificationLogStore.load(file, 10).get(0).id());
            assertTrue(NotificationLogStore.load(file, 10).get(0).read());
        }
    }

    @Test
    public void closeFlushesOutstandingChanges() throws Exception {
        Path file = temporary.newFolder().toPath().resolve("history.json");
        try (NotificationLogWriter writer = new NotificationLogWriter(file)) {
            writer.submit(Collections.singletonList(new NotificationLogEntry().id(7).read(true)));
        }
        assertEquals(7, NotificationLogStore.load(file, 10).get(0).id());
    }
}
