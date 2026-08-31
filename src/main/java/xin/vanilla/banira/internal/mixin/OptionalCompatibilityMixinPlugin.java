package xin.vanilla.banira.internal.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.net.URL;
import java.util.List;
import java.util.Set;

/** Prevents optional third-party mixins from resolving classes that are not installed. */
public final class OptionalCompatibilityMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.contains(".compat.jei.")
                && !mixinClassName.contains(".compat.ftblibrary.")
                && !mixinClassName.contains(".compat.ipn.")) {
            return true;
        }
        String resource = targetClassName.replace('.', '/') + ".class";
        URL target = OptionalCompatibilityMixinPlugin.class.getClassLoader().getResource(resource);
        return target != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}
