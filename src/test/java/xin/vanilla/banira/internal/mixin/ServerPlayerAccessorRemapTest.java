package xin.vanilla.banira.internal.mixin;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ServerPlayerAccessorRemapTest {
    private static final String ACCESSOR = "xin/vanilla/banira/internal/mixin/accessors/ServerPlayerAccessor";
    private static final boolean LANGUAGE_REMAP = true;
    private static final int LANGUAGE_ACCESSORS = 0;

    @Test
    public void minecraftTargetClassRetainsRemapping() throws Exception {
        ClassNode node = read(ACCESSOR);
        AnnotationNode mixin = annotation(node.visibleAnnotations, node.invisibleAnnotations,
                "Lorg/spongepowered/asm/mixin/Mixin;");
        assertNotNull(mixin);
        assertNotEquals(Boolean.FALSE, value(mixin, "remap"));
    }

    @Test
    public void languageRemappingMatchesFieldOwnership() throws Exception {
        int found = 0;
        for (MethodNode method : read(ACCESSOR).methods) {
            AnnotationNode accessor = accessor(method);
            if (accessor != null && "language".equals(value(accessor, "value"))) {
                found++;
                if (LANGUAGE_REMAP) {
                    assertNotEquals(method.name, Boolean.FALSE, value(accessor, "remap"));
                } else {
                    assertEquals(method.name, Boolean.FALSE, value(accessor, "remap"));
                }
            }
        }
        assertEquals(LANGUAGE_ACCESSORS, found);
    }

    @Test
    public void mappedVanillaAccessorsKeepTheirMappings() throws Exception {
        verifyVanilla(read(ACCESSOR));
    }

    @Test
    public void vanillaMappingGuardRejectsMissingAccessor() throws Exception {
        ClassNode node = read(ACCESSOR);
        for (MethodNode method : node.methods) {
            AnnotationNode accessor = accessor(method);
            if (accessor != null && !"language".equals(value(accessor, "value"))) {
                if (method.visibleAnnotations != null) method.visibleAnnotations.remove(accessor);
                if (method.invisibleAnnotations != null) method.invisibleAnnotations.remove(accessor);
                break;
            }
        }
        boolean rejected = false;
        try {
            verifyVanilla(node);
        } catch (AssertionError expected) {
            rejected = true;
        }
        assertTrue("Missing vanilla accessor must be rejected", rejected);
    }

    private static void verifyVanilla(ClassNode node) {
        assertNotEquals(Boolean.FALSE, value(annotation(node.visibleAnnotations, node.invisibleAnnotations,
                "Lorg/spongepowered/asm/mixin/Mixin;"), "remap"));
        java.util.Set<String> expected = new java.util.HashSet<>(java.util.Arrays.asList(
                "chatVisibility#(Lnet/minecraft/world/entity/player/ChatVisiblity;)V",
                "canChatColor#(Z)V",
                "allowsListing#()Z",
                "allowsListing#(Z)V",
                "requestedViewDistance#(I)V",
                "textFilteringEnabled#(Z)V"));
        java.util.Set<String> actual = new java.util.HashSet<>();
        for (MethodNode method : node.methods) {
            AnnotationNode accessor = accessor(method);
            if (accessor != null && !"language".equals(value(accessor, "value"))) {
                assertNotEquals(method.name, Boolean.FALSE, value(accessor, "remap"));
                actual.add(value(accessor, "value") + "#" + method.desc);
            }
        }
        assertEquals("Complete vanilla accessor contract", expected, actual);
    }

    @Test
    public void languageGetterAndSetterHaveStableDescriptors() throws Exception {
        List<String> descriptors = new ArrayList<>();
        for (MethodNode method : read(ACCESSOR).methods) {
            AnnotationNode accessor = accessor(method);
            if (accessor != null && "language".equals(value(accessor, "value"))) {
                descriptors.add(method.desc);
            }
        }
        assertEquals(LANGUAGE_ACCESSORS, descriptors.size());
        if (LANGUAGE_ACCESSORS != 0) {
            assertTrue(descriptors.contains("()Ljava/lang/String;"));
            assertTrue(descriptors.contains("(Ljava/lang/String;)V"));
        }
    }

    private static ClassNode read(String name) throws Exception {
        try (InputStream stream = ServerPlayerAccessorRemapTest.class.getClassLoader()
                .getResourceAsStream(name + ".class")) {
            assertNotNull(name, stream);
            ClassNode result = new ClassNode();
            new ClassReader(stream).accept(result, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            return result;
        }
    }

    private static AnnotationNode accessor(MethodNode method) {
        return annotation(method.visibleAnnotations, method.invisibleAnnotations,
                "Lorg/spongepowered/asm/mixin/gen/Accessor;");
    }

    private static AnnotationNode annotation(List<AnnotationNode> visible,
                                             List<AnnotationNode> invisible, String descriptor) {
        for (List<AnnotationNode> annotations : java.util.Arrays.asList(visible, invisible)) {
            if (annotations != null) {
                for (AnnotationNode annotation : annotations) {
                    if (descriptor.equals(annotation.desc)) {
                        return annotation;
                    }
                }
            }
        }
        return null;
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) {
            for (int index = 0; index < annotation.values.size(); index += 2) {
                if (key.equals(annotation.values.get(index))) {
                    return annotation.values.get(index + 1);
                }
            }
        }
        return null;
    }
}
