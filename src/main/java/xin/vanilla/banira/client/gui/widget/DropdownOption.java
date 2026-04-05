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
 * 下拉选项：内部值 {@link #value}、可选展示文案 {@link #displayLabel}、可选左侧图标、可选悬浮提示。
 */
@Getter
@Accessors(chain = true, fluent = true)
public class DropdownOption {

    private final String value;
    /**
     * 非空时在下拉列表与折叠输入框中优先显示，选中与回调仍使用 {@link #value}
     */
    @Nullable
    private final String label;
    private final ItemStack icon;
    /**
     * 非空时优先于 {@link #icon} 绘制（适用于 ResourceLocation 纹理或图集子区域）
     */
    @Nullable
    private final Texture[] texture;
    @Nullable
    private final Component tooltip;

    /**
     * 展示用文案；无单独标签时返回 {@link #value}
     */
    public String displayLabel() {
        return (label != null && !label.isEmpty()) ? label : value;
    }

    public DropdownOption(String value) {
        this(value, null, ItemStack.EMPTY, null, null);
    }

    /**
     * @param displayLabel 列表与输入框显示；{@code null} 或空则显示 {@code value}
     */
    public DropdownOption(String value, @Nullable String displayLabel) {
        this(value, displayLabel, ItemStack.EMPTY, null, null);
    }

    public DropdownOption(String value, ItemStack icon, @Nullable Component tooltip) {
        this(value, null, icon, null, tooltip);
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
        this(value, null, icon, texture, tooltip);
    }

    public DropdownOption(String value, @Nullable String displayLabel, ItemStack icon, @Nullable Texture[] texture, @Nullable Component tooltip) {
        this.value = value;
        this.label = displayLabel;
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
