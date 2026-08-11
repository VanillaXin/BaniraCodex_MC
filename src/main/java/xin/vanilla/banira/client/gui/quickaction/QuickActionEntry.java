package xin.vanilla.banira.client.gui.quickaction;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 单条快捷操作定义（由 {@link QuickActionRegistry} 持有）
 */
@Accessors(chain = true, fluent = true)
public class QuickActionEntry {

    @Getter
    @Setter
    @Nonnull
    private String id;

    @Getter
    @Setter
    @Nonnull
    private Component label;

    @Getter
    @Setter
    @Nonnull
    private QuickIcon quickIcon = QuickIcon.item(new ItemStack(Items.PAPER));

    @Getter
    @Setter
    @Nonnull
    private EnumQuickActionDisplay display = EnumQuickActionDisplay.ICON;

    @Getter
    @Setter
    @Nullable
    private Consumer<QuickActionContext> onActivate;

    /** 左键没有独立动作时，打开菜单时跳过的内部菜单项数量。 */
    @Getter
    @Setter
    private int primaryMenuItemOffset;

    /**
     * 在托盘上右键该图标时，与「隐藏此格」一并展示的自定义菜单（顺序即显示顺序），由 {@link QuickActionRegistry} 写入。
     */
    final List<QuickActionContextMenuItem> contextMenuItems = new ArrayList<>();
}
