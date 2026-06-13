package xin.vanilla.banira.platform;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.client.BaniraApiInputBridge;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.client.BaniraClientService;
import xin.vanilla.banira.platform.command.BaniraCommandService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;
import xin.vanilla.banira.platform.resource.BaniraResourceService;
import xin.vanilla.banira.platform.server.BaniraServerService;
import xin.vanilla.banira.platform.world.BaniraWorldService;

import java.nio.file.Path;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Loader-neutral platform surface exposed to dependent mods.
 */
public interface BaniraPlatform {
    String loaderType();

    default String minecraftVersion() {
        return "1.16.5";
    }

    boolean isClient();

    boolean isDedicatedServer();

    boolean isDevelopment();

    boolean isModLoaded(String modId);

    String modDisplayName(String modId);

    String modIdFromMainClass(Class<?> modMainClass);

    Class<?> modMainClass(String modId);

    String lastKnownUsername(UUID uuid);

    Path configDir();

    /**
     * 当前加载器和 MC 版本的数据路径服务。
     */
    default BaniraPathService pathService() {
        BaniraPlatform self = this;
        return new BaniraPathService() {
            @Override
            public String rootDirectoryName() {
                return "vanilla.xin";
            }

            @Override
            public Path configPath() {
                return self.configDir().resolve(rootDirectoryName());
            }

            @Override
            public Path worldDataPath() {
                return self.server().worldDataPath(rootDirectoryName());
            }

            @Override
            public Path playerDataPath() {
                Path worldDataPath = worldDataPath();
                return worldDataPath != null ? worldDataPath.resolve("playerdata") : null;
            }

            @Override
            public Path vanillaPlayerDataPath() {
                return self.server().worldPlayerDataPath();
            }
        };
    }

    /**
     * 当前加载器的客户端输入服务。
     */
    default BaniraInputService inputService() {
        return BaniraApiInputBridge.service();
    }

    BaniraLifecycle lifecycle();
    /**
     * 根级配置服务；子 mod 只通过该入口注册、读取配置。
     */
    BaniraConfigService configService();

    BaniraCommandService command();

    BaniraClientService client();

    /**
     * @deprecated 子 mod 请使用 {@link #networkService()}；该入口仅保留给 1.16.5 内部适配。
     */
    @Deprecated
    xin.vanilla.banira.platform.network.BaniraNetworkService network();

    /**
     * 根级网络服务是新版公共入口；16.5 分支内部仍通过旧 channel 实现真实发送与注册。
     */
    default BaniraNetworkService networkService() {
        BaniraPlatform self = this;
        return new BaniraNetworkService() {
            @Override
            public NetworkPacketRegistrar registrar(String channelName, IIdentifier identifier) {
                BaniraNetworkChannel channel = self.network().create(channelName, identifier);
                return new NetworkPacketRegistrar() {
                    @Override
                    public <MSG extends INetworkPacket> void register(
                            int packetId,
                            Class<MSG> packetClass,
                            BiConsumer<MSG, xin.vanilla.banira.common.network.BaniraPacketBuffer> encoder,
                            Function<xin.vanilla.banira.common.network.BaniraPacketBuffer, MSG> decoder,
                            BiConsumer<MSG, xin.vanilla.banira.common.network.BaniraNetworkContext> handler) {
                        channel.register(packetClass, encoder, decoder, handler);
                    }
                };
            }

            @Override
            public void sendToServer(BaniraNetworkPacket packet) {
                INetworkPacket legacyPacket = asLegacyPacket(packet);
                BaniraNetworkChannel channel = legacyPacket.networkChannel();
                if (!self.network().hasChannel(channel)) {
                    return;
                }
                PlayerEntity player = self.client().localPlayer();
                if (player == null) {
                    return;
                }
                if (!(packet instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(player, channel.modId())) {
                    return;
                }
                channel.sendToServer(legacyPacket);
            }

            @Override
            public void sendToPlayer(BaniraNetworkPacket packet, ServerPlayerEntity player) {
                INetworkPacket legacyPacket = asLegacyPacket(packet);
                BaniraNetworkChannel channel = legacyPacket.networkChannel();
                if (!self.network().hasChannel(player, channel)) {
                    return;
                }
                if (!PlayerUtils.isRemoteClientModInstalled(player, channel.modId())) {
                    return;
                }
                channel.sendToPlayer(player, legacyPacket);
            }

            @Override
            public boolean hasDefaultChannel() {
                return self.network().hasChannel(NetworkInit.DEFAULT_CHANNEL);
            }

            @Override
            public boolean hasLocalChannel(ResourceLocation channel) {
                return self.network().hasChannel(channel);
            }

            @Override
            public boolean hasPlayerChannel(ServerPlayerEntity player, ResourceLocation channel) {
                return self.network().hasChannel(player, channel);
            }

            private INetworkPacket asLegacyPacket(BaniraNetworkPacket packet) {
                if (packet instanceof INetworkPacket) {
                    return (INetworkPacket) packet;
                }
                throw new IllegalArgumentException("BaniraNetworkPacket must also implement INetworkPacket on Forge 1.16.5");
            }
        };
    }

    /**
     * 根级注册表服务；具体加载器和 MC 版本差异留在实现层。
     */
    BaniraRegistryService registryService();

    BaniraWorldService world();

    BaniraServerService server();

    BaniraResourceService resources();
}
