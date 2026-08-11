package xin.vanilla.banira.common.network.packet;

import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.TestBaniraPacketBuffer;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.notification.NotificationTypeSyncEntry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommonPacketSerializationTest {

    @Test
    public void customPlayerConfigRoundTripsThroughBaniraBuffer() {
        CustomPlayerConfigSyncToServer original = new CustomPlayerConfigSyncToServer("zh_cn", "OVERLAY");

        CustomPlayerConfigSyncToServer decoded = new CustomPlayerConfigSyncToServer(write(original::toBytes));

        assertEquals("zh_cn", decoded.language());
        assertEquals("OVERLAY", decoded.notificationReceiveMode());
    }

    @Test
    public void customPlayerConfigNullsEncodeAsEmptyValues() {
        CustomPlayerConfigSyncToServer original = new CustomPlayerConfigSyncToServer(null, null);

        CustomPlayerConfigSyncToServer decoded = new CustomPlayerConfigSyncToServer(write(original::toBytes));

        assertEquals("", decoded.language());
        assertEquals("", decoded.notificationReceiveMode());
    }

    @Test
    public void requestRoundTripsThroughBaniraBuffer() {
        RequestToBoth decoded = new RequestToBoth(write(new RequestToBoth(42)::toBytes));

        assertEquals(42, decoded.requestType());
    }

    @Test
    public void modLoadedRoundTripsAndDropsEmptyIds() {
        ModLoadedToBoth original = new ModLoadedToBoth(List.of("banira_codex", "", "child_mod"));

        ModLoadedToBoth decoded = new ModLoadedToBoth(write(original::toBytes));

        assertEquals(List.of("banira_codex", "child_mod"), decoded.modids());
    }

    @Test
    public void modLoadedCapsPacketSize() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < ModLoadedToBoth.MAX_MODIDS + 8; i++) {
            many.add("mod_" + i);
        }

        ModLoadedToBoth decoded = new ModLoadedToBoth(write(new ModLoadedToBoth(many)::toBytes));

        assertEquals(ModLoadedToBoth.MAX_MODIDS, decoded.modids().size());
        assertEquals("mod_0", decoded.modids().get(0));
        assertEquals("mod_" + (ModLoadedToBoth.MAX_MODIDS - 1), decoded.modids().get(ModLoadedToBoth.MAX_MODIDS - 1));
    }

    @Test
    public void notificationTypesRoundTripAndNormalizeIds() {
        NotificationTypesSyncToClient original = new NotificationTypesSyncToClient(List.of(
                new NotificationTypeSyncEntry(" Child Mod ", EnumNotificationTypeDisplayMode.ACTION_BAR),
                new NotificationTypeSyncEntry(null, null)
        ));

        NotificationTypesSyncToClient decoded = new NotificationTypesSyncToClient(write(original::toBytes));

        assertEquals(2, decoded.entries().size());
        assertEquals(NotificationTypeKeys.normalizeOrDefault(" Child Mod "), decoded.entries().get(0).typeId());
        assertEquals(EnumNotificationTypeDisplayMode.ACTION_BAR, decoded.entries().get(0).defaultDisplayIfAbsent());
        assertEquals(NotificationTypeKeys.DEFAULT, decoded.entries().get(1).typeId());
        assertEquals(null, decoded.entries().get(1).defaultDisplayIfAbsent());
    }

    @Test
    public void notificationToClientRoundTripsThroughBaniraBuffer() {
        NotificationToClient original = new NotificationToClient(
                BaniraComponent.get().literal("Saved"),
                EnumPosition.BOTTOM_CENTER,
                EnumMoveType.FADE_IN,
                2400L,
                EnumNotificationStyle.SUCCESS,
                "config_editor"
        );

        NotificationToClient decoded = new NotificationToClient(write(original::toBytes));

        assertEquals(original.componentJson(), decoded.componentJson());
        assertEquals(EnumPosition.BOTTOM_CENTER.name(), decoded.positionName());
        assertEquals(EnumMoveType.FADE_IN.name(), decoded.animationName());
        assertEquals(2400L, decoded.durationTime());
        assertEquals(EnumNotificationStyle.SUCCESS.name(), decoded.styleName());
        assertEquals(NotificationTypeKeys.normalizeOrDefault("config_editor"), decoded.typeId());
        assertTrue(decoded.componentJson().contains("Saved"));
    }

    @Test
    public void notificationToClientDefaultsInvalidConstructorValues() {
        NotificationToClient decoded = new NotificationToClient(write(new NotificationToClient(
                BaniraComponent.get().literal("Fallback"),
                null,
                null,
                0L,
                null,
                null
        )::toBytes));

        assertEquals(EnumPosition.TOP_RIGHT.name(), decoded.positionName());
        assertEquals(EnumMoveType.AUTO.name(), decoded.animationName());
        assertEquals(5000L, decoded.durationTime());
        assertEquals(EnumNotificationStyle.NORMAL.name(), decoded.styleName());
        assertEquals(NotificationTypeKeys.DEFAULT, decoded.typeId());
    }

    private static TestBaniraPacketBuffer write(BufferWriter writer) {
        TestBaniraPacketBuffer buffer = new TestBaniraPacketBuffer();
        writer.write(buffer);
        return buffer.rewind();
    }

    private interface BufferWriter {
        void write(TestBaniraPacketBuffer buffer);
    }
}
