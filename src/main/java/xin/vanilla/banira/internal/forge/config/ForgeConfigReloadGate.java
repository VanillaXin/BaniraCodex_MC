package xin.vanilla.banira.internal.forge.config;

import java.util.function.BooleanSupplier;

/**
 * A registration owns one delivery gate shared by the native and supplemental watchers.
 */
final class ForgeConfigReloadGate implements Runnable, AutoCloseable {
    private final BooleanSupplier changed;
    private final Runnable callback;
    private boolean active = true;

    ForgeConfigReloadGate(BooleanSupplier changed, Runnable callback) {
        this.changed = changed;
        this.callback = callback;
    }

    @Override
    public synchronized void run() {
        if (active && changed.getAsBoolean()) callback.run();
    }

    @Override
    public synchronized void close() {
        active = false;
    }
}
