package xin.vanilla.banira.common.network.packet;

import org.junit.Test;
import xin.vanilla.banira.common.network.BaniraNetworkContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigSnapshotConnectionTest {
    private static final class Context implements BaniraNetworkContext {
        Object origin;
        boolean client = true;
        int handled;
        final List<Runnable> queued = new ArrayList<>();

        public Object connectionKey() {
            return origin;
        }

        public void enqueueWork(Runnable work) {
            queued.add(work);
        }

        public void markHandled() {
            handled++;
        }

        public boolean isClientSide() {
            return client;
        }

        public boolean isServerSide() {
            return !client;
        }

        public Object sender() {
            return null;
        }

        void drain() {
            new ArrayList<>(queued).forEach(Runnable::run);
            queued.clear();
        }
    }

    private void receive(Context context, AtomicReference<Object> current, List<ConfigSnapshotToClient> applied) throws Exception {
        Method dispatch;
        try {
            dispatch = ConfigSnapshotToClient.class.getDeclaredMethod("handle", ConfigSnapshotToClient.class,
                    BaniraNetworkContext.class, Predicate.class, Consumer.class);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Connection-fenced snapshot dispatch unavailable", missing);
        }
        dispatch.setAccessible(true);
        dispatch.invoke(null, new ConfigSnapshotToClient("shared-common.toml", Map.of("value", "7")), context,
                (Predicate<Object>) key -> key == current.get(), (Consumer<ConfigSnapshotToClient>) applied::add);
    }

    @Test
    public void currentConnectionAppliesOnceAfterMainThreadDispatch() throws Exception {
        Context context = new Context();
        context.origin = new Object();
        List<ConfigSnapshotToClient> applied = new ArrayList<>();
        receive(context, new AtomicReference<>(context.origin), applied);
        assertEquals(1, context.handled);
        assertTrue(applied.isEmpty());
        context.drain();
        context.drain();
        assertEquals(1, applied.size());
    }

    @Test
    public void queuedOldReplyCannotEnterNewConnectionWithTheSameConfigurationName() throws Exception {
        Context context = new Context();
        context.origin = new Object();
        AtomicReference<Object> current = new AtomicReference<>(context.origin);
        List<ConfigSnapshotToClient> applied = new ArrayList<>();
        receive(context, current, applied);
        current.set(new Object());
        context.origin = current.get();
        context.drain();
        assertTrue(applied.isEmpty());
        assertEquals(1, context.handled);
    }

    @Test
    public void disconnectBeforeDispatchDropsTheReply() throws Exception {
        Context context = new Context();
        context.origin = new Object();
        AtomicReference<Object> current = new AtomicReference<>(context.origin);
        List<ConfigSnapshotToClient> applied = new ArrayList<>();
        receive(context, current, applied);
        current.set(null);
        context.drain();
        assertTrue(applied.isEmpty());
    }

    @Test
    public void unavailableTransportIdentityAndWrongSideFailClosed() throws Exception {
        Context context = new Context();
        List<ConfigSnapshotToClient> applied = new ArrayList<>();
        receive(context, new AtomicReference<>(), applied);
        context.drain();
        assertTrue(applied.isEmpty());
        context.origin = new Object();
        context.client = false;
        receive(context, new AtomicReference<>(context.origin), applied);
        context.drain();
        assertTrue(applied.isEmpty());
        assertEquals(2, context.handled);
    }
}
