package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import xin.vanilla.banira.internal.config.ClientConfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class AuiClientIsolationTest {
    @Test
    public void preferenceDefaultsToEnabled() throws Exception {
        java.lang.reflect.Field field;
        try {
            field = ClientConfig.class.getDeclaredField("useAui");
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing useAui default", e);
        }
        field.setAccessible(true);
        assertEquals(true, field.get(new ClientConfig()));
    }

    @Test
    public void contractAndControllerHaveNoClientOrVendorSymbolLinks() throws Exception {
        for (String name : new String[]{"internal.compat.aui.AuiCapabilityContract",
                "internal.compat.aui.AuiApiBindings", "internal.client.ui.UiBackendController"}) {
            Class<?> type = AuiCapabilityContractTest.required("xin.vanilla.banira." + name);
            type.getDeclaredMethods();
            try (InputStream input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
                org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
                new org.objectweb.asm.ClassReader(input).accept(node, 0);
                for (org.objectweb.asm.tree.MethodNode method : node.methods) {
                    assertFalse(method.desc.contains("com/sighs/"));
                    assertFalse(method.desc.contains("net/minecraft/client/"));
                    for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof org.objectweb.asm.tree.TypeInsnNode) {
                            String owner = ((org.objectweb.asm.tree.TypeInsnNode) instruction).desc;
                            assertFalse(owner.startsWith("com/sighs/"));
                            assertFalse(owner.startsWith("net/minecraft/client/"));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void metadataLeavesCapabilityFallbackAvailable() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/META-INF/mods.toml")) {
            String metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue("AUI must be optional client dependency", metadata.contains("modId = \"apricityui\""));
            String block = metadata.substring(metadata.indexOf("modId = \"apricityui\""));
            assertTrue(block.contains("mandatory = false"));
            assertTrue(block.contains("side = \"CLIENT\""));
            assertTrue(block.contains("versionRange = \"[0,)\""));
        }
    }

    @Test
    public void serverLoaderCanInitializeContractsWithClientClassesDenied() throws Exception {
        ClassLoader server = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("com.sighs.apricityui.")) {
                    throw new ClassNotFoundException("Client-only class denied: " + name);
                }
                if (!name.startsWith("xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract")
                        && !name.equals("xin.vanilla.banira.internal.compat.aui.AuiApiBindings")
                        && !name.equals("xin.vanilla.banira.internal.client.ui.UiBackendController")) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        try (InputStream input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            byte[] bytes = input.readAllBytes();
                            loaded = defineClass(name, bytes, 0, bytes.length);
                        } catch (java.io.IOException failure) {
                            throw new ClassNotFoundException(name, failure);
                        }
                    }
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        };
        for (String name : new String[]{"internal.compat.aui.AuiCapabilityContract",
                "internal.compat.aui.AuiApiBindings", "internal.client.ui.UiBackendController"}) {
            Class.forName("xin.vanilla.banira." + name, true, server).getDeclaredMethods();
        }
    }
}
