package xin.vanilla.banira.internal.network.packet;

import lombok.Getter;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.banira.common.data.ArraySet;
import xin.vanilla.banira.common.network.packet.SplitPacket;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.internal.network.data.AdvancementData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Getter
public class AdvancementToClient extends SplitPacket
        implements SplitPacket.MergeableSplitPacket<AdvancementToClient>,
        SplitPacket.SplittableSplitPacket<AdvancementToClient> {
    private final ArraySet<AdvancementData> advancements;

    public AdvancementToClient(ArraySet<AdvancementData> advancements) {
        super();
        this.advancements = advancements;
    }

    public AdvancementToClient(PacketBuffer buf) {
        super(buf);
        int size = buf.readVarInt();
        ArraySet<AdvancementData> advancements = new ArraySet<>();
        for (int i = 0; i < size; i++) {
            advancements.add(AdvancementData.readFromBuffer(buf));
        }
        this.advancements = advancements;
    }

    private AdvancementToClient(List<AdvancementToClient> packets) {
        super();
        this.advancements = new ArraySet<>();
        this.advancements.addAll(packets.stream().flatMap(packet -> packet.getAdvancements().stream()).collect(Collectors.toList()));
    }

    /**
     * 处理数据包
     */
    public static void handle(AdvancementToClient packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                AdvancementUtils.advancementData(packet.getAdvancements());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @Override
    public int getChunkSize() {
        return 1024;
    }

    /**
     * 合并多个分包
     */
    @Override
    public AdvancementToClient mergePackets(List<AdvancementToClient> packets) {
        return new AdvancementToClient(packets);
    }

    /**
     * 将数据包拆分为多个小包
     */
    @Override
    public List<AdvancementToClient> splitPacket() {
        List<AdvancementToClient> result = new ArrayList<>();
        for (int i = 0, index = 0; i < advancements.size() / getChunkSize() + 1; i++) {
            AdvancementToClient packet = new AdvancementToClient(new ArraySet<>());
            for (int j = 0; j < getChunkSize(); j++) {
                if (index >= advancements.size()) break;
                packet.advancements.add(this.advancements.get(index));
                index++;
            }
            packet.setId(this.getId());
            packet.setSort(i);
            result.add(packet);
        }
        result.forEach(packet -> packet.setTotal(result.size()));
        return result;
    }

    public void toBytes(PacketBuffer buf) {
        super.toBytes(buf);
        buf.writeVarInt(this.advancements.size());
        for (AdvancementData data : this.advancements) {
            data.writeToBuffer(buf);
        }
    }

}
