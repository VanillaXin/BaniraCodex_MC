package xin.vanilla.banira.internal.network.packet;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.util.BiomeUtils;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


@Getter
public class BiomeToClient extends SplitPacket
        implements SplitPacket.MergeableSplitPacket<BiomeToClient>,
        SplitPacket.SplittableSplitPacket<BiomeToClient>,
        NetworkPacket {

    public static final CustomPacketPayload.Type<BiomeToClient> TYPE =
            new CustomPacketPayload.Type<>(Identifier.id().create("biome_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BiomeToClient> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(BiomeToClient::toBytes, BiomeToClient::new);


    private final List<String> biomeIds;

    public BiomeToClient(Collection<String> biomeIds) {
        super();
        this.biomeIds = biomeIds != null ? new ArrayList<>(biomeIds) : new ArrayList<>();
    }

    public BiomeToClient(FriendlyByteBuf buf) {
        super(buf);
        int size = buf.readVarInt();
        this.biomeIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            biomeIds.add(buf.readUtf(256));
        }
    }

    public static void handle(BiomeToClient packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow() == PacketFlow.CLIENTBOUND) {
                BiomeUtils.setClientBiomeIds(packet.getBiomeIds());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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

    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeVarInt(biomeIds.size());
        for (String id : biomeIds) {
            buf.writeUtf(id != null ? id : "", 256);
        }
    }
}
