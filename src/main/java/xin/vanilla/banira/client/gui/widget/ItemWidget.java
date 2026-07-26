package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ItemUtils;
import xin.vanilla.banira.internal.client.BaniraItemRenderBridge;
import xin.vanilla.banira.internal.client.InputStateManager;

import javax.annotation.Nonnull;
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
    private String cachedItemId;
    private ItemStack cachedItemIdStack;

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
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        ItemStack currentItemStack = getCurrentItemStack();
        if (currentItemStack == null || currentItemStack.isEmpty()) {
            renderChildren(stack, partialTicks);
            return;
        }

        currentItemStack.setCount(itemCount);

        renderItem(screen.getFont(), currentItemStack, (int) absoluteX(), (int) absoluteY(), showCountText);

        renderChildren(stack, partialTicks);

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

        if (!itemId.equals(cachedItemId) || cachedItemIdStack == null) {
            cachedItemId = itemId;
            cachedItemIdStack = ItemUtils.deserializeItemStack(itemId);
        }
        return cachedItemIdStack.isEmpty() ? ItemStack.EMPTY : cachedItemIdStack.copy();
    }

    /**
     * 渲染 Tooltip
     */
    private void renderTooltip(PoseStack stack, int mouseX, int mouseY, ItemStack itemStack) {
        List<Component> tooltip = BaniraItemRenderBridge.tooltipLines(itemStack, InputStateManager.isShiftPressingStatic());

        if (!tooltip.isEmpty()) {
            stack.pushPose();
            stack.translate(-absoluteX(), -absoluteY(), 0);
            if (vanillaTooltip) {
                screen.renderComponentTooltip(stack, tooltip, mouseX, mouseY);
            } else {
                EnumSeason season = seasonTooltip && screen != null ? screen.season() : null;
                TooltipWidget.drawItemTooltipLines(stack, tooltip, mouseX, mouseY, season);
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
        this.cachedItemId = null;
        this.cachedItemIdStack = null;
        return this;
    }

    /**
     * 设置物品堆栈
     */
    public ItemWidget itemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemId = null;
        this.cachedItemId = null;
        this.cachedItemIdStack = null;
        return this;
    }


    /**
     * 用方块图集精灵做平面绘制，具体模型与 tint 处理交给 internal bridge。
     */
    public static void renderGuiItemFlatBlit(@Nonnull PoseStack pose, @Nonnull ItemStack stack, int x, int y, int size) {
        BaniraItemRenderBridge.renderFlatIcon(pose, stack, x, y, size);
    }

    /**
     * 绘制物品图标
     */
    public static void renderItem(Font font, ItemStack itemStack, int x, int y, boolean showText) {
        BaniraItemRenderBridge.renderItem(font, itemStack, x, y, showText);
    }
}
