package xin.vanilla.banira.internal.client.dev;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.notification.NotificationBatch;
import xin.vanilla.banira.common.notification.NotificationBudget;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeNotificationFixture;
import xin.vanilla.banira.internal.mixin.injections.NetworkSmokeChatMixin;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaniraNetworkSmokeNotificationTest {
    private String previousEnabled;
    private String previousPhase;

    @Before
    public void setUp() throws Exception {
        BaniraPlatforms.install(new TestBaniraPlatform());
        previousEnabled = System.getProperty("banira.networkSmoke");
        previousPhase = System.getProperty("banira.networkSmoke.phase");
        System.setProperty("banira.networkSmoke", "true");
        System.setProperty("banira.networkSmoke.phase", "phase-two");
        details().clear();
        field("vanillaPages").setInt(null, 0);
    }

    @After
    public void tearDown() throws Exception {
        restore("banira.networkSmoke", previousEnabled);
        restore("banira.networkSmoke.phase", previousPhase);
        details().clear();
        field("vanillaPages").setInt(null, 0);
    }

    @Test
    public void native256EntriesReachChatObserverInFivePagesWithCompleteOrderedContent() throws Exception {
        List<Component> entries = new ArrayList<>();
        List<String> expected = new ArrayList<>();
        for (int index = 0; index < 256; index++) {
            String text = BaniraNetworkSmokeNotificationFixture.text(index);
            expected.add(text);
            entries.add(BaniraComponent.get().literal(text).color(0xFF00FF00));
        }
        List<NotificationBudget.Payload> pages = NotificationBatch.prepare(
                BaniraComponent.get().literal(BaniraNetworkSmokeNotificationFixture.PREFIX), entries,
                BaniraComponent.get().literal("|"), "en_us");
        assertEquals(5, pages.size());
        for (NotificationBudget.Payload page : pages) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                new ClientboundChatPacket(page.vanillaMessage(), ChatType.SYSTEM, new UUID(0, 0)).write(buffer);
                ClientboundChatPacket decoded = new ClientboundChatPacket(buffer);
                observe(decoded.getMessage());
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
        assertEquals(expected, details());
        assertEquals(5, field("vanillaPages").getInt(null));
    }

    @Test
    public void observerIgnoresNormalChatDisabledSmokeAndFirstPhase() throws Exception {
        observe(new TextComponent("unrelated"));
        System.setProperty("banira.networkSmoke", "false");
        observe(new TextComponent(BaniraNetworkSmokeNotificationFixture.PREFIX + "ignored"));
        System.setProperty("banira.networkSmoke", "true");
        System.setProperty("banira.networkSmoke.phase", "phase-one");
        observe(new TextComponent(BaniraNetworkSmokeNotificationFixture.PREFIX + "ignored"));
        assertTrue(details().isEmpty());
        assertEquals(0, field("vanillaPages").getInt(null));
    }

    private static void observe(net.minecraft.network.chat.Component message) throws Exception {
        // The real mapped target and injection callback share this signature.
        net.minecraft.client.gui.Gui.class.getDeclaredMethod("handleChat", ChatType.class,
                net.minecraft.network.chat.Component.class, UUID.class);
        Method method = NetworkSmokeChatMixin.class.getDeclaredMethod("banira$observeNativeNotification",
                ChatType.class, net.minecraft.network.chat.Component.class, UUID.class, CallbackInfo.class);
        method.setAccessible(true);
        method.invoke(new NetworkSmokeChatMixin(), ChatType.SYSTEM, message, new UUID(0, 0),
                new CallbackInfo("handleChat", false));
    }

    @SuppressWarnings("unchecked")
    private static List<String> details() throws Exception {
        return (List<String>) field("vanillaDetails").get(null);
    }

    private static Field field(String name) throws Exception {
        Field field = BaniraNetworkSmokeClientRunner.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void restore(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }
}
