package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class AuiCapabilityContractTest {
    public static Class<?> required(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException failure) {
            throw new AssertionError(name, failure);
        }
    }

    private Map<String, ClassNode> declarations() {
        Map<String, ClassNode> nodes = new LinkedHashMap<>();
        for (AuiCapabilityContract.Member member : AuiCapabilityContract.requiredMembers()) {
            ClassNode node = owner(nodes, member.owner);
            node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | (member.isStatic ? Opcodes.ACC_STATIC : 0),
                    member.name, member.descriptor, null, null));
        }
        for (String[] field : new String[][]{
                {"init/Element", "isVisible", "Z"}, {"init/Element", "scrollHeight", "D"},
                {"init/Element$DOMRect", "x", "D"}, {"init/Element$DOMRect", "y", "D"},
                {"init/Element$DOMRect", "right", "D"}, {"init/Element$DOMRect", "bottom", "D"},
                {"layout/Position", "x", "D"}, {"layout/Position", "y", "D"},
                {"event/KeyEvent", "keyCode", "I"}, {"event/KeyEvent", "modifiers", "I"},
                {"event/MouseEvent", "button", "I"}}) {
            owner(nodes, "com/sighs/apricityui/" + field[0]).fields.add(
                    new FieldNode(Opcodes.ACC_PUBLIC, field[1], field[2], null, null));
        }
        return nodes;
    }

    private ClassNode owner(Map<String, ClassNode> nodes, String name) {
        if (!nodes.containsKey(name)) {
            ClassNode node = new ClassNode();
            node.name = name;
            node.superName = "java/lang/Object";
            node.access = Opcodes.ACC_PUBLIC;
            node.version = Opcodes.V1_8;
            nodes.put(name, node);
        }
        return nodes.get(name);
    }

    private ClassLoader fixture(Consumer<Map<String, ClassNode>> edit, boolean marker) {
        Map<String, ClassNode> nodes = declarations();
        edit.accept(nodes);
        Map<String, byte[]> resources = new LinkedHashMap<>();
        nodes.forEach((name, node) -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            resources.put(name + ".class", writer.toByteArray());
        });
        if (marker) resources.put("META-INF/banira-aui-compile-api.properties", new byte[]{1});
        return new ClassLoader(null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) {
                throw new AssertionError("Inspection initialized optional class: " + name);
            }

            @Override
            public InputStream getResourceAsStream(String name) {
                byte[] bytes = resources.get(name);
                return bytes == null ? null : new ByteArrayInputStream(bytes);
            }
        };
    }

    @Test
    public void absentEngineFallsBackWithoutInitializingClient() {
        assertFalse(AuiCapabilityContract.inspect(null).compatible());
        assertFalse(AuiCapabilityContract.inspect(new ClassLoader(null) {}).compatible());
    }

    @Test
    public void declarationShapeCanBeInspectedWithoutLoadingClasses() {
        assertTrue(AuiCapabilityContract.inspect(fixture(nodes -> {}, false)).compatible());
    }

    @Test
    public void everyRequiredOwnerRejectsNewerThanJava8Bytecode() {
        for (String name : declarations().keySet()) {
            for (int version : new int[]{Opcodes.V9, Opcodes.V17}) {
                assertFalse(name, AuiCapabilityContract.inspect(fixture(nodes -> nodes.get(name).version = version, false), Opcodes.V1_8).compatible());
            }
        }
    }

    @Test
    public void verifiedJava17ShapesAreAllowedOnlyOnCapableHosts() {
        ClassLoader resources = fixture(nodes -> nodes.values().forEach(node -> node.version = Opcodes.V17), false);
        assertFalse(AuiCapabilityContract.inspect(resources, Opcodes.V1_8).compatible());
        assertTrue(AuiCapabilityContract.inspect(resources, Opcodes.V17).compatible());
        assertTrue(AuiCapabilityContract.inspect(resources, Opcodes.V21).compatible());
    }

    @Test
    public void unknownAndPreviewVersionsRemainRejectedOnModernHosts() {
        for (String name : declarations().keySet()) {
            for (int version : new int[]{0, 44, Opcodes.V18, Opcodes.V21, Opcodes.V17 | Opcodes.V_PREVIEW}) {
                assertFalse(name, AuiCapabilityContract.inspect(
                        fixture(nodes -> nodes.get(name).version = version, false), Opcodes.V21).compatible());
            }
        }
    }

    @Test
    public void runtimeVersionParsingKeepsTheVerifiedCeiling() {
        assertEquals(Opcodes.V1_8, AuiCapabilityContract.maximumClassVersion("52.0"));
        assertEquals(Opcodes.V17, AuiCapabilityContract.maximumClassVersion("61.0"));
        assertEquals(Opcodes.V17, AuiCapabilityContract.maximumClassVersion("65.0"));
        for (String invalid : new String[]{null, "", "NaN", "+61.0", "61.1", "61", "0.0", "2147483648.0"}) {
            assertEquals(Opcodes.V1_8, AuiCapabilityContract.maximumClassVersion(invalid));
        }
    }

    @Test
    public void declarationsStayRejectedOnModernHosts() {
        assertFalse(AuiCapabilityContract.inspect(fixture(
                nodes -> nodes.values().forEach(node -> node.version = Opcodes.V17), true), Opcodes.V17).compatible());
    }

    @Test
    public void missingMethodOrWrongReturnFallsBack() {
        assertFalse(AuiCapabilityContract.inspect(fixture(nodes ->
                nodes.get("com/sighs/apricityui/init/Document").methods.removeIf(method -> method.name.equals("setManuallyRendered")), false)).compatible());
        assertFalse(AuiCapabilityContract.inspect(fixture(nodes ->
                nodes.get("com/sighs/apricityui/init/Element").methods.stream().filter(method -> method.name.equals("getValue"))
                        .forEach(method -> method.desc = "()Ljava/lang/Object;"), false)).compatible());
    }

    @Test
    public void publicAndStaticMethodContractIsExact() {
        for (int access : new int[]{Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, Opcodes.ACC_PUBLIC,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT}) {
            assertFalse(AuiCapabilityContract.inspect(fixture(nodes ->
                    nodes.get("com/sighs/apricityui/init/Document").methods.stream().filter(method -> method.name.equals("create"))
                            .forEach(method -> method.access = access), false)).compatible());
        }
    }

    @Test
    public void missingPublicVisibilityFieldFallsBack() {
        assertFalse(AuiCapabilityContract.inspect(fixture(nodes ->
                nodes.get("com/sighs/apricityui/init/Element").fields.removeIf(field -> field.name.equals("isVisible")), false)).compatible());
    }

    @Test
    public void compileDeclarationsAreAlwaysRejectedAtRuntime() throws Exception {
        ClassLoader stub = fixture(nodes -> {}, true);
        AuiCapabilityContract.Result result = AuiCapabilityContract.inspect(stub);
        assertFalse(result.compatible());
        assertTrue(result.mismatch().contains("Compile-only"));
        try {
            AuiApiBindings.bind(stub, result);
            fail("Declarations must never bind as an engine");
        } catch (IllegalStateException expected) {
            assertEquals(result.mismatch(), expected.getMessage());
        }
    }

    @Test
    public void malformedClassIsIncompatible() {
        assertFalse(AuiCapabilityContract.inspect(new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return name.endsWith(".class") ? new ByteArrayInputStream(new byte[]{1, 2, 3}) : null;
            }
        }).compatible());
    }

    @Test
    public void overlayDescriptorUsesActualForge16MatrixStack() {
        assertTrue(AuiCapabilityContract.requiredMembers().stream().anyMatch(member -> member.name.equals("drawOverlayDocument")
                && member.descriptor.equals("(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/sighs/apricityui/init/Document;)V")));
        assertFalse(AuiCapabilityContract.requiredMembers().stream().anyMatch(member -> member.descriptor.contains("PoseStack")));
    }
}
