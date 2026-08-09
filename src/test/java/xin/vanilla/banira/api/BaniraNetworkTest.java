package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.platform.BaniraNetworkPacket;
import xin.vanilla.banira.platform.BaniraNetworkService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import javax.annotation.Nonnull;

import static org.junit.Assert.*;

public class BaniraNetworkTest {

    @Test
    public void sendsThroughPlatformService() {
        RecordingNetworkService service = new RecordingNetworkService();
        BaniraPlatforms.install(new TestBaniraPlatform().networkService(service));

        TestPacket packet = new TestPacket();
        Object player = new Object();

        BaniraNetwork.sendToServer(packet);
        BaniraNetwork.sendToPlayer(packet, player);

        assertSame(packet, service.serverPacket);
        assertSame(packet, service.playerPacket);
        assertSame(player, service.player);
    }

    @Test
    public void readsChannelStateThroughPlatformService() {
        RecordingNetworkService service = new RecordingNetworkService();
        service.defaultChannel = true;
        service.localChannel = true;
        service.playerChannel = true;
        BaniraPlatforms.install(new TestBaniraPlatform().networkService(service));

        Object player = new Object();

        assertTrue(BaniraNetwork.hasBaniraServer());
        assertTrue(BaniraNetwork.hasLocalChannel("example:main"));
        assertTrue(BaniraNetwork.hasPlayerChannel(player, "example:main"));
        assertEquals("example:main", service.localChannelId);
        assertSame(player, service.channelPlayer);
        assertEquals("example:main", service.playerChannelId);
    }

    private static final class TestPacket implements BaniraNetworkPacket {
    }

    private static final class RecordingNetworkService implements BaniraNetworkService {
        private BaniraNetworkPacket serverPacket;
        private BaniraNetworkPacket playerPacket;
        private Object player;
        private boolean defaultChannel;
        private boolean localChannel;
        private boolean playerChannel;
        private String localChannelId;
        private Object channelPlayer;
        private String playerChannelId;

        @Override
        public @Nonnull NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier) {
            throw new UnsupportedOperationException("registrar");
        }

        @Override
        public void sendToServer(@Nonnull BaniraNetworkPacket packet) {
            this.serverPacket = packet;
        }

        @Override
        public void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
            this.playerPacket = packet;
            this.player = player;
        }

        @Override
        public boolean hasDefaultChannel() {
            return defaultChannel;
        }

        @Override
        public boolean hasLocalChannel(@Nonnull String channelId) {
            this.localChannelId = channelId;
            return localChannel;
        }

        @Override
        public boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
            this.channelPlayer = player;
            this.playerChannelId = channelId;
            return playerChannel;
        }
    }
}
