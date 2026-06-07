package xin.vanilla.banira.common.network;

import lombok.Data;
import xin.vanilla.banira.common.util.PacketUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分包数据包
 */
@Data
public abstract class SplitPacket {
    private static final Random random = new Random();

    /**
     * 分包ID
     */
    private String id;
    /**
     * 总包数
     */
    private int total;
    /**
     * 当前包序号
     */
    private int sort;

    protected SplitPacket() {
        this.id = String.format("%d.%d", System.currentTimeMillis(), random.nextInt(999999999));
    }

    protected SplitPacket(BaniraPacketBuffer buf) {
        this.id = buf.readUtf();
        this.total = buf.readInt();
        this.sort = buf.readInt();
    }

    /**
     * 处理接收到的分包
     *
     * @param packet 接收到的分包
     * @param <T>    分包类型
     * @return 完整的包列表，若还未接收完所有分包则返回空列表
     */
    public static <T extends SplitPacket> List<T> handle(T packet) {
        List<T> result = new ArrayList<>();
        if (isInvalidPacket(packet)) {
            return result;
        }
        Map<String, List<? extends SplitPacket>> packetCache = PacketUtils.packetCache();
        @SuppressWarnings("unchecked")
        List<T> splitPackets = (List<T>) packetCache.computeIfAbsent(packet.getId(), k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (splitPackets) {
            boolean duplicateSort = splitPackets.stream().anyMatch(cached -> cached.getSort() == packet.getSort());
            if (duplicateSort) {
                // 同一批次出现重复序号时丢弃整批，避免错误合并污染业务数据。
                packetCache.remove(packet.getId());
                return result;
            }
            splitPackets.add(packet);
            if (splitPackets.size() > packet.getTotal()) {
                packetCache.remove(packet.getId());
                return result;
            }
            if (splitPackets.size() == packet.getTotal()) {
                result = splitPackets.stream()
                        .sorted(Comparator.comparingInt(SplitPacket::getSort))
                        .collect(Collectors.toList());
                packetCache.remove(packet.getId());
                cleanupExpiredPacketCache(packetCache);
            }
        }
        return result;
    }

    private static boolean isInvalidPacket(SplitPacket packet) {
        return packet == null
                || packet.getId() == null
                || packet.getId().trim().isEmpty()
                || packet.getTotal() <= 0
                || packet.getSort() < 0
                || packet.getSort() >= packet.getTotal();
    }

    private static void cleanupExpiredPacketCache(Map<String, List<? extends SplitPacket>> packetCache) {
        packetCache.keySet().stream()
                .filter(SplitPacket::isExpiredPacketCacheKey)
                .forEach(packetCache::remove);
    }

    private static boolean isExpiredPacketCacheKey(String key) {
        String[] parts = key.split("\\.");
        if (parts.length == 0) {
            return false;
        }
        try {
            return Math.abs(System.currentTimeMillis() - Long.parseLong(parts[0])) > 1000 * 60 * 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 合并分包
     *
     * @param packets 分包列表
     * @param <T>     分包类型
     * @return 合并后的完整数据包
     */
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

    /**
     * 拆分包
     *
     * @return 拆分后的分包列表
     */
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

    /**
     * 获取每个分包的大小
     */
    public abstract int getChunkSize();

    /**
     * 可合并的分包接口
     */
    public interface MergeableSplitPacket<T extends SplitPacket> {
        /**
         * 合并分包
         *
         * @param packets 分包列表
         * @return 合并后的完整数据包
         */
        T mergePackets(List<T> packets);
    }

    /**
     * 可分拆的分包接口
     */
    public interface SplittableSplitPacket<T extends SplitPacket> {
        /**
         * 拆分包
         *
         * @return 拆分后的分包列表
         */
        List<T> splitPacket();
    }
}
