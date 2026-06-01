package xin.vanilla.banira.internal.network.packet;

import lombok.Getter;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.util.BiomeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


@Getter
public class BiomeToClient extends SplitPacket
        implements SplitPacket.MergeableSplitPacket<BiomeToClient>,
        SplitPacket.SplittableSplitPacket<BiomeToClient>,
        NetworkPacket {

    private final List<String> biomeIds;

    public BiomeToClient(Collection<String> biomeIds) {
        super();
        this.biomeIds = biomeIds != null ? new ArrayList<>(biomeIds) : new ArrayList<>();
    }

    public BiomeToClient(BaniraPacketBuffer buf) {
        super(buf);
        int size = buf.readVarInt();
        this.biomeIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            biomeIds.add(buf.readUtf(256));
        }
    }

    public static void handle(BiomeToClient packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isClientReception()) {
                BiomeUtils.setClientBiomeIds(packet.getBiomeIds());
            }
        });
        ctx.markHandled();
    }

    @Override
    public int getChunkSize() {
        return 64;
    }

    @Override
    public BiomeToClient mergePackets(List<BiomeToClient> packets) {
        List<String> allIds = packets.stream()
                .flatMap(p -> p.getBiomeIds().stream())
                .collect(Collectors.toList());
        return new BiomeToClient(allIds);
    }

    @Override
    public List<BiomeToClient> splitPacket() {
        List<BiomeToClient> result = new ArrayList<>();
        for (int i = 0, index = 0; i < biomeIds.size() / getChunkSize() + 1; i++) {
            List<String> chunk = new ArrayList<>();
            for (int j = 0; j < getChunkSize(); j++) {
                if (index >= biomeIds.size()) break;
                chunk.add(biomeIds.get(index));
                index++;
            }
            BiomeToClient packet = new BiomeToClient(chunk);
            packet.setId(this.getId());
            packet.setSort(i);
            result.add(packet);
        }
        result.forEach(packet -> packet.setTotal(result.size()));
        return result;
    }

    public void toBytes(BaniraPacketBuffer buf) {
        super.toBytes(buf);
        buf.writeVarInt(biomeIds.size());
        for (String id : biomeIds) {
            buf.writeUtf(id != null ? id : "", 256);
        }
    }
}
