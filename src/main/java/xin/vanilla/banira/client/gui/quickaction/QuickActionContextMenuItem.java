package xin.vanilla.banira.client.gui.quickaction;

import lombok.Getter;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 单条快捷项在托盘上右键时展示的自定义菜单行（与「隐藏此格」并列）。
 */
public class QuickActionContextMenuItem {

    @Getter
    @Nonnull
    private final Component label;

    @Getter
    @Nullable
    private final QuickIcon menuIcon;

    @Getter
    @Nullable
    private final Consumer<QuickActionContext> onActivate;

    public QuickActionContextMenuItem(@Nonnull Component label, @Nullable Consumer<QuickActionContext> onActivate) {
        this(label, null, onActivate);
    }

    public QuickActionContextMenuItem(@Nonnull Component label, @Nullable QuickIcon menuIcon, @Nullable Consumer<QuickActionContext> onActivate) {
        this.label = label;
        this.menuIcon = menuIcon;
        this.onActivate = onActivate;
    }
}
