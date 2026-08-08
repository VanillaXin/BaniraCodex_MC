package xin.vanilla.banira.client.gui.quickaction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;
import xin.vanilla.banira.internal.config.ClientConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** 协调第三方背包按钮的原始、Banira 与 FTB Library 三种展示宿主。 */
public final class ExternalInventoryButtonManager {
    public static final String FTB_SOURCE_ID = "ftb_library";
    private static final String ADOPTED_PREFIX = "banira_codex:external/";
    private static final String BANIRA_SOURCE_ID = "banira";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ExternalInventoryButtonManager INSTANCE =
            new ExternalInventoryButtonManager();

    private final List<ExternalInventoryActionProvider> providers = new CopyOnWriteArrayList<>();
    private final Set<String> adoptedEntryIds = new LinkedHashSet<>();
    private volatile FtbHostBridge ftbHostBridge = FtbHostBridge.NONE;
    private volatile EnumExternalInventoryButtonHost effectiveHost =
            EnumExternalInventoryButtonHost.ORIGINAL;
    private boolean refreshing;

    public static ExternalInventoryButtonManager get() {
        return INSTANCE;
    }

    ExternalInventoryButtonManager() {
    }

    public void registerProvider(@Nonnull ExternalInventoryActionProvider provider) {
        String sourceId = normalizeId(provider.sourceId());
        providers.removeIf(existing -> normalizeId(existing.sourceId()).equals(sourceId));
        providers.add(provider);
    }

    public void setFtbHostBridge(@Nullable FtbHostBridge bridge) {
        ftbHostBridge = bridge == null ? FtbHostBridge.NONE : bridge;
    }

    public void refreshCurrentScreen() {
        refreshConfigured(Minecraft.getInstance().screen);
    }

    /** 加载器可在 Minecraft.screen 赋值前传入即将打开的真实界面。 */
    public void refreshForScreen(@Nullable Screen screen) {
        refreshConfigured(screen);
    }

    private void refreshConfigured(@Nullable Screen screen) {
        EnumExternalInventoryButtonHost configured;
        try {
            configured = ClientConfig.get().externalInventoryButtonHost();
        } catch (RuntimeException exception) {
            configured = EnumExternalInventoryButtonHost.ORIGINAL;
        }
        refresh(configured, screen);
    }

    synchronized void refresh(EnumExternalInventoryButtonHost configuredHost,
                              @Nullable Screen screen) {
        if (refreshing) return;
        refreshing = true;
        try {
            EnumExternalInventoryButtonHost previous = effectiveHost;
            EnumExternalInventoryButtonHost resolved = ExternalInventoryButtonPolicy.resolve(
                    configuredHost, ftbHostBridge.available());
            clearAdoptedEntries();
            if (previous == EnumExternalInventoryButtonHost.FTB_LIBRARY
                    || resolved == EnumExternalInventoryButtonHost.ORIGINAL) {
                ftbHostBridge.clear();
            }

            if (resolved == EnumExternalInventoryButtonHost.BANIRA) {
                registerInBanira(collectProviderActions(screen, null));
            } else if (resolved == EnumExternalInventoryButtonHost.FTB_LIBRARY) {
                List<ExternalInventoryAction> actions = collectProviderActions(screen, FTB_SOURCE_ID);
                actions.addAll(snapshotBaniraActions());
                ftbHostBridge.replace(screen, actions);
            }
            effectiveHost = resolved;
            QuickActionOverlay.get().onRegistryChanged();
        } finally {
            refreshing = false;
        }
    }

    public EnumExternalInventoryButtonHost effectiveHost() {
        return effectiveHost;
    }

    public boolean suppressesNativeButtons(String sourceId) {
        if (effectiveHost == EnumExternalInventoryButtonHost.BANIRA) return true;
        return effectiveHost == EnumExternalInventoryButtonHost.FTB_LIBRARY
                && !FTB_SOURCE_ID.equals(normalizeId(sourceId));
    }

    public boolean suppressesBaniraOverlay() {
        return effectiveHost == EnumExternalInventoryButtonHost.FTB_LIBRARY;
    }

    synchronized String diagnosticSnapshot(@Nullable Screen screen) {
        StringBuilder result = new StringBuilder();
        result.append("effectiveHost=").append(effectiveHost).append('\n');
        result.append("screen=").append(screen == null ? "null" : screen.getClass().getName()).append('\n');
        result.append("ftbHostAvailable=").append(ftbHostBridge.available()).append('\n');
        result.append("providerCount=").append(providers.size()).append('\n');
        for (ExternalInventoryActionProvider provider : providers) {
            String sourceId = normalizeId(provider.sourceId());
            try {
                List<ExternalInventoryAction> actions = provider.actions(screen);
                result.append("provider[").append(sourceId).append("]=");
                if (actions != null) {
                    for (ExternalInventoryAction action : actions) {
                        if (action != null) result.append(normalizeId(action.id())).append(',');
                    }
                }
                result.append('\n');
            } catch (Throwable throwable) {
                result.append("provider[").append(sourceId).append("]=ERROR:")
                        .append(throwable.getClass().getName()).append(':')
                        .append(throwable.getMessage()).append('\n');
            }
        }
        result.append("adoptedEntryCount=").append(adoptedEntryIds.size()).append('\n');
        for (String id : adoptedEntryIds) result.append("adopted=").append(id).append('\n');
        return result.toString();
    }

    synchronized void clearAdoptedEntries() {
        QuickActionRegistry registry = QuickActionRegistry.get();
        for (String id : adoptedEntryIds) registry.unregister(id);
        adoptedEntryIds.clear();
    }

    private void registerInBanira(List<ExternalInventoryAction> actions) {
        QuickActionRegistry registry = QuickActionRegistry.get();
        for (ExternalInventoryAction action : actions) {
            String id = registryId(action.sourceId(), action.id());
            registry.registerListOnly(id, action.icon(), action.label(),
                    action.onActivate(), action.contextMenuItems().toArray(
                            new QuickActionContextMenuItem[0]));
            adoptedEntryIds.add(id);
        }
    }

    private List<ExternalInventoryAction> collectProviderActions(
            @Nullable Screen screen, @Nullable String excludedSourceId
    ) {
        Map<String, ExternalInventoryAction> unique = new LinkedHashMap<>();
        for (ExternalInventoryActionProvider provider : providers) {
            String sourceId = normalizeId(provider.sourceId());
            if (sourceId.isEmpty() || sourceId.equals(normalizeId(excludedSourceId))) continue;
            try {
                List<ExternalInventoryAction> actions = provider.actions(screen);
                if (actions == null) continue;
                for (ExternalInventoryAction action : actions) {
                    if (action == null || normalizeId(action.id()).isEmpty()) continue;
                    ExternalInventoryAction sourced = action.withSourceId(sourceId);
                    unique.put(registryId(sourceId, sourced.id()), sourced);
                }
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to collect external inventory buttons from {}", sourceId, throwable);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private List<ExternalInventoryAction> snapshotBaniraActions() {
        List<ExternalInventoryAction> result = new ArrayList<>();
        for (QuickActionEntry entry : QuickActionRegistry.get().allEntriesInOrder()) {
            if (entry.id().startsWith(ADOPTED_PREFIX)) continue;
            result.add(new ExternalInventoryAction(entry.id(), entry.label(), entry.quickIcon(),
                    entry.onActivate(), new ArrayList<>(entry.contextMenuItems))
                    .withSourceId(BANIRA_SOURCE_ID));
        }
        return result;
    }

    static String registryId(String sourceId, String actionId) {
        return ADOPTED_PREFIX + normalizeId(sourceId) + "/" + normalizeId(actionId);
    }

    private static String normalizeId(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    /** Forge FTB 兼容层实现的宿主桥；默认实现使 FTB 保持完全可选。 */
    public interface FtbHostBridge {
        FtbHostBridge NONE = new FtbHostBridge() {
            @Override
            public boolean available() {
                return false;
            }

            @Override
            public void replace(Screen screen, List<ExternalInventoryAction> actions) {
            }

            @Override
            public void clear() {
            }
        };

        boolean available();

        void replace(@Nullable Screen screen, @Nonnull List<ExternalInventoryAction> actions);

        void clear();
    }
}
