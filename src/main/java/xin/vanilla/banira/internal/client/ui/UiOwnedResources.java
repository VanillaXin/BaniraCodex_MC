package xin.vanilla.banira.internal.client.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class UiOwnedResources implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    private boolean closed;

    public AutoCloseable own(AutoCloseable resource) {
        Objects.requireNonNull(resource, "resource");
        if (closed) {
            try { resource.close(); }
            catch (Exception failure) { throw new IllegalStateException(failure); }
            throw new IllegalStateException("UI resources already closed");
        }
        AutoCloseable lease = new AutoCloseable() {
            private AutoCloseable current = resource;
            @Override public void close() throws Exception {
                AutoCloseable closing = current;
                if (closing == null) return;
                current = null;
                resources.remove(this);
                closing.close();
            }
        };
        resources.push(lease);
        return lease;
    }

    public boolean closed() { return closed; }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        IllegalStateException failure = null;
        while (!resources.isEmpty()) {
            try { resources.pop().close(); }
            catch (Exception | LinkageError error) {
                if (failure == null) failure = new IllegalStateException("Unable to release UI resources", error);
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }
}
