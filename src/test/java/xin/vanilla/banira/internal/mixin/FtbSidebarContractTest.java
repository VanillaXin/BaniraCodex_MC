package xin.vanilla.banira.internal.mixin;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public final class FtbSidebarContractTest {
    private static final String PLUGIN = "xin.vanilla.banira.internal.fabric.mixin.FabricMixinConfigPlugin";
    private static final String BUTTON = "dev.ftb.mods.ftblibrary.sidebar.SidebarButton";
    private static final String WIDGET = "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton";
    private static final String MANAGER = "dev.ftb.mods.ftblibrary.sidebar.SidebarButtonManager";
    private static final String BUTTON_MIXIN =
            "xin.vanilla.banira.internal.mixin.compat.ftblibrary.SidebarButtonMixin";
    private static final String WIDGET_MIXIN =
            "xin.vanilla.banira.internal.mixin.compat.ftblibrary.SidebarGroupGuiButtonMixin";

    @Test
    public void fixedNativePairRetainsItsActualVersionContract() throws Exception {
        Fixture fixture = fixedFixture();
        ClassNode widget = fixture.node(WIDGET);
        assertTrue(widget.methods.stream().anyMatch(method -> method.name.equals("method_25394")
                && method.desc.equals("(Lnet/minecraft/class_4587;IIF)V")));
        assertTrue(widget.fields.stream().anyMatch(field -> field.name.equals("lastDrawnArea")
                && field.desc.equals("Lnet/minecraft/class_768;")));
        assertTrue(fixture.node(MANAGER).fields.stream().anyMatch(field ->
                field.name.equals("groups") && field.desc.equals("Ljava/util/List;")));
        assertTrue(fixture.node(BUTTON).methods.stream().anyMatch(method ->
                method.name.equals("onClicked") && method.desc.equals("(Z)V")));
        IMixinConfigPlugin plugin = fixture.plugin();
        assertTrue(plugin.shouldApplyMixin(BUTTON, BUTTON_MIXIN));
        assertTrue(plugin.shouldApplyMixin(WIDGET, WIDGET_MIXIN));
    }

    @Test
    public void missingWidgetRejectsTheSurvivingButton() throws Exception {
        Fixture fixture = fixedFixture();
        fixture.hide(WIDGET);
        assertFalse(fixture.plugin().shouldApplyMixin(BUTTON, BUTTON_MIXIN));
    }

    @Test
    public void missingButtonRejectsTheSurvivingWidget() throws Exception {
        Fixture fixture = fixedFixture();
        fixture.hide(BUTTON);
        assertFalse(fixture.plugin().shouldApplyMixin(WIDGET, WIDGET_MIXIN));
    }

    @Test
    public void missingClickSignatureRejectsBothTargets() throws Exception {
        Fixture fixture = fixedFixture();
        ClassNode button = fixture.node(BUTTON);
        assertTrue(button.methods.removeIf(method -> method.name.equals("onClicked")
                && method.desc.equals("(Z)V")));
        fixture.replace(button);
        assertRejected(fixture);
    }

    @Test
    public void missingNativeRenderSignatureRejectsBothTargets() throws Exception {
        Fixture fixture = fixedFixture();
        ClassNode widget = fixture.node(WIDGET);
        assertTrue(widget.methods.removeIf(method -> method.name.equals("method_25394")
                && method.desc.equals("(Lnet/minecraft/class_4587;IIF)V")));
        fixture.replace(widget);
        assertRejected(fixture);
    }

    @Test
    public void changedReservedAreaTypeRejectsBothTargets() throws Exception {
        Fixture fixture = fixedFixture();
        ClassNode widget = fixture.node(WIDGET);
        assertTrue(widget.fields.stream().anyMatch(field -> field.name.equals("lastDrawnArea")));
        widget.fields.stream().filter(field -> field.name.equals("lastDrawnArea"))
                .forEach(field -> field.desc = "Ljava/lang/Object;");
        fixture.replace(widget);
        assertRejected(fixture);
    }

    @Test
    public void changedManagerStorageTypeRejectsBothTargets() throws Exception {
        Fixture fixture = fixedFixture();
        ClassNode manager = fixture.node(MANAGER);
        assertTrue(manager.fields.stream().anyMatch(field -> field.name.equals("groups")));
        manager.fields.stream().filter(field -> field.name.equals("groups"))
                .forEach(field -> field.desc = "Ljava/lang/Object;");
        fixture.replace(manager);
        assertRejected(fixture);
    }


    @Test
    public void nativeTargetsWithoutAppliedCallbacksCannotAuthorizeTakeover() throws Exception {
        Fixture fixture = fixedFixture();
        IMixinConfigPlugin plugin = fixture.plugin();
        plugin.postApply(BUTTON, fixture.node(BUTTON), BUTTON_MIXIN, null);
        plugin.postApply(WIDGET, fixture.node(WIDGET), WIDGET_MIXIN, null);
        assertFalse(hooksApplied(fixture));
    }

    @Test
    public void completeCallEdgesAuthorizeBothNamespacesAndALostClickRevokesTakeover() throws Exception {
        for (Fixture fixture : new Fixture[]{fixedFixture(), namedFixture()}) {
            IMixinConfigPlugin plugin = fixture.plugin();
            ClassNode button = fixture.node(BUTTON);
            ClassNode widget = fixture.node(WIDGET);
            String render = widget.methods.stream().anyMatch(method -> method.name.equals("method_25394"))
                    ? "method_25394" : "render";
            String press = widget.methods.stream().anyMatch(method -> method.name.equals("method_25306"))
                    ? "method_25306" : "onPress";
            wireHandler(fixture, button, BUTTON_MIXIN, "getLangKey", "banira$getLangKey");
            wireHandler(fixture, button, BUTTON_MIXIN, "onClicked", "banira$onClicked");
            wireHandler(fixture, widget, WIDGET_MIXIN, render, "banira$render");
            plugin.postApply(BUTTON, button, BUTTON_MIXIN, null);
            plugin.postApply(WIDGET, widget, WIDGET_MIXIN, null);
            assertFalse("A render-only integration must retain native controls", hooksApplied(fixture));

            wireHandler(fixture, widget, WIDGET_MIXIN, press, "banira$onPress");
            plugin.postApply(WIDGET, widget, WIDGET_MIXIN, null);
            assertTrue(hooksApplied(fixture));
            assertNativeSelector(fixture, "banira$render", "method_25394(Lnet/minecraft/class_4587;IIF)V");
            assertNativeSelector(fixture, "banira$render", "render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V");
            assertNativeSelector(fixture, "banira$onPress", "method_25306()V");
            assertNativeSelector(fixture, "banira$onPress", "onPress()V");

            widget.methods.removeIf(method -> method.name.equals("banira$onPress"));
            plugin.postApply(WIDGET, widget, WIDGET_MIXIN, null);
            assertFalse("A lost call edge must revoke takeover in either namespace", hooksApplied(fixture));
        }
    }

    @Test
    public void mismatchedPostApplyTargetIdentityCannotAuthorizeTakeover() throws Exception {
        Fixture fixture = fixedFixture();
        IMixinConfigPlugin plugin = fixture.plugin();
        ClassNode button = fixture.node(BUTTON);
        ClassNode widget = fixture.node(WIDGET);
        wireHandler(fixture, button, BUTTON_MIXIN, "getLangKey", "banira$getLangKey");
        wireHandler(fixture, button, BUTTON_MIXIN, "onClicked", "banira$onClicked");
        wireHandler(fixture, widget, WIDGET_MIXIN, "method_25394", "banira$render");
        wireHandler(fixture, widget, WIDGET_MIXIN, "method_25306", "banira$onPress");
        plugin.postApply(BUTTON, button, BUTTON_MIXIN, null);
        plugin.postApply(WIDGET, widget, WIDGET_MIXIN, null);
        assertTrue(hooksApplied(fixture));

        plugin.postApply(BUTTON, widget, WIDGET_MIXIN, null);
        assertFalse(hooksApplied(fixture));
    }

    @Test
    public void iconBindingWritesPrivateInstanceFieldsAndRejectsInvalidBindings() throws Exception {
        Fixture fixture = fixedFixture();
        java.lang.reflect.Method bind = fixture.loadClass(
                "xin.vanilla.banira.internal.compat.FtbLibraryContract")
                .getMethod("bindIcon", Class.class, Class.class);
        java.lang.reflect.Field field = (java.lang.reflect.Field) bind.invoke(
                null, MutableIconButton.class, IconFixture.class);
        MutableIconButton button = new MutableIconButton();
        IconFixture icon = new IconFixture();
        field.set(button, icon);
        assertSame(icon, field.get(button));
        for (Class<?> invalid : new Class<?>[]{MissingIconButton.class, StaticIconButton.class,
                FinalIconButton.class, WrongIconButton.class}) {
            try {
                bind.invoke(null, invalid, IconFixture.class);
                fail("Invalid native icon binding must fail before any hosted action is published");
            } catch (java.lang.reflect.InvocationTargetException exception) {
                assertTrue(exception.getCause() instanceof NoSuchFieldException);
            }
        }
    }

    @Test
    public void bridgeBootstrapRequiresAppliedHooksAndReflectionBeforeRegistering() throws Exception {
        Fixture fixture = fixedFixture();
        ClassNode bridge = fixture.node(
                "xin.vanilla.banira.internal.fabric.compat.ftblibrary.FtbLibraryCompatibility");
        MethodNode init = bridge.methods.stream().filter(method -> method.name.equals("init"))
                .findFirst().orElseThrow(AssertionError::new);
        int applied = -1;
        int binding = -1;
        int registration = -1;
        for (int i = 0; i < init.instructions.size(); i++) {
            org.objectweb.asm.tree.AbstractInsnNode instruction = init.instructions.get(i);
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.owner.equals("xin/vanilla/banira/internal/compat/FtbLibraryContract")) {
                if (call.name.equals("hooksApplied")) applied = i;
                if (call.name.equals("bindIcon")) binding = i;
            }
            if (call.owner.equals("xin/vanilla/banira/client/gui/quickaction/ExternalInventoryButtonManager")
                    && call.name.equals("registerProvider")) registration = i;
        }
        assertTrue("Applied callbacks and native reflection must precede provider publication",
                applied >= 0 && binding > applied && registration > binding);
    }

    private static boolean hooksApplied(Fixture fixture) throws Exception {
        return (Boolean) fixture.loadClass("xin.vanilla.banira.internal.compat.FtbLibraryContract")
                .getMethod("hooksApplied").invoke(null);
    }

    static void wireHandler(Fixture fixture, ClassNode target, String mixinName,
                                     String methodName, String handlerName) throws IOException {
        // Model actual compiled handler call edges without defining optional JVM classes.
        MethodNode handler = fixture.node(mixinName).methods.stream().filter(method ->
                method.name.equals(handlerName)).findFirst().orElseThrow(AssertionError::new);
        MethodNode method = target.methods.stream().filter(value -> value.name.equals(methodName))
                .findFirst().orElseThrow(AssertionError::new);
        target.methods.add(handler);
        org.objectweb.asm.tree.InsnList edge = new org.objectweb.asm.tree.InsnList();
        edge.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
        for (Type argument : Type.getArgumentTypes(handler.desc)) {
            int opcode;
            switch (argument.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY: opcode = Opcodes.ACONST_NULL; break;
                case Type.FLOAT: opcode = Opcodes.FCONST_0; break;
                case Type.DOUBLE: opcode = Opcodes.DCONST_0; break;
                case Type.LONG: opcode = Opcodes.LCONST_0; break;
                default: opcode = Opcodes.ICONST_0;
            }
            edge.add(new InsnNode(opcode));
        }
        edge.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.name,
                handler.name, handler.desc, false));
        method.instructions.insert(edge);
    }

    private static void assertNativeSelector(Fixture fixture, String handlerName, String selector)
            throws IOException {
        MethodNode handler = fixture.node(WIDGET_MIXIN).methods.stream().filter(method ->
                method.name.equals(handlerName)).findFirst().orElseThrow(AssertionError::new);
        assertTrue(handler.visibleAnnotations.stream()
                .filter(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;"))
                .anyMatch(annotation -> {
                    for (int i = 0; i < annotation.values.size(); i += 2) {
                        if (annotation.values.get(i).equals("method")) {
                            return ((java.util.List<?>) annotation.values.get(i + 1)).contains(selector);
                        }
                    }
                    return false;
                }));
    }

    public static final class IconFixture {
    }

    private static final class MutableIconButton {
        private IconFixture icon;
    }

    private static final class MissingIconButton {
    }

    private static final class StaticIconButton {
        private static IconFixture icon;
    }

    private static final class FinalIconButton {
        private final IconFixture icon = new IconFixture();
    }

    private static final class WrongIconButton {
        private Object icon;
    }


    @Test
    public void mappedNativePairRetainsTheActualDevelopmentNamespace() throws Exception {
        Fixture fixture = namedFixture();
        ClassNode widget = fixture.node(WIDGET);
        assertTrue(widget.methods.stream().anyMatch(method -> method.name.equals("render")
                && method.desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V")));
        assertTrue(widget.fields.stream().anyMatch(field -> field.name.equals("lastDrawnArea")
                && field.desc.equals("Lnet/minecraft/client/renderer/Rect2i;")));
        IMixinConfigPlugin plugin = fixture.plugin();
        assertTrue(plugin.shouldApplyMixin(BUTTON, BUTTON_MIXIN));
        assertTrue(plugin.shouldApplyMixin(WIDGET, WIDGET_MIXIN));
    }

    @Test
    public void unrelatedExistingClassCannotQualifyForAnFtbMixin() throws Exception {
        IMixinConfigPlugin plugin = fixedFixture().plugin();
        assertFalse(plugin.shouldApplyMixin("java.lang.String", BUTTON_MIXIN));
        assertFalse(plugin.shouldApplyMixin("java.lang.String", WIDGET_MIXIN));
    }

    private static Fixture namedFixture() throws IOException {
        Path root = Paths.get(System.getProperty("user.dir"), ".gradle", "loom-cache", "remapped_mods");
        if (Files.isDirectory(root)) {
            try (Stream<Path> files = Files.walk(root, 12)) {
                Path artifact = files.filter(path -> path.getFileName().toString()
                        .startsWith("ftb-library-fabric-438495-")
                        && path.getFileName().toString().endsWith("-4661833.jar"))
                        .sorted().findFirst().orElse(null);
                if (artifact != null) return new Fixture(artifact);
            }
        }
        throw new IOException("Missing fixed Loom named FTB artifact 4661833 under " + root);
    }

    private static void assertRejected(Fixture fixture) throws Exception {
        IMixinConfigPlugin plugin = fixture.plugin();
        assertFalse("A peer contract failure must disable the whole FTB group",
                plugin.shouldApplyMixin(BUTTON, BUTTON_MIXIN));
        assertFalse(plugin.shouldApplyMixin(WIDGET, WIDGET_MIXIN));
    }

    private static Fixture fixedFixture() throws IOException {
        return new Fixture("ftb-library-fabric-438495", "4661833");
    }

    // Exposes installed-mod identity and the raw Jar namespace without starting Fabric Loader.
    public static Object installedApi(Class<?> api) {
        return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
            if (method.getName().equals("isModLoaded")) return "ftblibrary".equals(args[0]);
            if (method.getName().equals("getMappingResolver")) {
                Class<?> resolver = method.getReturnType();
                return Proxy.newProxyInstance(resolver.getClassLoader(), new Class<?>[]{resolver},
                        (mapping, operation, values) -> {
                            String name = operation.getName();
                            if (name.equals("getCurrentRuntimeNamespace")) return "intermediary";
                            if (name.equals("getNamespaces")) return Arrays.asList("intermediary", "named");
                            if (name.equals("mapClassName") || name.equals("unmapClassName")) return values[1];
                            if (name.equals("mapMethodName") || name.equals("mapFieldName")) return values[2];
                            throw new UnsupportedOperationException(name);
                        });
            }
            if (method.getName().equals("toString")) return "Installed FTB fixture";
            throw new UnsupportedOperationException(method.getName());
        });
    }

    static final class Fixture extends ClassLoader {
        private static final String FABRIC_API = "net.fabricmc.loader.api.FabricLoader";
        private final Path[] artifacts;
        private final Map<String, byte[]> overrides = new HashMap<>();

        Fixture(String... coordinates) throws IOException {
            super(FtbSidebarContractTest.class.getClassLoader());
            artifacts = new Path[coordinates.length / 2];
            for (int i = 0; i < artifacts.length; i++) {
                artifacts[i] = fixedJar(coordinates[i * 2], coordinates[i * 2 + 1]);
            }
        }

        private Fixture(Path artifact) {
            super(FtbSidebarContractTest.class.getClassLoader());
            artifacts = new Path[]{artifact};
        }

        ClassNode node(String className) throws IOException {
            try (InputStream input = getResourceAsStream(resource(className))) {
                assertNotNull("Missing fixed native resource " + className, input);
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, 0);
                return node;
            }
        }

        void hide(String className) {
            overrides.put(resource(className), null);
        }

        void replace(ClassNode node) {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            overrides.put(node.name + ".class", writer.toByteArray());
        }

        IMixinConfigPlugin plugin() throws ReflectiveOperationException {
            return (IMixinConfigPlugin) loadClass(PLUGIN).getDeclaredConstructor().newInstance();
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(PLUGIN) && !name.startsWith(PLUGIN + "$")
                    && !name.startsWith("xin.vanilla.banira.internal.compat.") && !name.equals(FABRIC_API)) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                try (InputStream input = getParent().getResourceAsStream(resource(name))) {
                    if (input == null) throw new ClassNotFoundException(name);
                    byte[] code = readBytes(input);
                    if (name.equals(FABRIC_API)) code = installedFabricApi(code);
                    loaded = defineClass(name, code, 0, code.length);
                } catch (IOException exception) {
                    throw new ClassNotFoundException(name, exception);
                }
            }
            if (resolve) resolveClass(loaded);
            return loaded;
        }

        @Override
        public URL getResource(String name) {
            if (overrides.containsKey(name)) return memory(name, overrides.get(name));
            if (!name.startsWith("dev/ftb/mods/ftblibrary/") && !name.startsWith("org/anti_ad/mc/")) {
                return super.getResource(name);
            }
            for (Path artifact : artifacts) {
                try (JarFile jar = new JarFile(artifact.toFile())) {
                    JarEntry entry = jar.getJarEntry(name);
                    if (entry != null) {
                        try (InputStream input = jar.getInputStream(entry)) {
                            return memory(name, readBytes(input));
                        }
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot read fixed native artifact " + artifact, exception);
                }
            }
            return null;
        }

        private static byte[] installedFabricApi(byte[] code) {
            ClassNode node = new ClassNode();
            new ClassReader(code).accept(node, 0);
            MethodNode instance = node.methods.stream().filter(method -> method.name.equals("getInstance")
                    && method.desc.equals("()Lnet/fabricmc/loader/api/FabricLoader;"))
                    .findFirst().orElseThrow(AssertionError::new);
            instance.instructions.clear();
            instance.tryCatchBlocks.clear();
            if (instance.localVariables != null) instance.localVariables.clear();
            instance.instructions.add(new LdcInsnNode(Type.getObjectType("net/fabricmc/loader/api/FabricLoader")));
            instance.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "xin/vanilla/banira/internal/mixin/FtbSidebarContractTest", "installedApi",
                    "(Ljava/lang/Class;)Ljava/lang/Object;", false));
            instance.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.CHECKCAST,
                    "net/fabricmc/loader/api/FabricLoader"));
            instance.instructions.add(new InsnNode(Opcodes.ARETURN));
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        }

        private static Path fixedJar(String artifact, String version) throws IOException {
            Set<Path> homes = new LinkedHashSet<>();
            String property = System.getProperty("gradle.user.home");
            String environment = System.getenv("GRADLE_USER_HOME");
            if (property != null) homes.add(Paths.get(property));
            if (environment != null) homes.add(Paths.get(environment));
            homes.add(Paths.get(System.getProperty("user.home"), ".gradle"));
            String fileName = artifact + "-" + version + ".jar";
            for (Path home : homes) {
                Path root = home.resolve("caches/modules-2/files-2.1/curse.maven").resolve(artifact).resolve(version);
                if (!Files.isDirectory(root)) continue;
                try (Stream<Path> paths = Files.walk(root, 2)) {
                    Path jar = paths.filter(path -> path.getFileName().toString().equals(fileName))
                            .sorted().findFirst().orElse(null);
                    if (jar != null) return jar;
                }
            }
            throw new IOException("Fixed native artifact is not cached: " + artifact + ":" + version);
        }

        private static URL memory(String name, byte[] code) {
            if (code == null) return null;
            try {
                return new URL(null, "memory:/" + name, new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) {
                        return new URLConnection(url) {
                            @Override
                            public void connect() {
                            }

                            @Override
                            public InputStream getInputStream() {
                                return new ByteArrayInputStream(code);
                            }
                        };
                    }
                });
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        private static String resource(String name) {
            return name.replace('.', '/') + ".class";
        }

        private static byte[] readBytes(InputStream input) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }
}
