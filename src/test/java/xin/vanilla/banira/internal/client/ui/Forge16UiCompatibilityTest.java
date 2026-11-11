package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xin.vanilla.banira.api.client.ui.UiHudFrame;
import xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract;

import java.io.File;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

public class Forge16UiCompatibilityTest {
    @Test
    public void adaptedDeclarationsAreJava8AndContainNoEngineImplementation() throws Exception {
        try (JarFile jar = new JarFile(System.getProperty("banira.aui.declarationsJar"))) {
            assertNotNull(jar.getEntry("META-INF/banira-aui-compile-api.properties"));
            assertNull(jar.getEntry("META-INF/mods.toml"));
            Enumeration<JarEntry> entries = jar.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode node = new ClassNode();
                try (InputStream input = jar.getInputStream(entry)) {
                    new ClassReader(input).accept(node, 0);
                }
                assertEquals(52, node.version);
                for (MethodNode method : node.methods) {
                    if ((method.access & Opcodes.ACC_ABSTRACT) != 0) continue;
                    assertTrue(Arrays.stream(method.instructions.toArray()).anyMatch(i -> i.getOpcode() == Opcodes.ATHROW));
                    for (AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode)
                            assertEquals("java/lang/UnsupportedOperationException", ((MethodInsnNode) instruction).owner);
                    }
                }
                count++;
            }
            assertTrue(count > 30);
        }
    }

    @Test
    public void declarationsCannotActivateTheRuntimeBackend() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{
                new File(System.getProperty("banira.aui.declarationsJar")).toURI().toURL()}, null)) {
            AuiCapabilityContract.Result result = AuiCapabilityContract.inspect(loader);
            assertFalse(result.compatible());
            assertTrue(result.mismatch().contains("Compile-only"));
        }
    }

    @Test
    public void adaptedPublicSignaturesUseLegacyMinecraftTypes() throws Exception {
        try (JarFile jar = new JarFile(System.getProperty("banira.aui.declarationsJar"))) {
            ClassNode screen = code(jar, "screen/ApricityScreen");
            assertEquals("net/minecraft/client/gui/screen/Screen", screen.superName);
            assertTrue(code(jar, "render/Base").methods.stream().anyMatch(method -> method.name.equals("drawOverlayDocument")
                    && method.desc.equals("(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/sighs/apricityui/init/Document;)V")));
        }
    }

    private ClassNode code(JarFile jar, String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = jar.getInputStream(jar.getJarEntry("com/sighs/apricityui/" + name + ".class"))) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void legacyHudValueObjectsRemainImmutableAndValueEqual() {
        UiHudFrame.Text first = new UiHudFrame.Text("clock", "00:10", 0, 0, 40, 10, 9, 1, 0, 0, 0, -1);
        UiHudFrame.Text second = new UiHudFrame.Text("clock", "00:10", 0, 0, 40, 10, 9, 1, 0, 0, 0, -1);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        List<UiHudFrame.Part> source = new ArrayList<>();
        source.add(first);
        UiHudFrame frame = new UiHudFrame(source);
        source.clear();
        assertEquals(1, frame.parts().size());
        assertThrows(UnsupportedOperationException.class, () -> frame.parts().clear());
    }

    @Test
    public void unsealedJava8SurfaceStillRejectsUnknownHudParts() {
        UiHudFrame.Part unknown = () -> "custom";
        assertThrows(IllegalArgumentException.class, () -> new UiHudFrame(Collections.singletonList(unknown)));
    }
}
