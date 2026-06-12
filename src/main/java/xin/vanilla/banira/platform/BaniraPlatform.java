package xin.vanilla.banira.platform;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.client.BaniraApiInputBridge;
import xin.vanilla.banira.internal.config.BaniraConfigHandleAdapter;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.client.BaniraClientService;
import xin.vanilla.banira.platform.command.BaniraCommandService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;
import xin.vanilla.banira.platform.resource.BaniraResourceService;
import xin.vanilla.banira.platform.server.BaniraServerService;
import xin.vanilla.banira.platform.world.BaniraWorldService;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
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
     * @deprecated 子 mod 请使用 {@link #configService()}；该入口仅保留给 1.16.5 内部适配。
     */
    @Deprecated
    xin.vanilla.banira.platform.config.BaniraConfigService config();

    /**
     * 根级配置服务是新版公共入口，旧的 platform.config 服务仅作为当前分支内部实现保留。
     */
    default BaniraConfigService configService() {
        BaniraPlatform self = this;
        return new BaniraConfigService() {
            @Override
            public <T> void register(Class<T> configClass, String modId) {
                self.config().register(configClass, modId);
            }

            @Override
            public <T> T get(Class<T> configClass) {
                return self.config().get(configClass);
            }

            @Override
            public BaniraConfigHandle handle(Class<?> configClass) {
                ConfigHolder holder = self.config().getHolder(configClass);
                return holder != null ? new BaniraConfigHandleAdapter(holder) : null;
            }
        };
    }

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
     * @deprecated 子 mod 请使用 {@link #registryService()}；该入口仅保留给 1.16.5 内部适配。
     */
    @Deprecated
    xin.vanilla.banira.platform.registry.BaniraRegistryService registry();

    /**
     * 根级注册表服务是新版公共入口；旧的 platform.registry 服务仅作为当前分支内部实现保留。
     */
    default BaniraRegistryService registryService() {
        BaniraPlatform self = this;
        return new BaniraRegistryService() {
            @Override
            public net.minecraft.util.ResourceLocation blockKey(net.minecraft.block.Block block) {
                return self.registry().blockId(block);
            }

            @Override
            public net.minecraft.block.Block block(net.minecraft.util.ResourceLocation id) {
                return self.registry().block(id);
            }

            @Override
            public Collection<net.minecraft.block.Block> blocks() {
                return self.registry().blocks();
            }

            @Override
            public net.minecraft.util.ResourceLocation itemKey(net.minecraft.item.Item item) {
                return self.registry().itemId(item);
            }

            @Override
            public net.minecraft.item.Item item(net.minecraft.util.ResourceLocation id) {
                return self.registry().item(id);
            }

            @Override
            public Collection<net.minecraft.item.Item> items() {
                return self.registry().items();
            }

            @Override
            public Collection<net.minecraft.util.ResourceLocation> itemTagIds(net.minecraft.item.Item item) {
                return Collections.emptyList();
            }

            @Override
            public net.minecraft.util.ResourceLocation entityTypeKey(net.minecraft.entity.EntityType<?> entityType) {
                return self.registry().entityTypeId(entityType);
            }

            @Override
            public net.minecraft.entity.EntityType<?> entityType(net.minecraft.util.ResourceLocation id) {
                return self.registry().entityType(id);
            }

            @Override
            public Collection<net.minecraft.entity.EntityType<?>> entityTypes() {
                return self.registry().entityTypes();
            }

            @Override
            public net.minecraft.util.ResourceLocation effectKey(net.minecraft.potion.Effect effect) {
                return self.registry().effectId(effect);
            }

            @Override
            public net.minecraft.potion.Effect effect(net.minecraft.util.ResourceLocation id) {
                return self.registry().effect(id);
            }

            @Override
            public Collection<net.minecraft.potion.Effect> effects() {
                return self.registry().effects();
            }

            @Override
            public net.minecraft.world.biome.Biome biome(net.minecraft.util.ResourceLocation id) {
                return self.registry().biome(id);
            }

            @Override
            public Collection<net.minecraft.util.ResourceLocation> biomeIds() {
                java.util.List<net.minecraft.util.ResourceLocation> ids = new java.util.ArrayList<>();
                for (String id : self.registry().biomeIds()) {
                    ids.add(new net.minecraft.util.ResourceLocation(id));
                }
                return ids;
            }
        };
    }

    BaniraWorldService world();

    BaniraServerService server();

    BaniraResourceService resources();
}
