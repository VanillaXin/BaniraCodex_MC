package xin.vanilla.banira.internal.compat.aui;

import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipFile;

/** Generates non-executable declarations from the pinned public binary contract. */
public final class AuiCompileApiTool {
    static final String ROOT = "com/sighs/apricityui/";
    static final String SOURCE = "8BD99EC21320A8D4ABFE65B7C913B31CFC0F29C08DFC758FF4F4557683D5D3C2";
    static final String MARKER = "META-INF/banira-aui-compile-api.properties";
    static final String[][] FIELDS = {{"init/Element", "isVisible", "Z"}, {"init/Element", "scrollHeight", "D"},
            {"init/Element$DOMRect", "x", "D"}, {"init/Element$DOMRect", "y", "D"},
            {"init/Element$DOMRect", "right", "D"}, {"init/Element$DOMRect", "bottom", "D"},
            {"layout/Position", "x", "D"}, {"layout/Position", "y", "D"}, {"event/KeyEvent", "keyCode", "I"},
            {"event/KeyEvent", "modifiers", "I"}, {"event/MouseEvent", "button", "I"}};
    private AuiCompileApiTool() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected official jar and fresh output directory");
        generate(Path.of(args[0]), Path.of(args[1]));
    }
    static String sha(byte[] bytes) throws Exception { return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    static Map<String, ClassNode> declarations(Path official) throws Exception {
        if (!sha(Files.readAllBytes(official)).equals(SOURCE)) throw new IllegalArgumentException("Unpinned official AUI input");
        Map<String, ClassNode> result = new TreeMap<>();
        try (ZipFile zip = new ZipFile(official.toFile())) {
            for (var member : AuiCapabilityContract.requiredMembers()) addClass(zip, result, member.owner);
            for (String[] field : FIELDS) addClass(zip, result, ROOT + field[0]);
            for (ClassNode node : result.values()) {
                node.methods.removeIf(method -> AuiCapabilityContract.requiredMembers().stream().noneMatch(member -> member.owner.equals(node.name)
                        && member.name.equals(method.name) && member.descriptor.equals(method.desc)));
                node.fields.removeIf(field -> Arrays.stream(FIELDS).noneMatch(required -> (ROOT + required[0]).equals(node.name)
                        && required[1].equals(field.name) && required[2].equals(field.desc)));
                node.innerClasses.removeIf(inner -> !result.containsKey(inner.name));
                for (var member : AuiCapabilityContract.requiredMembers()) if (member.owner.equals(node.name)
                        && node.methods.stream().noneMatch(method -> method.name.equals(member.name) && method.desc.equals(member.descriptor)))
                    throw new IllegalArgumentException("Missing official member " + member.key());
            }
        }
        return result;
    }
    private static void addClass(ZipFile zip, Map<String, ClassNode> classes, String name) throws IOException {
        if (name == null || !name.startsWith(ROOT) || classes.containsKey(name)) return;
        var entry = zip.getEntry(name + ".class"); if (entry == null) throw new IOException("Missing contract type " + name);
        ClassNode node = new ClassNode(); try (InputStream input = zip.getInputStream(entry)) { new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES); }
        classes.put(name, node); addClass(zip, classes, node.superName);
        for (String face : node.interfaces) addClass(zip, classes, face);
    }
    static byte[] contract(Map<String, ClassNode> nodes) {
        JsonObject root = new JsonObject(); root.addProperty("schema", 1); root.addProperty("sourceSha256", SOURCE);
        JsonObject classes = new JsonObject(); root.add("classes", classes);
        for (ClassNode node : nodes.values()) {
            JsonObject type = new JsonObject(); classes.add(node.name, type);
            type.addProperty("super", node.superName); type.addProperty("access", node.access & ~Opcodes.ACC_RECORD);
            JsonArray faces = new JsonArray(); node.interfaces.forEach(faces::add); type.add("interfaces", faces);
            JsonArray methods = new JsonArray(); type.add("methods", methods);
            for (MethodNode method : node.methods) { JsonObject value = new JsonObject(); value.addProperty("name", method.name); value.addProperty("desc", method.desc); value.addProperty("access", method.access); methods.add(value); }
            JsonArray fields = new JsonArray(); type.add("fields", fields);
            for (FieldNode field : node.fields) { JsonObject value = new JsonObject(); value.addProperty("name", field.name); value.addProperty("desc", field.desc); value.addProperty("access", field.access); fields.add(value); }
        }
        return (new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    }
    static byte[] declaration(ClassNode node, int major) {
        ClassWriter writer = new ClassWriter(0);
        boolean record = "java/lang/Record".equals(node.superName);
        writer.visit(major, node.access & ~Opcodes.ACC_RECORD, node.name, record ? null : node.signature,
                record ? "java/lang/Object" : node.superName, node.interfaces.toArray(new String[0]));
        for (InnerClassNode inner : node.innerClasses) writer.visitInnerClass(inner.name, inner.outerName, inner.innerName, inner.access & ~Opcodes.ACC_RECORD);
        for (FieldNode field : node.fields) writer.visitField(field.access, field.name, field.desc, field.signature, null).visitEnd();
        for (MethodNode method : node.methods) {
            MethodVisitor visitor = writer.visitMethod(method.access & ~(Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT), method.name, method.desc, method.signature, method.exceptions.toArray(new String[0]));
            visitor.visitCode(); visitor.visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException"); visitor.visitInsn(Opcodes.DUP);
            visitor.visitLdcInsn("Compile-only AUI declaration");
            visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false); visitor.visitInsn(Opcodes.ATHROW);
            int locals = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
            for (Type argument : Type.getArgumentTypes(method.desc)) locals += argument.getSize();
            visitor.visitMaxs(3, locals); visitor.visitEnd();
        }
        writer.visitEnd(); return writer.toByteArray();
    }
    static void generate(Path official, Path output) throws Exception {
        if (!output.isAbsolute() || Files.exists(output.resolve("api-52.jar")) || Files.exists(output.resolve("api-61.jar"))) throw new IllegalArgumentException("Fresh absolute declaration output required");
        Map<String, ClassNode> nodes = declarations(official); byte[] contract = contract(nodes); Files.createDirectories(output);
        Files.write(output.resolve("contract.json"), contract);
        for (int major : new int[]{52, 61}) {
            Manifest manifest = new Manifest(); var attributes = manifest.getMainAttributes(); attributes.putValue("Manifest-Version", "1.0");
            attributes.putValue("Banira-AUI-Compile-Only", "true"); attributes.putValue("Banira-AUI-Source-SHA256", SOURCE);
            attributes.putValue("Banira-AUI-Major", Integer.toString(major)); attributes.putValue("Banira-AUI-Contract-SHA256", sha(contract));
            try (JarOutputStream zip = new JarOutputStream(Files.newOutputStream(output.resolve("api-" + major + ".jar")), manifest)) {
                entry(zip, MARKER, ("compileOnly=true\nsource=" + SOURCE + "\nmajor=" + major + "\n").getBytes(StandardCharsets.UTF_8));
                for (ClassNode node : nodes.values()) entry(zip, node.name + ".class", declaration(node, major));
            }
        }
        fixture(output.resolve("fixture"));
    }
    private static void entry(JarOutputStream zip, String name, byte[] bytes) throws IOException {
        JarEntry entry = new JarEntry(name); entry.setTime(0); zip.putNextEntry(entry); zip.write(bytes); zip.closeEntry();
    }
    private static void fixture(Path root) throws IOException {
        Files.createDirectories(root.resolve("net/minecraft/client/gui/screens")); Files.createDirectories(root.resolve("com/mojang/blaze3d/vertex"));
        Files.writeString(root.resolve("net/minecraft/client/gui/screens/Screen.java"), "package net.minecraft.client.gui.screens; public class Screen {}", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("com/mojang/blaze3d/vertex/PoseStack.java"), "package com.mojang.blaze3d.vertex; public class PoseStack {}", StandardCharsets.UTF_8);
        StringBuilder source = new StringBuilder("public class AuiDeclarationFixture extends com.sighs.apricityui.screen.ApricityScreen { public AuiDeclarationFixture(){super(\"fixture\");} void contract(){\n");
        for (var member : AuiCapabilityContract.requiredMembers()) {
            String owner = member.owner.replace('/', '.').replace('$', '.');
            source.append(member.name.equals("<init>") ? "new " + owner : member.isStatic ? owner + "." + member.name : "((" + owner + ")null)." + member.name).append('(');
            Type[] arguments = Type.getArgumentTypes(member.descriptor);
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) source.append(','); Type argument = arguments[i];
                source.append(argument.getSort() == Type.BOOLEAN ? "false" : argument.getSort() == Type.LONG ? "0L" : argument.getSort() == Type.DOUBLE ? "0D" : argument.getSort() == Type.FLOAT ? "0F"
                        : argument.getSort() < Type.ARRAY ? "0" : "(" + argument.getClassName().replace('$', '.') + ")null");
            }
            source.append(");\n");
        }
        for (String[] field : FIELDS) source.append(field[2].equals("D") ? "double" : "int").append(" value").append(source.length()).append("=((").append((ROOT + field[0]).replace('/', '.').replace('$', '.')).append(")null).").append(field[1]).append(";\n");
        source.append("}}\n"); Files.writeString(root.resolve("AuiDeclarationFixture.java"), source, StandardCharsets.UTF_8);
    }
}
