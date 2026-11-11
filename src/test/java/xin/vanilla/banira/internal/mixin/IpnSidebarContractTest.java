package xin.vanilla.banira.internal.mixin;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class IpnSidebarContractTest {
    private static final String RENDER = "org.anti_ad.mc.ipnext.gui.inject.base.SortButtonWidget";
    private static final String CLICK = "org.anti_ad.mc.ipnext.gui.inject.base.ProfileButtonWidget";
    private static final String HANDLER = "org.anti_ad.mc.ipnext.gui.inject.ContainerScreenEventHandler";
    private static final String WIDGET = "org.anti_ad.mc.common.gui.widgets.Widget";
    private static final String SETTINGS = "org.anti_ad.mc.ipnext.gui.inject.base.SettingsWidget";
    private static final String RENDER_MIXIN = "xin.vanilla.banira.internal.mixin.compat.ipn.IpnButtonMixin";
    private static final String CLICK_MIXIN = "xin.vanilla.banira.internal.mixin.compat.ipn.IpnProfileButtonClickMixin";
    private static final String RENDER_DESCRIPTOR = "(Lorg/anti_ad/mc/common/gui/NativeContext;IIF)V";

    @Test
    public void fixedIpnAndLibIpnRetainTheirNativeContracts() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        assertTrue(fixture.node(RENDER).methods.stream().anyMatch(method ->
                method.name.equals("render") && method.desc.equals(RENDER_DESCRIPTOR)));
        assertTrue(fixture.node(CLICK).methods.stream().anyMatch(method ->
                method.name.equals("mouseClicked") && method.desc.equals("(III)Z")));
        assertTrue(fixture.node(WIDGET).methods.stream().anyMatch(method -> method.name.equals("getParent")
                && method.desc.equals("()Lorg/anti_ad/mc/common/gui/widgets/Widget;")));
        fixture.node("org.anti_ad.mc.common.gui.NativeContext");
        fixture.node("org.anti_ad.mc.ipnext.gui.inject.EditorWidget");
        IMixinConfigPlugin plugin = fixture.plugin();
        assertTrue(plugin.shouldApplyMixin(RENDER, RENDER_MIXIN));
        assertTrue(plugin.shouldApplyMixin(CLICK, CLICK_MIXIN));
    }

    @Test
    public void missingClickTargetRejectsTheRenderTarget() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        fixture.hide(CLICK);
        assertFalse(fixture.plugin().shouldApplyMixin(RENDER, RENDER_MIXIN));
    }

    @Test
    public void missingRenderTargetRejectsTheClickTarget() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        fixture.hide(RENDER);
        assertFalse(fixture.plugin().shouldApplyMixin(CLICK, CLICK_MIXIN));
    }

    @Test
    public void missingNativeClickSignatureRejectsBothTargets() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        ClassNode click = fixture.node(CLICK);
        assertTrue(click.methods.removeIf(method -> method.name.equals("mouseClicked")
                && method.desc.equals("(III)Z")));
        fixture.replace(click);
        assertRejected(fixture);
    }

    @Test
    public void missingNativeRenderSignatureRejectsBothTargets() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        ClassNode render = fixture.node(RENDER);
        assertTrue(render.methods.removeIf(method -> method.name.equals("render")
                && method.desc.equals(RENDER_DESCRIPTOR)));
        fixture.replace(render);
        assertRejected(fixture);
    }

    @Test
    public void changedCurrentWidgetsContractRejectsBothTargets() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        ClassNode handler = fixture.node(HANDLER);
        assertTrue(handler.methods.stream().anyMatch(method -> method.name.equals("getCurrentWidgets")
                && method.desc.equals("()Ljava/util/List;")));
        handler.methods.stream().filter(method -> method.name.equals("getCurrentWidgets")
                        && method.desc.equals("()Ljava/util/List;"))
                .forEach(method -> method.desc = "()Ljava/util/Collection;");
        fixture.replace(handler);
        assertRejected(fixture);
    }

    @Test
    public void missingExactLibIpnParentAccessorRejectsBothTargets() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        ClassNode widget = fixture.node(WIDGET);
        assertTrue(widget.methods.removeIf(method -> method.name.equals("getParent")
                && method.desc.equals("()Lorg/anti_ad/mc/common/gui/widgets/Widget;")));
        fixture.replace(widget);
        assertRejected(fixture);
    }

    @Test
    public void staticSettingsActionCannotReplaceTheNativeInstanceContract() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        ClassNode settings = fixture.node(SETTINGS);
        assertTrue(settings.methods.stream().anyMatch(method -> method.name.equals("onClick")
                && method.desc.equals("()V")));
        settings.methods.stream().filter(method -> method.name.equals("onClick")
                        && method.desc.equals("()V"))
                .forEach(method -> method.access |= Opcodes.ACC_STATIC);
        fixture.replace(settings);
        assertRejected(fixture);
    }

    @Test
    public void nonStaticEventHandlerCannotReplaceTheNativeSingleton() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        ClassNode handler = fixture.node(HANDLER);
        assertTrue(handler.fields.stream().anyMatch(field -> field.name.equals("INSTANCE")));
        handler.fields.stream().filter(field -> field.name.equals("INSTANCE"))
                .forEach(field -> field.access &= ~Opcodes.ACC_STATIC);
        fixture.replace(handler);
        assertRejected(fixture);
    }

    @Test
    public void nativeTargetsWithoutAppliedCallbacksRetainNativeOwnership() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        IMixinConfigPlugin plugin = fixture.plugin();
        plugin.postApply(RENDER, fixture.node(RENDER), RENDER_MIXIN, null);
        plugin.postApply(CLICK, fixture.node(CLICK), CLICK_MIXIN, null);
        assertFalse(hooksApplied(fixture));
    }

    @Test
    public void completeRenderAndClickCallEdgesAuthorizeAndMissingClickRevokesTakeover() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        IMixinConfigPlugin plugin = fixture.plugin();
        ClassNode render = fixture.node(RENDER);
        ClassNode click = fixture.node(CLICK);
        FtbSidebarContractTest.wireHandler(fixture, render, RENDER_MIXIN, "render", "banira$render");
        plugin.postApply(RENDER, render, RENDER_MIXIN, null);
        plugin.postApply(CLICK, click, CLICK_MIXIN, null);
        assertFalse("A render-only IPN integration must retain native controls", hooksApplied(fixture));

        FtbSidebarContractTest.wireHandler(fixture, click, CLICK_MIXIN, "mouseClicked", "banira$mouseClicked");
        plugin.postApply(CLICK, click, CLICK_MIXIN, null);
        assertTrue(hooksApplied(fixture));
        click.methods.removeIf(method -> method.name.equals("banira$mouseClicked"));
        plugin.postApply(CLICK, click, CLICK_MIXIN, null);
        assertFalse("A lost click call edge must revoke the whole IPN takeover", hooksApplied(fixture));

        MethodNode init = fixture.node(
                        "xin.vanilla.banira.internal.forge.compat.ipn.InventoryProfilesNextCompatibility")
                .methods.stream().filter(method -> method.name.equals("init"))
                .findFirst().orElseThrow(AssertionError::new);
        int gate = -1;
        int registration = -1;
        for (int i = 0; i < init.instructions.size(); i++) {
            if (!(init.instructions.get(i) instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) init.instructions.get(i);
            if (call.owner.equals("xin/vanilla/banira/internal/compat/IpnInventoryContract")
                    && call.name.equals("hooksApplied") && call.desc.equals("()Z")) gate = i;
            if (call.owner.equals("xin/vanilla/banira/client/gui/quickaction/ExternalInventoryButtonManager")
                    && call.name.equals("registerProvider")) registration = i;
        }
        assertTrue("Bridge registration must be guarded by the actual applied contract",
                gate >= 0 && registration > gate);
    }

    @Test
    public void wrongPostApplyTargetIdentityCannotAuthorizeIpnTakeover() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        IMixinConfigPlugin plugin = fixture.plugin();
        ClassNode render = fixture.node(RENDER);
        ClassNode click = fixture.node(CLICK);
        FtbSidebarContractTest.wireHandler(fixture, render, RENDER_MIXIN, "render", "banira$render");
        FtbSidebarContractTest.wireHandler(fixture, click, CLICK_MIXIN, "mouseClicked", "banira$mouseClicked");
        plugin.postApply(RENDER, render, RENDER_MIXIN, null);
        plugin.postApply(CLICK, click, CLICK_MIXIN, null);
        assertTrue(hooksApplied(fixture));
        plugin.postApply(RENDER, click, CLICK_MIXIN, null);
        assertFalse(hooksApplied(fixture));
    }

    @Test
    public void editorOutsideTheNativeWidgetHierarchyRejectsBothTargets() throws Exception {
        FtbSidebarContractTest.Fixture fixture = fixture();
        ClassNode editor = fixture.node("org.anti_ad.mc.ipnext.gui.inject.EditorWidget");
        editor.superName = "java/lang/Object";
        fixture.replace(editor);
        assertRejected(fixture);
    }

    private static boolean hooksApplied(FtbSidebarContractTest.Fixture fixture) throws Exception {
        return (Boolean) fixture.loadClass("xin.vanilla.banira.internal.compat.IpnInventoryContract")
                .getMethod("hooksApplied").invoke(null);
    }

    private static FtbSidebarContractTest.Fixture fixture() throws IOException {
        return new FtbSidebarContractTest.Fixture(
                "inventory-profiles-next-495267", "6093589", "libipn-679177", "4574569");
    }

    private static void assertRejected(FtbSidebarContractTest.Fixture fixture) throws Exception {
        IMixinConfigPlugin plugin = fixture.plugin();
        assertFalse("IPN render and click must share the complete native contract",
                plugin.shouldApplyMixin(RENDER, RENDER_MIXIN));
        assertFalse(plugin.shouldApplyMixin(CLICK, CLICK_MIXIN));
    }
}
