package xin.vanilla.banira.internal.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Avoids resolving optional classes without matching native contracts.
 */
public final class OptionalCompatibilityMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.contains(".compat.jei.")
                && !mixinClassName.contains(".compat.ftblibrary.")
                && !mixinClassName.contains(".compat.ipn.")
                && !mixinClassName.contains(".compat.minimap.")) return true;
        URL target = OptionalCompatibilityMixinPlugin.class.getClassLoader().getResource(targetClassName.replace('.', '/') + ".class");
        if (target == null) return false;
        if (!mixinClassName.contains(".compat.minimap.")) return true;
        try (InputStream input = target.openStream()) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG);
            return matchesMinimap(mixinClassName, node);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean matchesMinimap(String mixin, ClassNode target) {
        if (mixin.endsWith("XaeroInfoDisplaysMixin"))
            return method(target, "forEach", "(Ljava/util/function/Consumer;)V");
        String stack = "Lcom/mojang/blaze3d/vertex/PoseStack;";
        if (mixin.endsWith("FtbChunksNotificationMixin"))
            return field(target, "MINIMAP_TEXT_LIST", "Ljava/util/List;")
                    && method(target, "renderHud", "(" + stack + "F)V");
        if (mixin.endsWith("JourneyMapInfoSlotsMixin")) {
            String font = "Lnet/minecraft/client/gui/Font;";
            String label = "Ljourneymap/client/ui/theme/Theme$LabelSpec;";
            String slots = "[Ljourneymap/client/ui/theme/ThemeLabelSource$InfoSlot;";
            return field(target, "labels", "Ljava/util/List;")
                    && method(target, "getInfoLabelAreaHeight", "(" + font + label + slots + ")I")
                    && method(target, "positionLabels", "(" + font + "II" + label + slots + ")V");
        }
        return false;
    }

    private static boolean method(ClassNode target, String name, String descriptor) {
        return target.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor));
    }

    private static boolean field(ClassNode target, String name, String descriptor) {
        return target.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(descriptor));
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
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
