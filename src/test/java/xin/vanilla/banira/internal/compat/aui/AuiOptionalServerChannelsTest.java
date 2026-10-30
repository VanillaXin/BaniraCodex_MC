package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class AuiOptionalServerChannelsTest {
    private Class<?> policy() {
        return AuiCapabilityContractTest.required("xin.vanilla.banira.internal.compat.aui.AuiOptionalServerChannels");
    }

    @SuppressWarnings("unchecked")
    private Predicate<String> versions(String namespace, String path, Predicate<String> original) throws Exception {
        return (Predicate<String>) policy().getMethod("clientVersions", String.class, String.class, Predicate.class, String.class)
                .invoke(null, namespace, path, original, "absent-token");
    }

    @Test
    public void bothOwnedChannelsAcceptAbsenceAndKeepInstalledVersionChecks() throws Exception {
        for (String path : new String[]{"oelib_c2s", "oelib_s2c"}) {
            Predicate<String> versions = versions("apricityui", path, "1"::equals);
            assertTrue(versions.test("absent-token"));
            assertTrue(versions.test("1"));
            assertFalse(versions.test("2"));
            assertFalse(versions.test("vanilla-token"));
            assertFalse(versions.test(null));
        }
    }

    @Test
    public void unrelatedChannelsKeepTheOriginalPredicateIdentity() throws Exception {
        Predicate<String> original = "1"::equals;
        assertSame(original, versions("other", "oelib_c2s", original));
        assertSame(original, versions("apricityui", "other", original));
        assertSame(original, versions("apricityui", "oelib_c2s_extra", original));
    }

    @Test
    public void originalVersionPolicyRemainsAuthoritative() throws Exception {
        Predicate<String> versions = versions("apricityui", "oelib_s2c", "custom-protocol"::equals);
        assertTrue(versions.test("custom-protocol"));
        assertFalse(versions.test("1"));
    }

    @Test
    public void officialBinaryMatchesWithoutInitializingVendorClasses() throws Exception {
        assertTrue(matches(official()));
    }

    @Test
    public void changedRegistrationSiteIsRejected() throws Exception {
        ClassNode node = official();
        node.methods.removeIf(method -> method.name.equals("lambda$clientboundChannelOf$3"));
        assertFalse(matches(node));
    }

    @Test
    public void wrongProtocolContractIsRejected() throws Exception {
        ClassNode node = official();
        node.fields.stream().filter(field -> field.name.equals("PROTOCOL_VERSION")).forEach(field -> field.value = "2");
        assertFalse(matches(node));
    }

    @Test
    public void controlOriginalPredicateRejectsMissingAndWrongVersions() {
        Predicate<String> original = "1"::equals;
        assertFalse(original.test("absent-token"));
        assertFalse(original.test("2"));
        assertTrue(original.test("1"));
    }

    @Test
    public void registrationWrapsTheClientPredicateAndLeavesTheServerPredicateUntouched() throws Exception {
        ClassNode node = resource("/xin/vanilla/banira/internal/mixin/compat/aui/AuiNetworkManagerMixin.class");
        var method = node.methods.stream().filter(value -> value.name.equals("banira$optionalServer")).findFirst().orElseThrow();
        MethodInsnNode registration = null, policy = null;
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals("net/minecraftforge/network/NetworkRegistry") && call.name.equals("newSimpleChannel"))
                    registration = call;
                if (call.owner.equals("xin/vanilla/banira/internal/compat/aui/AuiOptionalServerChannels"))
                    policy = call;
            }
        assertNotNull(registration);
        AbstractInsnNode server = previous(registration);
        assertTrue("The final registration argument must be the untouched server predicate", server instanceof VarInsnNode);
        assertEquals(Opcodes.ALOAD, server.getOpcode());
        assertEquals(3, ((VarInsnNode) server).var);
        assertNotNull(policy);
        AbstractInsnNode absent = previous(policy);
        if (absent instanceof LdcInsnNode constant) {
            assertEquals(net.minecraftforge.network.NetworkRegistry.ABSENT, constant.cst);
        } else {
            assertTrue(absent instanceof FieldInsnNode);
            assertEquals("net/minecraftforge/network/NetworkRegistry", ((FieldInsnNode) absent).owner);
            assertEquals("ABSENT", ((FieldInsnNode) absent).name);
            assertEquals("Ljava/lang/String;", ((FieldInsnNode) absent).desc);
        }
        AbstractInsnNode originalClient = previous(absent);
        assertTrue(originalClient instanceof VarInsnNode);
        assertEquals(Opcodes.ALOAD, originalClient.getOpcode());
        assertEquals("The policy must receive the original client predicate", 2, ((VarInsnNode) originalClient).var);
    }

    @Test
    public void forgeChecksTheServerVersionUsingItsClientPredicate() throws Exception {
        ClassNode node = resource("/net/minecraftforge/network/NetworkInstance.class");
        var method = node.methods.stream().filter(value -> value.name.equals("tryServerVersionOnClient")).findFirst().orElseThrow();
        int clientFields = 0, serverFields = 0;
        for (var instruction : method.instructions)
            if (instruction instanceof FieldInsnNode field) {
                if (field.name.equals("clientAcceptedVersions")) clientFields++;
                if (field.name.equals("serverAcceptedVersions")) serverFields++;
            }
        assertEquals(1, clientFields);
        assertEquals(0, serverFields);
    }

    private ClassNode resource(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private AbstractInsnNode previous(AbstractInsnNode instruction) {
        do {
            instruction = instruction.getPrevious();
        } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }

    private boolean matches(ClassNode node) throws Exception {
        return (boolean) policy().getMethod("matches", ClassNode.class).invoke(null, node);
    }

    private ClassNode official() throws Exception {
        try (URLClassLoader jar = new URLClassLoader(new java.net.URL[]{Path.of(System.getProperty("banira.aui.contractJar")).toUri().toURL()}, null);
             var input = jar.getResourceAsStream("com/sighs/apricityui/network/forge/NetworkManagerImpl.class")) {
            assertNotNull(input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }
}
