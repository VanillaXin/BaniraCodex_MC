package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import xin.vanilla.banira.internal.config.ClientConfig;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class AuiClientIsolationTest {
    static byte[] read(InputStream input) throws java.io.IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
        return output.toByteArray();
    }

    @Test
    public void preferenceDefaultsToEnabled() throws Exception {
        java.lang.reflect.Field field = ClientConfig.class.getDeclaredField("useAui");
        field.setAccessible(true);
        assertEquals(true, field.get(new ClientConfig()));
    }

    @Test
    public void contractsHaveNoClientOrVendorSymbolLinks() throws Exception {
        for (String name : new String[]{"internal.compat.aui.AuiCapabilityContract",
                "internal.compat.aui.AuiApiBindings", "internal.client.ui.UiBackendController"}) {
            Class<?> type = AuiCapabilityContractTest.required("xin.vanilla.banira." + name);
            type.getDeclaredMethods();
            try (InputStream input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, 0);
                assertTrue(node.version <= 52);
                for (MethodNode method : node.methods) {
                    assertFalse(method.desc.contains("com/sighs/"));
                    assertFalse(method.desc.contains("net/minecraft/client/"));
                    for (AbstractInsnNode instruction : method.instructions) {
                        String owner = instruction instanceof TypeInsnNode ? ((TypeInsnNode) instruction).desc
                                : instruction instanceof MethodInsnNode ? ((MethodInsnNode) instruction).owner
                                : instruction instanceof FieldInsnNode ? ((FieldInsnNode) instruction).owner : "";
                        assertFalse(owner, owner.startsWith("com/sighs/") || owner.startsWith("net/minecraft/client/"));
                    }
                }
            }
        }
    }

    @Test
    public void metadataKeepsEngineOptionalAndClientOnly() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/fabric.mod.json")) {
            assertNotNull(input);
            com.google.gson.JsonObject metadata = new com.google.gson.JsonParser()
                    .parse(new String(read(input), StandardCharsets.UTF_8)).getAsJsonObject();
            assertFalse(metadata.getAsJsonObject("depends").has("apricityui"));
            assertEquals(">=1.2.9", metadata.getAsJsonObject("suggests").get("apricityui").getAsString());
            assertEquals("*", metadata.get("environment").getAsString());
            com.google.gson.JsonObject entries = metadata.getAsJsonObject("entrypoints");
            assertEquals("xin.vanilla.banira.internal.fabric.client.FabricBaniraCodexClient",
                    entries.getAsJsonArray("client").get(0).getAsString());
            assertEquals("xin.vanilla.banira.internal.fabric.FabricBaniraCodexEntry",
                    entries.getAsJsonArray("main").get(0).getAsString());
        }
    }

    @Test
    public void serverLoaderInitializesFoundationWithoutClientClasses() throws Exception {
        ClassLoader server = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("com.sighs.apricityui.")) {
                    throw new ClassNotFoundException("Denied client class: " + name);
                }
                if (!name.startsWith("xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract")
                        && !name.equals("xin.vanilla.banira.internal.compat.aui.AuiApiBindings")
                        && !name.equals("xin.vanilla.banira.internal.client.ui.UiBackendController"))
                    return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        try (InputStream input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            byte[] bytes = read(input);
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
        for (String name : new String[]{"internal.compat.aui.AuiCapabilityContract", "internal.compat.aui.AuiApiBindings",
                "internal.client.ui.UiBackendController"})
            Class.forName("xin.vanilla.banira." + name, true, server).getDeclaredMethods();
    }
}
