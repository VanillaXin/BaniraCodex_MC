package xin.vanilla.banira.client.gui.widget;

import xin.vanilla.banira.internal.client.ui.NativeGuiGraphics;

import org.joml.Matrix3x2fStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ItemUtils;

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
    public void render(GuiGraphicsExtractor graphics, float partialTicks) {
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
                Item.TooltipContext.of(Minecraft.getInstance().level),
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
            g.pose().pushMatrix();
            g.pose().identity();
            if (vanillaTooltip) {
                g.setTooltipForNextFrame(screen.getFont(), tooltip, Optional.empty(), mx, my);
            } else {
                TooltipWidget.drawItemTooltip(g.pose(), tipStack, mx, my, seasonFinal);
            }
            g.pose().popMatrix();
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
     * 菜单物品与其他 GUI 图元共用当前提取器及矩阵。
     */
    public static void renderGuiItemFlatBlit(@Nonnull Matrix3x2fStack pose, @Nonnull Minecraft mc, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        NativeGuiGraphics.draw(pose,
                graphics -> renderGuiItemScaled(graphics, stack, x, y, size));
    }

    /**
     * 在 GUI 中按像素边长绘制物品堆栈
     */
    public static void renderGuiItemScaled(@Nonnull GuiGraphicsExtractor graphics, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        float s = size / 16f;
        Matrix3x2fStack pose = NativeGuiGraphics.bind(graphics);
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(s, s);
        try {
            graphics.item(stack, 0, 0);
        } finally {
            pose.popMatrix();
        }
    }

    /**
     * 绘制物品图标
     */
    public static void renderItem(GuiGraphicsExtractor graphics, Font font, ItemStack itemStack, int x, int y, boolean showText) {
        renderGuiItemScaled(graphics, itemStack, x, y, 16);
        if (showText && !itemStack.isEmpty()) {
            Matrix3x2fStack pose = NativeGuiGraphics.bind(graphics);
            pose.pushMatrix();
            try {
                graphics.nextStratum();
                graphics.itemDecorations(font, itemStack, x, y, String.valueOf(itemStack.getCount()));
                graphics.nextStratum();
            } finally {
                pose.popMatrix();
            }
        }
    }
}
