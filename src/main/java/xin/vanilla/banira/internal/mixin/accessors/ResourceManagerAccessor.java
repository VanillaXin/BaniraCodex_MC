package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = SimpleReloadableResourceManager.class)
public interface ResourceManagerAccessor {
    @Accessor(value = "packs")
    List<IResourcePack> banira$packs();
}
