package xin.vanilla.banira.internal.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.BaniraUi;
import xin.vanilla.banira.api.client.ui.UiContainerLayer;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class UiLegacyRenderContractTest {
    @Test
    public void publicContainerAndHudRenderingUseTheLegacyMatrixStack() throws Exception {
        assertEquals(boolean.class, UiContainerLayer.class.getMethod("render", MatrixStack.class, Map.class).getReturnType());
        assertEquals(boolean.class, BaniraUi.Driver.class.getMethod("renderHud", String.class, MatrixStack.class).getReturnType());
        assertEquals(boolean.class, BaniraUi.class.getMethod("renderHud", String.class, MatrixStack.class).getReturnType());
    }

    @Test
    public void internalRenderersUseTheSameLegacyGraphicsType() throws Exception {
        assertEquals(void.class, UiContainerLayerHost.Renderer.class.getMethod("render", MatrixStack.class, Map.class).getReturnType());
        assertEquals(void.class, AuiHudHost.ManualRenderer.class.getMethod("renderHud", MatrixStack.class).getReturnType());
        assertEquals(boolean.class, AuiHudHost.class.getMethod("renderManual", String.class, MatrixStack.class).getReturnType());
    }

    @Test
    public void containerLayerRemainsAppearanceOnly() {
        Set<String> methods = Arrays.stream(UiContainerLayer.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName).collect(Collectors.toSet());
        assertEquals(com.google.common.collect.ImmutableSet.of("view", "render", "fallback", "close"), methods);
    }
}
