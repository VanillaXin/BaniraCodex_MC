package xin.vanilla.banira.internal.mixin;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the loader-independent optional-integration guard.
 */
public final class OptionalCompatibilityMixinPluginTest {
    private static final String FTB_BUTTON = "dev.ftb.mods.ftblibrary.sidebar.SidebarButton";
    private static final String FTB_GROUP = "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton";
    private static final String BUTTON_MIXIN =
            "xin.vanilla.banira.internal.mixin.compat.ftblibrary.SidebarButtonMixin";
    private static final String GROUP_MIXIN =
            "xin.vanilla.banira.internal.mixin.compat.ftblibrary.SidebarGroupGuiButtonMixin";
    private final OptionalCompatibilityMixinPlugin plugin = new OptionalCompatibilityMixinPlugin();

    @Test
    public void keepsCoreMixinsWithoutOptionalTargets() {
        assertTrue(plugin.shouldApplyMixin("java.lang.String",
                "xin.vanilla.banira.internal.mixin.injections.ServerPlayerMixin"));
    }

    @Test
    public void skipsOptionalMixinWhenItsTargetIsMissing() {
        assertFalse(plugin.shouldApplyMixin("missing.optional.JeiButton",
                "xin.vanilla.banira.internal.mixin.compat.jei.BookmarkButtonMixin"));
        assertFalse(plugin.shouldApplyMixin("missing.optional.IpnButton",
                "xin.vanilla.banira.internal.mixin.compat.ipn.IpnButtonMixin"));
    }

    @Test
    public void acceptsTheFixedNativeFtbTargetPair() throws Exception {
        nativeClass(FTB_BUTTON);
        nativeClass(FTB_GROUP);
        IMixinConfigPlugin isolated = isolatedPlugin(Collections.emptyMap());

        assertTrue(isolated.shouldApplyMixin(FTB_BUTTON, BUTTON_MIXIN));
        assertTrue(isolated.shouldApplyMixin(FTB_GROUP, GROUP_MIXIN));
    }

    @Test
    public void rejectsTheWholeFtbGroupWhenOnlyTheGroupWidgetIsMissing() throws Exception {
        nativeClass(FTB_BUTTON);
        IMixinConfigPlugin isolated = isolatedPlugin(
                Collections.singletonMap(resource(FTB_GROUP), null));

        assertFalse("A surviving button target must not enable half of the FTB integration",
                isolated.shouldApplyMixin(FTB_BUTTON, BUTTON_MIXIN));
        assertFalse(isolated.shouldApplyMixin(FTB_GROUP, GROUP_MIXIN));
    }

    @Test
    public void rejectsTheWholeFtbGroupWhenTheClickSignatureChanges() throws Exception {
        ClassNode button = nativeClass(FTB_BUTTON);
        assertTrue("The fixed native dependency must contain the original click contract",
                button.methods.removeIf(method -> method.name.equals("onClicked")
                        && method.desc.equals("(Z)V")));
        IMixinConfigPlugin isolated = isolatedPlugin(
                Collections.singletonMap(resource(FTB_BUTTON), bytes(button)));

        assertFalse("The unchanged widget must not take over without a working button contract",
                isolated.shouldApplyMixin(FTB_GROUP, GROUP_MIXIN));
        assertFalse(isolated.shouldApplyMixin(FTB_BUTTON, BUTTON_MIXIN));
    }

    @Test
    public void rejectsTheWholeFtbGroupWhenTheReservedAreaFieldChangesType() throws Exception {
        ClassNode group = nativeClass(FTB_GROUP);
        assertTrue(group.fields.stream().anyMatch(field -> field.name.equals("lastDrawnArea")));
        group.fields.stream().filter(field -> field.name.equals("lastDrawnArea"))
                .forEach(field -> field.desc = "Ljava/lang/Object;");
        IMixinConfigPlugin isolated = isolatedPlugin(
                Collections.singletonMap(resource(FTB_GROUP), bytes(group)));

        assertFalse("Class presence is insufficient when a peer field contract has changed",
                isolated.shouldApplyMixin(FTB_BUTTON, BUTTON_MIXIN));
        assertFalse(isolated.shouldApplyMixin(FTB_GROUP, GROUP_MIXIN));
    }

    @Test
    public void rejectsTheWholeFtbGroupWhenManagerGroupsAreNoLongerPublic() throws Exception {
        String managerName = "dev.ftb.mods.ftblibrary.sidebar.SidebarButtonManager";
        ClassNode manager = nativeClass(managerName);
        assertTrue(manager.fields.stream().anyMatch(field -> field.name.equals("groups")));
        manager.fields.stream().filter(field -> field.name.equals("groups"))
                .forEach(field -> field.access = (field.access & ~Opcodes.ACC_PUBLIC) | Opcodes.ACC_PRIVATE);
        IMixinConfigPlugin isolated = isolatedPlugin(
                Collections.singletonMap(resource(managerName), bytes(manager)));

        assertFalse(isolated.shouldApplyMixin(FTB_BUTTON, BUTTON_MIXIN));
        assertFalse(isolated.shouldApplyMixin(FTB_GROUP, GROUP_MIXIN));
    }

    @Test
    public void acceptsRuntimeSrgWidgetMethodsWithTheSamePoseStackContract() throws Exception {
        ClassNode widget = nativeClass(FTB_GROUP);
        MethodNode render = widget.methods.stream().filter(method ->
                        (method.name.equals("render") || method.name.equals("m_6305_"))
                                && method.desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
                .findFirst().orElseThrow(AssertionError::new);
        MethodNode press = widget.methods.stream().filter(method ->
                        (method.name.equals("onPress") || method.name.equals("m_5691_"))
                                && method.desc.equals("()V"))
                .findFirst().orElseThrow(AssertionError::new);
        render.name = "m_6305_";
        press.name = "m_5691_";
        IMixinConfigPlugin isolated = isolatedPlugin(
                Collections.singletonMap(resource(FTB_GROUP), bytes(widget)));

        assertTrue(isolated.shouldApplyMixin(FTB_BUTTON, BUTTON_MIXIN));
        assertTrue(isolated.shouldApplyMixin(FTB_GROUP, GROUP_MIXIN));
        assertWidgetSelector("banira$render", "m_6305_(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V");
        assertWidgetSelector("banira$onPress", "m_5691_()V");
    }

    @Test
    public void unappliedMixinCallbacksCannotAuthorizeTakeover() throws Exception {
        IMixinConfigPlugin isolated = isolatedPlugin(Collections.emptyMap());

        isolated.postApply(FTB_BUTTON, nativeClass(FTB_BUTTON), BUTTON_MIXIN, null);
        isolated.postApply(FTB_GROUP, nativeClass(FTB_GROUP), GROUP_MIXIN, null);

        assertFalse(hooksApplied(isolated));
    }

    @Test
    public void requiresBothAppliedTargetsAndEveryCallbackBeforeAuthorizingTakeover() throws Exception {
        IMixinConfigPlugin isolated = isolatedPlugin(Collections.emptyMap());
        ClassNode button = nativeClass(FTB_BUTTON);
        ClassNode widget = nativeClass(FTB_GROUP);
        wireHandler(button, BUTTON_MIXIN, "getLangKey", "banira$getLangKey");
        wireHandler(button, BUTTON_MIXIN, "onClicked", "banira$onClicked");
        String renderName = widget.methods.stream().filter(method ->
                        method.name.equals("render") || method.name.equals("m_6305_"))
                .findFirst().orElseThrow(AssertionError::new).name;
        String pressName = widget.methods.stream().filter(method ->
                        method.name.equals("onPress") || method.name.equals("m_5691_"))
                .findFirst().orElseThrow(AssertionError::new).name;
        wireHandler(widget, GROUP_MIXIN, renderName, "banira$render");
        isolated.postApply(FTB_BUTTON, button, BUTTON_MIXIN, null);
        isolated.postApply(FTB_GROUP, widget, GROUP_MIXIN, null);
        assertFalse("A render hook without the click hook cannot authorize takeover", hooksApplied(isolated));

        wireHandler(widget, GROUP_MIXIN, pressName, "banira$onPress");
        isolated.postApply(FTB_GROUP, widget, GROUP_MIXIN, null);
        assertTrue(hooksApplied(isolated));

        widget.methods.removeIf(method -> method.name.equals("banira$onPress"));
        isolated.postApply(FTB_GROUP, widget, GROUP_MIXIN, null);
        assertFalse("Lost callbacks must revoke the previously recorded readiness", hooksApplied(isolated));
    }

    private static void assertWidgetSelector(String handlerName, String selector) throws IOException {
        MethodNode handler = nativeClass(GROUP_MIXIN).methods.stream()
                .filter(method -> method.name.equals(handlerName)).findFirst().orElseThrow(AssertionError::new);
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

    private static boolean hooksApplied(IMixinConfigPlugin isolated) throws Exception {
        Class<?> contract = isolated.getClass().getClassLoader()
                .loadClass("xin.vanilla.banira.internal.compat.FtbLibraryContract");
        return (Boolean) contract.getMethod("hooksApplied").invoke(null);
    }

    private static void wireHandler(ClassNode target, String mixinName, String methodName,
                                    String handlerName) throws IOException {
        // Model postApply call edges only; these nodes are never defined as JVM classes.
        MethodNode handler = nativeClass(mixinName).methods.stream()
                .filter(method -> method.name.equals(handlerName)).findFirst().orElseThrow(AssertionError::new);
        MethodNode method = target.methods.stream().filter(value -> value.name.equals(methodName))
                .findFirst().orElseThrow(AssertionError::new);
        target.methods.add(handler);
        method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.name,
                handler.name, handler.desc, false));
    }

    private static ClassNode nativeClass(String name) throws IOException {
        try (InputStream input = OptionalCompatibilityMixinPluginTest.class.getClassLoader()
                .getResourceAsStream(resource(name))) {
            org.junit.Assert.assertNotNull("Missing fixed native test dependency: " + name, input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static String resource(String name) {
        return name.replace('.', '/') + ".class";
    }

    private static IMixinConfigPlugin isolatedPlugin(Map<String, byte[]> overrides) throws Exception {
        ClassLoader parent = OptionalCompatibilityMixinPluginTest.class.getClassLoader();
        // Isolate production guard state while exposing modified native resources without defining them.
        ClassLoader loader = new ClassLoader(parent) {
            @Override
            protected synchronized Class<?> loadClass(String name, boolean resolve)
                    throws ClassNotFoundException {
                if (!name.startsWith("xin.vanilla.banira.internal.mixin.")
                        && !name.startsWith("xin.vanilla.banira.internal.compat.")) {
                    return super.loadClass(name, resolve);
                }
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (InputStream input = parent.getResourceAsStream(resource(name))) {
                        if (input == null) throw new ClassNotFoundException(name);
                        byte[] code = input.readAllBytes();
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
                if (!overrides.containsKey(name)) return super.getResource(name);
                byte[] code = overrides.get(name);
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
        };
        return (IMixinConfigPlugin) loader.loadClass(OptionalCompatibilityMixinPlugin.class.getName())
                .getDeclaredConstructor().newInstance();
    }
}
