package xin.vanilla.banira.platform;

import org.junit.Test;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;

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
                public void sendToPlayer(BaniraNetworkPacket packet, Object player) {
                }

                @Override
                public boolean hasDefaultChannel() {
                    return false;
                }

                @Override
                public boolean hasLocalChannel(String channelId) {
                    return false;
                }

                @Override
                public boolean hasPlayerChannel(Object player, String channelId) {
                    return false;
                }
            };
        }

        @Override
        public BaniraRegistryService registryService() {
            return NoopRegistryService.INSTANCE;
        }

    }
}
