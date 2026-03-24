package xin.vanilla.banira.internal.network.packet;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import xin.vanilla.banira.common.network.packet.SplitPacket;
import xin.vanilla.banira.common.util.DimensionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Getter
public class DimensionToClient extends SplitPacket
        implements SplitPacket.MergeableSplitPacket<DimensionToClient>,
        SplitPacket.SplittableSplitPacket<DimensionToClient> {

    private final List<String> dimensionIds;

    public DimensionToClient(List<String> dimensionIds) {
        super();
        this.dimensionIds = dimensionIds != null ? new ArrayList<>(dimensionIds) : new ArrayList<>();
    }

    public DimensionToClient(FriendlyByteBuf buf) {
        super(buf);
        int size = buf.readVarInt();
        this.dimensionIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            dimensionIds.add(buf.readUtf(256));
        }
    }

    public static void handle(DimensionToClient packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                DimensionUtils.setClientDimensionIds(packet.getDimensionIds());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @Override
    public int getChunkSize() {
        return 64;
    }

    @Override
    public DimensionToClient mergePackets(List<DimensionToClient> packets) {
        List<String> allIds = packets.stream()
                .flatMap(p -> p.getDimensionIds().stream())
                .collect(Collectors.toList());
        return new DimensionToClient(allIds);
    }

    @Override
    public List<DimensionToClient> splitPacket() {
        List<DimensionToClient> result = new ArrayList<>();
        for (int i = 0, index = 0; i < dimensionIds.size() / getChunkSize() + 1; i++) {
            List<String> chunk = new ArrayList<>();
            for (int j = 0; j < getChunkSize(); j++) {
                if (index >= dimensionIds.size()) break;
                chunk.add(dimensionIds.get(index));
                index++;
            }
            DimensionToClient packet = new DimensionToClient(chunk);
            packet.setId(this.getId());
            packet.setSort(i);
            result.add(packet);
        }
        result.forEach(packet -> packet.setTotal(result.size()));
        return result;
    }

    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeVarInt(dimensionIds.size());
        for (String id : dimensionIds) {
            buf.writeUtf(id != null ? id : "", 256);
        }
    }
}
