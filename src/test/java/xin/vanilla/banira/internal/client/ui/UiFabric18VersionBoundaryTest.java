package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;
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
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        String contract = Files.readString(Path.of("gradle/aui-contract.gradle"));
        assertTrue(metadata.contains("\"fabricloader\""));
        assertFalse(metadata.contains("ApricityUI-forge"));
        assertTrue(contract.contains("ApricityUI-fabric-1.18.2"));
        assertTrue(contract.contains("no runtime artifact is configured"));
        assertFalse(contract.contains("modRuntimeOnly"));
    }
}
