package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.internal.compat.aui.AuiApiBindings;
import xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;

public final class AuiUiBackend {
    private final Supplier<UiSessionHost> host;
    private Method factory;
    private Method hudFactory;
    private AuiApiBindings bindings;

    public AuiUiBackend(Supplier<UiSessionHost> host) { this.host = Objects.requireNonNull(host, "host"); }

    public NativeUiView create(UiPageSpec page, Object previousScreen) {
        try {
            if (factory == null) {
                ClassLoader loader = getClass().getClassLoader();
                bindings = AuiApiBindings.bind(loader, AuiCapabilityContract.inspect(loader));
                factory = Class.forName("xin.vanilla.banira.internal.client.ui.AuiPageHost", false, loader)
                        .getMethod("create", UiPageSpec.class, Object.class, UiSessionHost.class);
            }
            return (NativeUiView) factory.invoke(null, page, previousScreen, host.get());
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error) throw (Error) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new IllegalStateException("AUI page factory failed", cause);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("AUI page factory unavailable", failure);
        }
    }

    public NativeUiView createHud(xin.vanilla.banira.api.client.ui.UiHudSpec spec) {
        try {
            if (hudFactory == null) {
                ClassLoader loader = getClass().getClassLoader();
                bindings = AuiApiBindings.bind(loader, AuiCapabilityContract.inspect(loader));
                hudFactory = Class.forName("xin.vanilla.banira.internal.client.ui.AuiHudDocument", false, loader)
                        .getMethod("create", xin.vanilla.banira.api.client.ui.UiHudSpec.class);
            }
            return (NativeUiView) hudFactory.invoke(null, spec);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error) throw (Error) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new IllegalStateException("AUI HUD factory failed", cause);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("AUI HUD factory unavailable", failure); }
    }

    public void reload() { factory = null; hudFactory = null; bindings = null; }
}
