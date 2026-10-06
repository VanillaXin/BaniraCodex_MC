package xin.vanilla.banira.internal.mixin.injections;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ConnectionReadOrderMixinTest {
    private NioEventLoopGroup group;
    private Channel channel;
    private final CountDownLatch resume = new CountDownLatch(1);

    @Before
    public void openChannel() throws Exception {
        group = new NioEventLoopGroup(1);
        channel = group.register(new NioSocketChannel()).sync().channel();
    }

    @After
    public void closeChannel() throws Exception {
        resume.countDown();
        if (channel != null) channel.close().sync();
        if (group != null) group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
    }

    @Test(timeout = 10000)
    public void disablingReadWaitsForTheNetworkEventLoop() throws Exception {
        pauseEventLoop();
        assertSame(channel.config(), disableRead());
        assertTrue("Sender must not change autoRead off the network thread", channel.config().isAutoRead());
        resume.countDown();
        drainEventLoop();
        assertFalse(channel.config().isAutoRead());
    }

    @Test(timeout = 10000)
    public void disableCannotOvertakeQueuedProtocolChanges() throws Exception {
        pauseEventLoop();
        List<Boolean> observed = new ArrayList<>();
        channel.eventLoop().execute(() -> observed.add(channel.config().isAutoRead()));
        disableRead();
        channel.eventLoop().execute(() -> {
            observed.add(channel.config().isAutoRead());
            channel.config().setAutoRead(true);
            observed.add(channel.config().isAutoRead());
        });
        resume.countDown();
        drainEventLoop();
        assertEquals(Arrays.asList(true, false, true), observed);
        assertTrue(channel.config().isAutoRead());
    }

    @Test(timeout = 10000)
    public void networkThreadDisablePrecedesInlineProtocolEnable() throws Exception {
        channel.eventLoop().submit(() -> {
            assertSame(channel.config(), disableRead());
            assertFalse("Already on network thread: disable before inline protocol enable", channel.config().isAutoRead());
            channel.config().setAutoRead(true);
            return null;
        }).get(5, TimeUnit.SECONDS);
        drainEventLoop();
        assertTrue("No deferred disable may remain after enabling the new protocol", channel.config().isAutoRead());
    }

    private void pauseEventLoop() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        channel.eventLoop().execute(() -> {
            entered.countDown();
            try {
                if (!resume.await(5, TimeUnit.SECONDS)) throw new AssertionError("Event loop not resumed");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError(error);
            }
        });
        assertTrue("Event loop not started", entered.await(5, TimeUnit.SECONDS));
    }

    private void drainEventLoop() throws Exception {
        channel.eventLoop().submit(() -> {
        }).get(5, TimeUnit.SECONDS);
    }

    private ChannelConfig disableRead() throws Exception {
        ConnectionReadOrderMixin mixin = new ConnectionReadOrderMixin() {
        };
        Field field = ConnectionReadOrderMixin.class.getDeclaredField("channel");
        field.setAccessible(true);
        field.set(mixin, channel);
        Method handler = ConnectionReadOrderMixin.class.getDeclaredMethod(
                "banira$setAutoReadInOrder", ChannelConfig.class, boolean.class);
        handler.setAccessible(true);
        return (ChannelConfig) handler.invoke(mixin, channel.config(), false);
    }
}
