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
        if (!mixinClassName.contains(".compat.minimap.") && !mixinClassName.contains(".compat.jei.")) return true;
        try (InputStream input = target.openStream()) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG);
            return mixinClassName.contains(".compat.jei.") ? matchesJei(mixinClassName, node)
                    && matchesJeiBundle(jeiTarget("mezz.jei.gui.overlay.bookmarks.BookmarkButtonController"),
                        jeiTarget("mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButtonController"),
                        jeiTarget("mezz.jei.gui.elements.IconButton"))
                    : matchesMinimap(mixinClassName, node);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static ClassNode jeiTarget(String name) throws IOException {
        URL target = OptionalCompatibilityMixinPlugin.class.getClassLoader().getResource(name.replace('.', '/') + ".class");
        if (target == null) return null;
        try (InputStream input = target.openStream()) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG);
            return node;
        }
    }

    private static boolean matchesJeiBundle(ClassNode bookmark, ClassNode history, ClassNode icon) {
        return bookmark != null && history != null && icon != null
                && matchesJei("BookmarkButtonMixin", bookmark)
                && matchesJei("LookupHistoryButtonMixin", history)
                && matchesJei("GuiIconToggleButtonMixin", icon);
    }

    private static boolean matchesJei(String mixin, ClassNode target) {
        String drawable = "Lmezz/jei/api/gui/drawable/IDrawable;";
        String config = "Lmezz/jei/common/config/IClientConfig;";
        String toggles = "Lmezz/jei/common/config/IClientToggleState;";
        String input = "Lmezz/jei/api/gui/inputs/IJeiUserInput;";
        if (mixin.endsWith("LookupHistoryButtonMixin"))
            return field(target, "offIcon", drawable) && field(target, "onIcon", drawable)
                    && field(target, "clientConfig", config) && method(target, "<init>", "(" + config + ")V")
                    && method(target, "onPress", "(" + input + ")Z");
        if (mixin.endsWith("BookmarkButtonMixin"))
            return field(target, "offIcon", drawable) && field(target, "onIcon", drawable)
                    && field(target, "toggleState", toggles)
                    && method(target, "<init>", "(Lmezz/jei/gui/overlay/bookmarks/BookmarkOverlay;Lmezz/jei/gui/bookmarks/BookmarkList;"
                        + toggles + "Lmezz/jei/common/input/IInternalKeyMappings;)V")
                    && method(target, "onPress", "(" + input + ")Z");
        if (mixin.endsWith("GuiIconToggleButtonMixin"))
            return field(target, "controller", "Lmezz/jei/api/gui/buttons/IIconButtonController;")
                    && method(target, "draw", "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
                    && method(target, "drawTooltips", "(Lnet/minecraft/client/gui/GuiGraphics;II)V")
                    && method(target, "isMouseOver", "(DD)Z");
        return false;
    }

    private static boolean matchesMinimap(String mixin, ClassNode target) {
        if (mixin.endsWith("XaeroInfoDisplaysMixin"))
            return method(target, "forEach", "(Ljava/util/function/Consumer;)V");
        if (mixin.endsWith("XaeroInfoDisplayManagerMixin"))
            return method(target, "add", "(Lxaero/hud/minimap/info/InfoDisplay;)V")
                    && method(target, "applyLocalConfig", "()V");
        String graphics = "Lnet/minecraft/client/gui/GuiGraphics;";
        if (mixin.endsWith("JourneyMapTextBoundsMixin"))
            return matchesJourneyBounds(target, graphics, "Lnet/minecraft/network/chat/Component;",
                    "Lnet/minecraft/client/renderer/MultiBufferSource;");
        String delta = "Lnet/minecraft/client/DeltaTracker;";
        if (mixin.endsWith("FtbChunksNotificationMixin"))
            return method(target, "setupComponents", "()V") && method(target, "renderHud", "(" + graphics + delta + ")V");
        if (mixin.endsWith("MapAtlasesNotificationMixin"))
            return field(target, "globalScale", "F") && method(target, "render", "(" + graphics + delta + ")V")
                    && method(target, "renderText", "(" + graphics + "IILpepjebs/mapatlases/client/Anchoring;)V");
        return false;
    }

    private static boolean matchesJourneyBounds(ClassNode target, String graphics, String component, String buffers) {
        return method(target, "drawBatchLabel", "(" + graphics + component
                + "Ljourneymap/client/render/draw/DrawStep$Pass;" + buffers
                + "DDLjourneymap/client/render/draw/DrawUtil$HAlign;"
                + "Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V");
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
