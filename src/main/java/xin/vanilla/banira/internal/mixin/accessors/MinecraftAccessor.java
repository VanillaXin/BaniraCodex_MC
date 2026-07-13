package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.16 未公开物品着色器 getter，集中在 mixin 边界访问。 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("itemColors")
    ItemColors banira$itemColors();
}
