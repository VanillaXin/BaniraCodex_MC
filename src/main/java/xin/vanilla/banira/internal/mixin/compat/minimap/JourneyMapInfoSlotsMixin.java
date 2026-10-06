package xin.vanilla.banira.internal.mixin.compat.minimap;

import journeymap.client.ui.theme.ThemeLabelSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xin.vanilla.banira.internal.compat.minimap.LegacyJourneyMapNotifications;

@Pseudo
@Mixin(targets = "journeymap.client.ui.minimap.DisplayVars", remap = false)
public abstract class JourneyMapInfoSlotsMixin {
    @ModifyArg(method = "<init>", index = 2, require = 0, remap = false,
            at = @At(value = "INVOKE", target = "Ljourneymap/client/ui/minimap/DisplayVars;getInfoLabelAreaHeight", ordinal = 1, remap = false))
    private ThemeLabelSource.InfoSlot[] banira$measureUnread(ThemeLabelSource.InfoSlot[] slots) {
        return LegacyJourneyMapNotifications.append(slots);
    }

    @ModifyArg(method = "<init>", index = 4, require = 0, remap = false,
            at = @At(value = "INVOKE", target = "Ljourneymap/client/ui/minimap/DisplayVars;positionLabels", ordinal = 1, remap = false))
    private ThemeLabelSource.InfoSlot[] banira$positionUnread(ThemeLabelSource.InfoSlot[] slots) {
        return LegacyJourneyMapNotifications.append(slots);
    }
}
