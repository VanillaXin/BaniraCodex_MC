package xin.vanilla.banira.internal.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.ClassReader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Prevents optional third-party mixins from resolving classes that are not installed.
 */
public final class OptionalCompatibilityMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.contains(".compat.jei.")
                && !mixinClassName.contains(".compat.ftblibrary.")
                && !mixinClassName.contains(".compat.ipn.")
                && !mixinClassName.contains(".compat.minimap.")) {
            return true;
        }
        String resource = targetClassName.replace('.', '/') + ".class";
        URL target = OptionalCompatibilityMixinPlugin.class.getClassLoader().getResource(resource);
        if (target == null) return false;
        if (!mixinClassName.contains(".compat.minimap.")) return true;
        try (InputStream stream = target.openStream()) {
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return matchesMinimap(mixinClassName, node);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean matchesMinimap(String mixin, ClassNode target) {
        String stack = "Lcom/mojang/blaze3d/matrix/MatrixStack;";
        if (mixin.endsWith("XaeroInfoDisplaysMixin")) {
            return method(target, "forEach", "(Ljava/util/function/Consumer;)V");
        }
        if (mixin.endsWith("FtbChunksNotificationMixin")) {
            return field(target, "MINIMAP_TEXT_LIST", "Ljava/util/List;")
                    && method(target, "renderHud", "(" + stack + "F)V");
        }
        if (mixin.endsWith("VoxelMapNotificationMixin")) {
            return field(target, "mapX", "I") && field(target, "mapY", "I") && field(target, "scHeight", "I") && field(target, "scWidth", "I")
                    && field(target, "fullscreenMap", "Z") && field(target, "error", "Ljava/lang/String;")
                    && field(target, "options", "Lcom/mamiyaotaru/voxelmap/MapSettingsManager;")
                    && method(target, "drawMinimap", "(" + stack + "Lnet/minecraft/client/Minecraft;)V")
                    && method(target, "drawDirections", "(" + stack + "II)V");
        }
        if (mixin.endsWith("MapAtlasesNotificationMixin")) {
            return method(target, "renderMapHUDFromItemStack", "(" + stack
                    + "Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/gui/MapItemRenderer;)V");
        }
        return false;
    }

    private static boolean method(ClassNode target, String name, String descriptor) {
        return target.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor));
    }

    private static boolean field(ClassNode target, String name, String descriptor) {
        return target.fields.stream().anyMatch(field -> field.name.equals(name) && field.desc.equals(descriptor));
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
