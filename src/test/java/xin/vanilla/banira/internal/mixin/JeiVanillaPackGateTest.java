package xin.vanilla.banira.internal.mixin;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

public class JeiVanillaPackGateTest {
    private static final String CALL = "(Ljava/util/function/Consumer;Lnet/minecraft/server/packs/repository/Pack$PackConstructor;)V";

    @Test public void acceptsDevelopmentCall() throws Exception { assertTrue(matches(target("loadPacks", CALL))); }
    @Test public void acceptsProductionSrgCall() throws Exception { assertTrue(matches(target("m_7686_", CALL))); }
    @Test public void skipsMissingFallbackInOlderJei() throws Exception { assertFalse(matches(new ClassNode())); }
    @Test public void skipsChangedConstructorContract() throws Exception { assertFalse(matches(target("loadPacks", "()V"))); }
    @Test public void skipsChangedFallbackReturnType() throws Exception {
        ClassNode node = target("loadPacks", CALL);
        node.methods.get(0).desc = "()Ljava/lang/Object;";
        assertFalse(matches(node));
    }
    @Test public void skipsAmbiguousCalls() throws Exception {
        ClassNode node = target("loadPacks", CALL);
        node.methods.get(0).instructions.add(call("loadPacks", CALL));
        assertFalse(matches(node));
    }

    private static ClassNode target(String name, String descriptor) {
        ClassNode node = new ClassNode();
        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "openVanillaServerDataPack", "()Lnet/minecraft/server/packs/PackResources;", null, null);
        method.instructions.add(call(name, descriptor));
        node.methods.add(method);
        return node;
    }

    private static MethodInsnNode call(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/server/packs/repository/ServerPacksSource", name, descriptor, false);
    }

    private static boolean matches(ClassNode node) throws Exception {
        Method gate = Arrays.stream(OptionalCompatibilityMixinPlugin.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("matchesJeiVanillaPacks")).findFirst().orElse(null);
        assertNotNull("Optional JEI fallback requires an exact call-contract gate", gate);
        gate.setAccessible(true);
        return (Boolean) gate.invoke(null, node);
    }
}
