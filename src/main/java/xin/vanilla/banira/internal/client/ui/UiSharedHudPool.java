package xin.vanilla.banira.internal.client.ui;

import java.util.Objects;

public final class UiSharedHudPool<D, B> {
    public interface Platform<D, B> {
        D open();
        boolean current(D document);
        B branch(D document);
        void show(D document, B branch);
        void hide(D document, B branch);
        void remove(D document, B branch);
        void close(D document);
    }

    private final Platform<D, B> platform;
    private State active;

    private final class State {
        final D document;
        int owners;
        B selected;
        State(D document) { this.document = Objects.requireNonNull(document, "document"); }
    }

    public UiSharedHudPool(Platform<D, B> platform) { this.platform = Objects.requireNonNull(platform, "platform"); }

    public Lease acquire() {
        if (active == null || !platform.current(active.document)) active = new State(platform.open());
        State state = active;
        try {
            B branch = Objects.requireNonNull(platform.branch(state.document), "branch");
            state.owners++;
            return new Lease(state, branch);
        } catch (RuntimeException | LinkageError failure) {
            if (state.owners == 0) {
                if (active == state) active = null;
                try { platform.close(state.document); }
                catch (RuntimeException | LinkageError cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }

    public final class Lease implements AutoCloseable {
        private final State state;
        private final B branch;
        private boolean closed;
        private Lease(State state, B branch) { this.state = state; this.branch = branch; }
        public D document() { return state.document; }
        public B branch() { return branch; }
        public void show() {
            if (closed || !platform.current(state.document)) throw new IllegalStateException("Shared HUD lease expired");
            if (state.selected == branch) return;
            if (state.selected != null) platform.hide(state.document, state.selected);
            state.selected = null;
            platform.show(state.document, branch);
            state.selected = branch;
        }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (state.selected == branch) state.selected = null;
            Throwable failure = null;
            try { platform.remove(state.document, branch); }
            catch (RuntimeException | LinkageError cleanup) { failure = cleanup; }
            if (--state.owners == 0) {
                if (active == state) active = null;
                try { platform.close(state.document); }
                catch (RuntimeException | LinkageError cleanup) { if (failure == null) failure = cleanup; else failure.addSuppressed(cleanup); }
            }
            if (failure instanceof RuntimeException) throw (RuntimeException) failure;
            if (failure instanceof LinkageError) throw (LinkageError) failure;
        }
    }
}
