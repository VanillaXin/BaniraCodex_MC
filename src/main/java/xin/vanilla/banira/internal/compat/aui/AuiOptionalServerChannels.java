package xin.vanilla.banira.internal.compat.aui;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Predicate;

public final class AuiOptionalServerChannels {
    private static final String CHANNEL = "(Ljava/lang/String;)Lnet/minecraftforge/network/simple/SimpleChannel;";
    private static final String REGISTER = "(Lnet/minecraft/resources/ResourceLocation;Ljava/util/function/Supplier;"
            + "Ljava/util/function/Predicate;Ljava/util/function/Predicate;)Lnet/minecraftforge/network/simple/SimpleChannel;";

    private AuiOptionalServerChannels() { }

    public static Predicate<String> clientVersions(String namespace, String path, Predicate<String> original, String absent) {
        if (!"apricityui".equals(namespace) || !("oelib_c2s".equals(path) || "oelib_s2c".equals(path))) return original;
        return version -> absent.equals(version) || original.test(version);
    }

    public static boolean matches(ClassNode node) {
        return node.name.equals("com/sighs/apricityui/network/forge/NetworkManagerImpl")
                && node.fields.stream().anyMatch(field -> field.name.equals("PROTOCOL_VERSION")
                && field.desc.equals("Ljava/lang/String;") && "1".equals(field.value))
                && registration(node, "lambda$serverboundChannelOf$1", "oelib_c2s")
                && registration(node, "lambda$clientboundChannelOf$3", "oelib_s2c");
    }

    private static boolean registration(ClassNode node, String name, String path) {
        var methods = node.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(CHANNEL)
                && (method.access & Opcodes.ACC_STATIC) != 0).toList();
        if (methods.size() != 1) return false;
        MethodNode method = methods.get(0);
        int calls = 0, protocolConstants = 0, channelConstants = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC
                    && call.owner.equals("net/minecraftforge/network/NetworkRegistry")
                    && call.name.equals("newSimpleChannel") && call.desc.equals(REGISTER)) calls++;
            if (instruction instanceof LdcInsnNode constant) {
                if ("1".equals(constant.cst)) protocolConstants++;
                if (path.equals(constant.cst)) channelConstants++;
            }
        }
        return calls == 1 && protocolConstants == 2 && channelConstants == 1;
    }
}
