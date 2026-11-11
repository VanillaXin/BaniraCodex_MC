package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.api.client.ui.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class UiRendererPortTest {

    @Test
    public void clientBootstrapInstallsAndDrivesTheSharedPageHost() throws Exception {
        ClassNode node = bytecode("internal/client/BaniraCodexClientBootstrap");
        assertTrue(calls(node, "api/client/ui/BaniraUi", "install"));
        for (String operation : com.google.common.collect.ImmutableList.of("hudHost", "switchBackend", "reload", "tick"))
            assertTrue(operation, calls(node, "internal/client/ui/UiSessionHost", operation));
        assertTrue(calls(node, "internal/client/ui/CommonUiPages", "tick"));
        assertTrue(calls(node, "internal/client/ui/CommonUiPages", "disconnect"));
        ClassNode pages = bytecode("internal/client/ui/CommonUiPages");
        assertTrue(calls(pages, "internal/client/ui/UiSessionHost", "observeScreen"));
        assertTrue(calls(pages, "internal/client/ui/UiSessionHost", "disconnect"));
        assertTrue(calls(node, "internal/client/ui/AuiUiBackend", "reload"));
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("uiSessions") && method.desc.equals("()Lxin/vanilla/banira/internal/client/ui/UiSessionHost;")));
    }

    @Test
    public void installedDriverDelegatesPagesAndAppearanceWithoutReplacingNativeInventoryInput() throws Exception {
        ClassNode node = bytecode("internal/client/BaniraCodexClientBootstrap$1");
        for (String operation : com.google.common.collect.ImmutableList.of("open", "registerHud", "screenRemoved", "ownsScreen", "close"))
            assertTrue(operation, calls(node, "internal/client/ui/UiSessionHost", operation));
        assertTrue(calls(node, "internal/client/ui/UiContainerLayerHost", "create"));
        assertFalse(node.methods.stream().anyMatch(method -> method.name.equals("mouseClicked") || method.name.equals("slotClicked")));
        assertTrue(calls(node, "internal/client/ui/AuiHudHost", "renderManual"));
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("renderHud")
                && method.desc.equals("(Ljava/lang/String;Lcom/mojang/blaze3d/matrix/MatrixStack;)Z")));
    }

    private static ClassNode bytecode(String type) throws Exception {
        ClassNode node = new ClassNode();
        try (lombok.var input = UiRendererPortTest.class.getResourceAsStream("/xin/vanilla/banira/" + type + ".class")) {
            assertNotNull(type, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static boolean calls(ClassNode node, String owner, String name) {
        return node.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray())).anyMatch(instruction -> instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("xin/vanilla/banira/" + owner) && ((MethodInsnNode) instruction).name.equals(name));
    }

    @Test
    public void rendererResourcesRetainTheSharedBudget() throws Exception {
        assertEquals(com.google.common.collect.ImmutableList.of(), UiBoundaryChecks.resources(UiBoundaryChecks.ownResources("banira_codex"), true));
    }

    @Test
    public void retainedTemplatesHaveTheirLocalStylesAndCloseIcon() throws Exception {
        String root = "assets/apricityui/apricity/banira_codex/";
        for (String template : com.google.common.collect.ImmutableList.of("form", "confirm", "text", "debug", "navigation")) {
            String path = root + "ui/" + template + ".html";
            try (lombok.var input = getClass().getResourceAsStream("/" + path)) {
                assertNotNull(path, input);
                lombok.var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                lombok.var links = factory.newDocumentBuilder().parse(input).getElementsByTagName("link");
                for (int i = 0; i < links.getLength(); i++) {
                    String href = ((org.w3c.dom.Element) links.item(i)).getAttribute("href");
                    String resource = java.nio.file.Paths.get(path).getParent().resolve(href).normalize().toString().replace('\\', '/');
                    assertNotNull(resource, getClass().getResource("/" + resource));
                }
            }
        }
        assertNotNull(getClass().getResource("/" + root + "icons/close.png"));
    }

    @Test
    public void constructingAndReloadingBackendDoesNotResolveTheOptionalEngine() {
        AtomicInteger hosts = new AtomicInteger();
        AuiUiBackend backend = new AuiUiBackend(() -> {
            hosts.incrementAndGet();
            throw new AssertionError("Host should stay lazy");
        });
        backend.reload();
        backend.reload();
        assertEquals(0, hosts.get());
    }

    @Test
    public void unavailableEngineFallsBackToTheSameNativePageOnce() {
        assertNotNull(getClass().getClassLoader().getResource("META-INF/banira-aui-compile-api.properties"));
        assertFalse(xin.vanilla.banira.internal.compat.aui.AuiCapabilityContract.inspect(getClass().getClassLoader()).compatible());
        AtomicReference<UiSessionHost> owner = new AtomicReference<>();
        AuiUiBackend aui = new AuiUiBackend(owner::get);
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        AtomicReference<Object> displayed = new AtomicReference<>();
        UiSessionHost host = new UiSessionHost(backend, aui::create, displayed::set, value -> value != null);
        owner.set(host);
        Object nativePage = new Object();
        AtomicInteger factories = new AtomicInteger(), binds = new AtomicInteger(), closes = new AtomicInteger();
        UiPageSpec page = new UiPageSpec("banira_codex:renderer-test", "banira_codex/ui/form.html", new UiPageSession() {

            public void bind(UiView view) {
                binds.incrementAndGet();
            }

            public void detach(UiDetachReason reason) {
            }

            public void close(UiCloseReason reason) {
                closes.incrementAndGet();
            }
        }, previous -> {
            factories.incrementAndGet();
            return nativePage;
        });
        assertTrue(host.open(page, null));
        assertSame(nativePage, displayed.get());
        assertEquals(1, factories.get());
        assertEquals(1, binds.get());
        assertFalse(backend.wantsAui(page.id()));
        assertTrue(backend.wantsAui("banira_codex:unrelated"));
        host.disconnect();
        host.disconnect();
        assertEquals(1, closes.get());
    }

    @Test
    public void resourceIdentityUsesThePublicLegacyConstructor() throws Exception {
        for (String type : com.google.common.collect.ImmutableList.of("internal/compat/aui/AuiDocumentAccess", "internal/client/ui/UiDomView")) {
            ClassNode node = new ClassNode();
            try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + type + ".class")) {
                assertNotNull(input);
                new ClassReader(input).accept(node, 0);
            }
            int factories = 0;
            for (lombok.var method : node.methods)
                for (lombok.var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("net/minecraft/util/ResourceLocation")) {
                        assertEquals("Legacy resource identity must use its public constructor", "<init>", ((MethodInsnNode) instruction).name);
                        if (((MethodInsnNode) instruction).name.equals("<init>"))
                            factories++;
                    }
            assertEquals(type, 1, factories);
        }
    }
}
