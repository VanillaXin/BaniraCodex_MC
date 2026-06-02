package xin.vanilla.banira.platform;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.junit.Test;
import xin.vanilla.banira.platform.config.BaniraConfigService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkService;
import xin.vanilla.banira.platform.registry.BaniraRegistryService;
import xin.vanilla.banira.platform.server.BaniraServerService;
import xin.vanilla.banira.platform.world.BaniraWorldService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.Assert.*;

public class BaniraPlatformsTest {
    @Test
    public void installExposesActivePlatform() {
        BaniraPlatform platform = new FakePlatform();

        BaniraPlatforms.install(platform);

        assertTrue(BaniraPlatforms.isInstalled());
        assertSame(platform, BaniraPlatforms.get());
        assertEquals("test", BaniraPlatforms.get().loaderType());
    }

    private static final class FakePlatform implements BaniraPlatform {
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
        public BaniraLifecycle lifecycle() {
            return null;
        }

        @Override
        public BaniraConfigService config() {
            return null;
        }

        @Override
        public BaniraNetworkService network() {
            return null;
        }

        @Override
        public BaniraRegistryService registry() {
            return null;
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
                public void refreshPlayerPermission(net.minecraft.entity.player.ServerPlayerEntity player) {
                }
            };
        }
    }
}
