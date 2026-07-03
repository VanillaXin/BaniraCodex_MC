package xin.vanilla.banira.client.event;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 跨版本统一的 HUD overlay 事件：不同 Forge 版本的原生 HUD 事件差异由 Mixin/adapter 转成此对象。
 */
public final class BaniraGuiOverlayEvent {
    private BaniraGuiOverlayEvent() {
    }

    /**
     * 与原 Forge {@code VanillaGuiOverlay.PLAYER_LIST} 一致的层 id（用于回调判断）。
     */
    public static final ResourceLocation PLAYER_LIST = new ResourceLocation("minecraft", "player_list");

    /**
     * 对应 HUD 层绘制前。
     */
    public record Pre(GuiGraphics guiGraphics, float partialTick, ResourceLocation overlayId) {
    }

    /**
     * 对应 HUD 层绘制后。
     */
    public record Post(GuiGraphics guiGraphics, float partialTick, ResourceLocation overlayId) {
    }
}
