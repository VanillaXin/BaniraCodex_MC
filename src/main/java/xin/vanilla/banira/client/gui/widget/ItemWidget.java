package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ItemUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 物品Widget
 */
@Accessors(chain = true, fluent = true)
public class ItemWidget extends BaseWidget {
    @Getter
    private String itemId;

    @Getter
    private ItemStack itemStack;

    @Getter
    @Setter
    private int itemCount = 1;

    @Getter
    @Setter
    private boolean showCountText = true;

    @Getter
    @Setter
    private boolean enableTooltip = true;

    @Getter
    @Setter
    private boolean vanillaTooltip = false;

    @Getter
    @Setter
    private boolean seasonTooltip = true;

    public ItemWidget(BaniraScreen screen) {
        super(screen);
    }

    public ItemWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public ItemWidget(BaniraScreen screen, ScreenCoordinate bounds, String itemId) {
        super(screen, bounds);
        this.itemId = itemId;
    }

    /**
     * 构造函数
     */
    public ItemWidget(BaniraScreen screen, ScreenCoordinate bounds, ItemStack itemStack) {
        super(screen, bounds);
        this.itemStack = itemStack;
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        ItemStack currentItemStack = getCurrentItemStack();
        if (currentItemStack == null || currentItemStack.isEmpty()) {
            renderChildren(graphics, partialTicks);
            return;
        }

        currentItemStack.setCount(itemCount);

        renderItem(graphics, screen.getFont(), currentItemStack, (int) x(), (int) y(), showCountText);

        renderChildren(graphics, partialTicks);

        if (enableTooltip && !ItemUtils.isItemNull(currentItemStack)) {
            int mouseX = (int) screen.inputState().mouseX();
            int mouseY = (int) screen.inputState().mouseY();
            if (mouseInside) {
                deferTooltipRender(currentItemStack, mouseX, mouseY);
            }
        }
    }

    /**
     * 获取当前的物品堆栈
     */
    @Nullable
    private ItemStack getCurrentItemStack() {
        if (itemStack != null) {
            return itemStack.copy();
        }

        if (itemId == null || itemId.isEmpty()) {
            return null;
        }

        return ItemUtils.deserializeItemStack(itemId);
    }

    /**
     * 延迟到帧末、在单位矩阵下绘制，避免父级 translate 导致错位
     */
    private void deferTooltipRender(ItemStack itemStack, int mouseX, int mouseY) {
        List<net.minecraft.network.chat.Component> tooltip = itemStack.getTooltipLines(
                Minecraft.getInstance().player,
                screen.inputState().isShiftPressing() ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );
        if (tooltip.isEmpty()) {
            return;
        }
        final int mx = mouseX;
        final int my = mouseY;
        final ItemStack tipStack = itemStack.copy();
        EnumSeason season = seasonTooltip && screen != null ? screen.season() : null;
        final EnumSeason seasonFinal = season;
        screen.addDeferredTooltipRender(g -> {
            g.pose().pushPose();
            g.pose().last().pose().identity();
            if (vanillaTooltip) {
                g.renderTooltip(screen.getFont(), tooltip, Optional.empty(), mx, my);
            } else {
                TooltipWidget.drawItemTooltip(g.pose(), tipStack, mx, my, seasonFinal);
            }
            g.pose().popPose();
        });
    }

    /**
     * 设置物品ID
     */
    public ItemWidget itemId(String itemId) {
        this.itemId = itemId;
        this.itemStack = null;
        return this;
    }

    /**
     * 设置物品堆栈
     */
    public ItemWidget itemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemId = null;
        return this;
    }


    /**
     * 用方块图集精灵做平面绘制，走 {@link com.mojang.blaze3d.vertex.PoseStack} 与 {@link AbstractGuiUtils#blit}
     */
    public static void renderGuiItemFlatBlit(@Nonnull PoseStack pose, @Nonnull Minecraft mc, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, null, mc.player, 0);
        TextureAtlasSprite sprite = model.getParticleIcon();
        net.minecraft.client.color.item.ItemColors itemColors = BaniraClientRuntime.itemColors();
        int tint = itemColors == null ? -1 : itemColors.getColor(stack, 0);
        if (tint != -1) {
            float cr = (float) (tint >> 16 & 255) / 255.0F;
            float cg = (float) (tint >> 8 & 255) / 255.0F;
            float cb = (float) (tint & 255) / 255.0F;
            RenderSystem.setShaderColor(cr, cg, cb, 1f);
        }
        AbstractGuiUtils.blit(pose, TextureAtlas.LOCATION_BLOCKS, x, y, 0, size, size, sprite);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        AbstractGuiUtils.restoreGuiRenderState();
    }

    /**
     * 在 GUI 中按像素边长绘制物品堆栈
     */
    public static void renderGuiItemScaled(@Nonnull GuiGraphics graphics, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float s = size / 16f;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 200f);
        pose.scale(s, s, s);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        Lighting.setupFor3DItems();
        try {
            graphics.renderItem(stack, 0, 0);
        } finally {
            pose.popPose();
            Lighting.setupForFlatItems();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            AbstractGuiUtils.restoreGuiRenderState();
        }
    }

    /**
     * 绘制物品图标
     */
    public static void renderItem(GuiGraphics graphics, Font font, ItemStack itemStack, int x, int y, boolean showText) {
        renderGuiItemScaled(graphics, itemStack, x, y, 16);
        if (showText && !itemStack.isEmpty()) {
            // 立体方块模型可能越过普通装饰深度，数量层单独禁用深度遮挡。
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            try {
                graphics.renderItemDecorations(font, itemStack, x, y, String.valueOf(itemStack.getCount()));
                graphics.flush();
            } finally {
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            }
        }
    }
}
