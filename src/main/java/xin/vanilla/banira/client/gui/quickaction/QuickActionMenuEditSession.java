package xin.vanilla.banira.client.gui.quickaction;

import javax.annotation.Nonnull;

/**
 * 记录右键菜单编辑目标。目标使用稳定键保存，列表重排或隐藏后仍能继续操作同一项。
 */
final class QuickActionMenuEditSession {
    private final String targetKey;

    QuickActionMenuEditSession(@Nonnull String targetKey) {
        this.targetKey = targetKey;
    }

    String targetKey() {
        return targetKey;
    }

    boolean isHidden(QuickActionLayout layout) {
        return layout.hiddenMenuItemIds().contains(targetKey);
    }

    boolean move(QuickActionLayout layout, int direction) {
        return layout.moveMenuItem(targetKey, direction);
    }

    /** @return 切换后的隐藏状态 */
    boolean toggleVisibility(QuickActionLayout layout) {
        if (layout.hiddenMenuItemIds().remove(targetKey)) {
            return false;
        }
        layout.hiddenMenuItemIds().add(targetKey);
        return true;
    }
}
