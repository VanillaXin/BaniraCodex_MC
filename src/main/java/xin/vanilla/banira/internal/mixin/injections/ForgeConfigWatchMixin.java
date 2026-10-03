package xin.vanilla.banira.internal.mixin.injections;

import com.electronwill.nightconfig.core.file.FileWatcher;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.forge.config.ForgeConfigAdapter;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(value = ConfigFileTypeHandler.class, remap = false)
public abstract class ForgeConfigWatchMixin {
    @Redirect(method = "lambda$reader$1", at = @At(value = "INVOKE",
            target = "Lcom/electronwill/nightconfig/core/file/FileWatcher;addWatch(Ljava/nio/file/Path;Ljava/lang/Runnable;)V"))
    private void banira$watchEvents(FileWatcher watcher, Path path, Runnable callback, Path base, ModConfig config) throws IOException {
        ForgeConfigAdapter.watch(watcher, config, path, callback);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void banira$unwatchEvents(Path base, ModConfig config, CallbackInfo callback) {
        ForgeConfigAdapter.unwatch(config, base.resolve(config.getFileName()));
    }
}
