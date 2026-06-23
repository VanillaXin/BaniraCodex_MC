package xin.vanilla.banira.api.notification;

import org.junit.After;
import org.junit.Test;
import xin.vanilla.banira.api.client.notification.BaniraClientNotificationTypes;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;

import static org.junit.Assert.*;

public class BaniraNotificationTypesTest {

    private static final String TYPE_ID = "child_mod.notice";

    @After
    public void clearServerRegistration() {
        BaniraNotificationTypes.unregister(TYPE_ID);
    }

    @Test
    public void normalizesBlankTypeToDefault() {
        assertEquals(BaniraNotificationTypes.DEFAULT, BaniraNotificationTypes.normalizeOrDefault("   "));
    }

    @Test
    public void registersServerTypeLayoutAndClientDisplayHint() {
        BaniraNotificationTypes.register(
                " " + TYPE_ID + " ",
                EnumPosition.BOTTOM_CENTER,
                EnumMoveType.FADE_IN,
                EnumNotificationTypeDisplayMode.ACTION_BAR
        );

        assertTrue(BaniraNotificationTypes.sortedSnapshot().contains(TYPE_ID));
        assertEquals(EnumPosition.BOTTOM_CENTER, BaniraNotificationTypes.defaultPosition(TYPE_ID));
        assertEquals(EnumMoveType.FADE_IN, BaniraNotificationTypes.defaultAnimation(TYPE_ID));
    }

    @Test
    public void registersClientTypeDisplayDefault() {
        BaniraClientNotificationTypes.register(" " + TYPE_ID + " ", EnumNotificationTypeDisplayMode.VANILLA_CHAT);

        assertTrue(BaniraClientNotificationTypes.knownTypesSorted().contains(TYPE_ID));
        assertEquals(EnumNotificationTypeDisplayMode.VANILLA_CHAT, BaniraClientNotificationTypes.displayDefault(TYPE_ID));
    }
}
