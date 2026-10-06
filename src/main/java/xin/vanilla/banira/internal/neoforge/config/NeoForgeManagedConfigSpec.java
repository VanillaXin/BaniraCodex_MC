package xin.vanilla.banira.internal.neoforge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Adapts NeoForge's loaded-config lifecycle without native autosave of stale values.
 */
final class NeoForgeManagedConfigSpec implements IConfigSpec {
    private final ModConfigSpec delegate;
    private final NeoForgeConfigValueStore store;
    private final java.util.function.Consumer<ILoadedConfig> bindLoaded;
    private ModConfig owner;

    NeoForgeManagedConfigSpec(ModConfigSpec delegate, NeoForgeConfigValueStore store) {
        this(delegate, store, loaded -> ((NeoForgeConfigSpecAccess) (Object) delegate).banira$bindLoadedConfig(loaded));
    }

    NeoForgeManagedConfigSpec(ModConfigSpec delegate, NeoForgeConfigValueStore store,
                              java.util.function.Consumer<ILoadedConfig> bindLoaded) {
        this.delegate = delegate;
        this.store = store;
        this.bindLoaded = bindLoaded;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public void validateSpec(ModConfig config) {
        owner = config;
        delegate.validateSpec(config);
    }

    // Validation and correction take place in an isolated candidate before publication.
    @Override
    public boolean isCorrect(UnmodifiableCommentedConfig config) {
        return true;
    }

    @Override
    public void correct(CommentedConfig config) {
        delegate.correct(config);
    }

    @Override
    public void acceptConfig(ILoadedConfig loaded) {
        NeoForgeConfigFile file = store.managedFile();
        if (loaded == null) {
            if (file != null) {
                file.close();
                store.detach(file);
            }
            delegate.acceptConfig(null);
            return;
        }
        java.nio.file.Path path;
        try {
            path = owner.getFullPath();
        } catch (IllegalStateException remote) {
            if (file != null) {
                file.close();
                store.detach(file);
            }
            delegate.acceptConfig(loaded);
            return;
        }
        if (file != null && !file.getNioPath().equals(path)) {
            file.close();
            store.detach(file);
            file = null;
        }
        if (file == null) {
            file = (NeoForgeConfigFile) store.wrap(CommentedFileConfig.builder(path).sync().build());
        }
        file.nativePublisher(candidate -> {
            CommentedConfig nativeConfig = loaded.config();
            java.util.function.Consumer<CommentedConfig> replace = target -> {
                target.clear();
                target.clearComments();
                target.putAll(candidate);
                target.putAllComments(candidate);
            };
            if (nativeConfig instanceof com.electronwill.nightconfig.core.concurrent.ConcurrentCommentedConfig concurrent) {
                concurrent.bulkCommentedUpdate(replace);
            } else {
                synchronized (nativeConfig) {
                    replace.accept(nativeConfig);
                }
            }
        });
        file.load();
        // Retain NeoForge's sealed loaded record and caches after our isolated validation.
        bindLoaded.accept(loaded);
        delegate.afterReload();
    }
}
