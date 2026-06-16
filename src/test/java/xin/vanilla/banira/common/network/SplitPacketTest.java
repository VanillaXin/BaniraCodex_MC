package xin.vanilla.banira.common.network;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.util.PacketUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SplitPacketTest {

    @Before
    public void clearCacheBefore() {
        PacketUtils.packetCache().clear();
    }

    @After
    public void clearCacheAfter() {
        PacketUtils.packetCache().clear();
    }

    @Test
    public void returnsEmptyUntilAllPartsArrive() {
        TestSplitPacket first = packet("partial", 2, 0, "a");

        List<TestSplitPacket> result = SplitPacket.handle(first);

        assertTrue(result.isEmpty());
        assertTrue(PacketUtils.packetCache().containsKey("partial"));
    }

    @Test
    public void returnsSortedPartsAndClearsCacheWhenComplete() {
        SplitPacket.handle(packet("complete", 3, 2, "c"));
        SplitPacket.handle(packet("complete", 3, 0, "a"));

        List<TestSplitPacket> result = SplitPacket.handle(packet("complete", 3, 1, "b"));

        assertEquals(List.of("a", "b", "c"), payloads(result));
        assertFalse(PacketUtils.packetCache().containsKey("complete"));
        assertEquals("abc", SplitPacket.merge(result).payload);
    }

    @Test
    public void invalidPartDoesNotPolluteCache() {
        assertTrue(SplitPacket.handle(packet("", 2, 0, "blank")).isEmpty());
        assertTrue(SplitPacket.handle(packet("bad_total", 0, 0, "bad")).isEmpty());
        assertTrue(SplitPacket.handle(packet("bad_sort", 2, 2, "bad")).isEmpty());

        assertTrue(PacketUtils.packetCache().isEmpty());
    }

    @Test
    public void duplicateSortDropsCurrentBatch() {
        SplitPacket.handle(packet("dup", 2, 0, "a"));

        List<TestSplitPacket> result = SplitPacket.handle(packet("dup", 2, 0, "duplicate"));

        assertTrue(result.isEmpty());
        assertFalse(PacketUtils.packetCache().containsKey("dup"));
    }

    @Test
    public void completionCleansExpiredCacheKeys() {
        String expiredId = (System.currentTimeMillis() - 1000 * 60 * 6) + ".old";
        PacketUtils.packetCache().put(expiredId, new ArrayList<>(List.of(packet(expiredId, 2, 0, "old"))));

        SplitPacket.handle(packet("fresh", 1, 0, "fresh"));

        assertFalse(PacketUtils.packetCache().containsKey(expiredId));
    }

    private static TestSplitPacket packet(String id, int total, int sort, String payload) {
        TestSplitPacket packet = new TestSplitPacket(payload);
        packet.setId(id);
        packet.setTotal(total);
        packet.setSort(sort);
        return packet;
    }

    private static List<String> payloads(List<TestSplitPacket> packets) {
        return packets.stream().map(packet -> packet.payload).toList();
    }

    private static final class TestSplitPacket extends SplitPacket
            implements INetworkPacket, SplitPacket.MergeableSplitPacket<TestSplitPacket> {
        private final String payload;

        private TestSplitPacket(String payload) {
            this.payload = payload;
        }

        @Override
        public TestSplitPacket mergePackets(List<TestSplitPacket> packets) {
            return new TestSplitPacket(String.join("", payloads(packets)));
        }

        @Override
        public int getChunkSize() {
            return 1;
        }
    }
}
