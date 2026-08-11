package xin.vanilla.banira.client.gui.quickaction;

import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** 从一个可选模组读取当前界面可用的背包操作。 */
public interface ExternalInventoryActionProvider {
    @Nonnull
    String sourceId();

    @Nonnull
    List<ExternalInventoryAction> actions(@Nullable Screen screen);
}
