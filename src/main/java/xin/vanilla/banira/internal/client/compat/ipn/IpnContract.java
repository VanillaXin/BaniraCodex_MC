package xin.vanilla.banira.internal.client.compat.ipn;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

public final class IpnContract {
    private static final String BRIDGE = "xin/vanilla/banira/internal/client/compat/ipn/IpnCompatibility";
    private static final String[][] HOOKS = {
            {"IpnScreenMixin", "org/anti_ad/mc/ipnext/gui/inject/ContainerScreenEventHandler", "preRender", "()V", "update"},
            {"IpnSortButtonMixin", "org/anti_ad/mc/ipnext/gui/inject/base/SortButtonWidget", "render",
                    "(Lorg/anti_ad/mc/common/gui/NativeContext;IIF)V", "suppress"},
            {"IpnTexturedButtonMixin", "org/anti_ad/mc/ipnext/gui/inject/base/TexturedButtonWidget", "mouseClicked", "(III)Z", "suppress"},
            {"IpnWidgetMixin", "org/anti_ad/mc/common/gui/widgets/Widget", "mouseScrolled", "(IID)Z", "suppress"}
    };
    private static final Set<String> APPLIED = new HashSet<>();

    private IpnContract() {
    }

    public static boolean matches(String mixin, ClassNode target) {
        String[] hook = hook(mixin, target);
        if (hook == null) return false;
        for (MethodNode method : target.methods) {
            if (method.name.equals(hook[2]) && method.desc.equals(hook[3])
                    && (method.access & Opcodes.ACC_PUBLIC) != 0
                    && (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) == 0) return true;
        }
        return false;
    }

    private static String[] hook(String mixin, ClassNode target) {
        for (String[] hook : HOOKS) {
            if (mixin.endsWith("." + hook[0]) && target.name.equals(hook[1])) return hook;
        }
        return null;
    }

    public static synchronized void recordApplied(String mixin, ClassNode target) {
        String[] hook = hook(mixin, target);
        if (hook == null) return;
        APPLIED.remove(hook[0]);
        if (!matches(mixin, target)) return;
        // postApply alone does not prove that an optional injector found its target.
        for (MethodNode method : target.methods) {
            if (!method.name.equals(hook[2]) || !method.desc.equals(hook[3])) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!call.owner.equals(target.name)) continue;
                for (MethodNode handler : target.methods) {
                    if (!handler.name.equals(call.name) || !handler.desc.equals(call.desc)) continue;
                    for (AbstractInsnNode body : handler.instructions) {
                        if (!(body instanceof MethodInsnNode)) continue;
                        MethodInsnNode bridge = (MethodInsnNode) body;
                        if (bridge.getOpcode() == Opcodes.INVOKESTATIC && bridge.owner.equals(BRIDGE)
                                && bridge.name.equals(hook[4])) APPLIED.add(hook[0]);
                    }
                }
            }
        }
    }

    public static synchronized boolean hooksApplied() {
        for (String[] hook : HOOKS) if (!APPLIED.contains(hook[0])) return false;
        return true;
    }
}
