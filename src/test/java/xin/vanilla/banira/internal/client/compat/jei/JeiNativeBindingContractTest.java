package xin.vanilla.banira.internal.client.compat.jei;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import static org.junit.Assert.*;

public class JeiNativeBindingContractTest {
    @Test public void bridgeDoesNotHardLinkRemovedConfigGetterOrInputPackage() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in = getClass().getResourceAsStream("/xin/vanilla/banira/internal/neoforge/compat/jei/JeiCompatibility.class")) {
            assertNotNull(in); new ClassReader(in).accept(node,0);
        }
        for (MethodNode method : node.methods) for (AbstractInsnNode instruction : method.instructions)
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                assertFalse("Removed config getter",call.owner.equals("mezz/jei/common/config/IClientConfig") && call.name.equals("isLookupHistoryEnabled"));
                assertFalse("Moved input implementation",call.owner.equals("mezz/jei/gui/input/UserInput"));
            }
    }
    @Test public void lateNativeCapturesRefreshTheAdoptedActions() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in = getClass().getResourceAsStream("/xin/vanilla/banira/internal/neoforge/compat/jei/JeiCompatibility.class")) {
            assertNotNull(in); new ClassReader(in).accept(node,0);
        }
        for (String name : new String[]{"capture","captureLookupHistory"}) {
            boolean refresh = false;
            for (MethodNode method : node.methods) if (method.name.equals(name))
                for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode)
                    refresh |= ((MethodInsnNode)instruction).name.equals("refreshCurrentScreen");
            assertTrue("Late native capture must refresh adopted entries: "+name,refresh);
        }
    }
}
