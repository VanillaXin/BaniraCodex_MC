package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import static org.junit.Assert.*;

public class UiTransientInputTest {
    private static final String ROOT = "xin/vanilla/banira/internal/compat/aui/";

    @Test
    public void stylesheetIsOwnedByTheDocumentAcrossAsyncStyleReplacement() throws Exception {
        ClassNode node = new ClassNode();
        try (var in = getClass().getResourceAsStream("/" + ROOT + "AuiDocumentAccess.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        boolean found = false, appended = false;
        for (var method : node.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    assertNotEquals("registerStylesheet", call.name);
                    if (call.name.equals("createElement") && call.getPrevious() instanceof org.objectweb.asm.tree.LdcInsnNode arg
                            && "style".equals(arg.cst)) found = true;
                    if (call.name.equals("appendChild") && call.getPrevious() instanceof org.objectweb.asm.tree.FieldInsnNode field
                            && field.name.equals("transientStylesheet")) appended = true;
                }
        assertTrue(found && appended);
    }

    @Test
    public void transientInputCommitsTheCurrentViewportBeforeHitTesting() throws Exception {
        ClassNode node = new ClassNode();
        try (var in = getClass().getResourceAsStream("/" + ROOT + "AuiDocumentAccess.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        java.util.List<String> order = new java.util.ArrayList<>();
        for (var method : node.methods)
            if (method.name.equals("prepareCurrent")) for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && java.util.List.of("applyViewport",
                        "commitPendingStyleRecalcForRender", "commitRenderState", "updateTransientInput").contains(call.name))
                    order.add(call.name);
        assertEquals(java.util.List.of("applyViewport", "commitPendingStyleRecalcForRender",
                "commitRenderState", "updateTransientInput"), order);
        assertTrue(node.methods.stream().filter(m -> m.name.equals("prepare")).anyMatch(m ->
                java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false)
                        .anyMatch(i -> i instanceof MethodInsnNode c && c.name.equals("withContext"))));
    }

    private Class<?> layout() throws Exception {
        assertNotNull("Shared transient layout contract missing", getClass().getResource("/" + ROOT + "AuiTransientLayout.class"));
        return Class.forName(ROOT.replace('/', '.') + "AuiTransientLayout");
    }

    private ScreenCoordinate tooltip(String text, double x, double y, double width, double height) throws Exception {
        return (ScreenCoordinate) layout().getMethod("tooltip", String.class, double.class, double.class, double.class, double.class)
                .invoke(null, text, x, y, width, height);
    }

    @Test
    public void tooltipClampsToSmallViewportWithoutChangingThePage() throws Exception {
        ScreenCoordinate r = tooltip("A long description ".repeat(30), 319, 179, 320, 180);
        assertTrue(r.x() >= 4 && r.y() >= 4);
        assertTrue(r.x() + r.width() <= 316 && r.y() + r.height() <= 176);
    }

    @Test
    public void shortTooltipDoesNotReserveAnOversizedPanel() throws Exception {
        assertTrue(tooltip("Reset", 20, 20, 640, 400).width() < 100);
        assertTrue(tooltip("Reset", 20, 20, 640, 400).height() < 30);
    }

    @Test
    public void chineseAndMultilineTooltipRemainBounded() throws Exception {
        ScreenCoordinate r = tooltip("\u4e2d\u6587\u8bf4\u660e\n".repeat(25), 600, 380, 640, 400);
        assertTrue(r.width() > 0 && r.height() <= 392);
        assertTrue(r.x() + r.width() <= 636 && r.y() + r.height() <= 396);
    }

    @Test
    public void shortChineseTooltipReservesGlyphAndBorderSlack() throws Exception {
        ScreenCoordinate r = tooltip("\u901a\u77e5\u8bb0\u5f55", 100, 100, 640, 400);
        assertTrue(r.width() >= 52 && r.height() < 30);
    }


    @Test
    public void adapterDoesNotMountVendorTooltipsOnMouseMove() throws Exception {
        ClassNode node = new ClassNode();
        try (var in = getClass().getResourceAsStream("/" + ROOT + "AuiDocumentAccess.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        boolean vendorBind = false;
        try (var in = getClass().getResourceAsStream("/" + ROOT + "AuiDocumentAccess$AuiNode.class")) {
            assertNotNull(in);
            ClassNode child = new ClassNode();
            new ClassReader(in).accept(child, 0);
            for (var method : child.methods)
                for (var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode call && call.owner.equals("com/sighs/apricityui/ui/Tooltip") && call.name.equals("bind"))
                        vendorBind = true;
        }
        assertFalse("Per-hover vendor nodes can alter flex layout", vendorBind);
    }


    @Test
    public void imeCoordinatesUseClientAreaAndClampScaledBounds() throws Exception {
        Object area = layout().getMethod("ime", ScreenCoordinate.class, int.class, int.class, int.class, int.class)
                .invoke(null, new ScreenCoordinate(420, 80, 120, 20), 640, 400, 1280, 800);
        assertEquals(840, area.getClass().getMethod("left").invoke(area));
        assertEquals(160, area.getClass().getMethod("top").invoke(area));
        int x = (int) area.getClass().getMethod("x").invoke(area);
        int y = (int) area.getClass().getMethod("y").invoke(area);
        assertTrue(x >= 840 && x < 1080 && y >= 160 && y <= 200);
    }

    @Test
    public void imeRejectsZeroSizedOrNonfiniteFields() throws Exception {
        var method = layout().getMethod("ime", ScreenCoordinate.class, int.class, int.class, int.class, int.class);
        assertNull(method.invoke(null, new ScreenCoordinate(1, 1, 0, 20), 640, 400, 1280, 800));
        assertNull(method.invoke(null, new ScreenCoordinate(Double.NaN, 1, 20, 20), 640, 400, 1280, 800));
        assertNull(method.invoke(null, new ScreenCoordinate(1, 1, 20, 20), 0, 400, 1280, 800));
    }

    @Test
    public void imeUsesBothClientScaleAxesAndClipsTheExclusionArea() throws Exception {
        Object area = layout().getMethod("ime", ScreenCoordinate.class, int.class, int.class, int.class, int.class)
                .invoke(null, new ScreenCoordinate(-4, 390, 40, 40), 640, 400, 1920, 800);
        assertEquals(0, area.getClass().getMethod("left").invoke(area));
        assertEquals(780, area.getClass().getMethod("top").invoke(area));
        assertEquals(108, area.getClass().getMethod("right").invoke(area));
        assertEquals(800, area.getClass().getMethod("bottom").invoke(area));
        assertEquals(799, area.getClass().getMethod("y").invoke(area));
    }

    @Test
    public void windowsStructuresMatchImm32Abi() throws Exception {
        for (String[] type : new String[][]{{"Composition", "28"}, {"Candidate", "32"}}) {
            Class<?> cls = Class.forName(ROOT.replace('/', '.') + "AuiWindowsIme$" + type[0]);
            var constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object value = constructor.newInstance();
            assertEquals(Integer.parseInt(type[1]), cls.getMethod("size").invoke(value));
        }
    }

    @Test
    public void imePositionsBeforeFirstCompositionAndRestoresDefault() throws Exception {
        verifyImeRestore(false, false);
    }

    @Test
    public void imeDoesNotRestoreOverAnotherOwnersPosition() throws Exception {
        verifyImeRestore(true, true);
    }

    @Test
    public void imeRestoresAReadableCandidateForm() throws Exception {
        verifyImeRestore(false, true);
    }

    private void verifyImeRestore(boolean externalChange, boolean candidateKnown) throws Exception {
        Class<?> api = Class.forName(ROOT.replace('/', '.') + "AuiWindowsIme$Imm");
        java.util.Map<String, Object> forms = new java.util.HashMap<>();
        if (candidateKnown) {
            Class<?> type = Class.forName(ROOT.replace('/', '.') + "AuiWindowsIme$Candidate");
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object candidate = ctor.newInstance();
            type.getField("style").setInt(candidate, 64);
            type.getField("x").setInt(candidate, 20);
            forms.put("candidate", candidate);
        }
        int[] releases = {0};
        Object fake = java.lang.reflect.Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("ImmGetContext")) return com.sun.jna.Pointer.createConstant(2);
            if (name.equals("ImmReleaseContext")) {
                releases[0]++;
                return 1;
            }
            String key = name.contains("Composition") ? "composition" : "candidate";
            Object form = args[args.length - 1];
            if (name.startsWith("ImmGet")) {
                Object stored = forms.get(key);
                if (stored == null) return 0;
                for (var field : form.getClass().getFields())
                    if (field.getType() == int.class
                            && !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                        field.setInt(form, field.getInt(stored));
                return 1;
            }
            var copy = form.getClass().getDeclaredMethod("copy");
            copy.setAccessible(true);
            forms.put(key, copy.invoke(form));
            return 1;
        });
        Class<?> driver = Class.forName(ROOT.replace('/', '.') + "AuiWindowsIme$Driver");
        var constructor = driver.getDeclaredConstructor(api);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(fake);
        Object area = layout().getMethod("ime", ScreenCoordinate.class, int.class, int.class, int.class, int.class)
                .invoke(null, new ScreenCoordinate(50, 50, 100, 20), 640, 400, 1280, 800);
        var update = driver.getDeclaredMethod("update", long.class, area.getClass());
        update.setAccessible(true);
        update.invoke(instance, 1L, area);
        assertEquals(2, forms.get("composition").getClass().getField("style").getInt(forms.get("composition")));
        if (candidateKnown)
            assertEquals(128, forms.get("candidate").getClass().getField("style").getInt(forms.get("candidate")));
        else assertFalse(forms.containsKey("candidate"));
        if (externalChange) for (Object form : forms.values()) form.getClass().getField("x").setInt(form, 999);
        var close = driver.getDeclaredMethod("close");
        close.setAccessible(true);
        close.invoke(instance);
        for (var entry : forms.entrySet()) {
            Object form = entry.getValue();
            boolean candidate = entry.getKey().equals("candidate");
            assertEquals(externalChange ? 999 : candidate ? 20 : 0, form.getClass().getField("x").getInt(form));
            if (!externalChange) assertEquals(candidate ? 64 : 0, form.getClass().getField("style").getInt(form));
        }
        assertEquals(2, releases[0]);
    }

    @Test
    public void imeBridgeChangesOnlyPositionAndReleasesItsContext() throws Exception {
        ClassNode node = new ClassNode();
        try (var in = getClass().getResourceAsStream("/" + ROOT + "AuiWindowsIme$Driver.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        java.util.Set<String> calls = new java.util.HashSet<>();
        for (var method : node.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.owner.endsWith("AuiWindowsIme$Imm"))
                    calls.add(call.name);
        assertTrue(calls.contains("ImmGetContext") && calls.contains("ImmReleaseContext"));
        assertTrue(calls.contains("ImmGetCompositionWindow") && calls.contains("ImmSetCompositionWindow"));
        assertTrue(calls.contains("ImmGetCandidateWindow") && calls.contains("ImmSetCandidateWindow"));
        assertEquals(6, calls.size());
        assertTrue(node.methods.stream().filter(method -> method.name.equals("update")).allMatch(method -> !method.tryCatchBlocks.isEmpty()));
    }

    @Test
    public void ownedTooltipIsHiddenBeforeJoiningTheDocument() throws Exception {
        ClassNode node = new ClassNode();
        try (var in = getClass().getResourceAsStream("/" + ROOT + "AuiTransientTooltip.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        java.util.List<String> calls = new java.util.ArrayList<>();
        for (var method : node.methods)
            if (method.name.equals("<init>")) for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && java.util.List.of("hide", "appendChild").contains(call.name))
                    calls.add(call.name);
        assertEquals(java.util.List.of("hide", "appendChild"), calls);
    }

    @Test
    public void controlExistingFormRetainsBothNativeFileAndColorKinds() {
        assertNotNull(xin.vanilla.banira.client.gui.InputFormScreen.WidgetType.FILE);
        assertNotNull(xin.vanilla.banira.client.gui.InputFormScreen.WidgetType.COLOR);
    }


}
