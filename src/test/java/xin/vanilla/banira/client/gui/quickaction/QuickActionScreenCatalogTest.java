package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;
import org.objectweb.asm.*;

import java.util.Arrays;

import static org.junit.Assert.*;

public class QuickActionScreenCatalogTest {
    private ClassWriter fixture(int access) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, access, "example/Options", null, "net/minecraft/client/gui/screens/Screen", null);
        // Deliberately non-executable initializer: indexing must only read bytecode.
        MethodVisitor initializer = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        initializer.visitInsn(Opcodes.ACONST_NULL);
        initializer.visitInsn(Opcodes.ATHROW);
        initializer.visitMaxs(1, 0);
        initializer.visitEnd();
        return writer;
    }

    @Test
    public void exposesPublicScalarAndParentConstructorsWithoutLoadingClasses() {
        ClassWriter writer = fixture(Opcodes.ACC_PUBLIC);
        writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(ILjava/lang/String;Z)V", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Lnet/minecraft/client/gui/screens/Screen;)V", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "(D)V", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", null, null).visitEnd();
        QuickActionScreenCatalog.Entry entry = QuickActionScreenCatalog.inspect("Example", new ClassReader(writer.toByteArray()));
        assertEquals("example.Options", entry.className);
        assertEquals(3, entry.constructors.size());
        assertTrue(entry.constructors.stream().anyMatch(ctor -> ctor.parent));
        assertTrue(entry.constructors.stream().anyMatch(ctor -> Arrays.equals(ctor.types, new String[]{"int", "java.lang.String", "boolean"})));
    }

    @Test
    public void rejectsAbstractClassesAndDoesNotExposeInaccessibleConstructors() {
        assertNull(QuickActionScreenCatalog.inspect("Example", new ClassReader(fixture(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT).toByteArray())));
        ClassWriter writer = fixture(0);
        writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).visitEnd();
        assertTrue(QuickActionScreenCatalog.inspect("Example", new ClassReader(writer.toByteArray())).constructors.isEmpty());
    }

    @Test
    public void leavesContextBoundScreensDiscoverableButUnavailable() {
        ClassWriter writer = fixture(Opcodes.ACC_PUBLIC);
        writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/util/Map;[I)V", null, null).visitEnd();
        QuickActionScreenCatalog.Entry entry = QuickActionScreenCatalog.inspect("Example", new ClassReader(writer.toByteArray()));
        assertNotNull(entry);
        assertEquals("Example", entry.mod);
        assertTrue(entry.constructors.isEmpty());
    }

    @Test
    public void extractsOnlyComponentPassedToTheSuperclassInsteadOfUnrelatedLabels() {
        ClassWriter writer = fixture(Opcodes.ACC_PUBLIC);
        MethodVisitor ctor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitLdcInsn("example.unrelated");
        ctor.visitLdcInsn("example.title");
        ctor.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/network/chat/Component", "translatable",
                "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", true);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/minecraft/client/gui/screens/Screen", "<init>",
                "(Lnet/minecraft/network/chat/Component;)V", false);
        ctor.visitLdcInsn("example.button");
        ctor.visitEnd();
        QuickActionScreenCatalog.Entry entry = QuickActionScreenCatalog.inspect("Example", new ClassReader(writer.toByteArray()));
        assertEquals(Arrays.asList("example.title"), entry.titleKeys);
    }
}
