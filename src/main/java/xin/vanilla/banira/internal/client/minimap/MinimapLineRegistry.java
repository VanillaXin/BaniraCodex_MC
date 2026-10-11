package xin.vanilla.banira.internal.client.minimap;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

public final class MinimapLineRegistry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long LIVE_NANOS = 500_000_000L;
    private static final long LOG_INTERVAL_NANOS = 60_000_000_000L;
    private static final MinimapLineRegistry SHARED = new MinimapLineRegistry();
    private static final Comparator<MinimapLine> ORDER = Comparator.comparingInt(MinimapLine::order)
            .thenComparing(line -> line.id().toString());

    private final Map<ResourceLocation, MinimapLine> lines = new LinkedHashMap<>();
    private final List<Observer> observers = new ArrayList<>();
    private final Map<Object, Long> lastFailure = new HashMap<>();
    private final Map<EnumNotificationHudHost, Long> lastSeen = new EnumMap<>(EnumNotificationHudHost.class);
    private final Map<EnumNotificationHudHost, List<ResourceLocation>> layouts = new EnumMap<>(EnumNotificationHudHost.class);
    private long layoutRevision;

    public static MinimapLineRegistry shared() {
        return SHARED;
    }

    public boolean register(MinimapLine line) {
        Objects.requireNonNull(line, "line");
        List<Observer> listeners;
        synchronized (this) {
            if (lines.containsKey(line.id())) return false;
            lines.put(line.id(), line);
            listeners = new ArrayList<>(observers);
            for (Observer listener : listeners) listener.enqueue(line);
        }
        for (Observer listener : listeners) listener.flush();
        return true;
    }

    public synchronized List<MinimapLine> registrations() {
        List<MinimapLine> result = new ArrayList<>(lines.values());
        result.sort(ORDER);
        return Collections.unmodifiableList(result);
    }

    public void onRegistration(Consumer<MinimapLine> callback) {
        Observer observer = new Observer(Objects.requireNonNull(callback, "callback"));
        synchronized (this) {
            for (MinimapLine line : registrations()) observer.enqueue(line);
            observers.add(observer);
        }
        observer.flush();
    }

    public synchronized void seen(EnumNotificationHudHost host, long nowNanos) {
        if (isMap(host)) lastSeen.put(host, nowNanos);
    }

    public synchronized EnumNotificationHudHost selectedHost(EnumNotificationHudHost preference, long nowNanos) {
        return select(preference, lastSeen, nowNanos);
    }

    public List<ResourceLocation> requestedIds(EnumNotificationHudHost host) {
        if (!isMap(host)) return Collections.emptyList();
        List<ResourceLocation> result = new ArrayList<>();
        for (MinimapLine line : registrations()) {
            try {
                EnumNotificationHudHost preference = line.host();
                if (preference != EnumNotificationHudHost.AUTO && preference != host) continue;
                ITextComponent text = line.text();
                if (text != null && !text.getString().isEmpty()) result.add(line.id());
            } catch (RuntimeException | LinkageError exception) {
                logFailure(line.id(), exception);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<MinimapLineSnapshot> snapshot(EnumNotificationHudHost host, long nowNanos) {
        List<MinimapLine> registered;
        Map<EnumNotificationHudHost, Long> hosts;
        synchronized (this) {
            registered = registrations();
            hosts = new EnumMap<>(lastSeen);
        }
        if (!isMap(host)) return Collections.emptyList();
        List<MinimapLineSnapshot> result = new ArrayList<>();
        for (MinimapLine line : registered) {
            try {
                if (select(line.host(), hosts, nowNanos) != host) continue;
                ITextComponent text = line.text();
                if (text != null && !text.getString().isEmpty()) {
                    result.add(new MinimapLineSnapshot(line.id(), text, line.order()));
                }
            } catch (RuntimeException | LinkageError exception) {
                logFailure(line.id(), exception);
            }
        }
        List<ResourceLocation> ids = new ArrayList<>();
        for (MinimapLineSnapshot row : result) ids.add(row.id());
        synchronized (this) {
            List<ResourceLocation> previous = layouts.get(host);
            if (!ids.equals(previous == null ? Collections.emptyList() : previous)) {
                layouts.put(host, ids);
                layoutRevision++;
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized void resetHosts() {
        lastSeen.clear();
        if (!layouts.isEmpty()) {
            layouts.clear();
            layoutRevision++;
        }
    }

    public synchronized long layoutRevision() {
        return layoutRevision;
    }

    private static EnumNotificationHudHost select(EnumNotificationHudHost preference,
                                                   Map<EnumNotificationHudHost, Long> hosts, long nowNanos) {
        if (preference == EnumNotificationHudHost.AUTO) {
            for (EnumNotificationHudHost host : EnumNotificationHudHost.values()) {
                if (active(host, hosts, nowNanos)) return host;
            }
        } else if (active(preference, hosts, nowNanos)) {
            return preference;
        }
        return EnumNotificationHudHost.STANDALONE;
    }

    private static boolean active(EnumNotificationHudHost host, Map<EnumNotificationHudHost, Long> hosts, long nowNanos) {
        Long seen = hosts.get(host);
        if (!isMap(host) || seen == null) return false;
        long elapsed = nowNanos - seen;
        return elapsed >= 0 && elapsed <= LIVE_NANOS;
    }

    private static boolean isMap(EnumNotificationHudHost host) {
        return host != null && host != EnumNotificationHudHost.AUTO && host != EnumNotificationHudHost.STANDALONE;
    }

    private void logFailure(Object key, Throwable failure) {
        long now = System.nanoTime();
        synchronized (this) {
            Long previous = lastFailure.get(key);
            if (previous != null && now - previous >= 0 && now - previous < LOG_INTERVAL_NANOS) return;
            lastFailure.put(key, now);
        }
        LOGGER.warn("Minimap information line failed: {}", key, failure);
    }

    private final class Observer {
        private final Consumer<MinimapLine> callback;
        private final Queue<MinimapLine> pending = new ArrayDeque<>();
        private boolean flushing;

        private Observer(Consumer<MinimapLine> callback) {
            this.callback = callback;
        }

        private synchronized void enqueue(MinimapLine line) {
            pending.add(line);
        }

        private void flush() {
            synchronized (this) {
                if (flushing) return;
                flushing = true;
            }
            // Registration callbacks may register another line while a replay is draining.
            while (true) {
                MinimapLine line;
                synchronized (this) {
                    line = pending.poll();
                    if (line == null) {
                        flushing = false;
                        return;
                    }
                }
                try {
                    callback.accept(line);
                } catch (RuntimeException | LinkageError exception) {
                    logFailure(this, exception);
                }
            }
        }
    }
}
