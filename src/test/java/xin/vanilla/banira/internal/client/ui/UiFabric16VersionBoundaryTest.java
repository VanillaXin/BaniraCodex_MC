package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;

public class UiFabric16VersionBoundaryTest {
    @Test
    public void transientStylesUseTheMinecraft16ResourceStream() throws Exception {
        ClassNode node = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        lombok.var calls = node.methods.stream().flatMap(method -> Arrays.stream(method.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode).map(instruction -> (MethodInsnNode) instruction).collect(java.util.stream.Collectors.toList());
        assertTrue(calls.stream().anyMatch(call -> call.name.equals("getResource")
                && call.desc.endsWith("Lnet/minecraft/server/packs/resources/Resource;")));
        assertTrue(calls.stream().anyMatch(call -> call.owner.equals("net/minecraft/server/packs/resources/Resource")
                && call.name.equals("getInputStream") && call.desc.equals("()Ljava/io/InputStream;")));
        assertFalse(calls.stream().anyMatch(call -> call.owner.equals("net/minecraft/server/packs/resources/ResourceManager")
                && call.name.equals("open")));
    }

    @Test
    public void runtimeMetadataAndCompileContractKeepSeparatePlatforms() throws Exception {
        String metadata = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/fabric.mod.json")), java.nio.charset.StandardCharsets.UTF_8);
        String contract = new String(Files.readAllBytes(java.nio.file.Paths.get("gradle/aui-contract.gradle")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(metadata.contains("\"fabricloader\""));
        assertFalse(metadata.contains("ApricityUI-forge"));
        assertTrue(contract.contains("ApricityUI-forge-1.18.2"));
        assertTrue(contract.contains("fabric-1.16.5-declarations.jar"));
        assertTrue(contract.contains("Opcodes.V1_8"));
        assertTrue(contract.contains("configurations.compileOnly.extendsFrom(configurations.auiContract)"));
        assertFalse(contract.contains("modRuntimeOnly"));
    }
}
