package xin.vanilla.banira.internal.client.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class UiBackendController {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BooleanSupplier preference;
    private final BooleanSupplier capability;
    private final Set<String> owned = new HashSet<>();
    private final Set<String> isolated = new HashSet<>();
    private Boolean requested;
    private long revision;
    private Boolean available;
    private volatile Boolean pending;
    private volatile boolean reloadPending;

    public UiBackendController(BooleanSupplier preference, BooleanSupplier capability) {
        this.preference = Objects.requireNonNull(preference, "preference");
        this.capability = Objects.requireNonNull(capability, "capability");
    }

    public boolean wantsAui(String surface) {
        Objects.requireNonNull(surface, "surface");
        // Construction runs before Forge loads client config; resolve it only on UI demand.
        if (requested == null) requested = preference.getAsBoolean();
        if (!requested || isolated.contains(surface)) return false;
        if (available == null) {
            try {
                available = capability.getAsBoolean();
            } catch (RuntimeException | LinkageError failure) {
                available = false;
                LOGGER.warn("AUI activation failed; native interfaces remain active", failure);
            }
        }
        return available;
    }

    public boolean usesAui(String surface) {
        return owned.contains(surface);
    }

    public void attach(String surface) {
        if (wantsAui(surface)) owned.add(surface);
    }

    public void detach(String surface) {
        owned.remove(surface);
    }

    public void isolate(String surface) {
        owned.remove(surface);
        isolated.add(surface);
    }

    public void requestSwitch(boolean enabled) {
        pending = enabled;
    }

    public void requestReload() {
        reloadPending = true;
    }

    public void tick() {
        Boolean next = pending;
        pending = null;
        if (next != null && !Objects.equals(requested, next)) {
            requested = next;
            reload();
        }
        if (reloadPending) {
            reloadPending = false;
            reload();
        }
    }

    public void reload() {
        ++revision;
        available = null;
        owned.clear();
        isolated.clear();
    }

    public long revision() { return revision; }
}
