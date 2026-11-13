package xin.vanilla.banira.internal.fabric.compat.jei;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;

public final class JeiButtonContract {
    private static final Set<String> APPLIED = new HashSet<>();

    private JeiButtonContract() {
    }

    public static boolean matches(String mixin, ClassNode target) {
        if (mixin.endsWith("BookmarkButtonAccessor")) return method(target, "isIconToggledOn", "()Z");
        if (mixin.endsWith("BookmarkButtonMixin") || mixin.endsWith("LookupHistoryButtonMixin"))
            return method(target, "onMouseClicked", "(Lmezz/jei/common/input/UserInput;)Z");
        if (mixin.endsWith("GuiIconToggleButtonMixin"))
            return method(target, "draw", "(" + stackDescriptor() + "IIF)V")
                    && method(target, "drawTooltips", "(" + stackDescriptor() + "II)V")
                    && method(target, "isMouseOver", "(DD)Z");
        return false;
    }

    private static String stackDescriptor() {
        return "L" + net.fabricmc.loader.api.FabricLoader.getInstance().getMappingResolver()
                .mapClassName("intermediary", "net.minecraft.class_4587").replace('.', '/') + ";";
    }

    private static boolean method(ClassNode target, String name, String descriptor) {
        return target.methods.stream().anyMatch(value -> value.name.equals(name) && value.desc.equals(descriptor));
    }

    public static synchronized void applied(String mixin) {
        APPLIED.add(mixin.substring(mixin.lastIndexOf('.') + 1));
    }

    public static synchronized boolean ready(boolean history) {
        return APPLIED.contains("GuiIconToggleButtonMixin") && (history
                ? APPLIED.contains("LookupHistoryButtonMixin")
                : APPLIED.contains("BookmarkButtonMixin") && APPLIED.contains("BookmarkButtonAccessor"));
    }
}
