package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.*;

public class AuiCapabilityContractTest {
    public static Class<?> required(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Missing AUI selection contract: " + name, e);
        }
    }

    private boolean compatible(ClassLoader loader) throws Exception {
        Object result = required("xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract")
                .getMethod("inspect", ClassLoader.class).invoke(null, loader);
        return (boolean) result.getClass().getMethod("compatible").invoke(result);
    }

    @Test
    public void absentAuiFallsBackWithoutLoadingClientClasses() throws Exception {
        assertFalse(compatible(new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) { return null; }
        }));
    }

    @Test
    public void officialBinaryIsCompatibleWithoutInitializingEngine() throws Exception {
        required("xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract");
        try (URLClassLoader jar = official()) {
            assertTrue(compatible(resourcesOnly(jar, false)));
        }
    }

    @Test
    public void missingRequiredMethodFallsBack() throws Exception {
        required("xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract");
        try (URLClassLoader jar = official()) {
            assertFalse(compatible(resourcesOnly(jar, true)));
        }
    }

    @Test
    public void wrongReturnDescriptorFallsBack() throws Exception {
        required("xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract");
        try (URLClassLoader jar = official()) {
            ClassLoader changed = new ClassLoader(null) {
                @Override
                public InputStream getResourceAsStream(String name) {
                    InputStream stream = jar.getResourceAsStream(name);
                    if (!name.equals("com/sighs/apricityui/init/Element.class")) return stream;
                    try (InputStream input = stream) {
                        ClassNode node = new ClassNode();
                        new ClassReader(input).accept(node, 0);
                        node.methods.stream().filter(m -> m.name.equals("getValue"))
                                .forEach(m -> m.desc = "()Ljava/lang/Object;");
                        ClassWriter writer = new ClassWriter(0);
                        node.accept(writer);
                        return new ByteArrayInputStream(writer.toByteArray());
                    } catch (Exception e) { throw new AssertionError(e); }
                }
            };
            assertFalse(compatible(changed));
        }
    }

    @Test
    public void malformedClassIsIncompatible() throws Exception {
        assertFalse(compatible(new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return new ByteArrayInputStream(new byte[]{1, 2, 3});
            }
        }));
    }

    @Test
    public void officialBindingCachesOnlyKnownOperations() throws Exception {
        try (URLClassLoader jar = new URLClassLoader(new URL[]{new java.io.File(
                System.getProperty("banira.aui.contractJar")).toURI().toURL()}, getClass().getClassLoader())) {
            Class<?> contract = required("xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract");
            Object result = contract.getMethod("inspect", ClassLoader.class).invoke(null, jar);
            Class<?> bindings = required("xin.vanilla.banira.internal.compat.aui.AuiApiBindings");
            Object bound = bindings.getMethod("bind", ClassLoader.class, result.getClass()).invoke(null, jar, result);
            assertNotNull(bound);
            try {
                bindings.getMethod("invoke", String.class, Object.class, Object[].class)
                        .invoke(bound, "unapprovedOperation", null, new Object[0]);
                fail("Unknown operation was accepted");
            } catch (java.lang.reflect.InvocationTargetException e) {
                assertTrue(e.getCause() instanceof IllegalArgumentException);
            }
        }
    }

    private URLClassLoader official() throws Exception {
        String path = System.getProperty("banira.aui.contractJar");
        assertNotNull("Pinned official AUI binary must be provided", path);
        return new URLClassLoader(new URL[]{new java.io.File(path).toURI().toURL()}, null);
    }

    private ClassLoader resourcesOnly(URLClassLoader jar, boolean remove) {
        return new ClassLoader(null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) {
                throw new AssertionError("Capability inspection must not load " + name);
            }

            @Override
            public InputStream getResourceAsStream(String name) {
                InputStream stream = jar.getResourceAsStream(name);
                if (!remove || !name.equals("com/sighs/apricityui/init/Document.class")) return stream;
                try (InputStream input = stream) {
                    ClassNode node = new ClassNode();
                    new ClassReader(input).accept(node, 0);
                    node.methods.removeIf(m -> m.name.equals("setManuallyRendered"));
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    return new ByteArrayInputStream(writer.toByteArray());
                } catch (Exception e) { throw new AssertionError(e); }
            }
        };
    }
}
