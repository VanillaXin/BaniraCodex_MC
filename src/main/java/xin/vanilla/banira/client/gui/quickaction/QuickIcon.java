package xin.vanilla.banira.client.gui.quickaction;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import xin.vanilla.banira.client.gui.widget.ItemWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.data.KeyValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 快捷项在托盘上显示的图标来源：物品、药水效果或纹理资源。
 */
@Environment(EnvType.CLIENT)
@Accessors(chain = true, fluent = true)
public class QuickIcon {

    public enum Kind {
        ITEM,
        EFFECT,
        RESOURCE,
        NONE,
        ;
    }

    @Getter
    @Setter
    @Nonnull
    private Kind kind = Kind.ITEM;

    @Getter
    @Setter
    @Nonnull
    private ItemStack itemStack = new ItemStack(Items.PAPER);

    @Getter
    @Setter
    @Nullable
    private MobEffect mobEffect;

    @Getter
    @Setter
    @Nullable
    private Texture texture;

    /**
     * 由 {@link #resource(ResourceLocation)} 设置；绘制前再 {@link Texture#of}，避免注册阶段（资源未就绪）得到 uv 为 0 的 {@link Texture}。
     */
    @Nullable
    private ResourceLocation deferredResourceTexture;

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
        q.deferredResourceTexture = textureLocation;
        return q;
    }

    @Nonnull
    public static QuickIcon resource(@Nonnull Texture texture) {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.RESOURCE);
        q.texture(texture);
        q.deferredResourceTexture = texture.location();
        return q;
    }

    /**
     * 在右键菜单等场景绘制：物品使用图集精灵平面绘制，与圆角菜单的 PoseStack 一致，避免 3D GUI 物品不显示。
     */
    public void renderForMenu(@Nonnull PoseStack stack, @Nonnull Minecraft mc, int x, int y, int size) {
        if (size <= 0) {
            return;
        }
        // region 菜单行图标绘制前恢复 GUI 状态
        AbstractGuiUtils.restoreGuiRenderState();
        // endregion 菜单行图标绘制前恢复 GUI 状态
        if (kind == Kind.ITEM) {
            ItemWidget.renderGuiItemFlatBlit(stack, mc, itemStack, x, y, size);
            return;
        }
        render(stack, mc, x, y, size);
    }

    /**
     * 在 GUI 坐标系下绘制图标，尺寸为 {@code size}×{@code size}。
     */
    public void render(@Nonnull PoseStack stack, @Nonnull Minecraft mc, int x, int y, int size) {
        if (size <= 0) {
            return;
        }
        switch (kind) {
            case ITEM: {
                ItemWidget.renderGuiItemScaled(mc, itemStack, x, y, size);
                break;
            }
            case EFFECT: {
                MobEffect e = mobEffect != null ? mobEffect : MobEffects.LUCK;
                MobEffectInstance inst = new MobEffectInstance(e, 1, 0);
                EffectIconWidget.drawEffectIcon(stack, mc.font, inst, x, y, size, size, false);
                AbstractGuiUtils.restoreGuiRenderState();
                break;
            }
            case RESOURCE: {
                ensureResourceDrawableTexture();
                if (texture == null || texture.uvWidth() <= 0 || texture.uvHeight() <= 0) {
                    item(new ItemStack(Items.PAPER)).render(stack, mc, x, y, size);
                    return;
                }

                RenderSystem.enableTexture();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                ImageWidget.blit(stack, texture, x, y, size, size);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                AbstractGuiUtils.restoreGuiRenderState();
                break;
            }
            default:
                break;
        }
    }

    // region RESOURCE 纹理延迟解析

    private void ensureResourceDrawableTexture() {
        if (kind != Kind.RESOURCE) {
            return;
        }
        ResourceLocation rl = deferredResourceTexture != null ? deferredResourceTexture : (texture != null ? texture.location() : null);
        if (rl == null) {
            return;
        }
        Texture t = texture;
        if (t != null && t.uvWidth() > 0 && t.uvHeight() > 0) {
            return;
        }
        KeyValue<Integer, Integer> wh = TextureUtils.resolveTextureSizeForDraw(rl);
        if (wh.key() > 0 && wh.val() > 0) {
            texture = Texture.of(rl, wh.key(), wh.val());
            return;
        }
        texture = Texture.of(rl);
    }

    // endregion RESOURCE 纹理延迟解析

}
