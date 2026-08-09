package xin.vanilla.banira.common.util;

import net.minecraft.resources.ResourceLocation;
import org.junit.Test;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.platform.BaniraNetworkPacket;
import xin.vanilla.banira.platform.BaniraNetworkService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PacketUtilsTest {

    @Test
    public void sendPacketToServerDelegatesToPlatformNetworkService() {
        RecordingNetworkService service = installRecordingNetwork();
        TestPacket packet = new TestPacket("single");

        PacketUtils.sendPacketToServer(packet);

        assertEquals(List.of(packet), service.serverPackets);
    }

    @Test
    public void sendPacketToPlayerDelegatesToPlatformNetworkService() {
        RecordingNetworkService service = installRecordingNetwork();
        TestPacket packet = new TestPacket("player");

        PacketUtils.sendPacketToPlayer(packet, null);

        assertEquals(List.of(packet), service.playerPackets);
        assertEquals(1, service.players.size());
        assertSame(null, service.players.get(0));
    }

    @Test
    public void sendSplitPacketToServerSendsPartsInSplitOrder() {
        RecordingNetworkService service = installRecordingNetwork();
        TestPacket first = new TestPacket("a");
        TestPacket second = new TestPacket("b");

        PacketUtils.sendSplitPacketToServer(new TestPacket("root", List.of(first, second)));

        assertEquals(List.of(first, second), service.serverPackets);
    }

    @Test
    public void sendSplitPacketToPlayerSendsPartsInSplitOrder() {
        RecordingNetworkService service = installRecordingNetwork();
        TestPacket first = new TestPacket("a");
        TestPacket second = new TestPacket("b");

        PacketUtils.sendSplitPacketToPlayer(new TestPacket("root", List.of(first, second)), null);

        assertEquals(List.of(first, second), service.playerPackets);
        assertEquals(2, service.players.size());
        assertNull(service.players.get(0));
        assertNull(service.players.get(1));
    }

    @Test
    public void channelStatusQueriesDelegateToPlatformNetworkService() {
        RecordingNetworkService service = installRecordingNetwork();
        ResourceLocation localChannel = new ResourceLocation("network_test", "local");
        ResourceLocation playerChannel = new ResourceLocation("network_test", "player");
        service.defaultChannelResult = true;
        service.localChannelResult = true;
        service.playerChannelResult = false;

        assertTrue(PacketUtils.hasBaniraServer());
        assertTrue(PacketUtils.hasChannel(localChannel));
        assertFalse(PacketUtils.hasChannel(null, playerChannel));

        assertEquals(localChannel.toString(), service.queriedLocalChannel);
        assertNull(service.queriedPlayer);
        assertEquals(playerChannel.toString(), service.queriedPlayerChannel);
    }

    private static RecordingNetworkService installRecordingNetwork() {
        RecordingNetworkService service = new RecordingNetworkService();
        BaniraPlatforms.install(new TestBaniraPlatform().networkService(service));
        return service;
    }

    private static final class RecordingNetworkService implements BaniraNetworkService {
        private final List<INetworkPacket> serverPackets = new ArrayList<>();
        private final List<INetworkPacket> playerPackets = new ArrayList<>();
        private final List<Object> players = new ArrayList<>();
        private boolean defaultChannelResult;
        private boolean localChannelResult;
        private boolean playerChannelResult;
        private String queriedLocalChannel;
        private Object queriedPlayer;
        private String queriedPlayerChannel;

        @Override
        public @Nonnull xin.vanilla.banira.common.network.NetworkPacketRegistrar registrar(@Nonnull String channelName,
                                                                                           @Nonnull BaniraIdentifier identifier) {
            throw new UnsupportedOperationException("PacketUtilsTest does not register packets");
        }

        @Override
        public void sendToServer(@Nonnull BaniraNetworkPacket packet) {
            serverPackets.add((INetworkPacket) packet);
        }

        @Override
        public void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
            playerPackets.add((INetworkPacket) packet);
            players.add(player);
        }

        @Override
        public boolean hasDefaultChannel() {
            return defaultChannelResult;
        }

        @Override
        public boolean hasLocalChannel(@Nonnull String channelId) {
            queriedLocalChannel = channelId;
            return localChannelResult;
        }

        @Override
        public boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
            queriedPlayer = player;
            queriedPlayerChannel = channelId;
            return playerChannelResult;
        }
    }

    private static final class TestPacket extends SplitPacket
            implements INetworkPacket, SplitPacket.SplittableSplitPacket<TestPacket> {
        private final String name;
        private final List<TestPacket> splitParts;

        private TestPacket(String name) {
            this(name, null);
        }

        private TestPacket(String name, List<TestPacket> splitParts) {
            this.name = name;
            this.splitParts = splitParts;
        }

        @Override
        public List<TestPacket> splitPacket() {
            return splitParts == null ? List.of(this) : splitParts;
        }

        @Override
        public int getChunkSize() {
            return 1;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
