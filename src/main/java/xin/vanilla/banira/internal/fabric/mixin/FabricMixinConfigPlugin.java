package xin.vanilla.banira.internal.fabric.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

/** Avoids resolving optional classes without matching native contracts. */
public final class FabricMixinConfigPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.contains(".compat.jei.")
                && !mixinClassName.contains(".compat.ftblibrary.")
                && !mixinClassName.contains(".compat.ipn.")
                && !mixinClassName.contains(".compat.minimap.")) return true;
        URL target = FabricMixinConfigPlugin.class.getClassLoader().getResource(targetClassName.replace('.', '/') + ".class");
        if (target == null) return false;
        if (!mixinClassName.contains(".compat.minimap.")) {
            if (mixinClassName.contains(".compat.ftblibrary.")) return FabricLoader.getInstance().isModLoaded("ftblibrary");
            if (mixinClassName.contains(".compat.jei.")) return FabricLoader.getInstance().isModLoaded("jei");
            return true;
        }
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
        String graphics = descriptor("net.minecraft.class_332");
        String delta = descriptor("net.minecraft.class_9779");
        if (mixin.endsWith("FtbChunksNotificationMixin"))
            return method(target, "setupComponents", "()V") && method(target, "renderHud", "(" + graphics + delta + ")V");
        if (mixin.endsWith("MapAtlasesNotificationMixin"))
            return field(target, "globalScale", "F") && method(target, "render", "(" + graphics + delta + ")V")
                    && method(target, "renderText", "(" + graphics + "IILpepjebs/mapatlases/client/Anchoring;)V");
        if (mixin.endsWith("VoxelMapNotificationMixin"))
            return field(target, "scWidth", "I") && field(target, "scHeight", "I")
                    && field(target, "fullscreenMap", "Z") && field(target, "error", "Ljava/lang/String;")
                    && field(target, "options", "Lcom/mamiyaotaru/voxelmap/MapSettingsManager;")
                    && method(target, "drawMinimap", "(" + graphics + ")V")
                    && method(target, "drawDirections", "(" + graphics + "II)V");
        return false;
    }

    private static String descriptor(String intermediary) {
        return "L" + FabricLoader.getInstance().getMappingResolver()
                .mapClassName("intermediary", intermediary).replace('.', '/') + ";";
    }

    private static boolean method(ClassNode target, String name, String descriptor) {
        return target.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor));
    }

    private static boolean field(ClassNode target, String name, String descriptor) {
        return target.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(descriptor));
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
