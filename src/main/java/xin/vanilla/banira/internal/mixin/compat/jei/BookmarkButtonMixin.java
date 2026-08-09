package xin.vanilla.banira.internal.mixin.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.bookmarks.BookmarkList;
import mezz.jei.config.IWorldConfig;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;
import mezz.jei.input.click.MouseClickState;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.forge.compat.jei.JeiCompatibility;

/** 捕获 JEI 书签按钮并为代理入口暴露原点击动作。 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkButton", remap = false)
public abstract class BookmarkButtonMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void banira$capture(IDrawable offIcon, IDrawable onIcon,
                                BookmarkOverlay overlay, BookmarkList bookmarks,
                                IWorldConfig worldConfig, CallbackInfo callback) {
        JeiCompatibility.capture(this, offIcon, onIcon, worldConfig);
    }

    @Inject(method = "onMouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$blockNativeClick(Screen screen, double mouseX, double mouseY,
                                         int button, MouseClickState state,
                                         CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldBlockNativeClick(this)) callback.setReturnValue(false);
    }

}
