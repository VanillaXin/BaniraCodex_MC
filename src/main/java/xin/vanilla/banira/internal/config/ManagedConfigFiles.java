package xin.vanilla.banira.internal.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 在游戏线程定期检查 Banira 自管配置，避免后台线程直接修改运行时状态。 */
public final class ManagedConfigFiles {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long POLL_INTERVAL_NANOS = 500_000_000L;
    private static final Map<Path, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static volatile long lastCommonPoll;
    private static volatile long lastClientPoll;

    private ManagedConfigFiles() {
    }

    public static void register(Path path, Scope scope, Runnable reload) {
        Path key = normalize(path);
        ENTRIES.compute(key, (ignored, current) -> new Entry(
                key, scope, reload, current == null ? fingerprint(key) : current.fingerprint));
    }

    /** 自身写盘后更新基线，防止把保存动作误判为外部修改。 */
    public static void markWritten(Path path) {
        Entry entry = ENTRIES.get(normalize(path));
        if (entry != null) {
            entry.fingerprint = fingerprint(entry.path);
        }
    }

    public static void poll(Scope scope) {
        long now = System.nanoTime();
        if (!shouldPoll(scope, now)) return;
        for (Entry entry : ENTRIES.values()) {
            if (entry.scope != scope) continue;
            String current = fingerprint(entry.path);
            if (current.equals(entry.fingerprint)) continue;
            entry.fingerprint = current;
            try {
                entry.reload.run();
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to hot reload managed config {}", entry.path, exception);
            }
        }
    }

    static void pollNowForTests(Scope scope) {
        if (scope == Scope.CLIENT) lastClientPoll = 0;
        else lastCommonPoll = 0;
        poll(scope);
    }

    static void clearForTests() {
        ENTRIES.clear();
        lastClientPoll = 0;
        lastCommonPoll = 0;
    }

    private static synchronized boolean shouldPoll(Scope scope, long now) {
        long last = scope == Scope.CLIENT ? lastClientPoll : lastCommonPoll;
        if (last != 0 && now - last < POLL_INTERVAL_NANOS) return false;
        if (scope == Scope.CLIENT) lastClientPoll = now;
        else lastCommonPoll = now;
        return true;
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static String fingerprint(Path path) {
        try {
            if (!Files.isRegularFile(path)) return "missing";
            byte[] bytes = Files.readAllBytes(path);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value & 0xFF));
            return result.toString();
        } catch (Exception exception) {
            return "unreadable:" + exception.getClass().getName();
        }
    }

    public enum Scope {
        COMMON,
        CLIENT
    }

    private static final class Entry {
        private final Path path;
        private final Scope scope;
        private final Runnable reload;
        private volatile String fingerprint;

        private Entry(Path path, Scope scope, Runnable reload, String fingerprint) {
            this.path = path;
            this.scope = scope;
            this.reload = reload;
            this.fingerprint = fingerprint;
        }
    }
}
