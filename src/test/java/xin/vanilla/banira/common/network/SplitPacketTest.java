package xin.vanilla.banira.common.network;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SplitPacketTest {
    @Before
    public void clearAssemblies() {
        SplitPacket.clearAssembliesForTest();
    }

    @Test
    public void handleReturnsSortedPacketsWhenAllPartsArrive() {
        DummySplitPacket second = packet("merge", 3, 2);
        DummySplitPacket first = packet("merge", 3, 0);
        DummySplitPacket middle = packet("merge", 3, 1);

        assertTrue(SplitPacket.handle(second).isEmpty());
        assertTrue(SplitPacket.handle(first).isEmpty());

        List<DummySplitPacket> merged = SplitPacket.handle(middle);

        assertEquals(3, merged.size());
        assertSame(first, merged.get(0));
        assertSame(middle, merged.get(1));
        assertSame(second, merged.get(2));
        assertEquals(0, SplitPacket.assemblyCountForTest());
    }

    @Test
    public void handleDropsInvalidPackets() {
        assertTrue(SplitPacket.handle(packet("", 1, 0)).isEmpty());
        assertTrue(SplitPacket.handle(packet("bad-total", 0, 0)).isEmpty());
        assertTrue(SplitPacket.handle(packet("bad-sort", 1, 1)).isEmpty());
        assertTrue(SplitPacket.handle(packet("negative-sort", 1, -1)).isEmpty());

        assertEquals(0, SplitPacket.assemblyCountForTest());
    }

    @Test
    public void duplicateSortDropsCurrentAssembly() {
        DummySplitPacket original = packet("duplicate", 2, 0);
        DummySplitPacket replacement = packet("duplicate", 2, 0);
        DummySplitPacket last = packet("duplicate", 2, 1);

        assertTrue(SplitPacket.handle(original).isEmpty());
        assertTrue(SplitPacket.handle(replacement).isEmpty());
        assertEquals(0, SplitPacket.assemblyCountForTest());

        List<DummySplitPacket> merged = SplitPacket.handle(last);

        assertTrue(merged.isEmpty());
        assertEquals(1, SplitPacket.assemblyCountForTest());
    }

    @Test
    public void mismatchedTotalStartsNewAssembly() {
        DummySplitPacket old = packet("retotal", 3, 0);
        DummySplitPacket replacement = packet("retotal", 2, 0);
        DummySplitPacket last = packet("retotal", 2, 1);

        assertTrue(SplitPacket.handle(old).isEmpty());
        assertTrue(SplitPacket.handle(replacement).isEmpty());

        List<DummySplitPacket> merged = SplitPacket.handle(last);

        assertEquals(2, merged.size());
        assertSame(replacement, merged.get(0));
        assertSame(last, merged.get(1));
        assertEquals(0, SplitPacket.assemblyCountForTest());
    }

    private static DummySplitPacket packet(String id, int total, int sort) {
        DummySplitPacket packet = new DummySplitPacket();
        packet.setId(id);
        packet.setTotal(total);
        packet.setSort(sort);
        return packet;
    }

    private static final class DummySplitPacket extends SplitPacket {
        @Override
        public int getChunkSize() {
            return 1;
        }
    }
}
