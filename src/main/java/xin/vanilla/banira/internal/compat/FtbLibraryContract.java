package xin.vanilla.banira.internal.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public final class FtbLibraryContract {
    private static final String SIDEBAR = "dev/ftb/mods/ftblibrary/sidebar/";
    private static final String BUTTON = SIDEBAR + "SidebarButton";
    private static final String GROUP = SIDEBAR + "SidebarButtonGroup";
    private static final String MANAGER = SIDEBAR + "SidebarButtonManager";
    private static final String WIDGET = SIDEBAR + "SidebarGroupGuiButton";
    private static final String ICON = "dev/ftb/mods/ftblibrary/icon/Icon";
    private static final String BRIDGE =
            "xin/vanilla/banira/internal/fabric/compat/ftblibrary/FtbLibraryCompatibility";
    private static final String[] RENDER_NAMES = {"render", "method_25394"};
    private static final String[] PRESS_NAMES = {"onPress", "method_25306"};
    private static final int PUBLIC_INSTANCE = Opcodes.ACC_PUBLIC;
    private static final int NON_INSTANCE = Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT;
    private static volatile boolean buttonHooks;
    private static volatile boolean widgetHooks;

    private static final Namespace[] NAMESPACES = {
            new Namespace("Lnet/minecraft/class_2960;", "Lnet/minecraft/class_4587;", "Lnet/minecraft/class_768;"),
            new Namespace("Lnet/minecraft/resources/ResourceLocation;", "Lcom/mojang/blaze3d/vertex/PoseStack;", "Lnet/minecraft/client/renderer/Rect2i;")
    };

    private FtbLibraryContract() {
    }

    public static boolean matches() {
        return mismatch() == null;
    }

    public static boolean appliesTo(String target, String mixin) {
        String owner = target.replace('.', '/');
        return ((mixin.endsWith(".compat.ftblibrary.SidebarButtonMixin") && owner.equals(BUTTON))
                || (mixin.endsWith(".compat.ftblibrary.SidebarGroupGuiButtonMixin") && owner.equals(WIDGET)))
                && matches();
    }

    public static String mismatch() {
        return NativeContract.MISMATCH;
    }

    public static java.lang.reflect.Field bindIcon(Class<?> buttonType, Class<?> iconType)
            throws ReflectiveOperationException {
        java.lang.reflect.Field field = buttonType.getDeclaredField("icon");
        int access = field.getModifiers();
        if (field.getType() != iconType || java.lang.reflect.Modifier.isStatic(access)
                || java.lang.reflect.Modifier.isFinal(access)) {
            throw new NoSuchFieldException(buttonType.getName() + ".icon must be a mutable instance "
                    + iconType.getName() + " field");
        }
        field.setAccessible(true);
        return field;
    }

    public static boolean hooksApplied() {
        return matches() && buttonHooks && widgetHooks;
    }

    public static void recordApplied(String targetClassName, ClassNode target) {
        if (!matches()) return;
        if (!targetClassName.replace('.', '/').equals(target.name)) {
            buttonHooks = false;
            widgetHooks = false;
            return;
        }
        if (BUTTON.equals(target.name)) {
            buttonHooks = invokesHandler(target, new String[]{"getLangKey"}, "()Ljava/lang/String;",
                    "hostedButtonTranslationKey")
                    && invokesHandler(target, new String[]{"onClicked"}, "(Z)V", "activateHostedButton");
        } else if (WIDGET.equals(target.name)) {
            widgetHooks = false;
            for (Namespace namespace : NAMESPACES) {
                widgetHooks |= invokesHandler(target, RENDER_NAMES, "(" + namespace.stack + "IIF)V",
                        "shouldSuppressNativeGroup", "clearReservedArea")
                        && invokesHandler(target, PRESS_NAMES, "()V", "shouldSuppressNativeGroup");
            }
        }
    }

    private static String inspect(ClassLoader loader) {
        try {
            ClassNode button = read(loader, BUTTON);
            ClassNode group = read(loader, GROUP);
            ClassNode manager = read(loader, MANAGER);
            ClassNode widget = read(loader, WIDGET);
            ClassNode icon = read(loader, ICON);
            String mismatch = "";
            for (Namespace namespace : NAMESPACES) {
                try {
                    validate(loader, button, group, manager, widget, icon, namespace);
                    return null;
                } catch (IOException | RuntimeException exception) {
                    mismatch += namespace.stack + ": " + exception.getMessage() + "; ";
                }
            }
            return mismatch;
        } catch (IOException | RuntimeException exception) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }

    private static void validate(ClassLoader loader, ClassNode button, ClassNode group,
                                 ClassNode manager, ClassNode widget, ClassNode icon, Namespace namespace)
            throws IOException {
        requireConcrete(button);
        requireConcrete(group);
        requireConcrete(widget);
        requireMethod(button, "<init>", "(" + namespace.location + "L" + GROUP
                + ";Lcom/google/gson/JsonObject;)V");
        requireMethod(button, "getLangKey", "()Ljava/lang/String;");
        requireMethod(button, "onClicked", "(Z)V");
        requireMethod(button, "getIcon", "()L" + ICON + ";");
        requireMethod(button, "isActuallyVisible", "()Z");
        requireMethod(button, "setTooltipHandler", "(Ljava/util/function/Consumer;)V");
        requireField(button, "id", namespace.location, Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC);
        requireField(button, "icon", "L" + ICON + ";", 0,
                Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
        requireMethod(group, "<init>", "(" + namespace.location + "I)V");
        requireMethod(group, "getId", "()" + namespace.location);
        requireMethod(group, "getButtons", "()Ljava/util/List;");
        requireMethod(group, "compareTo", "(Ljava/lang/Object;)I");
        if (!group.interfaces.contains("java/lang/Comparable")) {
            throw new IllegalArgumentException(GROUP + " must implement java/lang/Comparable");
        }
        requireField(manager, "INSTANCE", "L" + MANAGER + ";",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, 0);
        requireField(manager, "groups", "Ljava/util/List;", Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC);
        requireField(widget, "lastDrawnArea", namespace.area,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Opcodes.ACC_FINAL);
        requireOneMethod(widget, RENDER_NAMES, "(" + namespace.stack + "IIF)V");
        requireOneMethod(widget, PRESS_NAMES, "()V");
        if ((icon.access & (Opcodes.ACC_FINAL | Opcodes.ACC_INTERFACE)) != 0) {
            throw new IllegalArgumentException(ICON + " must remain extensible");
        }
        requireMethod(icon, "<init>", "()V");
        if (!hasDraw(loader, icon, namespace.stack, new HashSet<>())) {
            throw new IllegalArgumentException(ICON + ".draw(" + namespace.stack + "IIII)V public instance contract missing");
        }
    }

    private static ClassNode read(ClassLoader loader, String name) throws IOException {
        try (InputStream input = loader.getResourceAsStream(name + ".class")) {
            if (input == null) throw new IOException("Missing native class " + name);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            if (!name.equals(node.name) || (node.access & Opcodes.ACC_PUBLIC) == 0) {
                throw new IllegalArgumentException("Unexpected or inaccessible native class " + name);
            }
            return node;
        }
    }

    private static void requireMethod(ClassNode owner, String name, String descriptor) {
        requireOneMethod(owner, new String[]{name}, descriptor);
    }

    private static void requireConcrete(ClassNode owner) {
        if ((owner.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) != 0) {
            throw new IllegalArgumentException(owner.name + " must remain a concrete class");
        }
    }

    private static void requireOneMethod(ClassNode owner, String[] names, String descriptor) {
        int count = 0;
        for (MethodNode method : owner.methods) {
            if (named(method, names) && method.desc.equals(descriptor)
                    && (method.access & PUBLIC_INSTANCE) == PUBLIC_INSTANCE
                    && (method.access & NON_INSTANCE) == 0) count++;
        }
        if (count != 1) {
            throw new IllegalArgumentException(owner.name + "." + String.join("|", names) + descriptor
                    + " expected one public concrete instance method, found " + count);
        }
    }

    private static void requireField(ClassNode owner, String name, String descriptor,
                                     int required, int forbidden) {
        String actual = "absent";
        for (FieldNode field : owner.fields) {
            if (field.name.equals(name)) actual = field.desc + " access=" + field.access;
            if (field.name.equals(name) && field.desc.equals(descriptor)
                    && (field.access & required) == required && (field.access & forbidden) == 0) return;
        }
        throw new IllegalArgumentException(owner.name + "." + name + ":" + descriptor
                + " field contract missing (required=" + required + ", forbidden=" + forbidden
                + ", actual=" + actual + ")");
    }

    private static boolean hasDraw(ClassLoader loader, ClassNode owner, String stack, Set<String> visited) throws IOException {
        if (!visited.add(owner.name)) return false;
        if (visited.size() > 16) throw new IllegalArgumentException("Icon draw hierarchy exceeds 16 native types");
        for (MethodNode method : owner.methods) {
            if (method.name.equals("draw") && method.desc.equals("(" + stack + "IIII)V")) {
                return (method.access & Opcodes.ACC_PUBLIC) != 0
                        && (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == 0;
            }
        }
        for (String parent : owner.interfaces) {
            if (hasDraw(loader, read(loader, parent), stack, visited)) return true;
        }
        return owner.superName != null && !owner.superName.equals("java/lang/Object")
                && hasDraw(loader, read(loader, owner.superName), stack, visited);
    }

    private static boolean named(MethodNode method, String[] names) {
        for (String name : names) if (method.name.equals(name)) return true;
        return false;
    }

    private static boolean invokesHandler(ClassNode owner, String[] names, String descriptor,
                                          String... callbacks) {
        for (MethodNode method : owner.methods) {
            if (!named(method, names) || !method.desc.equals(descriptor)) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!call.owner.equals(owner.name)) continue;
                for (MethodNode handler : owner.methods) {
                    if (handler == method || !handler.name.equals(call.name) || !handler.desc.equals(call.desc))
                        continue;
                    Set<String> found = new HashSet<>();
                    for (AbstractInsnNode body : handler.instructions) {
                        if (body instanceof MethodInsnNode) {
                            MethodInsnNode invoked = (MethodInsnNode) body;
                            if (invoked.getOpcode() == Opcodes.INVOKESTATIC && invoked.owner.equals(BRIDGE)) {
                                found.add(invoked.name);
                            }
                        }
                    }
                    boolean complete = true;
                    for (String callback : callbacks) complete &= found.contains(callback);
                    if (complete) return true;
                }
            }
        }
        return false;
    }


    private static final class Namespace {
        private final String location;
        private final String stack;
        private final String area;

        private Namespace(String location, String stack, String area) {
            this.location = location;
            this.stack = stack;
            this.area = area;
        }
    }

    private static final class NativeContract {
        private static final String MISMATCH = inspect(FtbLibraryContract.class.getClassLoader());
    }
}
