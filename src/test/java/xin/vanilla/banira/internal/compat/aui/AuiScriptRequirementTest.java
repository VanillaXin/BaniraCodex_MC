package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.junit.Assert.*;
import static org.objectweb.asm.Opcodes.*;

public class AuiScriptRequirementTest {
    private static final String CONTEXT = "dev.latvian.mods.rhino.Context";
    private static final String LEGACY = "com.sighs.apricityui.script.ScriptRuntimeSupport";
    private static final String MODERN = "com.sighs.apricityui.script.ApricityScriptSupport";

    @Test
    public void neitherAnUnregisteredAuiNorAnUnregisteredRhinoCanActivateAui() {
        ClassLoader inaccessible = new ClassLoader(null) {
            @Override
            public java.io.InputStream getResourceAsStream(String name) {
                throw new AssertionError("Optional classes must not be inspected until both mods are loaded");
            }
        };
        assertFalse(AuiCapabilityContract.inspectClient(inaccessible, "rhino"::equals).compatible());
        assertFalse(AuiCapabilityContract.inspectClient(inaccessible, "apricityui"::equals).compatible());
    }

    @Test
    public void scriptRuntimeCanBeAvailableWithoutKubeJs() {
        assertTrue(AuiCapabilityContract.scriptsAvailable(runtime(true, LEGACY, true, false)));
        assertTrue(AuiCapabilityContract.scriptsAvailable(runtime(true, MODERN, true, false)));
    }

    @Test
    public void scriptRuntimeNeedsLoadableRhinoAndAuiScriptSupport() {
        assertFalse(AuiCapabilityContract.scriptsAvailable(runtime(false, LEGACY, true, false)));
        assertFalse(AuiCapabilityContract.scriptsAvailable(runtime(true, "", true, false)));
    }

    @Test
    public void auiMayDeclineOrFailToInitializeTheRuntime() {
        assertFalse(AuiCapabilityContract.scriptsAvailable(runtime(true, LEGACY, false, false)));
        assertFalse(AuiCapabilityContract.scriptsAvailable(runtime(true, MODERN, true, true)));
    }

    private static ClassLoader runtime(boolean rhino, String support, boolean available, boolean fails) {
        return new ClassLoader(null) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (!(rhino && CONTEXT.equals(name)) && !support.equals(name)) throw new ClassNotFoundException(name);
                ClassWriter type = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                type.visit(V1_8, ACC_PUBLIC, name.replace('.', '/'), null, "java/lang/Object", null);
                if (support.equals(name)) {
                    MethodVisitor method = type.visitMethod(ACC_PUBLIC | ACC_STATIC, "rhinoAvailable", "()Z", null, null);
                    method.visitCode();
                    if (fails) {
                        method.visitTypeInsn(NEW, "java/lang/LinkageError");
                        method.visitInsn(DUP);
                        method.visitMethodInsn(INVOKESPECIAL, "java/lang/LinkageError", "<init>", "()V", false);
                        method.visitInsn(ATHROW);
                    } else {
                        method.visitInsn(available ? ICONST_1 : ICONST_0);
                        method.visitInsn(IRETURN);
                    }
                    method.visitMaxs(0, 0);
                    method.visitEnd();
                }
                type.visitEnd();
                byte[] bytes = type.toByteArray();
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
    }
}
