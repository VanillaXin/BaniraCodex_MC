package xin.vanilla.banira.client.notification;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.InputStream;

import static org.junit.Assert.*;

public class FtbMinimapContractTest {
    @Test
    public void appendsToTheReturnedNativeTextListWithoutShadowingOldState() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/mixin/compat/minimap/FtbChunksNotificationMixin.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        for (FieldNode field : node.fields)
            assertNotEquals("Removed native field must not be shadowed", "MINIMAP_TEXT_LIST", field.name);
        boolean builds = false, reads = false, appends = false;
        for (MethodNode method : node.methods) {
            if (method.visibleAnnotations != null) for (AnnotationNode annotation : method.visibleAnnotations) {
                if (!annotation.desc.endsWith("/Inject;")) continue;
                for (int i = 0; i < annotation.values.size(); i += 2)
                    if ("method".equals(annotation.values.get(i)))
                        builds |= annotation.values.get(i + 1).toString().contains("buildMinimapTextData");
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                reads |= call.name.equals("getReturnValue");
                appends |= call.owner.equals("java/util/List") && call.name.equals("add");
            }
        }
        assertTrue(builds && reads && appends);
    }
}
