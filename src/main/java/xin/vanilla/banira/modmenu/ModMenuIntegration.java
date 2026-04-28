package xin.vanilla.banira.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import xin.vanilla.banira.client.config.BaniraConfigScreenFactory;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BaniraConfigScreenFactory::create;
    }
}
