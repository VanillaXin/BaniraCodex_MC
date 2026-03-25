package xin.vanilla.banira.client.event;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 替代已移除的 {@code RenderGuiOverlayEvent}：在对应 HUD 层绘制流程中由 Mixin 触发。
 */
public final class BaniraGuiOverlayEvent {
    private BaniraGuiOverlayEvent() {
    }

    /** 与原 Forge {@code VanillaGuiOverlay.PLAYER_LIST} 一致的层 id（用于回调判断）。 */
    public static final ResourceLocation PLAYER_LIST = ResourceLocation.fromNamespaceAndPath("minecraft", "player_list");

    /**
     * 对应原 {@code RenderGuiOverlayEvent.Post}。
     */
    public record Post(GuiGraphics guiGraphics, float partialTick, ResourceLocation overlayId) {
    }
}
