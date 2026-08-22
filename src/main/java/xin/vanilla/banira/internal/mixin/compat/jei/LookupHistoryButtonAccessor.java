package xin.vanilla.banira.internal.mixin.compat.jei;

import mezz.jei.gui.input.UserInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 调用 JEI 查询历史按钮的原生点击逻辑。 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButton", remap = false)
public interface LookupHistoryButtonAccessor {
    @Invoker("onMouseClicked")
    boolean banira$invokeMouseClicked(UserInput input);
}
