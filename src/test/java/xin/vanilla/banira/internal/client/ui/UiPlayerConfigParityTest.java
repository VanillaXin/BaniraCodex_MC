package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.platform.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.*;

public class UiPlayerConfigParityTest {
    @Before public void platform() { BaniraPlatforms.install(new TestBaniraPlatform()); }
    private InputFormScreen.Args args(AtomicInteger saves) {
        return new InputFormScreen.Args().setParentScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.empty()) { })
                .setCallback(result -> saves.incrementAndGet())
                .addWidget(new InputFormScreen.Widget().name("language").title(Text.literal("Language")).defaultValue("client"))
                .addWidget(new InputFormScreen.Widget().name("notification").title(Text.literal("Notifications"))
                        .tooltip(Text.literal("Complete notification delivery description")).defaultValue("notification"));
    }

    @Test public void searchKeepsHiddenDraftAndUsesFullDescription() throws Exception {
        Object session = type("internal.client.ui.page.PlayerConfigUiSession").getConstructor(InputFormScreen.Args.class)
                .newInstance(args(new AtomicInteger()));
        Object fields = call(session, "fields");
        call(fields, "draft", new Class<?>[]{String.class, String.class}, "language", "zh_cn");
        NativeUiView root = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, root);
        assertEquals(2, root.rowProviders.get("fields").size());
        root.inputDraft("search", "delivery description");
        assertEquals(1, root.rowProviders.get("fields").size());
        assertEquals("zh_cn", call(fields, "draft", new Class<?>[]{String.class}, "language"));
        root.inputDraft("search", "");
        assertEquals(2, root.rowProviders.get("fields").size());
        call(session, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.DISCONNECT);
        assertEquals(0, root.subscriptionCount());
    }

    @Test public void originalSaveCallbackRunsOnceAcrossViewReplacement() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        Object session = type("internal.client.ui.page.PlayerConfigUiSession").getConstructor(InputFormScreen.Args.class).newInstance(args(saves));
        NativeUiView old = new NativeUiView(new Object()); call(session, "bind", new Class<?>[]{UiView.class}, old);
        call(session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        NativeUiView next = new NativeUiView(new Object()); call(session, "bind", new Class<?>[]{UiView.class}, next);
        old.activate("submit"); assertEquals(0, saves.get());
        next.activate("submit"); next.activate("submit"); assertEquals(1, saves.get());
    }
}
