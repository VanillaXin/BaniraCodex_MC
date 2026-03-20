package xin.vanilla.banira.client.gui.quickaction;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.client.gui.widget.EffectIconWidget;
import xin.vanilla.banira.client.gui.widget.ItemWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.data.KeyValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 快捷项在托盘上显示的图标来源：物品、药水效果或纹理资源。
 */
@OnlyIn(Dist.CLIENT)
@Accessors(chain = true, fluent = true)
public class QuickIcon {

    public enum Kind {
        ITEM,
        EFFECT,
        RESOURCE,
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
    private Effect mobEffect;

    @Getter
    @Setter
    @Nullable
    private ResourceLocation texture;

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
    public static QuickIcon effect(@Nonnull Effect effectType) {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.EFFECT);
        q.mobEffect(effectType != null ? effectType : Effects.LUCK);
        return q;
    }

    @Nonnull
    public static QuickIcon resource(@Nonnull ResourceLocation textureLocation) {
        QuickIcon q = new QuickIcon();
        q.kind(Kind.RESOURCE);
        q.texture(textureLocation);
        return q;
    }

    /**
     * 在右键菜单等场景绘制：物品使用图集精灵平面绘制，与圆角菜单的 MatrixStack 一致，避免 3D GUI 物品不显示。
     */
    public void renderForMenu(@Nonnull MatrixStack stack, @Nonnull Minecraft mc, int x, int y, int size) {
        if (size <= 0) {
            return;
        }
        if (kind == Kind.ITEM) {
            ItemWidget.renderGuiItemFlatBlit(stack, mc, itemStack, x, y, size);
            return;
        }
        render(stack, mc, x, y, size);
    }

    /**
     * 在 GUI 坐标系下绘制图标，尺寸为 {@code size}×{@code size}。
     */
    public void render(@Nonnull MatrixStack stack, @Nonnull Minecraft mc, int x, int y, int size) {
        if (size <= 0) {
            return;
        }
        switch (kind) {
            case ITEM: {
                ItemWidget.renderGuiItemScaled(mc, itemStack, x, y, size);
                break;
            }
            case EFFECT: {
                Effect e = mobEffect != null ? mobEffect : Effects.LUCK;
                EffectInstance inst = new EffectInstance(e, 1, 0);
                EffectIconWidget.drawEffectIcon(stack, mc.font, inst, x, y, size, size, false);
                break;
            }
            case RESOURCE: {
                ResourceLocation loc = texture;
                if (loc == null) {
                    item(new ItemStack(Items.PAPER)).render(stack, mc, x, y, size);
                    return;
                }
                KeyValue<Integer, Integer> sz = TextureUtils.getTextureSize(loc);
                int tw = sz.key() != null && sz.key() > 0 ? sz.key() : 16;
                int th = sz.value() != null && sz.value() > 0 ? sz.value() : 16;
                AbstractGuiUtils.blit(stack, loc, x, y, size, size, 0, 0, tw, th, tw, th);
                break;
            }
            default:
                break;
        }
    }

}
