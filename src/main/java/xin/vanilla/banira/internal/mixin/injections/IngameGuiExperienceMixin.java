package xin.vanilla.banira.internal.mixin.injections;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.client.BaniraHudSyntheticEvents;

@Mixin(Gui.class)
public class IngameGuiExperienceMixin {
    @Unique
    private boolean banira$experienceBarCanceled;
    @Unique
    private boolean banira$experienceBarFinished;
    @Unique
    private boolean banira$experienceTextStarted;
    @Unique
    private boolean banira$experienceTextCanceled;
    @Unique
    private int banira$experienceX;

    @Inject(method = "renderExperienceBar", at = @At("HEAD"))
    private void banira$beforeExperienceBar(PoseStack stack, int x, CallbackInfo ci) {
        banira$experienceX = x;
        banira$experienceBarFinished = false;
        banira$experienceTextStarted = false;
        banira$experienceTextCanceled = false;
        banira$experienceBarCanceled = BaniraHudSyntheticEvents.beforeExperienceBar(stack, x);
    }

    @Redirect(
            method = "renderExperienceBar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"
            ),
            require = 0
    )
    private void banira$drawExperienceBar(Gui gui, PoseStack stack, int x, int y, int u, int v, int width, int height) {
        if (!banira$experienceBarCanceled) {
            gui.blit(stack, x, y, u, v, width, height);
        }
    }

    @Redirect(
            method = "renderExperienceBar",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I"
            ),
            require = 0
    )
    private int banira$experienceLevel(LocalPlayer player, PoseStack stack, int x) {
        if (!banira$experienceTextStarted) {
            banira$finishExperienceBar(stack, banira$experienceX);
            banira$experienceTextStarted = true;
            banira$experienceTextCanceled = BaniraHudSyntheticEvents.beforeExperienceText(stack, banira$experienceX);
        }
        return banira$experienceTextCanceled ? 0 : player.experienceLevel;
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void banira$afterExperienceBar(PoseStack stack, int x, CallbackInfo ci) {
        banira$finishExperienceBar(stack, x);
        if (banira$experienceTextStarted) {
            BaniraHudSyntheticEvents.afterExperienceText(stack, x);
        }
    }

    @Unique
    private void banira$finishExperienceBar(PoseStack stack, int x) {
        if (!banira$experienceBarFinished) {
            banira$experienceBarFinished = true;
            BaniraHudSyntheticEvents.afterExperienceBar(stack, x);
        }
    }
}
