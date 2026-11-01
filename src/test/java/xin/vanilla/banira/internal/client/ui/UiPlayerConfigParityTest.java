package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.type;

public class UiPlayerConfigParityTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private InputFormScreen.Args args(AtomicInteger saves) {
        return new InputFormScreen.Args().setParentScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.empty()) {
                })
                .setCallback(result -> saves.incrementAndGet())
                .addWidget(new InputFormScreen.Widget().name("language").title(Text.literal("Language")).defaultValue("client"))
                .addWidget(new InputFormScreen.Widget().name("notification").title(Text.literal("Notifications"))
                        .tooltip(Text.literal("Complete notification delivery description")).defaultValue("notification"));
    }

    private Object remoteSession(java.util.function.Supplier<java.util.Map<String, String>> source) throws Exception {
        try {
            return type("internal.client.ui.page.PlayerConfigUiSession")
                    .getConstructor(InputFormScreen.Args.class, java.util.function.Supplier.class).newInstance(args(new AtomicInteger()), source);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Player drafts need authoritative refresh without replacing edits", missing);
        }
    }

    private Object remoteSession(InputFormScreen.Args args, java.util.function.Supplier<java.util.Map<String, String>> source) throws Exception {
        return type("internal.client.ui.page.PlayerConfigUiSession")
                .getConstructor(InputFormScreen.Args.class, java.util.function.Supplier.class).newInstance(args, source);
    }

    @Test
    public void missingAuthoritativeSnapshotDisablesSaveAndBlocksDirectSubmit() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        Object session = remoteSession(args(saves), () -> null);
        Object fields = call(session, "fields");
        assertEquals(false, call(fields, "canSubmit"));
        assertEquals(false, call(fields, "submit"));
        assertEquals(0, saves.get());
        assertEquals(false, call(fields, "finished"));
    }

    @Test
    public void snapshotArrivingBeforeTheNextTickIsMergedBeforeSubmit() throws Exception {
        java.util.concurrent.atomic.AtomicReference<java.util.Map<String, String>> source = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<String> saved = new java.util.concurrent.atomic.AtomicReference<>();
        Object session = remoteSession(args(new AtomicInteger()).setCallback(result -> saved.set(result.value("language") + "/" + result.value("notification"))), source::get);
        Object fields = call(session, "fields");
        source.set(java.util.Map.of("language", "server", "notification", "vanilla_message"));
        assertEquals(true, call(fields, "submit"));
        assertEquals("server/vanilla_message", saved.get());
    }

    @Test
    public void earlyUserDraftSurvivesReadinessAndIsSubmittedOnlyOnce() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<java.util.Map<String, String>> source = new java.util.concurrent.atomic.AtomicReference<>();
        Object session = remoteSession(args(saves), source::get);
        Object fields = call(session, "fields");
        call(fields, "draft", new Class<?>[]{String.class, String.class}, "language", "zh_cn");
        assertEquals(false, call(fields, "submit"));
        source.set(java.util.Map.of("language", "server", "notification", "vanilla_message"));
        assertEquals(true, call(fields, "submit"));
        assertEquals("zh_cn", call(fields, "draft", new Class<?>[]{String.class}, "language"));
        assertEquals("vanilla_message", call(fields, "draft", new Class<?>[]{String.class}, "notification"));
        assertEquals(false, call(fields, "submit"));
        assertEquals(1, saves.get());
    }

    @Test
    public void clearedSnapshotBlocksAnExistingUnsubmittedSession() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<java.util.Map<String, String>> source = new java.util.concurrent.atomic.AtomicReference<>(java.util.Map.of("language", "server", "notification", "vanilla_message"));
        Object session = remoteSession(args(saves), source::get);
        Object fields = call(session, "fields");
        call(session, "tick");
        source.set(null);
        assertEquals(false, call(fields, "canSubmit"));
        assertEquals(false, call(fields, "submit"));
        assertEquals(0, saves.get());
    }

    @Test
    public void localPlayerFormWithoutRemoteSourceStillSubmits() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        Object session = remoteSession(args(saves), null);
        assertEquals(true, call(call(session, "fields"), "submit"));
        assertEquals(1, saves.get());
    }

    @Test
    public void lateSnapshotRefreshesOnlyUneditedFieldsAndKeepsQuery() throws Exception {
        java.util.concurrent.atomic.AtomicReference<java.util.Map<String, String>> source = new java.util.concurrent.atomic.AtomicReference<>();
        Object session = remoteSession(source::get);
        Object fields = call(session, "fields");
        call(session, "query", new Class<?>[]{String.class}, "delivery");
        call(fields, "draft", new Class<?>[]{String.class, String.class}, "language", "zh_cn");
        source.set(java.util.Map.of("language", "server", "notification", "vanilla_message"));
        call(session, "tick");
        assertEquals("zh_cn", call(fields, "draft", new Class<?>[]{String.class}, "language"));
        assertEquals("vanilla_message", call(fields, "draft", new Class<?>[]{String.class}, "notification"));
        assertEquals("delivery", call(session, "query"));
    }

    @Test
    public void snapshotAfterCloseCannotChangeTheDraft() throws Exception {
        java.util.concurrent.atomic.AtomicReference<java.util.Map<String, String>> source = new java.util.concurrent.atomic.AtomicReference<>();
        Object session = remoteSession(source::get);
        Object fields = call(session, "fields");
        call(session, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.DISCONNECT);
        source.set(java.util.Map.of("language", "server", "notification", "vanilla_message"));
        call(session, "tick");
        assertEquals("client", call(fields, "draft", new Class<?>[]{String.class}, "language"));
    }

    @Test
    public void searchKeepsHiddenDraftAndUsesFullDescription() throws Exception {
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

    @Test
    public void filteredDomInputEditsOnlyItsCurrentFieldAndSavesOnce() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        Object session = type("internal.client.ui.page.PlayerConfigUiSession").getConstructor(InputFormScreen.Args.class).newInstance(args(saves));
        Object fields = call(session, "fields");
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        dom.ids.put("search", dom.new Node("input"));
        UiDomView view = (UiDomView) dom.view();
        call(session, "bind", new Class<?>[]{UiView.class}, view);
        view.afterLayout();
        dom.ids.get("search").value = "delivery description";
        dom.ids.get("search").emit("input");
        UiDomControlsTest.Dom.Node input = dom.created.stream()
                .filter(node -> !node.removed && node.tag.equals("input") && node.value.equals("notification"))
                .findFirst().orElseThrow();
        input.value = "changed";
        input.emit("input");
        assertEquals("client", call(fields, "draft", new Class<?>[]{String.class}, "language"));
        assertEquals("changed", call(fields, "draft", new Class<?>[]{String.class}, "notification"));
        dom.ids.get("submit").emit("click");
        dom.ids.get("submit").emit("click");
        assertEquals(1, saves.get());
        call(session, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.DISCONNECT);
        input.value = "late";
        input.emit("input");
        assertEquals("changed", call(fields, "draft", new Class<?>[]{String.class}, "notification"));
        assertEquals(0, view.subscriptionCount());
        view.close();
    }

    @Test
    public void originalSaveCallbackRunsOnceAcrossViewReplacement() throws Exception {
        AtomicInteger saves = new AtomicInteger();
        Object session = type("internal.client.ui.page.PlayerConfigUiSession").getConstructor(InputFormScreen.Args.class).newInstance(args(saves));
        NativeUiView old = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, old);
        call(session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        NativeUiView next = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, next);
        old.activate("submit");
        assertEquals(0, saves.get());
        next.activate("submit");
        next.activate("submit");
        assertEquals(1, saves.get());
    }
}
