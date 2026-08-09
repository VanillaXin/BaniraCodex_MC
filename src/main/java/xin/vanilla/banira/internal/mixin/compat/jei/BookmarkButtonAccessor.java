package xin.vanilla.banira.internal.mixin.compat.jei;

import mezz.jei.common.input.UserInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 供 JEI 代理入口调用原书签按钮动作的可选 Mixin 接口。 */
@Pseudo
@Mixin(targets = "mezz.jei.common.gui.overlay.bookmarks.BookmarkButton", remap = false)
public interface BookmarkButtonAccessor {
    @Invoker("onMouseClicked")
    boolean banira$invokeMouseClicked(UserInput input);
}
