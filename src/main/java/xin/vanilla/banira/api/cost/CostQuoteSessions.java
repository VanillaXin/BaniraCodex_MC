package xin.vanilla.banira.api.cost;

import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

public final class CostQuoteSessions {
    private final Thread owner = Thread.currentThread();
    private final Map<UUID, Connection> connections = new HashMap<>();
    private final LongSupplier clock, generation;
    private final Predicate<UUID> connected;

    public CostQuoteSessions(LongSupplier clock, LongSupplier generation, Predicate<UUID> connected) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.connected = Objects.requireNonNull(connected, "connected");
    }

    public void open(UUID player, UUID session) {
        requireOwner();
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(session, "session");
        if (!connected.test(player)) throw new IllegalArgumentException("Quote connection is not live");
        Connection old = connections.get(player);
        if (old == null || !old.session.equals(session)) connections.put(player, new Connection(session, clock.getAsLong()));
    }

    public void close(UUID player) { requireOwner(); connections.remove(player); }
    public void clear() { requireOwner(); connections.clear(); }
    public int connectionCount() { requireOwner(); return connections.size(); }

    public CostFailure accept(UUID player, UUID session, long requestId, long revision) {
        requireOwner();
        Connection connection = connections.get(player);
        if (connection == null || !connection.session.equals(session)) return CostFailure.ACTOR_UNAVAILABLE;
        if (!connected.test(player)) {
            connections.remove(player);
            return CostFailure.ACTOR_UNAVAILABLE;
        }
        if (!connection.consume(clock.getAsLong())) return CostFailure.RATE_LIMITED;
        if (requestId <= connection.lastRequest) return CostFailure.INVALID_OPERATION;
        connection.lastRequest = requestId;
        return revision == generation.getAsLong() ? CostFailure.NONE : CostFailure.CONFIGURATION_UNAVAILABLE;
    }

    private void requireOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Quote service requires its owning server thread");
    }

    private static final class Connection {
        final UUID session;
        long updated, lastRequest;
        double tokens = 8;
        Connection(UUID session, long now) { this.session = session; updated = now; }
        boolean consume(long now) {
            long delta = now - updated;
            if (delta > 0) {
                tokens = Math.min(8, tokens + delta / 250_000_000.0);
                updated = now;
            }
            if (tokens < 1) return false;
            tokens--;
            return true;
        }
    }
}
