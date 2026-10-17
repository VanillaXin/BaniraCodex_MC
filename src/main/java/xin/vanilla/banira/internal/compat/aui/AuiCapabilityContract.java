package xin.vanilla.banira.internal.compat.aui;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AuiCapabilityContract {
    private static final String ROOT = "com/sighs/apricityui/";
    private static final String DOCUMENT = "L" + ROOT + "init/Document;";
    private static final String ELEMENT = "L" + ROOT + "init/Element;";
    private static final List<Member> MEMBERS = members();

    private AuiCapabilityContract() {
    }

    public static Result inspect(ClassLoader loader) {
        if (loader == null) return new Result(false, "Missing class loader");
        try {
            Map<String, ClassNode> classes = new LinkedHashMap<>();
            for (Member member : MEMBERS) {
                ClassNode owner = classes.get(member.owner);
                if (owner == null) {
                    try (InputStream input = loader.getResourceAsStream(member.owner + ".class")) {
                        if (input == null) return new Result(false, "Missing " + member.owner);
                        owner = new ClassNode();
                        new ClassReader(input).accept(owner, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                    }
                    if (!member.owner.equals(owner.name) || (owner.access & Opcodes.ACC_PUBLIC) == 0
                            || owner.version > Opcodes.V17) {
                        return new Result(false, "Incompatible class " + member.owner);
                    }
                    classes.put(member.owner, owner);
                }
                boolean found = false;
                for (MethodNode method : owner.methods) {
                    if (member.name.equals(method.name) && member.descriptor.equals(method.desc)
                            && (method.access & Opcodes.ACC_PUBLIC) != 0
                            && (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0
                            && ((method.access & Opcodes.ACC_STATIC) != 0) == member.isStatic) {
                        found = true;
                        break;
                    }
                }
                if (!found) return new Result(false, "Missing contract " + member.key());
            }
            return new Result(true, "");
        } catch (IOException | RuntimeException | LinkageError failure) {
            return new Result(false, failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    static List<Member> requiredMembers() {
        return MEMBERS;
    }

    private static List<Member> members() {
        List<Member> result = new ArrayList<>();
        add(result, "screen/ApricityScreen", "<init>", "(Ljava/lang/String;)V");
        add(result, "screen/ApricityScreen", "getLinkedDocument", "()" + DOCUMENT);
        add(result, "screen/ApricityScreen", "setPauseGame", "(Z)L" + ROOT + "screen/ApricityScreen;");
        add(result, "screen/ApricityScreen", "setShowDefaultBackground", "(Z)L" + ROOT + "screen/ApricityScreen;");
        result.add(new Member(ROOT + "init/Document", "create", "(Ljava/lang/String;)" + DOCUMENT, true));
        add(result, "init/Document", "remove", "()V");
        add(result, "init/Document", "refresh", "()V");
        add(result, "init/Document", "setManuallyRendered", "(Z)V");
        add(result, "init/Document", "setReloadPersistent", "(Z)V");
        add(result, "init/Document", "getRefreshGeneration", "()J");
        add(result, "init/Document", "isDisposed", "()Z");
        add(result, "init/Document", "getElementById", "(Ljava/lang/String;)" + ELEMENT);
        add(result, "init/Document", "createElement", "(Ljava/lang/String;)" + ELEMENT);
        add(result, "init/Document", "getViewportVersion", "()J");
        add(result, "init/Document", "documentToGuiPosition", "(L" + ROOT + "layout/Position;)L" + ROOT + "layout/Position;");
        add(result, "init/Element", "setInnerText", "(Ljava/lang/String;)V");
        add(result, "init/Element", "setAttribute", "(Ljava/lang/String;Ljava/lang/String;)V");
        add(result, "init/Element", "removeAttribute", "(Ljava/lang/String;)V");
        add(result, "init/Element", "setValue", "(Ljava/lang/String;)V");
        add(result, "init/Element", "getValue", "()Ljava/lang/String;");
        add(result, "init/Element", "appendChild", "(" + ELEMENT + ")" + ELEMENT);
        add(result, "init/Element", "remove", "()V");
        add(result, "init/Element", "getBoundingClientRect", "()L" + ROOT + "init/Element$DOMRect;");
        add(result, "init/Element", "addEventListener", "(Ljava/lang/String;Ljava/util/function/Consumer;)V");
        add(result, "init/Element", "removeEventListener", "(Ljava/lang/String;Ljava/util/function/Consumer;Z)V");
        add(result, "event/Event", "preventDefault", "()V");
        add(result, "event/Event", "stopPropagation", "()V");
        add(result, "element/Input", "<init>", "(" + DOCUMENT + ")V");
        add(result, "element/Input", "getValue", "()Ljava/lang/String;");
        add(result, "element/MinecraftElement", "requestRepaint", "()V");
        add(result, "element/Item", "<init>", "(" + DOCUMENT + ")V");
        add(result, "element/Slot", "<init>", "(" + DOCUMENT + ")V");
        add(result, "element/Slot", "clearMenuSlotBinding", "()V");
        add(result, "element/RichText", "<init>", "(" + DOCUMENT + "Ljava/lang/String;)V");
        add(result, "layout/Position", "<init>", "(DD)V");
        add(result, "layout/Size", "width", "()D");
        add(result, "layout/Size", "height", "()D");
        result.add(new Member(ROOT + "render/Base", "drawOverlayDocument",
                "(Lcom/mojang/blaze3d/vertex/PoseStack;" + DOCUMENT + ")V", true));
        return Collections.unmodifiableList(result);
    }

    private static void add(List<Member> members, String owner, String name, String descriptor) {
        members.add(new Member(ROOT + owner, name, descriptor, false));
    }

    static final class Member {
        final String owner;
        final String name;
        final String descriptor;
        final boolean isStatic;

        Member(String owner, String name, String descriptor, boolean isStatic) {
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
            this.isStatic = isStatic;
        }

        String key() {
            return owner + "#" + name + descriptor;
        }
    }

    public static final class Result {
        private final boolean compatible;
        private final String mismatch;

        private Result(boolean compatible, String mismatch) {
            this.compatible = compatible;
            this.mismatch = mismatch;
        }

        public boolean compatible() {
            return compatible;
        }

        public String mismatch() {
            return mismatch;
        }
    }
}
