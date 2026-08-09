package xin.vanilla.banira.internal.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.forge.client.ForgeClientNetworkAccess;
import xin.vanilla.banira.platform.BaniraNetworkPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 分支的通道适配器；公共层只处理 Banira 网络包语义。
 */
public final class ForgeNetworkChannels {
    private static final Map<Class<?>, SimpleChannel> PACKET_CHANNELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SimpleChannel> CHANNELS_BY_NAME = new ConcurrentHashMap<>();
    private static SimpleChannel defaultChannel;

    private ForgeNetworkChannels() {
    }

    public static SimpleChannel resolve(BaniraNetworkPacket packet) {
        return PACKET_CHANNELS.getOrDefault(packet.getClass(), defaultChannel);
    }

    static void bind(Class<?> packetClass, SimpleChannel channel) {
        PACKET_CHANNELS.put(packetClass, channel);
        CHANNELS_BY_NAME.put(channel.getName(), channel);
    }

    static void installDefault(SimpleChannel channel) {
        CHANNELS_BY_NAME.put(channel.getName(), channel);
        if (defaultChannel == null) {
            defaultChannel = channel;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendToServer(BaniraNetworkPacket packet) {
        SimpleChannel channel = resolve(packet);
        if (!hasServerChannel(channel)) return;
        Player player = ForgeClientNetworkAccess.player();
        if (player == null) return;
        // 握手首包需要在远端状态建立前放行。
        if (!(packet instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(player, modId(channel))) {
            return;
        }

        channel.send(packet, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(BaniraNetworkPacket packet, ServerPlayer player) {
        SimpleChannel channel = resolve(packet);
        if (!hasPlayerChannel(player, channel)) return;
        if (!PlayerUtils.isRemoteClientModInstalled(player, modId(channel))) return;
        channel.send(packet, PacketDistributor.PLAYER.with(player));
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasDefaultChannel() {
        return defaultChannel != null && hasServerChannel(defaultChannel);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasLocalChannel(ResourceLocation channel) {
        return CHANNELS_BY_NAME.containsKey(channel);
    }

    public static boolean hasPlayerChannel(ServerPlayer player, ResourceLocation channel) {
        SimpleChannel registered = CHANNELS_BY_NAME.get(channel);
        return registered != null && registered.isRemotePresent(player.connection.getConnection());
    }

    private static boolean hasServerChannel(SimpleChannel channel) {
        return ForgeClientNetworkAccess.isRemotePresent(channel);
    }

    private static boolean hasPlayerChannel(ServerPlayer player, SimpleChannel channel) {
        return channel.isRemotePresent(player.connection.getConnection());
    }

    private static ResourceLocation channelName(SimpleChannel channel) {
        return channel.getName();
    }

    private static String modId(SimpleChannel channel) {
        return channelName(channel).getNamespace();
    }

}
