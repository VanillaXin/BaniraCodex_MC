package xin.vanilla.banira.internal.client.ui;

import net.minecraft.resources.ResourceLocation;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.BaniraUi;
import xin.vanilla.banira.api.client.ui.UiHudFrame;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class UiManualHudTest {
    private UiHudFrame.Sprite sprite(String id) {
        return new UiHudFrame.Sprite(id, ResourceLocation.fromNamespaceAndPath("aotake_sweep", "gui/" + id + ".png"), 30, 40, 20, 5, 90);
    }

    private UiHudFrame.Text text(String time) {
        return new UiHudFrame.Text("countdown", time, 50, 60, 30, 9, 9, .5, 0, 15, 0, 0xff5da530);
    }

    private UiDomControlsTest.Dom dom() throws Exception {
        var dom = new UiDomControlsTest.Dom();
        dom.ids.put("progress", dom.new Node("root"));
        return dom;
    }

    @Test
    public void unchangedFramesWriteZeroAndSecondsChangeOnlyText() throws Exception {
        var dom = dom();
        var ui = new ProgressDomPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
        var frame = new UiHudFrame(List.of(sprite("pole"), text("01:00"), sprite("leaf")));
        ui.update(frame);
        assertEquals(5, dom.created.size());
        int writes = dom.created.stream().mapToInt(n -> n.writes).sum();
        ui.update(new UiHudFrame(frame.parts()));
        assertEquals(writes, dom.created.stream().mapToInt(n -> n.writes).sum());
        ui.update(new UiHudFrame(List.of(sprite("pole"), text("00:59"), sprite("leaf"))));
        assertEquals(5, dom.created.size());
        assertEquals(writes + 2, dom.created.stream().mapToInt(n -> n.writes).sum());
        assertTrue(dom.created.stream().allMatch(n -> n.events.isEmpty()));
        dom.current = false;
        ui.update(new UiHudFrame(List.of()));
        assertFalse(dom.created.get(0).removed);
        ui.close();
        ui.close();
        assertTrue(dom.created.stream().allMatch(n -> n.removed));
    }

    @Test
    public void layoutChangeRetainsNativePoleTextLeafPaintOrder() throws Exception {
        var dom = dom();
        var ui = new ProgressDomPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
        ui.update(new UiHudFrame(List.of(sprite("leaf"))));
        Object leaf = dom.created.get(0).proxy;
        ui.update(new UiHudFrame(List.of(sprite("pole"), text("01:00"), sprite("leaf"))));
        var children = dom.ids.get("progress").children;
        assertEquals(leaf, children.get(2));
        assertEquals(3, children.size());
    }

    @Test
    public void sameIdSpriteBecomingTextKeepsItsPaintPosition() throws Exception {
        for (int position = 0; position < 3; position++) {
            var dom = dom();
            var ui = new ProgressDomPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
            var parts = new java.util.ArrayList<UiHudFrame.Part>(List.of(sprite("pole"), sprite("leaf")));
            parts.add(position, sprite("countdown"));
            ui.update(new UiHudFrame(parts));
            var old = dom.created.get(position);
            parts.set(position, text("00:59"));
            ui.update(new UiHudFrame(parts));
            assertTrue(old.removed);
            assertPaintOrder(dom, parts);
            int writes = dom.created.stream().mapToInt(node -> node.writes).sum();
            ui.update(new UiHudFrame(parts));
            assertEquals(writes, dom.created.stream().mapToInt(node -> node.writes).sum());
        }
    }

    @Test
    public void sameIdTextBecomingSpriteKeepsItsPaintPosition() throws Exception {
        for (int position = 0; position < 3; position++) {
            var dom = dom();
            var ui = new ProgressDomPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
            var parts = new java.util.ArrayList<UiHudFrame.Part>(List.of(sprite("pole"), sprite("leaf")));
            parts.add(position, text("00:59"));
            ui.update(new UiHudFrame(parts));
            var old = dom.created.stream().filter(node -> "countdown".equals(node.attributes.get("id"))).findFirst().orElseThrow();
            parts.set(position, sprite("countdown"));
            ui.update(new UiHudFrame(parts));
            assertTrue(old.removed);
            assertPaintOrder(dom, parts);
            ui.close();
            assertTrue(dom.created.stream().allMatch(node -> node.removed));
        }
    }

    private void assertPaintOrder(UiDomControlsTest.Dom dom, List<UiHudFrame.Part> parts) {
        var ids = dom.ids.get("progress").children.stream()
                .map(child -> dom.created.stream().filter(node -> node.proxy == child && !node.removed).findFirst().orElse(null))
                .filter(java.util.Objects::nonNull).map(node -> node.attributes.get("id")).toList();
        assertEquals(parts.stream().map(UiHudFrame.Part::id).toList(), ids);
    }

    @Test
    public void textureProjectionAndRotationRetainOriginalIdentity() throws Exception {
        var dom = dom();
        var ui = new ProgressDomPresentation((UiDomAccess) dom.access,
                b -> new ScreenCoordinate(b.x() * 2, b.y() * 2, b.width() * 2, b.height() * 2));
        ui.update(new UiHudFrame(List.of(sprite("leaf"))));
        var node = dom.created.get(0);
        assertEquals("texture", node.tag);
        assertEquals("aotake_sweep:gui/leaf.png", node.attributes.get("src"));
        assertEquals("60.0px", node.styleProperties.get("--hud-x"));
        assertEquals("40.0px", node.styleProperties.get("--hud-width"));
        assertEquals("90.0deg", node.styleProperties.get("--hud-angle"));
        assertEquals("progress-sprite", node.attributes.get("class"));
        assertFalse(node.attributes.containsKey("style"));
        ui.update(new UiHudFrame(List.of()));
        assertTrue(node.removed);
    }

    @Test
    public void finiteFramesRejectDuplicateIdsNanAndExcessNodes() {
        try {
            new UiHudFrame(List.of(sprite("leaf"), sprite("leaf")));
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            new UiHudFrame.Sprite("leaf", ResourceLocation.fromNamespaceAndPath("a", "b"), Double.NaN, 0, 1, 1, 0);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            new UiHudFrame(Collections.nCopies(17, sprite("leaf")));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void manualHudNeverDrawsInAutomaticPhaseButRetainsLease() throws Exception {
        var host = new UiHudOwnershipTest.Host();
        var binding = new UiHudOwnershipTest.Binding();
        var lease = host.register(new UiHudSpec("test:hud", "aotake_sweep/progress.html", binding, true));
        host.tick(true);
        host.call("renderOthers", new Class[]{Set.class, java.util.function.Consumer.class}, Set.of(),
                (java.util.function.Consumer<NativeUiView>) v -> fail("Duplicate automatic draw"));
        assertEquals(0, binding.binds);
        assertTrue(host.render(v -> {
        }));
        host.backend.requestSwitch(false);
        host.tick(true);
        assertFalse(host.render(v -> fail()));
        host.backend.requestSwitch(true);
        host.tick(true);
        assertTrue(host.render(v -> {
        }));
        lease.close();
        assertFalse(host.render(v -> fail()));
    }

    @Test
    public void pinnedEngineHasManagedTextureElement() throws Exception {
        String path = System.getProperty("banira.aui.contractJar");
        assertNotNull(path);
        try (var zip = new java.util.zip.ZipFile(path)) {
            var entry = zip.getEntry("com/sighs/apricityui/element/Texture.class");
            assertNotNull("Pinned engine needs managed texture rendering", entry);
            var node = new org.objectweb.asm.tree.ClassNode();
            try (var in = zip.getInputStream(entry)) {
                new org.objectweb.asm.ClassReader(in).accept(node, 0);
            }
            assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("<init>") && m.desc.equals("(Lcom/sighs/apricityui/init/Document;)V")));
            assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("setAttribute") && m.desc.equals("(Ljava/lang/String;Ljava/lang/String;)V")));
        }
    }

    @Test
    public void publicManualRenderIsOptionalForExistingDrivers() {
        assertTrue("Missing finite manual HUD render entry", java.util.Arrays.stream(BaniraUi.Driver.class.getMethods())
                .anyMatch(m -> m.getName().equals("renderHud") && m.isDefault()));
    }

    @Test
    public void manualSpecDoesNotChangeExistingAutomaticConstructor() throws Exception {
        assertTrue("Manual HUD must opt out of the automatic render phase", java.util.Arrays.stream(UiHudSpec.class.getMethods())
                .anyMatch(m -> m.getName().equals("manual")));
    }

    @Test
    public void finiteProgressPresentationExistsWithoutAuiDependency() {
        try {
            Class.forName("xin.vanilla.banira.api.client.ui.UiHudFrame");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("Missing finite HUD frame contract", missing);
        }
    }
}
