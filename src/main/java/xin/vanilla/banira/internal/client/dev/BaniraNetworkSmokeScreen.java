package xin.vanilla.banira.internal.client.dev;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.internal.DebugScreen;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeProfilePlan;

/** Render instrumentation and deterministic tooltip content for the dev-only network smoke. */
final class BaniraNetworkSmokeScreen extends DebugScreen {
    private int cycle;
    private Text tooltipText = Text.empty();
    private boolean tooltipSubmitted;
    private long renderFrames;
    private long renderTotalNanos;
    private long renderMaxNanos;
    private long tooltipFrames;
    private long tooltipColorFrames;
    private long tooltipTextureFrames;
    private int tooltipContentCycles;
    private int lastTooltipCycle;

    @Override
    public void runNetworkSmokeCycle(int cycle) {
        super.runNetworkSmokeCycle(cycle);
        this.cycle = cycle;
        StringBuilder content = new StringBuilder("Banira UI smoke cycle ").append(cycle);
        for (int line = 0; line <= cycle % 4; line++) {
            content.append('\n').append("Tooltip content ").append(line + 1).append(": ");
            for (int word = 0; word < 2 + cycle % 5; word++) {
                content.append("Banira ");
            }
        }
        tooltipText = Text.literal(content.toString());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        tooltipSubmitted = false;
        long startedAt = System.nanoTime();
        super.render(graphics, mouseX, mouseY, partialTicks);
        // Includes the widget tree and deferred tooltip preparation, not the later screen-post flush or GPU time.
        long elapsed = System.nanoTime() - startedAt;
        renderFrames++;
        renderTotalNanos += elapsed;
        renderMaxNanos = Math.max(renderMaxNanos, elapsed);
        if (tooltipSubmitted) {
            tooltipFrames++;
            if (useTooltipTexture()) tooltipTextureFrames++;
            else tooltipColorFrames++;
            if (cycle != lastTooltipCycle) {
                tooltipContentCycles++;
                lastTooltipCycle = cycle;
            }
        }
    }

    @Override
    public void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.onRender(graphics, mouseX, mouseY, partialTicks);
        if (cycle > 0) {
            // Submit last through BaniraScreen's deferred route; normal screen-post handling owns the transition flush.
            addDeferredTooltipRender(this::submitSmokeTooltip);
        }
    }

    private void submitSmokeTooltip(GuiGraphics graphics) {
        PoseStack stack = graphics.pose();
        stack.pushPose();
        try {
            stack.last().pose().identity();
            TooltipWidget.drawPopupMessage(stack,
                    FontDrawArgs.ofPopo(tooltipText.stack(stack).font(getFont()))
                            .x(width / 2.0D).y(height / 2.0D)
                            .fontSize(8 + cycle % 4).wrap(true).maxWidth(120 + cycle % 4 * 24)
                            .popupUseTexture(useTooltipTexture()),
                    getEffectiveTheme(), season());
            tooltipSubmitted = true;
        } finally {
            stack.popPose();
        }
    }

    private boolean useTooltipTexture() {
        return (cycle / 4 & 1) == 0;
    }

    boolean workloadVerified() {
        return renderFrames > 0 && tooltipFrames > 0
                && tooltipColorFrames > 0 && tooltipTextureFrames > 0
                && tooltipContentCycles >= BaniraNetworkSmokeProfilePlan.MINIMUM_CYCLES;
    }

    String workloadSummary() {
        long averageNanos = renderFrames == 0 ? 0L : renderTotalNanos / renderFrames;
        return "render-frames=" + renderFrames + " render-cpu-average-ns=" + averageNanos
                + " render-cpu-max-ns=" + renderMaxNanos + " tooltip-submitted-frames=" + tooltipFrames
                + " tooltip-content-cycles=" + tooltipContentCycles
                + " tooltip-color-frames=" + tooltipColorFrames + " tooltip-texture-frames=" + tooltipTextureFrames;
    }
}
