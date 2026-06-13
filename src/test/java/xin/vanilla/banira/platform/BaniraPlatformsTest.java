package xin.vanilla.banira.platform;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.junit.Test;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.platform.client.BaniraClientService;
import xin.vanilla.banira.platform.command.BaniraCommandService;
import xin.vanilla.banira.platform.resource.BaniraResourceService;
import xin.vanilla.banira.platform.server.BaniraServerService;
import xin.vanilla.banira.platform.world.BaniraWorldService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.Assert.*;

public class BaniraPlatformsTest {
    @Test
    public void installExposesActivePlatform() {
        BaniraPlatform platform = new FakePlatform();

        BaniraPlatforms.install(platform);

        assertTrue(BaniraPlatforms.isInstalled());
        assertSame(platform, BaniraPlatforms.get());
        assertEquals("test", BaniraPlatforms.get().loaderType());
        assertEquals("1.16.5", BaniraPlatforms.get().minecraftVersion());
        assertEquals(Paths.get("config", "vanilla.xin"), BaniraPlatforms.get().pathService().configPath());
        assertNotNull(BaniraPlatforms.get().inputService());
        assertNotNull(BaniraPlatforms.get().configService());
        assertNotNull(BaniraPlatforms.get().networkService());
        assertNotNull(BaniraPlatforms.get().registryService());
    }

    public static final class FakePlatform implements BaniraPlatform {
        public FakePlatform() {
        }

        @Override
        public String loaderType() {
            return "test";
        }

        @Override
        public boolean isClient() {
            return false;
        }

        @Override
        public boolean isDedicatedServer() {
            return true;
        }

        @Override
        public boolean isDevelopment() {
            return true;
        }

        @Override
        public boolean isModLoaded(String modId) {
            return false;
        }

        @Override
        public String modDisplayName(String modId) {
            return modId;
        }

        @Override
        public String modIdFromMainClass(Class<?> modMainClass) {
            return "test";
        }

        @Override
        public Class<?> modMainClass(String modId) {
            return FakePlatform.class;
        }

        @Override
        public String lastKnownUsername(UUID uuid) {
            return null;
        }

        @Override
        public Path configDir() {
            return Paths.get("config");
        }

        @Override
        public BaniraConfigService configService() {
            return new BaniraConfigService() {
                @Override
                public <T> void register(Class<T> configClass, String modId) {
                }

                @Override
                public <T> T get(Class<T> configClass) {
                    return null;
                }

                @Override
                public BaniraConfigHandle handle(Class<?> configClass) {
                    return null;
                }
            };
        }

        @Override
        public BaniraCommandService command() {
            return new BaniraCommandService() {
                @Override
                public boolean executePlayerCommand(net.minecraft.entity.player.ServerPlayerEntity player, String command, int permission, boolean suppressedOutput) {
                    return false;
                }

                @Override
                public boolean hasPermission(Object source, int permission) {
                    return false;
                }

                @Override
                public net.minecraft.entity.Entity sourceEntity(Object source) {
                    return null;
                }

                @Override
                public net.minecraft.entity.player.ServerPlayerEntity sourcePlayer(Object source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                    throw net.minecraft.command.CommandSource.ERROR_NOT_PLAYER.create();
                }

                @Override
                public net.minecraft.world.server.ServerWorld dimension(com.mojang.brigadier.context.CommandContext<?> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                    return null;
                }

                @Override
                public RegistryKey<World> dimensionKey(com.mojang.brigadier.context.CommandContext<?> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                    return World.OVERWORLD;
                }

                @Override
                public net.minecraft.entity.player.ServerPlayerEntity player(com.mojang.brigadier.context.CommandContext<?> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                    throw net.minecraft.command.CommandSource.ERROR_NOT_PLAYER.create();
                }

                @Override
                public net.minecraft.entity.player.ServerPlayerEntity playerOrSelf(com.mojang.brigadier.context.CommandContext<?> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                    throw net.minecraft.command.CommandSource.ERROR_NOT_PLAYER.create();
                }

                @Override
                public java.util.Collection<net.minecraft.entity.player.ServerPlayerEntity> players(com.mojang.brigadier.context.CommandContext<?> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                    return java.util.Collections.emptyList();
                }

                @Override
                public void sendSuccess(Object source, net.minecraft.util.text.ITextComponent message, boolean notifyAdmins) {
                }

                @Override
                public void sendFailure(Object source, net.minecraft.util.text.ITextComponent message) {
                }

                @Override
                public Object literal(String name) {
                    return new Object();
                }

                @Override
                public void executes(Object commandNode, xin.vanilla.banira.platform.command.BaniraCommandExecutor executor) {
                }

                @Override
                public void then(Object parentNode, Object childNode) {
                }

                @Override
                public void register(Object dispatcher, Object commandNode) {
                }

                @Override
                public void onRegisterDispatcher(java.util.function.Consumer<Object> callback) {
                }

                @Override
                public void dispatchRegisterDispatcher(Object dispatcher) {
                }
            };
        }

        @Override
        public BaniraClientService client() {
            return BaniraClientService.noop();
        }

        @Override
        public BaniraNetworkService networkService() {
            return new BaniraNetworkService() {
                @Override
                public NetworkPacketRegistrar registrar(String channelName, xin.vanilla.banira.common.util.IIdentifier identifier) {
                    return new NetworkPacketRegistrar() {
                        @Override
                        public <MSG extends INetworkPacket> void register(
                                int packetId,
                                Class<MSG> packetClass,
                                BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                Function<BaniraPacketBuffer, MSG> decoder,
                                BiConsumer<MSG, BaniraNetworkContext> handler) {
                        }
                    };
                }

                @Override
                public void sendToServer(BaniraNetworkPacket packet) {
                }

                @Override
                public void sendToPlayer(BaniraNetworkPacket packet, net.minecraft.entity.player.ServerPlayerEntity player) {
                }

                @Override
                public boolean hasDefaultChannel() {
                    return false;
                }

                @Override
                public boolean hasLocalChannel(ResourceLocation channel) {
                    return false;
                }

                @Override
                public boolean hasPlayerChannel(net.minecraft.entity.player.ServerPlayerEntity player, ResourceLocation channel) {
                    return false;
                }
            };
        }

        @Override
        public BaniraRegistryService registryService() {
            return NoopRegistryService.INSTANCE;
        }

        @Override
        public BaniraWorldService world() {
            return new BaniraWorldService() {
                @Override
                public net.minecraft.world.biome.Biome biome(ResourceLocation id) {
                    return null;
                }

                @Override
                public net.minecraft.world.biome.Biome biome(net.minecraft.world.server.ServerWorld world, ResourceLocation id) {
                    return null;
                }

                @Override
                public java.util.Set<String> biomeIds() {
                    return java.util.Collections.emptySet();
                }

                @Override
                public xin.vanilla.banira.common.data.WorldCoordinate findNearestBiome(net.minecraft.world.server.ServerWorld world, xin.vanilla.banira.common.data.WorldCoordinate start, net.minecraft.world.biome.Biome biome, int radius, int minDistance) {
                    return null;
                }

                @Override
                public net.minecraft.world.server.ServerWorld level(RegistryKey<World> dimension) {
                    return null;
                }

                @Override
                public java.util.Collection<net.minecraft.world.server.ServerWorld> loadedServerWorlds() {
                    return java.util.Collections.emptyList();
                }

                @Override
                public RegistryKey<World> dimensionKey(ResourceLocation dimension) {
                    return World.OVERWORLD;
                }

                @Override
                public java.util.Set<String> dimensionIds() {
                    return java.util.Collections.emptySet();
                }

                @Override
                public int minBuildHeight(World world) {
                    return 0;
                }

                @Override
                public int maxBuildHeight(World world) {
                    return 0;
                }

                @Override
                public xin.vanilla.banira.common.data.WorldCoordinate findNearestStructure(net.minecraft.world.server.ServerWorld world, xin.vanilla.banira.common.data.WorldCoordinate start, net.minecraft.world.gen.feature.structure.Structure<?> structure, int radius) {
                    return null;
                }
            };
        }

        @Override
        public BaniraServerService server() {
            return new BaniraServerService() {
                @Override
                public net.minecraft.server.MinecraftServer currentServer() {
                    return null;
                }

                @Override
                public java.util.List<net.minecraft.entity.player.ServerPlayerEntity> players() {
                    return java.util.Collections.emptyList();
                }

                @Override
                public net.minecraft.entity.player.ServerPlayerEntity player(UUID uuid) {
                    return null;
                }

                @Override
                public void broadcastRawPacket(net.minecraft.network.IPacket<?> packet) {
                }

                @Override
                public void broadcastSystemMessage(net.minecraft.server.MinecraftServer server, net.minecraft.util.text.ITextComponent message) {
                }

                @Override
                public void sendPlayerMessage(net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.text.ITextComponent message) {
                }

                @Override
                public void sendActionBarMessage(net.minecraft.entity.player.ServerPlayerEntity player, net.minecraft.util.text.ITextComponent message) {
                }

                @Override
                public void refreshPlayerPermission(net.minecraft.entity.player.ServerPlayerEntity player) {
                }

                @Override
                public java.util.Collection<net.minecraft.advancements.Advancement> advancements() {
                    return java.util.Collections.emptyList();
                }

                @Override
                public net.minecraft.resources.IResourceManager serverResourceManager() {
                    return null;
                }

                @Override
                public java.nio.file.Path worldDataPath(String directoryName) {
                    return java.nio.file.Paths.get("world", directoryName == null ? "" : directoryName);
                }

                @Override
                public java.nio.file.Path worldPlayerDataPath() {
                    return java.nio.file.Paths.get("world", "playerdata");
                }

                @Override
                public long tickCount() {
                    return 0;
                }
            };
        }

        @Override
        public BaniraResourceService resources() {
            return modId -> java.util.Collections.emptyMap();
        }
    }
}
