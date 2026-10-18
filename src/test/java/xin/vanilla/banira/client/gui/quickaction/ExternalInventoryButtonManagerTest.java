package xin.vanilla.banira.client.gui.quickaction;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ExternalInventoryButtonManagerTest {
    private static final String PROVIDER_ID = "test_provider";
    private static final String ACTION_ID = "test_action";

    private final ExternalInventoryButtonManager manager = new ExternalInventoryButtonManager();

    @After
    public void tearDown() {
        manager.clearAdoptedEntries();
    }

    @Test
    public void baniraHostRegistersAndRemovesProviderActions() {
        AtomicReference<java.util.List<ExternalInventoryAction>> actions = new AtomicReference<>(
                Collections.singletonList(action()));
        manager.registerProvider(provider(actions));

        manager.refresh(EnumExternalInventoryButtonHost.BANIRA, null);
        String adoptedId = ExternalInventoryButtonManager.registryId(PROVIDER_ID, ACTION_ID);
        QuickActionEntry adopted = QuickActionRegistry.get().getEntry(adoptedId);
        assertNotNull(adopted);
        assertTrue(adopted.display().showsInventoryIcon());
        assertTrue(QuickActionRegistry.get().registeredIconEntryIds().contains(adoptedId));

        actions.set(Collections.emptyList());
        manager.refresh(EnumExternalInventoryButtonHost.BANIRA, null);
        assertNull(QuickActionRegistry.get().getEntry(
                ExternalInventoryButtonManager.registryId(PROVIDER_ID, ACTION_ID)));
    }

    @Test
    public void ftbHostReceivesNonFtbProviderActions() {
        RecordingHost host = new RecordingHost();
        manager.setFtbHostBridge(host);
        manager.registerProvider(provider(new AtomicReference<>(
                Collections.singletonList(action()))));

        manager.refresh(EnumExternalInventoryButtonHost.FTB_LIBRARY, null);

        assertEquals(EnumExternalInventoryButtonHost.FTB_LIBRARY, manager.effectiveHost());
        assertNotNull(host.lastActions.stream()
                .filter(value -> PROVIDER_ID.equals(value.sourceId()))
                .findFirst().orElse(null));
    }

    @Test
    public void originalHostClearsEveryAdoptedSurface() {
        RecordingHost host = new RecordingHost();
        manager.setFtbHostBridge(host);
        manager.registerProvider(provider(new AtomicReference<>(
                Collections.singletonList(action()))));
        manager.refresh(EnumExternalInventoryButtonHost.BANIRA, null);

        manager.refresh(EnumExternalInventoryButtonHost.ORIGINAL, null);

        assertNull(QuickActionRegistry.get().getEntry(
                ExternalInventoryButtonManager.registryId(PROVIDER_ID, ACTION_ID)));
        assertEquals(1, host.clearCount);
    }

    @Test
    public void failedFtbCollectionKeepsNativeButtonsAndOtherProviders() {
        manager.registerProvider(provider(ExternalInventoryButtonManager.FTB_SOURCE_ID, () -> {
            throw new IllegalStateException("FTB contract unavailable");
        }));
        manager.registerProvider(provider(new AtomicReference<>(Collections.singletonList(action()))));

        manager.refresh(EnumExternalInventoryButtonHost.BANIRA, null);

        assertNotNull(QuickActionRegistry.get().getEntry(
                ExternalInventoryButtonManager.registryId(PROVIDER_ID, ACTION_ID)));
        assertTrue(manager.suppressesNativeButtons(PROVIDER_ID));
        assertFalse("A failed FTB provider must retain its native controls",
                manager.suppressesNativeButtons(ExternalInventoryButtonManager.FTB_SOURCE_ID));
    }

    @Test
    public void partiallyCollectedFtbActionsAreNotPublishedOrSuppressed() {
        manager.registerProvider(provider(ExternalInventoryButtonManager.FTB_SOURCE_ID,
                () -> new AbstractList<ExternalInventoryAction>() {
                    @Override
                    public ExternalInventoryAction get(int index) {
                        if (index == 0) return action();
                        throw new IllegalStateException("FTB action iteration failed");
                    }

                    @Override
                    public int size() {
                        return 2;
                    }
                }));
        manager.registerProvider(provider(new AtomicReference<>(Collections.singletonList(action()))));

        manager.refresh(EnumExternalInventoryButtonHost.BANIRA, null);

        assertNotNull(QuickActionRegistry.get().getEntry(
                ExternalInventoryButtonManager.registryId(PROVIDER_ID, ACTION_ID)));
        assertNull("A source must finish collection before publishing any of its actions",
                QuickActionRegistry.get().getEntry(ExternalInventoryButtonManager.registryId(
                        ExternalInventoryButtonManager.FTB_SOURCE_ID, ACTION_ID)));
        assertFalse(manager.suppressesNativeButtons(ExternalInventoryButtonManager.FTB_SOURCE_ID));
    }

    @Test
    public void ftbCollectionFailureRevokesEarlierTakeover() {
        AtomicBoolean failing = new AtomicBoolean();
        manager.registerProvider(provider(ExternalInventoryButtonManager.FTB_SOURCE_ID, () -> {
            if (failing.get()) throw new IllegalStateException("FTB contract lost");
            return Collections.singletonList(action());
        }));
        manager.refresh(EnumExternalInventoryButtonHost.BANIRA, null);
        String adoptedId = ExternalInventoryButtonManager.registryId(
                ExternalInventoryButtonManager.FTB_SOURCE_ID, ACTION_ID);
        assertNotNull(QuickActionRegistry.get().getEntry(adoptedId));
        assertTrue(manager.suppressesNativeButtons(ExternalInventoryButtonManager.FTB_SOURCE_ID));

        failing.set(true);
        manager.refresh(EnumExternalInventoryButtonHost.BANIRA, null);

        assertNull(QuickActionRegistry.get().getEntry(adoptedId));
        assertFalse("Previous success must not authorize suppression after collection fails",
                manager.suppressesNativeButtons(ExternalInventoryButtonManager.FTB_SOURCE_ID));
    }

    @Test
    public void failedIpnCollectionKeepsNativeButtonsUnderFtbHost() {
        String ipnSource = "inventory_profiles_next";
        RecordingHost host = new RecordingHost();
        manager.setFtbHostBridge(host);
        manager.registerProvider(provider(ipnSource, () -> {
            throw new IllegalStateException("IPN contract unavailable");
        }));
        manager.registerProvider(provider(new AtomicReference<>(Collections.singletonList(action()))));

        manager.refresh(EnumExternalInventoryButtonHost.FTB_LIBRARY, null);

        assertEquals(EnumExternalInventoryButtonHost.FTB_LIBRARY, manager.effectiveHost());
        assertTrue(host.lastActions.stream().anyMatch(value -> PROVIDER_ID.equals(value.sourceId())));
        assertFalse(host.lastActions.stream().anyMatch(value -> ipnSource.equals(value.sourceId())));
        assertTrue(manager.suppressesNativeButtons(PROVIDER_ID));
        assertFalse("FTB hosting must not hide IPN controls when no IPN actions were collected",
                manager.suppressesNativeButtons(ipnSource));
    }

    @Test
    public void failedHostPublicationRestoresNativeControlsAndClearsPartialHostState() {
        RecordingHost host = new RecordingHost();
        host.failReplacement = true;
        manager.setFtbHostBridge(host);
        manager.registerProvider(provider(new AtomicReference<>(Collections.singletonList(action()))));

        manager.refresh(EnumExternalInventoryButtonHost.FTB_LIBRARY, null);

        assertEquals(EnumExternalInventoryButtonHost.ORIGINAL, manager.effectiveHost());
        assertFalse(manager.suppressesNativeButtons(PROVIDER_ID));
        assertFalse(manager.suppressesBaniraOverlay());
        assertTrue(host.lastActions.isEmpty());
        assertEquals(1, host.clearCount);
        assertNull(QuickActionRegistry.get().getEntry(
                ExternalInventoryButtonManager.registryId(PROVIDER_ID, ACTION_ID)));
    }

    private static ExternalInventoryActionProvider provider(
            String sourceId, Supplier<List<ExternalInventoryAction>> actions
    ) {
        return new ExternalInventoryActionProvider() {
            @Override
            public String sourceId() {
                return sourceId;
            }

            @Override
            public List<ExternalInventoryAction> actions(net.minecraft.client.gui.screens.Screen screen) {
                return actions.get();
            }
        };
    }

    private static ExternalInventoryActionProvider provider(
            AtomicReference<java.util.List<ExternalInventoryAction>> actions
    ) {
        return new ExternalInventoryActionProvider() {
            @Override
            public String sourceId() {
                return PROVIDER_ID;
            }

            @Override
            public java.util.List<ExternalInventoryAction> actions(
                    net.minecraft.client.gui.screens.Screen screen
            ) {
                return actions.get();
            }
        };
    }

    private static ExternalInventoryAction action() {
        return new ExternalInventoryAction(ACTION_ID,
                BaniraComponent.get().literal("Test action"), QuickIcon.none(), context -> {
        });
    }

    @Test public void auiSwitchAndFtbTakeoverKeepOriginalRegistrationAuthority() throws Exception {
        var backend = new xin.vanilla.banira.internal.client.ui.UiBackendController(() -> true, () -> true);
        var views = new ArrayList<xin.vanilla.banira.internal.client.ui.NativeUiView>();
        var hud = new xin.vanilla.banira.internal.client.ui.AuiHudHost(backend, spec -> {
            var view = new xin.vanilla.banira.internal.client.ui.NativeUiView(null); views.add(view); return view;
        });
        var lease = hud.register(new xin.vanilla.banira.api.client.ui.UiHudSpec("test:quick", "banira_codex/hud/overlay.html",
                new xin.vanilla.banira.api.client.ui.UiHudBinding() {
                    public boolean visible() { return !manager.suppressesBaniraOverlay(); }
                    public void bind(xin.vanilla.banira.api.client.ui.UiView view) { }
                    public void detach(xin.vanilla.banira.api.client.ui.UiDetachReason reason) { }
                }));
        RecordingHost ftb = new RecordingHost(); manager.setFtbHostBridge(ftb);
        manager.registerProvider(provider(new AtomicReference<>(Collections.singletonList(action()))));
        manager.refresh(EnumExternalInventoryButtonHost.BANIRA, null); hud.tick(true);
        assertTrue(hud.render("test:quick", view -> {})); assertTrue(manager.suppressesNativeButtons(PROVIDER_ID));
        backend.requestSwitch(false); backend.tick(); hud.tick(true);
        assertFalse(hud.render("test:quick", view -> fail())); assertTrue(views.get(0).closed());
        assertTrue("Backend switch must not unregister adopted buttons", manager.suppressesNativeButtons(PROVIDER_ID));
        assertNotNull(QuickActionRegistry.get().getEntry(ExternalInventoryButtonManager.registryId(PROVIDER_ID, ACTION_ID)));
        backend.requestSwitch(true); backend.tick(); hud.tick(true); assertTrue(hud.render("test:quick", view -> {}));
        manager.refresh(EnumExternalInventoryButtonHost.FTB_LIBRARY, null); hud.tick(true);
        assertFalse(hud.render("test:quick", view -> fail())); assertTrue(views.get(1).closed()); assertFalse(ftb.lastActions.isEmpty());
        manager.refresh(EnumExternalInventoryButtonHost.ORIGINAL, null); hud.tick(true);
        assertTrue(hud.render("test:quick", view -> {})); assertFalse(manager.suppressesNativeButtons(PROVIDER_ID));
        assertTrue(ftb.lastActions.isEmpty()); lease.close(); assertTrue(views.get(2).closed());
        assertEquals(EnumExternalInventoryButtonHost.ORIGINAL, manager.effectiveHost());
    }

    private static final class RecordingHost implements ExternalInventoryButtonManager.FtbHostBridge {
        private java.util.List<ExternalInventoryAction> lastActions = Collections.emptyList();
        private int clearCount;
        private boolean failReplacement;

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void replace(net.minecraft.client.gui.screens.Screen screen,
                            java.util.List<ExternalInventoryAction> actions) {
            lastActions = actions;
            if (failReplacement) throw new IllegalStateException("FTB host publication failed");
        }

        @Override
        public void clear() {
            clearCount++;
            lastActions = Collections.emptyList();
        }
    }
}
