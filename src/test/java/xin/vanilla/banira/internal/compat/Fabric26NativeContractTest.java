package xin.vanilla.banira.internal.compat;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class Fabric26NativeContractTest {
    private ClassNode node(String name) throws Exception {
        var type = new ClassNode();
        try (var input = getClass().getResourceAsStream("/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(type, 0);
        }
        return type;
    }

    @Test
    public void classTweakerTargetsExistInThePinnedNativeClient() throws Exception {
        List<String> lines;
        try (var input = getClass().getResourceAsStream("/banira_codex.classtweaker")) {
            assertNotNull(input);
            lines = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
        assertEquals("classTweaker v1 official", lines.getFirst());
        int checked = 0;
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            assertEquals("accessible", parts[0]);
            var target = node(parts[2]);
            if (parts[1].equals("field")) {
                assertEquals(5, parts.length);
                assertTrue(line, target.fields.stream().anyMatch(field -> field.name.equals(parts[3]) && field.desc.equals(parts[4])));
            } else {
                assertEquals("class", parts[1]);
                assertEquals(3, parts.length);
            }
            checked++;
        }
        assertEquals("Keep the native extraction access surface narrow", 3, checked);
    }

    @Test
    public void nativeMixinSelectorsAccessorsAndRedirectCallSitesMatchTheRealBytecode() throws Exception {
        int checked = 0, redirects = 0, accessors = 0;
        for (String name : List.of("accessors/ServerPlayerAccessor", "injections/ServerPlayerMixin",
                "injections/PlayerListMixin", "injections/CreativeScreenQuickActionMixin",
                "injections/GuiExperienceMixin", "injections/MinecraftClientMixin",
                "injections/NetworkSmokeChatMixin", "injections/KeyCategoryLabelMixin",
                "injections/SmokeScreenshotFrameMixin")) {
            var mixin = node("xin/vanilla/banira/internal/mixin/" + name);
            var annotation = annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations).stream()
                    .filter(value -> value.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
            var targets = (List<?>) value(annotation, "value");
            assertEquals(1, targets.size());
            var target = node(((Type) targets.getFirst()).getInternalName());
            for (var handler : mixin.methods) {
                for (var injection : annotations(handler.visibleAnnotations, handler.invisibleAnnotations)) {
                    if (injection.desc.equals("Lorg/spongepowered/asm/mixin/gen/Accessor;")) {
                        String field = (String) value(injection, "value");
                        Type method = Type.getMethodType(handler.desc);
                        String descriptor = method.getReturnType().equals(Type.VOID_TYPE)
                                ? method.getArgumentTypes()[0].getDescriptor() : method.getReturnType().getDescriptor();
                        assertTrue(name + ": " + field, target.fields.stream().anyMatch(f -> f.name.equals(field) && f.desc.equals(descriptor)));
                        accessors++;
                        continue;
                    }
                    if (!injection.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")
                            && !injection.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")) continue;
                    Object raw = value(injection, "method");
                    List<?> selectors = raw instanceof List<?> list ? list : List.of(raw);
                    for (Object entry : selectors) {
                        String selector = (String) entry;
                        int descriptor = selector.indexOf('(');
                        String methodName = descriptor < 0 ? selector : selector.substring(0, descriptor);
                        List<MethodNode> matches = target.methods.stream().filter(method -> method.name.equals(methodName)
                                && (descriptor < 0 || method.desc.equals(selector.substring(descriptor)))).toList();
                        assertEquals(name + ": " + selector, 1, matches.size());
                        checked++;
                        if (!injection.desc.endsWith("/Redirect;")) continue;
                        Object atValue = value(injection, "at");
                        var at = atValue instanceof List<?> list ? (AnnotationNode) list.getFirst() : (AnnotationNode) atValue;
                        String callSite = (String) value(at, "target");
                        int ownerEnd = callSite.indexOf(';');
                        int arguments = callSite.indexOf('(', ownerEnd);
                        String owner = callSite.substring(1, ownerEnd);
                        String called = callSite.substring(ownerEnd + 1, arguments);
                        String signature = callSite.substring(arguments);
                        assertTrue(name + ": " + callSite, java.util.stream.StreamSupport.stream(matches.getFirst().instructions.spliterator(), false)
                                .anyMatch(instruction -> instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                                        && call.name.equals(called) && call.desc.equals(signature)));
                        redirects++;
                    }
                }
            }
        }
        assertTrue("All native lifecycle and GUI hooks were checked", checked >= 15);
        assertEquals(2, redirects);
        assertEquals(6, accessors);
    }

    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        var result = new ArrayList<AnnotationNode>();
        if (visible != null) result.addAll(visible);
        if (invisible != null) result.addAll(invisible);
        return result;
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) for (int i = 0; i < annotation.values.size(); i += 2)
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        throw new AssertionError(annotation.desc + " missing " + key);
    }
}
