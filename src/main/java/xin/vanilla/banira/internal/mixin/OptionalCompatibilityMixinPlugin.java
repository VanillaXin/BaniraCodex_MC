package xin.vanilla.banira.internal.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
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
        boolean aui = mixinClassName.contains(".compat.aui.");
        if (!mixinClassName.contains(".compat.minimap.") && !aui) return true;
        try (InputStream input = target.openStream()) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG);
            return aui ? mixinClassName.endsWith("AuiNetworkManagerMixin") && AuiOptionalServerChannels.matches(node)
                    : matchesMinimap(mixinClassName, node);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean matchesMinimap(String mixin, ClassNode target) {
        if (mixin.endsWith("XaeroInfoDisplaysMixin"))
            return method(target, "forEach", "(Ljava/util/function/Consumer;)V");
        if (mixin.endsWith("XaeroInfoDisplayManagerMixin"))
            return method(target, "add", "(Lxaero/hud/minimap/info/InfoDisplay;)V")
                    && method(target, "applyLocalConfig", "()V");
        String graphics = "Lnet/minecraft/client/gui/GuiGraphics;";
        if (mixin.endsWith("XaeroInformationRenderMixin"))
            return method(target, "render", "(" + graphics + "Lxaero/hud/minimap/module/MinimapSession;"
                    + "Lxaero/hud/minimap/Minimap;IILnet/minecraft/core/BlockPos;IIFLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V")
                    && calls(target, "xaero/common/misc/Misc", "drawNormalText",
                    "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFIZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V");
        if (mixin.endsWith("FtbChunksNotificationMixin"))
            return method(target, "setupComponents", "()V") && method(target, "renderHud", "(" + graphics + "F)V");
        if (mixin.endsWith("MapAtlasesNotificationMixin"))
            return field(target, "globalScale", "F") && method(target, "render", "(" + graphics + "FII)V");
        if (mixin.endsWith("JourneyMapTextBoundsMixin"))
            return method(target, "drawBatchLabel", "(" + graphics
                    + "Lnet/minecraft/network/chat/Component;Ljourneymap/client/render/draw/DrawStep$Pass;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;DDLjourneymap/client/render/draw/DrawUtil$HAlign;"
                    + "Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V");
        return false;
    }

    private static boolean calls(ClassNode target, String owner, String name, String descriptor) {
        int count = 0;
        for (var method : target.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode) {
                    var call = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                    if (call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(descriptor)) count++;
                }
        return count == 1;
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
