package xin.vanilla.banira.internal.compat.aui;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class AuiCapabilityContract {
    private static final String ROOT = "com/sighs/apricityui/";
    private static final String DOCUMENT = "L" + ROOT + "init/Document;";
    private static final String ELEMENT = "L" + ROOT + "init/Element;";

    private AuiCapabilityContract() {
    }

    private static final class ClientMembers {
        private static final List<Member> VALUES = members();
    }

    public static Result inspect(ClassLoader loader) {
        if (loader == null) return new Result(false, "Missing class loader");
        try (InputStream marker = loader.getResourceAsStream("META-INF/banira-aui-compile-api.properties")) {
            if (marker != null) return new Result(false, "Compile-only AUI declarations");
        } catch (IOException failure) {
            return new Result(false, "Cannot inspect AUI declaration marker");
        }
        try (InputStream screen = loader.getResourceAsStream(ROOT + "screen/ApricityScreen.class")) {
            if (screen == null) return new Result(false, "Missing AUI screen");
        } catch (IOException failure) {
            return new Result(false, "Cannot inspect AUI screen");
        }
        try {
            Map<String, ClassNode> classes = new LinkedHashMap<>();
            for (Member member : requiredMembers()) {
                ClassNode owner = classes.get(member.owner);
                if (owner == null) {
                    try (InputStream input = loader.getResourceAsStream(member.owner + ".class")) {
                        if (input == null) return new Result(false, "Missing " + member.owner);
                        owner = new ClassNode();
                        new ClassReader(input).accept(owner, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                    }
                    if (!member.owner.equals(owner.name) || (owner.access & Opcodes.ACC_PUBLIC) == 0
                            || owner.version > Opcodes.V25) {
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
            for (String[] field : new String[][]{
                    {"init/Element", "isVisible", "Z"}, {"init/Element", "scrollHeight", "D"},
                    {"init/Element$DOMRect", "x", "D"}, {"init/Element$DOMRect", "y", "D"},
                    {"init/Element$DOMRect", "right", "D"}, {"init/Element$DOMRect", "bottom", "D"},
                    {"layout/Position", "x", "D"}, {"layout/Position", "y", "D"}, {"event/KeyEvent", "keyCode", "I"},
                    {"event/KeyEvent", "modifiers", "I"}, {"event/MouseEvent", "button", "I"}}) {
                String name = ROOT + field[0];
                ClassNode owner = classes.get(name);
                if (owner == null) {
                    try (InputStream input = loader.getResourceAsStream(name + ".class")) {
                        if (input == null) return new Result(false, "Missing " + name);
                        owner = new ClassNode();
                        new ClassReader(input).accept(owner, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                    }
                    if (!name.equals(owner.name) || (owner.access & Opcodes.ACC_PUBLIC) == 0 || owner.version > Opcodes.V25)
                        return new Result(false, "Incompatible class " + name);
                    classes.put(name, owner);
                }
                boolean found = owner.fields.stream().anyMatch(value -> value.name.equals(field[1]) && value.desc.equals(field[2])
                        && (value.access & Opcodes.ACC_PUBLIC) != 0 && (value.access & Opcodes.ACC_STATIC) == 0);
                if (!found) return new Result(false, "Missing field " + name + "#" + field[1]);
            }
            return new Result(true, "");
        } catch (IOException | RuntimeException | LinkageError failure) {
            return new Result(false, failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    static List<Member> requiredMembers() {
        return ClientMembers.VALUES;
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
        result.add(new Member(ROOT + "loader/ClientLoader", "listFinalStaticResources", "()Ljava/util/List;", true));
        add(result, "init/Document", "isDisposed", "()Z");
        add(result, "init/Document", "interceptsMouseEvents", "()Z");
        add(result, "init/Document", "applyViewport", "(Z)V");
        result.add(new Member(ROOT + "init/Document", "withContext", "(" + DOCUMENT + ")L" + ROOT + "init/Document$ContextScope;", true));
        add(result, "init/Document$ContextScope", "close", "()V");
        add(result, "init/Document", "commitPendingStyleRecalcForRender", "()Z");
        add(result, "init/Document", "commitRenderState", "()V");
        add(result, "init/Document", "getElementById", "(Ljava/lang/String;)" + ELEMENT);
        add(result, "init/Document", "createElement", "(Ljava/lang/String;)" + ELEMENT);
        add(result, "init/Document", "getElementsByTagName", "(Ljava/lang/String;)Ljava/util/List;");
        add(result, "init/Document", "getDocumentElement", "()" + ELEMENT);
        add(result, "init/Document", "getFocusedElement", "()" + ELEMENT);
        add(result, "init/Element", "setTopLayer", "(Z)V");
        add(result, "element/AbstractText", "canEditText", "()Z");
        add(result, "init/Document", "guiToDocumentPosition", "(L" + ROOT + "layout/Position;)L" + ROOT + "layout/Position;");
        add(result, "init/Node", "getChildNodes", "()Ljava/util/List;");
        add(result, "init/Node", "getParentNode", "()L" + ROOT + "init/Node;");
        add(result, "init/Document", "hitTest", "(L" + ROOT + "layout/Position;)" + ELEMENT);
        add(result, "init/Element", "isDisabled", "()Z");
        add(result, "init/Document", "getViewportVersion", "()J");
        add(result, "init/Document", "documentToGuiPosition", "(L" + ROOT + "layout/Position;)L" + ROOT + "layout/Position;");
        add(result, "init/Element", "setInnerText", "(Ljava/lang/String;)V");
        add(result, "init/Element", "setAttribute", "(Ljava/lang/String;Ljava/lang/String;)V");
        add(result, "init/Element", "getAttribute", "(Ljava/lang/String;)Ljava/lang/String;");
        add(result, "init/Element", "getStyle", "()L" + ROOT + "style/Style;");
        add(result, "style/Style", "setProperty", "(Ljava/lang/String;Ljava/lang/String;)V");
        add(result, "init/Element", "removeAttribute", "(Ljava/lang/String;)V");
        add(result, "init/Element", "setValue", "(Ljava/lang/String;)V");
        add(result, "init/Element", "getValue", "()Ljava/lang/String;");
        add(result, "init/Element", "hasAttribute", "(Ljava/lang/String;)Z");
        add(result, "init/Element", "isChecked", "()Z");
        add(result, "init/Element", "setChecked", "(Z)V");
        add(result, "init/Element", "focus", "()V");
        add(result, "init/Element", "blur", "()V");
        add(result, "init/Element", "getScrollTop", "()D");
        add(result, "init/Element", "getTargetScrollTop", "()D");
        result.add(new Member(ROOT + "layout/Box", "of", "(" + ELEMENT + ")L" + ROOT + "layout/Box;", true));
        add(result, "layout/Box", "innerSize", "()L" + ROOT + "layout/Size;");
        add(result, "init/Element", "setScrollTop", "(D)V");
        add(result, "init/Element", "getClientHeight", "()D");
        add(result, "init/Element", "appendChild", "(" + ELEMENT + ")" + ELEMENT);
        result.add(new Member(ROOT + "init/Element", "init", "(" + ELEMENT + ")" + ELEMENT, true));
        add(result, "init/Element", "remove", "()V");
        add(result, "init/Element", "getBoundingClientRect", "()L" + ROOT + "init/Element$DOMRect;");
        add(result, "init/Element", "addEventListener", "(Ljava/lang/String;Ljava/util/function/Consumer;)V");
        add(result, "init/Element", "removeEventListener", "(Ljava/lang/String;Ljava/util/function/Consumer;Z)V");
        add(result, "event/Event", "preventDefault", "()V");
        add(result, "event/Event", "stopPropagation", "()V");
        add(result, "element/Input", "<init>", "(" + DOCUMENT + ")V");
        add(result, "element/Texture", "<init>", "(" + DOCUMENT + ")V");
        add(result, "element/Input", "getValue", "()Ljava/lang/String;");
        add(result, "element/AbstractText", "getCursor", "()I");
        add(result, "element/AbstractText", "getSelectionStart", "()I");
        add(result, "element/AbstractText", "getSelectionEnd", "()I");
        add(result, "element/AbstractText", "setSelectionRange", "(II)V");
        result.add(new Member(ROOT + "ui/ColorPicker", "pickIn", "(" + DOCUMENT + ELEMENT + "Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;", true));
        result.add(new Member(ROOT + "ui/ColorPicker", "closeActive", "()V", true));
        result.add(new Member(ROOT + "ui/ColorPicker", "isOpen", "()Z", true));
        result.add(new Member(ROOT + "ui/Tooltip", "bind", "(" + ELEMENT + "Ljava/lang/String;)L" + ROOT + "ui/Tooltip$Binding;", true));
        result.add(new Member(ROOT + "ui/Tooltip", "hide", "(" + DOCUMENT + ")V", true));
        add(result, "ui/Tooltip$Binding", "close", "()V");
        result.add(new Member(ROOT + "parser/Color", "parse", "(Ljava/lang/String;)I", true));
        add(result, "element/MinecraftElement", "requestRepaint", "()V");
        add(result, "element/Item", "<init>", "(" + DOCUMENT + ")V");
        add(result, "element/Slot", "<init>", "(" + DOCUMENT + ")V");
        add(result, "element/Slot", "clearMenuSlotBinding", "()V");
        add(result, "element/RichText", "<init>", "(" + DOCUMENT + "Ljava/lang/String;)V");
        add(result, "layout/Position", "<init>", "(DD)V");
        add(result, "layout/Size", "width", "()D");
        add(result, "layout/Size", "height", "()D");
        result.add(new Member(ROOT + "render/Base", "drawOverlayDocument",
                "(" + org.objectweb.asm.Type.getDescriptor(com.mojang.blaze3d.vertex.PoseStack.class) + DOCUMENT + ")V", true));
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
