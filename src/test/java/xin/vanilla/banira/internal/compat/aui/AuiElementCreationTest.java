package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AuiElementCreationTest {
    private static final String CREATE = "com/sighs/apricityui/init/Document.createElement";
    private static final String INIT = "com/sighs/apricityui/init/Element.init";
    private static final String WRAP = "xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.wrap";

    @Test
    public void dynamicElementIdentityIsInitializedBeforeItIsWrapped() throws Exception {
        ClassNode type = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        lombok.var create = type.methods.stream().filter(method -> method.name.equals("create")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        List<String> calls = new ArrayList<>();
        for (lombok.var instruction : create.instructions)
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                String name = call.owner + "." + call.name;
                if (com.google.common.collect.ImmutableList.of(CREATE, INIT, WRAP).contains(name)) calls.add(name);
            }
        assertTrue("AUI insertion must retain the initialized element identity", initializedBeforeWrapping(calls));
    }

    @Test
    public void initializationIsPartOfTheOptionalCapabilityContract() {
        assertTrue(AuiCapabilityContract.requiredMembers().stream().anyMatch(member ->
                member.owner.equals("com/sighs/apricityui/init/Element") && member.name.equals("init") && member.isStatic
                        && member.descriptor.equals("(Lcom/sighs/apricityui/init/Element;)Lcom/sighs/apricityui/init/Element;")));
    }

    @Test
    public void rawAndLateInitializationDoNotPassTheIdentityContract() {
        assertTrue(initializedBeforeWrapping(com.google.common.collect.ImmutableList.of(CREATE, INIT, WRAP)));
        assertFalse(initializedBeforeWrapping(com.google.common.collect.ImmutableList.of(CREATE, WRAP)));
        assertFalse(initializedBeforeWrapping(com.google.common.collect.ImmutableList.of(CREATE, WRAP, INIT)));
        assertFalse(initializedBeforeWrapping(com.google.common.collect.ImmutableList.of(INIT, CREATE, WRAP)));
    }

    private static boolean initializedBeforeWrapping(List<String> calls) {
        return calls.equals(com.google.common.collect.ImmutableList.of(CREATE, INIT, WRAP));
    }
}
