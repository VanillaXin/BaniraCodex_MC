package xin.vanilla.banira.internal.network.data;

import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * 进度信息
 */
@Data
@Accessors(chain = true, fluent = true)
public class AdvancementData {
    private static final ItemStack WIRE_NO_ICON_PLACEHOLDER = new ItemStack(Items.STRUCTURE_VOID);

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

    public static AdvancementData fromHolder(AdvancementHolder holder) {
        Advancement advancement = holder.value();
        return advancement.display()
                .map(d -> new AdvancementData(holder.id(), d))
                .orElseGet(() -> new AdvancementData(holder.id(), createDisplayInfo(holder.id().toString())));
    }

    public static AdvancementData readFromBuffer(RegistryFriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        boolean hasTabListIcon = buffer.readBoolean();
        DisplayInfo decoded = DisplayInfo.STREAM_CODEC.decode(buffer);
        DisplayInfo display = hasTabListIcon ? decoded : copyDisplayWithIcon(decoded, ItemStack.EMPTY);
        return new AdvancementData(id, display);
    }

    public static DisplayInfo emptyDisplayInfo() {
        return createDisplayInfo("");
    }

    public static DisplayInfo createDisplayInfo(String title) {
        return createDisplayInfo(title, "", ItemStack.EMPTY);
    }

    public static DisplayInfo createDisplayInfo(String title, String description) {
        return createDisplayInfo(title, description, ItemStack.EMPTY);
    }

    public static DisplayInfo createDisplayInfo(String title, String description, ItemStack itemStack) {
        ItemStack icon = itemStack == null || itemStack.isEmpty() || itemStack.getItem() == Items.AIR
                ? ItemStack.EMPTY
                : itemStack;
        return new DisplayInfo(icon
                , BaniraComponent.get().literal(title).toVanilla(), BaniraComponent.get().literal(description).toVanilla()
                , Optional.empty(), AdvancementType.TASK
                , false, false, false);
    }

    public static boolean hasTabListIcon(@Nullable ItemStack icon) {
        return icon != null && !icon.isEmpty() && icon.getItem() != Items.AIR;
    }

    @Nonnull
    public static ItemStack iconStackForListRendering(@Nullable ItemStack fromDisplayInfo) {
        if (!hasTabListIcon(fromDisplayInfo)) {
            return ItemStack.EMPTY;
        }
        return fromDisplayInfo.copy();
    }

    public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(id);
        DisplayInfo src = displayInfo();
        boolean hasIcon = hasTabListIcon(src.getIcon());
        buffer.writeBoolean(hasIcon);
        ItemStack wireIcon = hasIcon ? src.getIcon().copy() : WIRE_NO_ICON_PLACEHOLDER.copy();
        DisplayInfo wire = copyDisplayWithIcon(src, wireIcon);
        DisplayInfo.STREAM_CODEC.encode(buffer, wire);
    }

    private static DisplayInfo copyDisplayWithIcon(DisplayInfo d, ItemStack icon) {
        return new DisplayInfo(icon,
                d.getTitle(), d.getDescription(), d.getBackground(), d.getType(),
                d.shouldShowToast(), d.shouldAnnounceChat(), d.isHidden());
    }
}
