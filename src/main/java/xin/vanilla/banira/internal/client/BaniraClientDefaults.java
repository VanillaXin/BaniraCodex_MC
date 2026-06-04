package xin.vanilla.banira.internal.client;

/**
 * Registers Banira's built-in client callbacks against the public client event hub.
 */
public final class BaniraClientDefaults {

    private BaniraClientDefaults() {
    }

    public static void register() {
        BaniraClientNetworkDefaults.register();
        BaniraClientScreenDefaults.register();
        BaniraClientResourceService.registerDefaults();
        BaniraClientNotificationDefaults.register();
        BaniraHudLayerDiagnostics.register();
    }
}
