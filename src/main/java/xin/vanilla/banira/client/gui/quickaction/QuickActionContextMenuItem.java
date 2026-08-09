package xin.vanilla.banira.client.gui.quickaction;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 单条快捷项在托盘上右键时展示的自定义菜单行（与「隐藏此格」并列）。
 */
public class QuickActionContextMenuItem {

    /** 注册方提供的稳定标识；留空时由所属入口按原始索引生成兼容标识。 */
    @Getter
    @Accessors(fluent = true)
    @Nonnull
    private final String id;

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
        this("", label, null, onActivate);
    }

    public QuickActionContextMenuItem(@Nonnull Component label, @Nullable QuickIcon menuIcon, @Nullable Consumer<QuickActionContext> onActivate) {
        this("", label, menuIcon, onActivate);
    }

    public QuickActionContextMenuItem(@Nonnull String id, @Nonnull Component label,
                                      @Nullable Consumer<QuickActionContext> onActivate) {
        this(id, label, null, onActivate);
    }

    public QuickActionContextMenuItem(@Nonnull String id, @Nonnull Component label,
                                      @Nullable QuickIcon menuIcon,
                                      @Nullable Consumer<QuickActionContext> onActivate) {
        this.id = id == null ? "" : id;
        this.label = label;
        this.menuIcon = menuIcon;
        this.onActivate = onActivate;
    }
}
