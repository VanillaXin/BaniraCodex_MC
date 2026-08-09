package xin.vanilla.banira.client.gui.quickaction;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.gui.widget.EffectIconWidget;
import xin.vanilla.banira.client.gui.widget.ImageWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.internal.client.BaniraItemRenderBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 快捷项在托盘上显示的图标来源：物品、药水效果或纹理资源。
 */
@Accessors(chain = true, fluent = true)
public class QuickIcon {

    public enum Kind {
        ITEM,
        EFFECT,
        RESOURCE,
        CUSTOM,
        NONE,
        ;
    }

    @Getter
    @Setter
    @Nonnull
    private Kind kind = Kind.ITEM;

    @Getter
    @Setter
    @Nullable
    private ItemStack itemStack;

    @Getter
    @Setter
    @Nullable
    private MobEffect mobEffect;

    @Getter
    @Setter
    @Nullable
    private Texture texture;

    @Getter
    @Setter
    @Nullable
    private Renderer customRenderer;

    @Nonnull
    public static QuickIcon none() {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.NONE);
        return q;
    }

    @Nonnull
    public static QuickIcon item(@Nonnull ItemStack stack) {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.ITEM);
        if (stack.isEmpty()) {
            q.itemStack(new ItemStack(Items.PAPER));
        } else {
            q.itemStack(stack);
        }
        return q;
    }

    @Nonnull
    public static QuickIcon item(@Nullable Item item) {
        if (item == null) {
            return item(new ItemStack(Items.PAPER));
        }
        return item(new ItemStack(item));
    }

    @Nonnull
    public static QuickIcon effect(@Nonnull MobEffect effectType) {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.EFFECT);
        q.mobEffect(effectType != null ? effectType : MobEffects.LUCK);
        return q;
    }

    @Nonnull
    public static QuickIcon resource(@Nonnull ResourceLocation textureLocation) {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.RESOURCE);
        q.texture(Texture.of(textureLocation));
        return q;
    }

    @Nonnull
    public static QuickIcon resource(@Nonnull Texture texture) {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.RESOURCE);
        q.texture(texture);
        return q;
    }

    /** 供可选模组兼容层复用其原生图标绘制，不把对应模组类型带入快捷入口模型。 */
    @Nonnull
    public static QuickIcon custom(@Nonnull Renderer renderer) {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.CUSTOM);
        q.customRenderer(renderer);
        return q;
    }

    /**
     * 子 mod 可能在资源重载前注册快捷项；首次绘制时补齐当时无法读取的纹理尺寸。
     */
    @Nullable
    private Texture resolvedResourceTexture() {
        if (texture == null || (texture.uvWidth() > 0 && texture.uvHeight() > 0)) {
            return texture;
        }
        KeyValue<Integer, Integer> size = TextureUtils.resolveTextureSizeForDraw(texture.location());
        if (size.key() > 0 && size.val() > 0) {
            texture = Texture.of(texture.location(), size.key(), size.val());
        }
        return texture;
    }

    /**
     * 在右键菜单等场景绘制：物品使用图集精灵平面绘制，与圆角菜单的 PoseStack 一致，避免 3D GUI 物品不显示。
     */
    public void renderForMenu(@Nonnull PoseStack stack, int x, int y, int size) {
        if (size <= 0) {
            return;
        }
        if (kind == Kind.ITEM) {
            prepareDrawState();
            BaniraItemRenderBridge.renderFlatIcon(stack, resolvedItemStack(), x, y, size);
            return;
        }
        render(stack, x, y, size);
    }

    /**
     * 在 GUI 坐标系下绘制图标，尺寸为 {@code size}×{@code size}。
     */
    public void render(@Nonnull PoseStack stack, int x, int y, int size) {
        if (size <= 0) {
            return;
        }
        // 自定义绘制器也可用于无客户端上下文的契约测试；真实 GUI 绘制才需要恢复 GL 状态。
        Minecraft mc = Minecraft.getInstance();
        if (stack != null && mc != null) {
            prepareDrawState();
        }
        switch (kind) {
            case ITEM: {
                BaniraItemRenderBridge.renderScaled(resolvedItemStack(), x, y, size);
                break;
            }
            case EFFECT: {
                MobEffect e = mobEffect != null ? mobEffect : MobEffects.LUCK;
                MobEffectInstance inst = new MobEffectInstance(e, 1, 0);
                EffectIconWidget.drawEffectIcon(stack, AbstractGuiUtils.getFont(), inst, x, y, size, size, false);
                AbstractGuiUtils.restoreGuiRenderState();
                break;
            }
            case RESOURCE: {
                Texture resourceTexture = resolvedResourceTexture();
                if (resourceTexture == null || resourceTexture.uvWidth() <= 0 || resourceTexture.uvHeight() <= 0) {
                    item(new ItemStack(Items.PAPER)).render(stack, x, y, size);
                    return;
                }

                RenderSystem.enableTexture();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                ImageWidget.blit(stack, resourceTexture, x, y, size, size);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                AbstractGuiUtils.restoreGuiRenderState();
                break;
            }
            case CUSTOM: {
                if (customRenderer != null) {
                    customRenderer.render(stack, mc, x, y, size);
                }
                break;
            }
            default:
                break;
        }
    }

    /** 外部图标绘制器共享同一 GUI 管线，每次调用前都恢复可预期的纹理状态。 */
    private static void prepareDrawState() {
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @FunctionalInterface
    public interface Renderer {
        void render(PoseStack stack, Minecraft minecraft, int x, int y, int size);
    }

    private ItemStack resolvedItemStack() {
        return itemStack != null && !itemStack.isEmpty() ? itemStack : new ItemStack(Items.PAPER);
    }

}
