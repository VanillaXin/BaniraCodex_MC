package xin.vanilla.banira.internal.fabric.client;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.data.BaniraColorThemeLoader;

import javax.annotation.Nonnull;

/**
 * Fabric 资源重载适配器，避免 public client 主题 loader 直接实现 Fabric 接口。
 */
public final class FabricColorThemeReloadListener extends SimplePreparableReloadListener<Void> implements IdentifiableResourceReloadListener {
    public static final FabricColorThemeReloadListener INSTANCE = new FabricColorThemeReloadListener();

    private FabricColorThemeReloadListener() {
    }

    @Nonnull
    @Override
    protected Void prepare(@Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(@Nonnull Void unused, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        BaniraColorThemeLoader.get().reloadFrom(resourceManager);
    }

    @Override
    public ResourceLocation getFabricId() {
        return Identifier.id().create("color_themes");
    }
}
