package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiPageSession;
import xin.vanilla.banira.api.client.ui.UiPageSource;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class UiNativeSessions {
    private final List<WeakReference<UiPageSession>> sessions = new ArrayList<>();

    public void remember(UiPageSource page) {
        UiPageSession session = page.pageSession();
        sessions.removeIf(reference -> reference.get() == null);
        if (session == null) return;
        for (WeakReference<UiPageSession> reference : sessions) if (reference.get() == session) return;
        sessions.add(new WeakReference<>(session));
    }

    public void forget(UiPageSource page, boolean transferred) {
        if (!transferred) return;
        UiPageSession session = page.pageSession();
        sessions.removeIf(reference -> reference.get() == null || reference.get() == session);
    }

    public void disconnect() {
        List<UiPageSession> remaining = new ArrayList<>();
        for (WeakReference<UiPageSession> reference : sessions) {
            UiPageSession session = reference.get();
            if (session != null) remaining.add(session);
        }
        sessions.clear();
        IllegalStateException failure = null;
        for (UiPageSession session : remaining) {
            try {
                session.close(UiCloseReason.DISCONNECT);
            } catch (RuntimeException exception) {
                if (failure == null) failure = new IllegalStateException("Native UI disconnect failed");
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }
}
