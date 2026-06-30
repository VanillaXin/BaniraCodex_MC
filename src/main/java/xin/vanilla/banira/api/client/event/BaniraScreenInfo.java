package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 加载器无关的屏幕描述信息，避免公共事件直接暴露 Minecraft Screen。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraScreenInfo {
    private static final BaniraScreenInfo CLOSED = new BaniraScreenInfo(null, "", 0, 0, false);

    private final @Nullable String className;
    private final @Nonnull String title;
    private final int width;
    private final int height;
    private final boolean open;

    public BaniraScreenInfo(@Nullable String className, @Nullable String title, int width, int height, boolean open) {
        this.className = className;
        this.title = title == null ? "" : title;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.open = open;
    }

    public static BaniraScreenInfo closed() {
        return CLOSED;
    }

    public boolean matchesClassName(@Nonnull String expectedClassName) {
        return expectedClassName.equals(className);
    }
}
