package xin.vanilla.banira.common.config;

import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigEditSnapshotTest {
    @Test
    public void readOnlySnapshotsDeferRevisionWorkAndHashTheirOwnBytes() throws Exception {
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
        List<String> list = new ArrayList<>(Arrays.asList("a,b", "c"));
        ConfigEditSnapshot snapshot = new ConfigEditSnapshot(this, "test", bytes,
                Collections.singletonMap("list", list));
        Field revision = ConfigEditSnapshot.class.getDeclaredField("revision");
        revision.setAccessible(true);
        assertNull("Readers of values must not pay for an unused revision", revision.get(snapshot));
        bytes[0] = 'z';
        list.clear();
        snapshot.getSourceBytes()[0] = 'x';
        assertEquals(Arrays.asList("a,b", "c"), snapshot.getValues().get("list"));
        assertNull(revision.get(snapshot));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                snapshot.getRevision());
        assertSame(snapshot.getRevision(), snapshot.getRevision());
    }
}
