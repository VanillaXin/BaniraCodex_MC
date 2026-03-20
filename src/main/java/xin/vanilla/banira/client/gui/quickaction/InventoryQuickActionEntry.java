package xin.vanilla.banira.client.gui.quickaction;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 单条快捷操作定义（由 {@link InventoryQuickActionRegistry} 持有）
 */
@Accessors(chain = true, fluent = true)
public class InventoryQuickActionEntry {

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
    private InventoryQuickIcon quickIcon = InventoryQuickIcon.item(new ItemStack(Items.PAPER));

    @Getter
    @Setter
    @Nonnull
    private EnumInventoryQuickActionDisplay display = EnumInventoryQuickActionDisplay.ICON;

    @Getter
    @Setter
    @Nullable
    private Consumer<InventoryQuickActionContext> onActivate;
}
