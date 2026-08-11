package xin.vanilla.banira.common.network.packet;

import org.junit.Test;
import xin.vanilla.banira.common.network.TestBaniraPacketBuffer;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConfigPacketSerializationTest {

    @Test
    public void configSyncToServerRoundTripsThroughBaniraBuffer() {
        Map<String, String> changes = new LinkedHashMap<>();
        changes.put("alpha.count", "3");
        changes.put("alpha.name", "bamboo");
        ConfigSyncToServer original = new ConfigSyncToServer("example-common", changes);

        ConfigSyncToServer decoded = new ConfigSyncToServer(write(original::toBytes));

        assertEquals("example-common", decoded.configName());
        assertEquals(changes, decoded.changes());
    }

    @Test
    public void configSnapshotToClientRoundTripsThroughBaniraBuffer() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("alpha.enabled", "true");
        snapshot.put("alpha.modes", "ALPHA,BETA");
        ConfigSnapshotToClient original = new ConfigSnapshotToClient("example-common", snapshot);

        ConfigSnapshotToClient decoded = new ConfigSnapshotToClient(write(original::toBytes));

        assertEquals("example-common", decoded.configName());
        assertEquals(snapshot, decoded.snapshot());
    }

    @Test
    public void configFetchRequestRoundTripsThroughBaniraBuffer() {
        ConfigFetchRequestToServer original = new ConfigFetchRequestToServer("example-common");

        ConfigFetchRequestToServer decoded = new ConfigFetchRequestToServer(write(original::toBytes));

        assertEquals("example-common", decoded.configName());
    }

    @Test
    public void nullConstructorArgumentsEncodeAsEmptyValues() {
        Map<String, String> nullValue = new LinkedHashMap<>();
        nullValue.put("path", null);
        ConfigSyncToServer sync = new ConfigSyncToServer(null, nullValue);
        ConfigSnapshotToClient snapshot = new ConfigSnapshotToClient(null, nullValue);
        ConfigFetchRequestToServer request = new ConfigFetchRequestToServer((String) null);

        ConfigSyncToServer decodedSync = new ConfigSyncToServer(write(sync::toBytes));
        ConfigSnapshotToClient decodedSnapshot = new ConfigSnapshotToClient(write(snapshot::toBytes));
        ConfigFetchRequestToServer decodedRequest = new ConfigFetchRequestToServer(write(request::toBytes));

        assertEquals("", decodedSync.configName());
        assertEquals("", decodedSync.changes().get("path"));
        assertEquals("", decodedSnapshot.configName());
        assertEquals("", decodedSnapshot.snapshot().get("path"));
        assertEquals("", decodedRequest.configName());
    }

    private static TestBaniraPacketBuffer write(BufferWriter writer) {
        TestBaniraPacketBuffer buffer = new TestBaniraPacketBuffer();
        writer.write(buffer);
        return buffer.rewind();
    }

    private interface BufferWriter {
        void write(TestBaniraPacketBuffer buffer);
    }
}
