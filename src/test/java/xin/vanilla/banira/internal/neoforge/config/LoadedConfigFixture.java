package xin.vanilla.banira.internal.neoforge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;

final class LoadedConfigFixture {
    static void accept(ModConfigSpec spec, CommentedConfig config) {
        try {
            java.lang.reflect.Constructor<?> constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
                    .getDeclaredConstructor(CommentedConfig.class, java.nio.file.Path.class, net.neoforged.fml.config.ModConfig.class);
            constructor.setAccessible(true);
            bind(spec, (IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
            spec.afterReload();
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    static void bind(ModConfigSpec spec, IConfigSpec.ILoadedConfig loaded) {
        try {
            java.lang.reflect.Field field = ModConfigSpec.class.getDeclaredField("loadedConfig");
            field.setAccessible(true); field.set(spec, loaded);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
}
