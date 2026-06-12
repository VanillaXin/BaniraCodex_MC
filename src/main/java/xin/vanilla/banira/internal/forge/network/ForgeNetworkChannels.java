package xin.vanilla.banira.internal.forge.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.mixin.accessors.NetworkRegistryAccessor;
import xin.vanilla.banira.internal.mixin.accessors.SimpleChannelAccessor;
import xin.vanilla.banira.platform.BaniraNetworkPacket;

/**
 * Forge 分支的通道适配器；公共层只处理 Banira 网络包语义。
 */
public final class ForgeNetworkChannels {
    private static NetworkRegistryAccessor networkRegistry;
    private static SimpleChannel defaultChannel;

    private ForgeNetworkChannels() {
    }

    public static SimpleChannel resolve(BaniraNetworkPacket packet) {
        return defaultChannel;
    }

    static void installDefault(SimpleChannel channel) {
        if (defaultChannel == null) {
            defaultChannel = channel;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendToServer(BaniraNetworkPacket packet) {
        SimpleChannel channel = resolve(packet);
        if (!hasLocalChannel(channel)) return;
        LocalPlayer player = BaniraClientRuntime.localPlayer();
        if (player == null) return;
        // 握手首包需要在远端状态建立前放行。
        if (!(packet instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(player, modId(channel))) {
            return;
        }

        channel.sendToServer(packet);
    }

    public static void sendToPlayer(BaniraNetworkPacket packet, ServerPlayer player) {
        SimpleChannel channel = resolve(packet);
        if (!hasPlayerChannel(player, channel)) return;
        if (!PlayerUtils.isRemoteClientModInstalled(player, modId(channel))) return;
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasDefaultChannel() {
        return defaultChannel != null && hasLocalChannel(defaultChannel);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasLocalChannel(ResourceLocation channel) {
        init();
        return networkRegistry.banira$instances().containsKey(channel);
    }

    public static boolean hasPlayerChannel(ServerPlayer player, ResourceLocation channel) {
        init();
        return networkRegistry.banira$instances().containsKey(channel);
    }

    private static boolean hasLocalChannel(SimpleChannel channel) {
        return hasLocalChannel(channelName(channel));
    }

    private static boolean hasPlayerChannel(ServerPlayer player, SimpleChannel channel) {
        return hasPlayerChannel(player, channelName(channel));
    }

    private static ResourceLocation channelName(SimpleChannel channel) {
        return ((SimpleChannelAccessor) channel).banira$instance().getChannelName();
    }

    private static String modId(SimpleChannel channel) {
        return channelName(channel).getNamespace();
    }

    private static void init() {
        if (networkRegistry == null) {
            networkRegistry = (NetworkRegistryAccessor) new NetworkRegistry();
        }
    }
}
