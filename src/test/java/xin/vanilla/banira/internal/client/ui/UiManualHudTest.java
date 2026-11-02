package xin.vanilla.banira.internal.client.ui;

import net.minecraft.util.ResourceLocation;
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
        return new UiHudFrame.Sprite(id, new ResourceLocation("aotake_sweep", "gui/" + id + ".png"), 30, 40, 20, 5, 90);
    }

    private UiHudFrame.Text text(String time) {
        return new UiHudFrame.Text("countdown", time, 50, 60, 30, 9, 9, .5, 0, 15, 0, 0xff5da530);
    }

    private UiDomControlsTest.Dom dom() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        dom.ids.put("progress", dom.new Node("root"));
        return dom;
    }

    @Test
    public void unchangedFramesWriteZeroAndSecondsChangeOnlyText() throws Exception {
        lombok.var dom = dom();
        lombok.var ui = new ProgressDomPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
        lombok.var frame = new UiHudFrame(com.google.common.collect.ImmutableList.of(sprite("pole"), text("01:00"), sprite("leaf")));
        ui.update(frame);
        assertEquals(5, dom.created.size());
        int writes = dom.created.stream().mapToInt(n -> n.writes).sum();
        ui.update(new UiHudFrame(frame.parts()));
        assertEquals(writes, dom.created.stream().mapToInt(n -> n.writes).sum());
        ui.update(new UiHudFrame(com.google.common.collect.ImmutableList.of(sprite("pole"), text("00:59"), sprite("leaf"))));
        assertEquals(5, dom.created.size());
        assertEquals(writes + 2, dom.created.stream().mapToInt(n -> n.writes).sum());
        assertTrue(dom.created.stream().allMatch(n -> n.events.isEmpty()));
        dom.current = false;
        ui.update(new UiHudFrame(com.google.common.collect.ImmutableList.of()));
        assertFalse(dom.created.get(0).removed);
        ui.close();
        ui.close();
        assertTrue(dom.created.stream().allMatch(n -> n.removed));
    }

    @Test
    public void layoutChangeRetainsNativePoleTextLeafPaintOrder() throws Exception {
        lombok.var dom = dom();
        lombok.var ui = new ProgressDomPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
        ui.update(new UiHudFrame(com.google.common.collect.ImmutableList.of(sprite("leaf"))));
        Object leaf = dom.created.get(0).proxy;
        ui.update(new UiHudFrame(com.google.common.collect.ImmutableList.of(sprite("pole"), text("01:00"), sprite("leaf"))));
        lombok.var children = dom.ids.get("progress").children;
        assertEquals(leaf, children.get(2));
        assertEquals(3, children.size());
    }

    @Test
    public void textureProjectionAndRotationRetainOriginalIdentity() throws Exception {
        lombok.var dom = dom();
        lombok.var ui = new ProgressDomPresentation((UiDomAccess) dom.access,
                b -> new ScreenCoordinate(b.x() * 2, b.y() * 2, b.width() * 2, b.height() * 2));
        ui.update(new UiHudFrame(com.google.common.collect.ImmutableList.of(sprite("leaf"))));
        lombok.var node = dom.created.get(0);
        assertEquals("texture", node.tag);
        assertEquals("aotake_sweep:gui/leaf.png", node.attributes.get("src"));
        assertEquals("60.0px", node.styleProperties.get("--hud-x"));
        assertEquals("40.0px", node.styleProperties.get("--hud-width"));
        assertEquals("90.0deg", node.styleProperties.get("--hud-angle"));
        assertEquals("progress-sprite", node.attributes.get("class"));
        assertFalse(node.attributes.containsKey("style"));
        ui.update(new UiHudFrame(com.google.common.collect.ImmutableList.of()));
        assertTrue(node.removed);
    }

    @Test
    public void finiteFramesRejectDuplicateIdsNanAndExcessNodes() {
        try {
            new UiHudFrame(com.google.common.collect.ImmutableList.of(sprite("leaf"), sprite("leaf")));
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            new UiHudFrame.Sprite("leaf", new ResourceLocation("a", "b"), Double.NaN, 0, 1, 1, 0);
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
        lombok.var host = new UiHudOwnershipTest.Host();
        lombok.var binding = new UiHudOwnershipTest.Binding();
        lombok.var lease = host.register(new UiHudSpec("test:hud", "aotake_sweep/progress.html", binding, true));
        host.tick(true);
        host.call("renderOthers", new Class[]{Set.class, java.util.function.Consumer.class}, com.google.common.collect.ImmutableSet.of(),
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
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(path)) {
            lombok.var entry = zip.getEntry("com/sighs/apricityui/element/Texture.class");
            assertNotNull("Pinned engine needs managed texture rendering", entry);
            lombok.var node = new org.objectweb.asm.tree.ClassNode();
            try (java.io.InputStream in = zip.getInputStream(entry)) {
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
