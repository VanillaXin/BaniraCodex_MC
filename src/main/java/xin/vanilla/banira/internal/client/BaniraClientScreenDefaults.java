package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.util.LogoModifier;

public final class BaniraClientScreenDefaults {

    private BaniraClientScreenDefaults() {
    }

    public static void register() {
        BaniraClientEventHub.Screen.onChanged(event -> LogoModifier.modifyLogo());
    }
}
