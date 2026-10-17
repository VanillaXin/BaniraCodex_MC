package xin.vanilla.banira.api.client.ui;

import xin.vanilla.banira.api.client.BaniraQuickActionScreenFactory;
import java.util.Objects;

public final class UiPageSpec {
    private final String id;
    private final String template;
    private final UiPageSession session;
    private final BaniraQuickActionScreenFactory nativeFactory;

    public UiPageSpec(String id, String template, UiPageSession session,
                      BaniraQuickActionScreenFactory nativeFactory) {
        this.id = validId(id);
        this.template = validTemplate(template);
        this.session = Objects.requireNonNull(session, "session");
        this.nativeFactory = Objects.requireNonNull(nativeFactory, "nativeFactory");
    }

    static String validId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || id.contains("..")) {
            throw new IllegalArgumentException("Invalid UI surface ID");
        }
        return id;
    }

    static String validTemplate(String template) {
        if (template == null || !template.matches("[a-z0-9_-]+/[a-z0-9_./-]+\\.html")
                || template.contains("..") || template.contains("//")) {
            throw new IllegalArgumentException("UI template must be a local mod resource");
        }
        return template;
    }

    public String id() { return id; }
    public String template() { return template; }
    public UiPageSession session() { return session; }
    public BaniraQuickActionScreenFactory nativeFactory() { return nativeFactory; }
}
