package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ItemUtils;

import javax.annotation.Nullable;
import java.util.List;

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
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        // 获取物品堆栈
        ItemStack currentItemStack = getCurrentItemStack();
        if (currentItemStack == null || currentItemStack.isEmpty()) {
            renderChildren(stack, partialTicks);
            return;
        }

        currentItemStack.setCount(itemCount);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        renderItem(itemRenderer, screen.getFont(), currentItemStack, (int) absoluteX(), (int) absoluteY(), showCountText);

        renderChildren(stack, partialTicks);

        // 渲染 Tooltip
        if (enableTooltip && !ItemUtils.isItemNull(currentItemStack)) {
            int mouseX = (int) screen.inputState().mouseX();
            int mouseY = (int) screen.inputState().mouseY();
            if (mouseInside) {
                renderTooltip(stack, mouseX, mouseY, currentItemStack);
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
     * 渲染 Tooltip
     */
    private void renderTooltip(MatrixStack stack, int mouseX, int mouseY, ItemStack itemStack) {
        List<ITextComponent> tooltip = itemStack.getTooltipLines(
                Minecraft.getInstance().player,
                InputStateManager.isShiftPressingStatic() ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL
        );

        if (!tooltip.isEmpty()) {
            stack.pushPose();
            stack.translate(-absoluteX(), -absoluteY(), 0);
            if (vanillaTooltip) {
                screen.renderComponentTooltip(stack, tooltip, mouseX, mouseY);
            } else {
                EnumSeason season = seasonTooltip && screen != null ? screen.season() : null;
                TooltipWidget.drawItemTooltip(stack, itemStack, mouseX, mouseY, season);
            }
            stack.popPose();
        }
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
     * 绘制物品图标
     */
    public static void renderItem(ItemRenderer itemRenderer, FontRenderer font, ItemStack itemStack, int x, int y, boolean showText) {
        itemRenderer.renderGuiItem(itemStack, x, y);
        if (showText) {
            itemRenderer.renderGuiItemDecorations(font, itemStack, x, y, String.valueOf(itemStack.getCount()));
        }
    }
}
