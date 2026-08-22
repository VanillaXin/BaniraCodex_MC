package xin.vanilla.banira.client.gui.quickaction;

import org.junit.After;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
                BaniraComponent.get().literal("Test action"), QuickIcon.none(), context -> { });
    }

    private static final class RecordingHost implements ExternalInventoryButtonManager.FtbHostBridge {
        private java.util.List<ExternalInventoryAction> lastActions = Collections.emptyList();
        private int clearCount;

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void replace(net.minecraft.client.gui.screens.Screen screen,
                            java.util.List<ExternalInventoryAction> actions) {
            lastActions = actions;
        }

        @Override
        public void clear() {
            clearCount++;
            lastActions = Collections.emptyList();
        }
    }
}
