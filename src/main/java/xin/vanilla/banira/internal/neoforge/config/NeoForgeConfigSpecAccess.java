package xin.vanilla.banira.internal.neoforge.config;

import net.neoforged.fml.config.IConfigSpec;

/** Native loaded record binding after isolated schema validation. */
public interface NeoForgeConfigSpecAccess {
    void banira$bindLoadedConfig(IConfigSpec.ILoadedConfig loaded);
}
