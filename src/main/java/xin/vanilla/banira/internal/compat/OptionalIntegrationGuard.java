package xin.vanilla.banira.internal.compat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Isolates optional callbacks without disabling the target mod.
 */
public final class OptionalIntegrationGuard {
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private OptionalIntegrationGuard() {
    }

    public static void run(String id, Runnable action) {
        call(id, () -> {
            action.run();
            return null;
        }, null);
    }

    public static <T> T call(String id, Supplier<T> action, T fallback) {
        if (FAILED.contains(id)) return fallback;
        try {
            return action.get();
        } catch (RuntimeException | LinkageError failure) {
            if (FAILED.add(id)) {
                org.apache.logging.log4j.LogManager.getLogger().warn(
                        "Optional integration {} disabled; native functionality retained", id, failure);
            }
            return fallback;
        }
    }
}
