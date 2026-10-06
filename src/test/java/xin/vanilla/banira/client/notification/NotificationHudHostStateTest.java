package xin.vanilla.banira.client.notification;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import static org.junit.Assert.*;

public class NotificationHudHostStateTest {
    @Test
    public void absentOrHiddenMapFallsBackToStandalone() {
        NotificationHudHostState state = new NotificationHudHostState();
        assertEquals(EnumNotificationHudHost.STANDALONE, state.select(EnumNotificationHudHost.AUTO, 0));
        state.seen(EnumNotificationHudHost.XAERO, 10);
        assertEquals(EnumNotificationHudHost.XAERO, state.select(EnumNotificationHudHost.AUTO, 200));
        assertEquals(EnumNotificationHudHost.STANDALONE, state.select(EnumNotificationHudHost.AUTO, 511));
    }

    @Test
    public void automaticChoiceHasStablePriorityRegardlessOfCallbackOrder() {
        NotificationHudHostState state = new NotificationHudHostState();
        state.seen(EnumNotificationHudHost.FTB_CHUNKS, 10);
        state.seen(EnumNotificationHudHost.JOURNEYMAP, 10);
        state.seen(EnumNotificationHudHost.XAERO, 10);
        assertEquals(EnumNotificationHudHost.XAERO, state.select(EnumNotificationHudHost.AUTO, 20));
        state.seen(EnumNotificationHudHost.FTB_CHUNKS, 20);
        assertEquals(EnumNotificationHudHost.XAERO, state.select(EnumNotificationHudHost.AUTO, 20));
    }

    @Test
    public void explicitChoiceDoesNotSwitchToOtherInstalledMap() {
        NotificationHudHostState state = new NotificationHudHostState();
        state.seen(EnumNotificationHudHost.XAERO, 1);
        assertEquals(EnumNotificationHudHost.STANDALONE, state.select(EnumNotificationHudHost.JOURNEYMAP, 2));
        state.seen(EnumNotificationHudHost.JOURNEYMAP, 2);
        assertEquals(EnumNotificationHudHost.JOURNEYMAP, state.select(EnumNotificationHudHost.JOURNEYMAP, 2));
        assertEquals(EnumNotificationHudHost.STANDALONE, state.select(EnumNotificationHudHost.STANDALONE, 2));
    }

    @Test
    public void leavingWorldAndClockRollbackDiscardMapPresence() {
        NotificationHudHostState state = new NotificationHudHostState();
        state.seen(EnumNotificationHudHost.XAERO, 10);
        assertEquals(EnumNotificationHudHost.STANDALONE, state.select(EnumNotificationHudHost.AUTO, 9));
        state.reset();
        assertEquals(EnumNotificationHudHost.STANDALONE, state.select(EnumNotificationHudHost.AUTO, 10));
    }

    @Test
    public void compactAppearanceKeepsSizeAndHasNoBackground() {
        assertEquals(12, NotificationHudAppearance.ICON_SIZE);
        assertEquals(.75f, NotificationHudAppearance.COUNT_SCALE, .001f);
        assertEquals(0, NotificationHudAppearance.BACKGROUND_ALPHA);
    }

    @Test
    public void chatKeepsIndependentClickableEntryWithoutLosingMapPresence() {
        NotificationHudHostState state = new NotificationHudHostState();
        state.seen(EnumNotificationHudHost.XAERO, 10);
        assertEquals(EnumNotificationHudHost.STANDALONE, state.select(EnumNotificationHudHost.AUTO, 20, true));
        assertEquals(EnumNotificationHudHost.XAERO, state.select(EnumNotificationHudHost.AUTO, 20, false));
    }

    @Test
    public void missingMapDoesNotEnableOptionalMixin() {
        xin.vanilla.banira.internal.fabric.mixin.FabricMixinConfigPlugin plugin =
                new xin.vanilla.banira.internal.fabric.mixin.FabricMixinConfigPlugin();
        assertFalse(plugin.shouldApplyMixin("absent.map.NativeHud", "xin.vanilla.banira.internal.mixin.compat.minimap.OptionalMapMixin"));
        assertTrue(plugin.shouldApplyMixin("net.minecraft.client.Minecraft", "xin.vanilla.banira.internal.mixin.injections.MinecraftClientMixin"));
        assertFalse(plugin.shouldApplyMixin("xin.vanilla.banira.client.notification.NotificationHudState",
                "xin.vanilla.banira.internal.mixin.compat.minimap.XaeroInfoDisplaysMixin"));
    }
}
