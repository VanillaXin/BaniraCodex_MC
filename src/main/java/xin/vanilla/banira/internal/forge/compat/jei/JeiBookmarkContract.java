package xin.vanilla.banira.internal.forge.compat.jei;

import org.objectweb.asm.tree.ClassNode;

public final class JeiBookmarkContract {
    private static boolean buttonApplied;
    private static boolean iconsApplied;

    private JeiBookmarkContract() {
    }

    public static boolean matches(String mixin, ClassNode target) {
        if (mixin.endsWith("BookmarkButtonMixin")) {
            return method(target, "onMouseClicked", "(Lmezz/jei/input/UserInput;)Z")
                    || method(target, "onMouseClicked", "(Lmezz/jei/common/input/UserInput;)Z");
        }
        if (mixin.endsWith("GuiIconToggleButtonMixin")) {
            return method(target, "draw", "(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V")
                    && method(target, "drawTooltips", "(Lcom/mojang/blaze3d/vertex/PoseStack;II)V")
                    && method(target, "isMouseOver", "(DD)Z");
        }
        return false;
    }

    private static boolean method(ClassNode target, String name, String descriptor) {
        return target.methods.stream().anyMatch(value -> value.name.equals(name) && value.desc.equals(descriptor));
    }

    public static synchronized void applied(String mixin) {
        if (mixin.endsWith("BookmarkButtonMixin")) buttonApplied = true;
        if (mixin.endsWith("GuiIconToggleButtonMixin")) iconsApplied = true;
    }

    public static synchronized boolean ready() {
        return buttonApplied && iconsApplied;
    }
}
