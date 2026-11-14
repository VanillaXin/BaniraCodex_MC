package xin.vanilla.banira.internal.client.compat.jei;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Checks optional injection contracts without loading JEI classes.
 */
public final class JeiMixinSafety {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String[] BUTTONS = {"BookmarkButtonMixin", "BookmarkButtonAccessor",
            "LookupHistoryButtonMixin", "LookupHistoryButtonAccessor", "GuiIconToggleButtonMixin"};

    private JeiMixinSafety() {
    }

    public static boolean appliesTo(String targetName, String mixinName) {
        try {
            String prefix = mixinName.substring(0, mixinName.lastIndexOf('.') + 1);
            if (read(targetName) == null) return false;
            for (String name : BUTTONS) {
                ClassNode mixin = read(prefix + name);
                if (mixin == null) continue;
                for (String target : targets(mixin)) {
                    ClassNode node = read(target);
                    // Some JEI releases do not have the optional history button.
                    if (node != null && !matches(mixin, node)) return false;
                }
            }
            ClassNode mixin = read(mixinName);
            return mixin != null && matches(mixin, read(targetName));
        } catch (IOException | RuntimeException | LinkageError unavailable) {
            return false;
        }
    }

    static boolean matches(ClassNode mixin, ClassNode target) throws IOException {
        if (target == null) return false;
        for (FieldNode field : mixin.fields) {
            if (annotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW) == null) continue;
            if (!hasField(target, field.name, field.desc, field.access)) return false;
        }
        for (MethodNode hook : mixin.methods) {
            AnnotationNode invoker = annotation(hook.visibleAnnotations, hook.invisibleAnnotations, INVOKER);
            if (invoker != null) {
                String name = (String) value(invoker, "value");
                if (name == null || !hasMethod(target, name, hook.desc, hook.access)) return false;
            }
            if (annotation(hook.visibleAnnotations, hook.invisibleAnnotations, SHADOW) != null
                    && !hasMethod(target, hook.name, hook.desc, hook.access)) return false;
            AnnotationNode inject = annotation(hook.visibleAnnotations, hook.invisibleAnnotations, INJECT);
            if (inject == null) continue;
            for (String selector : strings(value(inject, "method"))) {
                if (selector.startsWith("<init>")) return false;
                boolean found = false;
                for (MethodNode method : target.methods) {
                    if (!selector.equals(method.name) && !selector.equals(method.name + method.desc)) continue;
                    if (!compatible(hook, method)) return false;
                    found = true;
                }
                if (!found) return false;
            }
        }
        return true;
    }

    private static boolean compatible(MethodNode hook, MethodNode target) {
        if (((hook.access ^ target.access) & Opcodes.ACC_STATIC) != 0) return false;
        Type[] captured = Type.getArgumentTypes(hook.desc);
        Type[] actual = Type.getArgumentTypes(target.desc);
        int count = 0;
        while (count < captured.length && !captured[count].getClassName().startsWith(
                "org.spongepowered.asm.mixin.injection.callback.CallbackInfo")) count++;
        if (count == captured.length || count + 1 != captured.length
                || count != 0 && count != actual.length) return false;
        boolean returnsValue = Type.getReturnType(target.desc).getSort() != Type.VOID;
        String callback = returnsValue
                ? "org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable"
                : "org.spongepowered.asm.mixin.injection.callback.CallbackInfo";
        if (!captured[count].getClassName().equals(callback)) return false;
        for (int i = 0; i < count; i++) {
            if (captured[i].equals(actual[i])) continue;
            if (!captured[i].getDescriptor().equals("Ljava/lang/Object;")
                    || actual[i].getSort() != Type.OBJECT || !coerced(hook, i)) return false;
        }
        return true;
    }

    private static boolean coerced(MethodNode method, int index) {
        String coerce = "Lorg/spongepowered/asm/mixin/injection/Coerce;";
        return annotation(method.visibleParameterAnnotations == null ? null : method.visibleParameterAnnotations[index],
                method.invisibleParameterAnnotations == null ? null : method.invisibleParameterAnnotations[index], coerce) != null;
    }

    private static boolean hasField(ClassNode target, String name, String descriptor, int access) throws IOException {
        for (int depth = 0; target != null && depth < 32; depth++, target = read(target.superName)) {
            for (FieldNode field : target.fields) {
                if (field.name.equals(name) && field.desc.equals(descriptor)
                        && ((field.access ^ access) & Opcodes.ACC_STATIC) == 0)
                    return depth == 0 || (field.access & Opcodes.ACC_PRIVATE) == 0;
            }
        }
        return false;
    }

    private static boolean hasMethod(ClassNode target, String name, String descriptor, int access) throws IOException {
        for (int depth = 0; target != null && depth < 32; depth++, target = read(target.superName)) {
            for (MethodNode method : target.methods) {
                if (method.name.equals(name) && method.desc.equals(descriptor)
                        && ((method.access ^ access) & Opcodes.ACC_STATIC) == 0)
                    return depth == 0 || (method.access & Opcodes.ACC_PRIVATE) == 0;
            }
        }
        return false;
    }

    private static List<String> targets(ClassNode node) {
        AnnotationNode mixin = annotation(node.visibleAnnotations, node.invisibleAnnotations, MIXIN);
        return mixin == null ? Collections.emptyList() : strings(value(mixin, "targets"));
    }

    private static List<String> strings(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof String) result.add((String) value);
        else if (value instanceof List<?>) for (Object item : (List<?>) value) result.add((String) item);
        return result;
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) for (int i = 0; i < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static AnnotationNode annotation(List<AnnotationNode> visible, List<AnnotationNode> invisible, String type) {
        if (visible != null) for (AnnotationNode node : visible) if (node.desc.equals(type)) return node;
        if (invisible != null) for (AnnotationNode node : invisible) if (node.desc.equals(type)) return node;
        return null;
    }

    private static ClassNode read(String name) throws IOException {
        if (name == null) return null;
        try (InputStream stream = JeiMixinSafety.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (stream == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
}
