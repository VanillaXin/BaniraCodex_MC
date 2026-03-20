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
}
