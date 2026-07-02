package xin.vanilla.banira.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.notification.NotificationTypeSyncEntry;
import xin.vanilla.banira.common.network.BaniraStreamCodecs;

import java.util.ArrayList;
import java.util.List;

/**
 * 将服务端当前已知的通知类型列表（及可选的「配置文件无条目时」展示方式建议）同步给刚登录的玩家客户端
 */
public class NotificationTypesSyncToClient implements NetworkPacket {

    public static final CustomPacketPayload.Type<NotificationTypesSyncToClient> TYPE =
            new CustomPacketPayload.Type<>(Identifier.id().create("notification_types"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NotificationTypesSyncToClient> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(NotificationTypesSyncToClient::toBytes, NotificationTypesSyncToClient::new);

    private static final int MAX_TYPE_ID_LENGTH = 128;
    private static final int MAX_TYPE_COUNT = 512;

    private final List<NotificationTypeSyncEntry> entries;

    public NotificationTypesSyncToClient(List<NotificationTypeSyncEntry> entries) {
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    public NotificationTypesSyncToClient(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0) {
            n = 0;
        }
        n = Math.min(n, MAX_TYPE_COUNT);
        this.entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = NotificationTypeKeys.normalizeOrDefault(buf.readUtf(MAX_TYPE_ID_LENGTH));
            boolean hasMode = buf.readBoolean();
            EnumNotificationTypeDisplayMode mode = null;
            if (hasMode) {
                int ord = buf.readVarInt();
                EnumNotificationTypeDisplayMode[] vals = EnumNotificationTypeDisplayMode.values();
                if (ord >= 0 && ord < vals.length) {
                    mode = vals[ord];
                }
            }
            this.entries.add(new NotificationTypeSyncEntry(id, mode));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        int n = Math.min(entries.size(), MAX_TYPE_COUNT);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            NotificationTypeSyncEntry e = entries.get(i);
            String id = e.typeId() != null ? NotificationTypeKeys.normalizeOrDefault(e.typeId()) : NotificationTypeKeys.DEFAULT;
            buf.writeUtf(id, MAX_TYPE_ID_LENGTH);
            EnumNotificationTypeDisplayMode m = e.defaultDisplayIfAbsent();
            buf.writeBoolean(m != null);
            if (m != null) {
                buf.writeVarInt(m.ordinal());
            }
        }
    }

    public List<NotificationTypeSyncEntry> entries() {
        return entries;
    }

    public static void handle(NotificationTypesSyncToClient packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow() == PacketFlow.CLIENTBOUND) {
                ClientSide.handle(packet);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientSide {
        private static void handle(NotificationTypesSyncToClient packet) {
            for (NotificationTypeSyncEntry e : packet.entries()) {
                if (e == null) {
                    continue;
                }
                NotificationTypeRegistry.ensureKnown(e.typeId());
                NotificationTypeRegistry.acceptServerSyncedDisplayDefault(e.typeId(), e.defaultDisplayIfAbsent());
                NotificationTypeSettingsStore.get().applyResolvedDisplayDefaultIfNoSavedEntry(e.typeId());
            }
        }
    }
}
