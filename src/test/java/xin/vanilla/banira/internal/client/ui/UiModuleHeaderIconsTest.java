package xin.vanilla.banira.internal.client.ui;

import org.junit.*;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.ui.UiGameElement;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.gui.quickaction.QuickActionEntry;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.Assert.*;

public class UiModuleHeaderIconsTest {
    @BeforeClass public static void bootstrap() { UiDomControlsTest.bootstrap(); }
    @Before public void platform() { new UiDomControlsTest().platform(); }
    private UiGameElement resolve(String module, QuickActionEntry... entries) throws Exception {
        Class<?> type;
        try { type = Class.forName("xin.vanilla.banira.internal.client.ui.page.UiModuleHeaderIcons"); }
        catch (ClassNotFoundException failure) { fail("Missing module header icon resolver"); return null; }
        Method method = type.getDeclaredMethod("resolve", String.class, List.class); method.setAccessible(true);
        return (UiGameElement) method.invoke(null, module, List.of(entries));
    }
    private QuickActionEntry entry(String id, Texture texture) {
        return new QuickActionEntry().id(id).quickIcon(QuickIcon.resource(texture));
    }
    @Test public void resourceIconIsScopedToTheActualOwnerNamespace() throws Exception {
        Texture own = Texture.of(new ResourceLocation("aotake_sweep", "gui/quick_icon.png"), 64, 64);
        UiGameElement result = resolve("aotake_sweep", entry("banira_codex:main", Texture.of(new ResourceLocation("banira_codex", "gui/quick_icon.png"),64,64)), entry("aotake_sweep:sweep", own));
        assertEquals(UiGameElement.Kind.SPRITE, result.kind()); assertEquals(own, result.option().texture()[0]);
    }
    @Test public void atlasCropIsRetainedRatherThanUsingAnItemSurrogate() throws Exception {
        Texture texture = Texture.of(new ResourceLocation("sakura_sign_in", "textures/gui/sign_in_calendar_original.png"),500,1000).u0(120).v0(920).uWidth(60).vHeight(60);
        UiGameElement result = resolve("sakura_sign_in", entry("sakura_sign_in:sign_in", texture));
        assertEquals(texture, result.option().texture()[0]); assertEquals(60, result.option().texture()[0].uWidth());
    }
    @Test public void absentNamespaceDoesNotBorrowAnotherModsLogo() throws Exception {
        assertNull(resolve("narcissus_farewell", entry("banira_codex:main",Texture.of(new ResourceLocation("banira_codex","gui/quick_icon.png"),64,64))));
    }
    @Test public void nonResourceEntriesDoNotBecomeModuleBrands() throws Exception {
        assertNull(resolve("sakura_sign_in",new QuickActionEntry().id("sakura_sign_in:sign_in").quickIcon(QuickIcon.none())));
    }
    @Test public void baniraMenuOnlyRegistrationStillSuppliesItsRealModIcon() throws Exception {
        Texture texture=Texture.of(new ResourceLocation("banira_codex","gui/quick_icon.png"),64,64);
        UiGameElement result=resolve("banira_codex",entry("banira_codex:quick_codex_navigation",texture)
                .display(xin.vanilla.banira.client.gui.quickaction.EnumQuickActionDisplay.LIST_ONLY));
        assertNotNull("Menu visibility is unrelated to module branding",result);
        assertEquals(texture,result.option().texture()[0]);
    }
}
