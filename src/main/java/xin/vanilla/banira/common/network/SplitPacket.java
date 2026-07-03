package xin.vanilla.banira.common.network;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 分包基类，负责把多个网络消息重新组装为完整业务包。
 */
public abstract class SplitPacket {
    private static final long ASSEMBLY_TIMEOUT_MS = 1000L * 60L * 5L;
    private static final Random RANDOM = new Random();
    // 合包状态属于网络层内部实现，不再通过 PacketUtils 暴露给子 mod。
    private static final Map<String, SplitPacketAssembly> ASSEMBLIES = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private int total;

    @Getter
    @Setter
    private int sort;

    protected SplitPacket() {
        this.id = String.format("%d.%d", System.currentTimeMillis(), RANDOM.nextInt(999999999));
    }

    protected SplitPacket(BaniraPacketBuffer buf) {
        this.id = buf.readUtf();
        this.total = buf.readInt();
        this.sort = buf.readInt();
    }

    public static <T extends SplitPacket> List<T> handle(T packet) {
        cleanupExpiredAssemblies();
        if (!isValidSplit(packet)) {
            return Collections.emptyList();
        }

        SplitPacketAssembly assembly = ASSEMBLIES.compute(packet.getId(), (id, current) -> {
            if (current == null || current.total != packet.getTotal() || current.isExpired()) {
                return new SplitPacketAssembly(packet.getTotal());
            }
            return current;
        });

        List<T> result;
        synchronized (assembly) {
            if (assembly.hasSort(packet.getSort())) {
                // 同一批次出现重复序号时丢弃整批，避免错误合并污染业务数据。
                ASSEMBLIES.remove(packet.getId(), assembly);
                return Collections.emptyList();
            }
            assembly.put(packet);
            if (!assembly.isComplete()) {
                return Collections.emptyList();
            }
            result = assembly.sortedPackets();
        }
        ASSEMBLIES.remove(packet.getId(), assembly);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T extends SplitPacket> T merge(List<T> packets) {
        if (packets == null || packets.isEmpty()) {
            return null;
        }
        if (packets.size() == 1) {
            return packets.get(0);
        }
        T first = packets.get(0);
        if (first instanceof MergeableSplitPacket) {
            return ((MergeableSplitPacket<T>) first).mergePackets(packets);
        }
        return first;
    }

    @SuppressWarnings("unchecked")
    public <T extends SplitPacket> List<T> split() {
        if (this instanceof SplittableSplitPacket) {
            return ((SplittableSplitPacket<T>) this).splitPacket();
        }
        return Collections.singletonList((T) this);
    }

    protected void toBytes(BaniraPacketBuffer buf) {
        buf.writeUtf(id);
        buf.writeInt(total);
        buf.writeInt(sort);
    }

    public abstract int getChunkSize();

    static void clearAssembliesForTest() {
        ASSEMBLIES.clear();
    }

    static int assemblyCountForTest() {
        return ASSEMBLIES.size();
    }

    private static boolean isValidSplit(SplitPacket packet) {
        return packet != null
                && packet.getId() != null
                && !packet.getId().trim().isEmpty()
                && packet.getTotal() > 0
                && packet.getSort() >= 0
                && packet.getSort() < packet.getTotal();
    }

    private static void cleanupExpiredAssemblies() {
        ASSEMBLIES.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public interface MergeableSplitPacket<T extends SplitPacket> {
        T mergePackets(List<T> packets);
    }

    public interface SplittableSplitPacket<T extends SplitPacket> {
        List<T> splitPacket();
    }

    private static final class SplitPacketAssembly {
        private final int total;
        private final Map<Integer, SplitPacket> packets = new HashMap<>();
        private long lastUpdatedMs;

        private SplitPacketAssembly(int total) {
            this.total = total;
            touch();
        }

        private void put(SplitPacket packet) {
            packets.put(packet.getSort(), packet);
            touch();
        }

        private boolean hasSort(int sort) {
            return packets.containsKey(sort);
        }

        private boolean isComplete() {
            return packets.size() == total;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - lastUpdatedMs > ASSEMBLY_TIMEOUT_MS;
        }

        private void touch() {
            lastUpdatedMs = System.currentTimeMillis();
        }

        @SuppressWarnings("unchecked")
        private <T extends SplitPacket> List<T> sortedPackets() {
            return packets.values().stream()
                    .sorted(Comparator.comparingInt(SplitPacket::getSort))
                    .map(packet -> (T) packet)
                    .collect(Collectors.toList());
        }
    }
}
