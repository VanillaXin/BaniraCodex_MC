package xin.vanilla.banira.client.gui.widget;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * 下拉选项：显示值、可选左侧图标（物品或纹理）、可选悬浮提示。
 */
@Getter
@Accessors(chain = true, fluent = true)
public class DropdownOption {

    private final String value;
    private final ItemStack icon;
    /**
     * 非空时优先于 {@link #icon} 绘制（适用于 ResourceLocation 纹理或图集子区域）
     */
    @Nullable
    private final Texture[] texture;
    @Nullable
    private final Component tooltip;

    public DropdownOption(String value) {
        this(value, ItemStack.EMPTY, null, null);
    }

    public DropdownOption(String value, ItemStack icon, @Nullable Component tooltip) {
        this(value, icon, null, tooltip);
    }

    /**
     * 使用整图纹理作为左侧图标（自动读取纹理尺寸与 UV）
     */
    public DropdownOption(String value, ResourceLocation location, @Nullable Component tooltip) {
        this(value, ItemStack.EMPTY, location != null ? new Texture[]{Texture.of(location)} : null, tooltip);
    }

    /**
     * 使用整图纹理作为左侧图标（自动读取纹理尺寸与 UV）
     */
    public DropdownOption(String value, ResourceLocation[] locations, @Nullable Component tooltip) {
        this(value, ItemStack.EMPTY, locations != null ? Arrays.stream(locations).map(Texture::of).toArray(Texture[]::new) : null, tooltip);
    }

    /**
     * 使用整图纹理作为左侧图标（自动读取纹理尺寸与 UV）
     */
    public DropdownOption(String value, Texture texture, @Nullable Component tooltip) {
        this(value, ItemStack.EMPTY, texture != null ? new Texture[]{texture} : null, tooltip);
    }

    /**
     * 使用整图纹理作为左侧图标（自动读取纹理尺寸与 UV）
     */
    public DropdownOption(String value, Texture[] textures, @Nullable Component tooltip) {
        this(value, ItemStack.EMPTY, textures, tooltip);
    }

    public DropdownOption(String value, ItemStack icon, @Nullable Texture[] texture, @Nullable Component tooltip) {
        this.value = value;
        this.icon = icon != null && !icon.isEmpty() ? icon : ItemStack.EMPTY;
        this.texture = texture;
        this.tooltip = tooltip;
    }

    public boolean hasIcon() {
        return hasTexture() || !icon.isEmpty();
    }

    /**
     * 是否使用纹理绘制（优先于物品）
     */
    public boolean hasTexture() {
        return texture != null && texture.length > 0;
    }
}
