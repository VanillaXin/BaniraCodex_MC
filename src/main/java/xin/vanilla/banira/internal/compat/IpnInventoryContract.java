package xin.vanilla.banira.internal.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public final class IpnInventoryContract {
    private static final String BASE = "org/anti_ad/mc/ipnext/gui/inject/";
    private static final String RENDER = BASE + "base/SortButtonWidget";
    private static final String CLICK = BASE + "base/ProfileButtonWidget";
    private static final String SETTINGS = BASE + "base/SettingsWidget";
    private static final String EDITOR = BASE + "EditorWidget";
    private static final String HANDLER = BASE + "ContainerScreenEventHandler";
    private static final String WIDGET = "org/anti_ad/mc/common/gui/widgets/Widget";
    private static final String CONTEXT = "org/anti_ad/mc/common/gui/NativeContext";
    private static final String DRAW = "(L" + CONTEXT + ";IIF)V";
    private static final String BRIDGE =
            "xin/vanilla/banira/internal/forge/compat/ipn/InventoryProfilesNextCompatibility";
    private static volatile boolean renderHook;
    private static volatile boolean clickHook;

    private IpnInventoryContract() {
    }

    public static boolean matches() {
        return mismatch() == null;
    }

    public static String mismatch() {
        return NativeContract.MISMATCH;
    }

    public static boolean hooksApplied() {
        return matches() && renderHook && clickHook;
    }

    public static void recordApplied(String targetClassName, ClassNode target) {
        if (!matches()) return;
        if (!targetClassName.replace('.', '/').equals(target.name)) {
            renderHook = false;
            clickHook = false;
            return;
        }
        if (RENDER.equals(target.name)) {
            renderHook = invokesHandler(target, "render", DRAW);
        } else if (CLICK.equals(target.name)) {
            clickHook = invokesHandler(target, "mouseClicked", "(III)Z");
        }
    }

    private static String inspect(ClassLoader loader) {
        try {
            ClassNode render = read(loader, RENDER);
            ClassNode click = read(loader, CLICK);
            ClassNode settings = read(loader, SETTINGS);
            ClassNode editor = read(loader, EDITOR);
            ClassNode widget = read(loader, WIDGET);
            ClassNode handler = read(loader, HANDLER);
            ClassNode context = read(loader, CONTEXT);
            for (ClassNode type : new ClassNode[]{render, click, settings, editor, widget, handler, context}) {
                if ((type.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0) {
                    throw new IllegalArgumentException(type.name + " must be a concrete IPN class");
                }
            }
            requireMethod(render, "render", DRAW);
            requireMethod(click, "mouseClicked", "(III)Z");
            requireMethod(widget, "getParent", "()L" + WIDGET + ";");
            requireMethod(settings, "onClick", "()V");
            requireMethod(handler, "getCurrentWidgets", "()Ljava/util/List;");
            requireMethod(handler, "showEditor", "()V");
            boolean singleton = false;
            for (FieldNode field : handler.fields) {
                if (field.name.equals("INSTANCE") && field.desc.equals("L" + HANDLER + ";")
                        && (field.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC))
                        == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) singleton = true;
            }
            if (!singleton) throw new IllegalArgumentException(HANDLER + ".INSTANCE public static contract missing");
            requireSubtype(loader, click, RENDER);
            requireSubtype(loader, render, WIDGET);
            requireSubtype(loader, settings, WIDGET);
            requireSubtype(loader, editor, WIDGET);
            // Profile buttons use the render hook inherited from SortButtonWidget.
            for (ClassNode current = click; !current.name.equals(RENDER);
                 current = read(loader, current.superName)) {
                for (MethodNode method : current.methods) {
                    if (method.name.equals("render") && method.desc.equals(DRAW)) {
                        throw new IllegalArgumentException(current.name + " overrides the native SortButtonWidget render hook");
                    }
                }
            }
            return null;
        } catch (IOException | RuntimeException exception) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }

    private static ClassNode read(ClassLoader loader, String name) throws IOException {
        try (InputStream input = loader.getResourceAsStream(name + ".class")) {
            if (input == null) throw new IOException("Missing native IPN class " + name);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            if (!name.equals(node.name) || (node.access & Opcodes.ACC_PUBLIC) == 0) {
                throw new IllegalArgumentException(name + " must be a public IPN class");
            }
            return node;
        }
    }

    private static void requireMethod(ClassNode owner, String name, String descriptor) {
        for (MethodNode method : owner.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)
                    && (method.access & Opcodes.ACC_PUBLIC) != 0
                    && (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) == 0) return;
        }
        throw new IllegalArgumentException(owner.name + "." + name + descriptor
                + " public concrete instance contract missing");
    }

    private static void requireSubtype(ClassLoader loader, ClassNode owner, String parent) throws IOException {
        Set<String> visited = new HashSet<>();
        ClassNode current = owner;
        while (visited.add(current.name) && visited.size() <= 16) {
            if (current.name.equals(parent)) return;
            if (current.superName == null || current.superName.equals("java/lang/Object")) break;
            current = read(loader, current.superName);
        }
        throw new IllegalArgumentException(owner.name + " must inherit " + parent + " within 16 native types");
    }

    private static boolean invokesHandler(ClassNode owner, String name, String descriptor) {
        for (MethodNode method : owner.methods) {
            if (!method.name.equals(name) || !method.desc.equals(descriptor)) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!call.owner.equals(owner.name)) continue;
                for (MethodNode handler : owner.methods) {
                    if (handler == method || !handler.name.equals(call.name) || !handler.desc.equals(call.desc))
                        continue;
                    for (AbstractInsnNode body : handler.instructions) {
                        if (body instanceof MethodInsnNode) {
                            MethodInsnNode invoked = (MethodInsnNode) body;
                            if (invoked.getOpcode() == Opcodes.INVOKESTATIC && invoked.owner.equals(BRIDGE)
                                    && invoked.name.equals("shouldSuppressProfileButton")
                                    && invoked.desc.equals("(Ljava/lang/Object;)Z")) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static final class NativeContract {
        private static final String MISMATCH = inspect(IpnInventoryContract.class.getClassLoader());
    }
}
