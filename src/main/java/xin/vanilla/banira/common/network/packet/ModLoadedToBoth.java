package xin.vanilla.banira.common.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.server.ServerSenderAccess;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 双向握手：客户端向服务端上报本端已安装且已注册的 mod；服务端处理后向客户端回传本端（服务端）已注册的 mod，
 * 分别写入 {@link PlayerUtils} 的远程客户端 / 远程服务端状态。
 */
@Getter
@Accessors(chain = true, fluent = true)
public class ModLoadedToBoth implements NetworkPacket {

    /**
     * 物理客户端收到服务端回包时的处理（由客户端初始化注册，避免 common 类引用 {@code Minecraft}）。
     */
    private static Consumer<ModLoadedToBoth> clientHandler = pkt -> {
    };

    /**
     * 注册客户端侧对「服务端回传的 mod 列表」的处理逻辑。
     */
    public static void registerClientHandler(@Nonnull Consumer<ModLoadedToBoth> handler) {
        if (handler != null) {
            clientHandler = handler;
        } else {
            clientHandler = pkt -> {
            };
        }
    }

    /**
     * 单包允许的最大 modid 条数，防止异常大包。
     */
    public static final int MAX_MODIDS = 128;

    private final List<String> modids;

    public ModLoadedToBoth(@Nonnull List<String> modids) {
        this.modids = List.copyOf(modids);
    }

    /**
     * 单 modid 上报
     */
    public ModLoadedToBoth(String modid) {
        this(singleNonEmpty(modid));
    }

    private static List<String> singleNonEmpty(String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(modid);
    }

    public ModLoadedToBoth(BaniraPacketBuffer buf) {
        int n = buf.readVarInt();
        if (n < 0) {
            this.modids = Collections.emptyList();
        } else {
            int use = Math.min(n, MAX_MODIDS);
            List<String> list = new ArrayList<>(use);
            for (int i = 0; i < use; i++) {
                String id = buf.readUtf(256);
                if (!StringUtils.isNullOrEmptyEx(id)) {
                    list.add(id);
                }
            }
            for (int i = use; i < n; i++) {
                buf.readUtf(256);
            }
            this.modids = Collections.unmodifiableList(list);
        }
    }

    public void toBytes(BaniraPacketBuffer buf) {
        List<String> toWrite = new ArrayList<>();
        for (String id : modids) {
            if (!StringUtils.isNullOrEmptyEx(id)) {
                toWrite.add(id);
            }
            if (toWrite.size() >= MAX_MODIDS) {
                break;
            }
        }
        buf.writeVarInt(toWrite.size());
        for (String id : toWrite) {
            buf.writeUtf(id, 256);
        }
    }

    public static void handle(ModLoadedToBoth packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isServerSide()) {
                Object sender = ctx.sender();
                if (sender == null || packet.modids().isEmpty()) {
                    return;
                }
                for (String modid : packet.modids()) {
                    ServerSenderAccess.setRemoteClientModInstalled(sender, modid, false);
                    ModLoadedPresence.dispatchServerSync(sender, modid);
                }
                List<String> serverIds = ModLoadedPresence.announcedModIds();
                if (!serverIds.isEmpty()) {
                    ServerSenderAccess.sendPacket(sender, new ModLoadedToBoth(serverIds));
                }
            } else {
                if (!packet.modids().isEmpty()) {
                    clientHandler.accept(packet);
                }
            }
        });
        ctx.markHandled();
    }
}
