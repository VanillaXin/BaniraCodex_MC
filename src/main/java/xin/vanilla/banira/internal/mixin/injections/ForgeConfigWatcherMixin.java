package xin.vanilla.banira.internal.mixin.injections;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xin.vanilla.banira.internal.forge.config.ForgeConfigFile;

@Mixin(targets = "net.minecraftforge.fml.config.ConfigFileTypeHandler$ConfigWatcher", remap = false)
public abstract class ForgeConfigWatcherMixin {
    @Shadow @Final private CommentedFileConfig commentedFileConfig;

    @Redirect(method = "run", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeConfigSpec;afterReload()V"))
    private void banira$watcherReload(ForgeConfigSpec spec) {
        // Managed load already invalidated caches under its monitor; leave other mods untouched.
        if (commentedFileConfig instanceof ForgeConfigFile
                && ((ForgeConfigFile) commentedFileConfig).belongsTo(spec)) return;
        spec.afterReload();
    }
}
