package xin.vanilla.banira.internal.compat.aui;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import static org.junit.Assert.*;

public class FabricAuiDescriptorTest {
    @Test
    public void overlayContractUsesTheRuntimeMappedPoseType() throws Exception {
        var overlay = AuiCapabilityContract.requiredMembers().stream()
                .filter(member -> member.name.equals("drawOverlayDocument")).findFirst().orElseThrow();
        assertEquals("(" + Type.getDescriptor(PoseStack.class) + "Lcom/sighs/apricityui/init/Document;)V", overlay.descriptor);
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("AuiCapabilityContract.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        boolean mapped = false;
        for (var method : node.methods) for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals("org/objectweb/asm/Type") && call.name.equals("getDescriptor"))
                mapped = true;
        assertTrue("The packaged descriptor must follow Loom's mapped class literal", mapped);
    }
}
