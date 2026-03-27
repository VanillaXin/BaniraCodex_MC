package xin.vanilla.banira.common.enums;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 为下拉选项等提供左侧图标（物品或纹理）。
 */
public interface IEnumDropdownIcon {

    /**
     * 左侧展示用的物品图标；若 {@link #dropdownTextureLocation()} 非空则优先绘制纹理。
     */
    default ItemStack dropdownIcon() {
        return ItemStack.EMPTY;
    }

    /**
     * 左侧展示用的纹理资源；非空时优先于 {@link #dropdownIcon()}。
     */
    @Nullable
    default ResourceLocation dropdownTextureLocation() {
        return null;
    }
}
