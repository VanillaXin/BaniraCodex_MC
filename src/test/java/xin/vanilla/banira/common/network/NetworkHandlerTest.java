package xin.vanilla.banira.common.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.junit.Test;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.platform.BaniraNetworkService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class NetworkHandlerTest {

    @Test
    public void packetIdsAreScopedPerChannel() {
        RecordingNetworkService service = new RecordingNetworkService();
        BaniraPlatforms.install(new TestBaniraPlatform().networkService(service));

        NetworkHandler alpha = NetworkHandler.create("alpha", new TestIdentifier("network_test"));
        NetworkHandler beta = NetworkHandler.create("beta", new TestIdentifier("network_test"));

        alpha.register(FirstPacket.class, noopEncoder(), FirstPacket::new, noopHandler());
        alpha.register(SecondPacket.class, noopEncoder(), SecondPacket::new, noopHandler());
        beta.register(ThirdPacket.class, noopEncoder(), ThirdPacket::new, noopHandler());
        alpha.register(ThirdPacket.class, noopEncoder(), ThirdPacket::new, noopHandler());

        assertEquals(List.of(0, 1, 2), service.packetIds("alpha"));
        assertEquals(List.of(0), service.packetIds("beta"));
        assertEquals(List.of(FirstPacket.class, SecondPacket.class, ThirdPacket.class), service.packetClasses("alpha"));
    }

    @Test
    public void splitRegistrationConsumesOnePacketId() {
        RecordingNetworkService service = new RecordingNetworkService();
        BaniraPlatforms.install(new TestBaniraPlatform().networkService(service));

        NetworkHandler handler = NetworkHandler.create("split", new TestIdentifier("network_test"));

        handler.register(FirstPacket.class, noopEncoder(), FirstPacket::new, noopHandler());
        handler.registerSplit(TestSplitPacket.class, noopEncoder(), TestSplitPacket::new, noopHandler());
        handler.register(SecondPacket.class, noopEncoder(), SecondPacket::new, noopHandler());

        assertEquals(List.of(0, 1, 2), service.packetIds("split"));
        assertEquals(List.of(FirstPacket.class, TestSplitPacket.class, SecondPacket.class), service.packetClasses("split"));
    }

    private static <MSG extends INetworkPacket> BiConsumer<MSG, BaniraPacketBuffer> noopEncoder() {
        return (packet, buffer) -> {
        };
    }

    private static <MSG extends INetworkPacket> BiConsumer<MSG, BaniraNetworkContext> noopHandler() {
        return (packet, context) -> {
        };
    }

    private record Registration(int packetId, Class<?> packetClass) {
    }

    private static final class RecordingNetworkService implements BaniraNetworkService {
        private final Map<String, List<Registration>> registrations = new LinkedHashMap<>();

        @Override
        public @Nonnull NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull IIdentifier identifier) {
            registrations.computeIfAbsent(channelName, ignored -> new ArrayList<>());
            return new RecordingRegistrar(registrations.get(channelName));
        }

        List<Integer> packetIds(String channelName) {
            return registrations.getOrDefault(channelName, List.of()).stream()
                    .map(Registration::packetId)
                    .collect(Collectors.toList());
        }

        List<Class<?>> packetClasses(String channelName) {
            return registrations.getOrDefault(channelName, List.of()).stream()
                    .map(Registration::packetClass)
                    .collect(Collectors.toList());
        }

        @Override
        public void sendToServer(@Nonnull INetworkPacket packet) {
        }

        @Override
        public void sendToPlayer(@Nonnull INetworkPacket packet, @Nonnull ServerPlayer player) {
        }

        @Override
        public boolean hasDefaultChannel() {
            return false;
        }

        @Override
        public boolean hasLocalChannel(@Nonnull ResourceLocation channel) {
            return false;
        }

        @Override
        public boolean hasPlayerChannel(@Nonnull ServerPlayer player, @Nonnull ResourceLocation channel) {
            return false;
        }
    }

    private record RecordingRegistrar(List<Registration> registrations) implements NetworkPacketRegistrar {
        @Override
        public <MSG extends INetworkPacket> void register(int packetId,
                                                          Class<MSG> packetClass,
                                                          BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                                          Function<BaniraPacketBuffer, MSG> decoder,
                                                          BiConsumer<MSG, BaniraNetworkContext> handler) {
            registrations.add(new Registration(packetId, packetClass));
        }
    }

    private record TestIdentifier(String modId) implements IIdentifier {
        @Override
        public IIdentifier instance() {
            return this;
        }
    }

    private static final class FirstPacket implements INetworkPacket {
        private FirstPacket(BaniraPacketBuffer ignored) {
        }
    }

    private static final class SecondPacket implements INetworkPacket {
        private SecondPacket(BaniraPacketBuffer ignored) {
        }
    }

    private static final class ThirdPacket implements INetworkPacket {
        private ThirdPacket(BaniraPacketBuffer ignored) {
        }
    }

    private static final class TestSplitPacket extends SplitPacket implements INetworkPacket {
        private TestSplitPacket(BaniraPacketBuffer ignored) {
        }

        @Override
        public int getChunkSize() {
            return 1;
        }
    }
}
