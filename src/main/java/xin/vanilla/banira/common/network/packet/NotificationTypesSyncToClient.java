package xin.vanilla.banira.common.network.packet;

import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.notification.NotificationTypeSyncEntry;
import xin.vanilla.banira.internal.client.BaniraClientPacketHandlers;

import java.util.ArrayList;
import java.util.List;

/**
 * 将服务端当前已知的通知类型列表（及可选的「配置文件无条目时」展示方式建议）同步给刚登录的玩家客户端
 */
public class NotificationTypesSyncToClient implements NetworkPacket {

    private static final int MAX_TYPE_ID_LENGTH = 128;
    private static final int MAX_TYPE_COUNT = 512;
    private static final int IMPORTANCE_EXTENSION = 0x424E4901;

    private final List<NotificationTypeSyncEntry> entries;

    public NotificationTypesSyncToClient(List<NotificationTypeSyncEntry> entries) {
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    public NotificationTypesSyncToClient(BaniraPacketBuffer buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_TYPE_COUNT) throw new IllegalArgumentException("Invalid notification type count");
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
        // Keep the original entry layout intact for older peers.
        if (buf.readableBytes() > 0) {
            if (buf.readInt() != IMPORTANCE_EXTENSION || buf.readVarInt() != n) {
                throw new IllegalArgumentException("Invalid notification importance extension");
            }
            for (int i = 0; i < n; i++) {
                int importance = buf.readByte();
                if (importance < 0 || importance > 2)
                    throw new IllegalArgumentException("Invalid notification importance");
                NotificationTypeSyncEntry entry = this.entries.get(i);
                this.entries.set(i, new NotificationTypeSyncEntry(entry.typeId(), entry.defaultDisplayIfAbsent(),
                        importance == 0 ? null : importance == 2));
            }
        }
    }

    public void toBytes(BaniraPacketBuffer buf) {
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
        buf.writeInt(IMPORTANCE_EXTENSION);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            Boolean important = entries.get(i).important();
            buf.writeByte(important == null ? 0 : important ? 2 : 1);
        }
    }

    public List<NotificationTypeSyncEntry> entries() {
        return entries;
    }

    public static void handle(NotificationTypesSyncToClient packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isClientSide()) {
                BaniraClientPacketHandlers.applyNotificationTypes(packet);
            }
        });
        ctx.markHandled();
    }
}
