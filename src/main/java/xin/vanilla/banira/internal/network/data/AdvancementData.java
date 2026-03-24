package xin.vanilla.banira.internal.network.data;

import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;

import javax.annotation.Nonnull;

/**
 * 进度信息
 */
@Data
@Accessors(chain = true, fluent = true)
public class AdvancementData {
    private final ResourceLocation id;
    private final DisplayInfo displayInfo;

    @Nonnull
    public ResourceLocation id() {
        return id == null ? Identifier.id().empty() : id;
    }

    @Nonnull
    public DisplayInfo displayInfo() {
        return displayInfo == null ? emptyDisplayInfo() : displayInfo;
    }

    public AdvancementData(@Nonnull ResourceLocation id, DisplayInfo displayInfo) {
        this.id = id;
        if (displayInfo == null) {
            this.displayInfo = emptyDisplayInfo();
        } else {
            this.displayInfo = displayInfo;
        }
    }

    public static AdvancementData fromAdvancement(Advancement advancement) {
        DisplayInfo displayInfo = advancement.getDisplay();
        if (displayInfo == null) {
            return new AdvancementData(advancement.getId(), createDisplayInfo(advancement.getId().toString()));
        }
        return new AdvancementData(advancement.getId(), displayInfo);
    }

    public static AdvancementData readFromBuffer(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        return new AdvancementData(id, DisplayInfo.fromNetwork(buffer));
    }

    public static DisplayInfo emptyDisplayInfo() {
        return createDisplayInfo("");
    }

    public static DisplayInfo createDisplayInfo(String title) {
        return createDisplayInfo(title, "", new ItemStack(Items.AIR));
    }

    public static DisplayInfo createDisplayInfo(String title, String description) {
        return createDisplayInfo(title, description, new ItemStack(Items.AIR));
    }

    public static DisplayInfo createDisplayInfo(String title, String description, ItemStack itemStack) {
        return new DisplayInfo(itemStack
                , BaniraComponent.get().literal(title).toVanilla(), BaniraComponent.get().literal(description).toVanilla()
                , Identifier.id().empty(), FrameType.TASK
                , false, false, false);
    }

    public void writeToBuffer(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(id);
        displayInfo.serializeToNetwork(buffer);
    }
}
