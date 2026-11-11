package xin.vanilla.banira.internal.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import xin.vanilla.banira.internal.compat.FtbLibraryContract;
import xin.vanilla.banira.internal.compat.aui.AuiOptionalServerChannels;

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
                && !mixinClassName.contains(".compat.minimap.")
                && !mixinClassName.contains(".compat.aui.")) return true;
        URL target = OptionalCompatibilityMixinPlugin.class.getClassLoader().getResource(targetClassName.replace('.', '/') + ".class");
        if (target == null) return false;
        if (mixinClassName.contains(".compat.ftblibrary.")) return FtbLibraryContract.matches();
        boolean vanillaPacks = mixinClassName.endsWith(".compat.jei.VanillaClientRecipeLoaderMixin");
        boolean aui = mixinClassName.contains(".compat.aui.");
        if (!mixinClassName.contains(".compat.minimap.") && !vanillaPacks && !aui) return true;
        try (InputStream input = target.openStream()) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG);
            return aui ? mixinClassName.endsWith("AuiNetworkManagerMixin") && AuiOptionalServerChannels.matches(node)
                    : vanillaPacks ? matchesJeiVanillaPacks(node) : matchesMinimap(mixinClassName, node);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean matchesJeiVanillaPacks(ClassNode target) {
        for (MethodNode method : target.methods) {
            if (!method.name.equals("openVanillaServerDataPack")
                    || !method.desc.equals("()Lnet/minecraft/server/packs/PackResources;")
                    || (method.access & Opcodes.ACC_STATIC) == 0) continue;
            int calls = 0;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && call.owner.equals("net/minecraft/server/packs/repository/ServerPacksSource")
                        && (call.name.equals("loadPacks") || call.name.equals("m_7686_"))
                        && call.desc.equals("(Ljava/util/function/Consumer;Lnet/minecraft/server/packs/repository/Pack$PackConstructor;)V"))
                    calls++;
            }
            return calls == 1;
        }
        return false;
    }

    private static boolean matchesMinimap(String mixin, ClassNode target) {
        if (mixin.endsWith("XaeroInfoDisplaysMixin"))
            return method(target, "forEach", "(Ljava/util/function/Consumer;)V");
        if (mixin.endsWith("XaeroInfoDisplayManagerMixin"))
            return method(target, "add", "(Lxaero/hud/minimap/info/InfoDisplay;)V")
                    && method(target, "applyLocalConfig", "()V");
        if (mixin.endsWith("JourneyMapTextBoundsMixin"))
            return method(target, "drawBatchLabel", "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/renderer/MultiBufferSource;DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;Ljava/lang/Integer;FIFDZD)V");
        String stack = "Lcom/mojang/blaze3d/vertex/PoseStack;";
        if (mixin.endsWith("FtbChunksNotificationMixin"))
            return method(target, "buildMinimapTextData", "(Lnet/minecraft/client/Minecraft;DDDLdev/ftb/mods/ftbchunks/client/map/MapDimension;)Ljava/util/List;")
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
        if (mixinClassName.contains(".compat.ftblibrary.")) {
            FtbLibraryContract.recordApplied(targetClassName, targetClass);
        }
    }
}
