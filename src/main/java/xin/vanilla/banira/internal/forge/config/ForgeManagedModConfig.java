package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Uses Forge's public file handler boundary, including when FML is outside the transforming loader.
 */
final class ForgeManagedModConfig extends ModConfig {
    private final ConfigFileTypeHandler handler;

    ForgeManagedModConfig(Type type, ForgeConfigSpec spec, ModContainer container, String fileName) {
        super(type, spec, container, fileName);
        handler = new ConfigFileTypeHandler() {
            @Override
            public Function<ModConfig, CommentedFileConfig> reader(Path basePath) {
                return config -> {
                    Path path = basePath.resolve(config.getFileName());
                    CommentedFileConfig bootstrap = bootstrap(path,
                            FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(config.getFileName()));
                    CommentedFileConfig file = ForgeConfigAdapter.wrapFile(config, bootstrap);
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    try {
                        file.load();
                        ForgeConfigAdapter.watch(FileWatcher.defaultInstance(), config, path, () -> {
                            Thread thread = Thread.currentThread();
                            ClassLoader previous = thread.getContextClassLoader();
                            try {
                                thread.setContextClassLoader(loader);
                                reload(config, file, () -> container.dispatchConfigEvent(IConfigEvent.reloading(config)));
                            } finally {
                                thread.setContextClassLoader(previous);
                            }
                        });
                        return file;
                    } catch (IOException | RuntimeException error) {
                        try {
                            ForgeConfigAdapter.releaseFile(config, (ForgeConfigFile) file);
                        } catch (RuntimeException cleanup) {
                            error.addSuppressed(cleanup);
                        }
                        throw new IllegalStateException("Cannot load or watch config " + path, error);
                    }
                };
            }

            @Override
            public void unload(Path basePath, ModConfig config) {
                if (config.getConfigData() instanceof ForgeConfigFile file)
                    ForgeConfigAdapter.releaseFile(config, file);
            }
        };
    }

    static void reload(ModConfig config, CommentedFileConfig file, Runnable dispatchReload) {
        if (config.getSpec().isCorrecting()) return;
        file.load();
        // The managed file already invalidates caches under its monitor before publication ends.
        dispatchReload.run();
    }

    static CommentedFileConfig bootstrap(Path path, Path defaults) {
        return CommentedFileConfig.builder(path).sync().preserveInsertionOrder()
                .onFileNotFound((target, format) -> {
                    Files.createDirectories(target.toAbsolutePath().getParent());
                    if (Files.exists(defaults)) Files.copy(defaults, target);
                    else {
                        Files.createFile(target);
                        format.initEmptyFile(target);
                    }
                    return true;
                }).build();
    }

    @Override
    public ConfigFileTypeHandler getHandler() {
        return handler;
    }

    @Override
    public void acceptSyncedConfig(byte[] bytes) {
        if (getConfigData() instanceof ForgeConfigFile file) ForgeConfigAdapter.releaseFile(this, file);
        super.acceptSyncedConfig(bytes);
    }

    @Override
    public void save() {
        // ConfigTracker saves before unload; malformed external edits must not abort that lifecycle.
        // Explicit holder saves use the strict transactional file directly and still report errors.
        if (getConfigData() instanceof ForgeConfigFile file) file.saveOnUnload();
        else if (getConfigData() instanceof CommentedFileConfig) super.save();
    }
}
