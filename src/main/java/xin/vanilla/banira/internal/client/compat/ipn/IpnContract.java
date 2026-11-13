package xin.vanilla.banira.internal.client.compat.ipn;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;

public final class IpnContract {
    private static final Set<String> APPLIED = new HashSet<>();

    private IpnContract() {
    }

    public static boolean matches(String mixin, ClassNode target) {
        if (mixin.endsWith("IpnScreenMixin")) return method(target, "preRender", "()V");
        if (mixin.endsWith("IpnSortButtonMixin"))
            return method(target, "render", "(Lorg/anti_ad/mc/common/gui/NativeContext;IIF)V");
        if (mixin.endsWith("IpnTexturedButtonMixin"))
            return method(target, "mouseClicked", "(III)Z")
                    && method(target, "mouseScrolled", "(IIDD)Z");
        return false;
    }

    private static boolean method(ClassNode target, String name, String descriptor) {
        return target.methods.stream().anyMatch(value -> value.name.equals(name) && value.desc.equals(descriptor));
    }

    public static synchronized void applied(String mixin) {
        APPLIED.add(mixin.substring(mixin.lastIndexOf('.') + 1));
    }

    public static synchronized boolean hooksApplied() {
        return APPLIED.containsAll(Set.of("IpnScreenMixin", "IpnSortButtonMixin", "IpnTexturedButtonMixin"));
    }
}
