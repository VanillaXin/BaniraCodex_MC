package xin.vanilla.banira.internal.mixin.injections;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.GenericBuilder;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xin.vanilla.banira.internal.forge.config.ForgeConfigAdapter;

import java.nio.file.Path;

@Mixin(value = ConfigFileTypeHandler.class, remap = false)
public abstract class ForgeConfigFileMixin {
    @Redirect(method = "lambda$reader$1", at = @At(value = "INVOKE",
            target = "Lcom/electronwill/nightconfig/core/file/GenericBuilder;build()Lcom/electronwill/nightconfig/core/file/FileConfig;"))
    private FileConfig banira$transactionalFile(GenericBuilder<?, ?> builder, Path basePath, ModConfig config) {
        return ForgeConfigAdapter.wrapFile(config, (CommentedFileConfig) builder.build());
    }
}
