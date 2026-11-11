package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.Arrays;

import static org.junit.Assert.*;

public class UiFabric18VersionBoundaryTest {
    @Test
    public void transientStylesUseTheMinecraft18ResourceStream() throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        var calls = node.methods.stream().flatMap(method -> Arrays.stream(method.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode).map(instruction -> (MethodInsnNode) instruction).toList();
        assertTrue(calls.stream().anyMatch(call -> call.name.equals("getResource")
                && call.desc.endsWith("Lnet/minecraft/server/packs/resources/Resource;")));
        assertTrue(calls.stream().anyMatch(call -> call.owner.equals("net/minecraft/server/packs/resources/Resource")
                && call.name.equals("getInputStream") && call.desc.equals("()Ljava/io/InputStream;")));
        assertFalse(calls.stream().anyMatch(call -> call.owner.equals("net/minecraft/server/packs/resources/ResourceManager")
                && call.name.equals("open")));
    }

    @Test
    public void runtimeMetadataAndCompileContractKeepSeparatePlatforms() throws Exception {
        try (var input = getClass().getResourceAsStream("/fabric.mod.json")) {
            assertNotNull(input);
            var metadata = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var dependencies = metadata.getAsJsonObject("depends");
            assertTrue(dependencies.has("fabricloader"));
            assertFalse(dependencies.has("forge"));
            assertFalse(dependencies.has("apricityui"));
        }
        assertEquals("false", System.getProperty("banira.aui.contractOnRuntimeClasspath"));
    }
}
