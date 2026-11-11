package xin.vanilla.banira.internal.compat.aui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.render.Base;
import com.sighs.apricityui.render.Mask;
import com.sighs.apricityui.spi.AuiServices;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Manual documents remain owner-controlled, but rasterization runs in the native PIP pass.
 */
public final class AuiDocumentRenderSubmission {
    private static final Map<GuiGraphicsExtractor, Set<Document>> SUBMITTED = new WeakHashMap<>();
    private static final Map<Document, Throwable> FAILURES = new WeakHashMap<>();
    private static boolean registered;

    private AuiDocumentRenderSubmission() {
    }

    public static void register() {
        if (registered) return;
        PictureInPictureRendererRegistry.register(context -> new Renderer(context.bufferSource()));
        registered = true;
    }

    public static void submit(GuiGraphicsExtractor graphics, Document document) {
        submit(graphics, document, false);
    }

    public static void submit(GuiGraphicsExtractor graphics, Document document, boolean foreground) {
        if (!registered) throw new IllegalStateException("Manual AUI renderer was not registered");
        Throwable failure = FAILURES.remove(document);
        if (failure != null) throw new IllegalStateException("Manual AUI render failed", failure);
        if (document.isDisposed()) throw new IllegalStateException("Manual AUI document was disposed");
        Set<Document> documents = SUBMITTED.computeIfAbsent(graphics,
                ignored -> Collections.newSetFromMap(new IdentityHashMap<>()));
        if (!documents.add(document)) return;
        graphics.guiRenderState.addPicturesInPictureState(new State(document, document.getRefreshGeneration(), foreground,
                0, 0, graphics.guiWidth(), graphics.guiHeight(), 1f, graphics.scissorStack.peek()));
    }

    private record State(Document document, long generation, boolean foreground, int x0, int y0, int x1, int y1,
                         float scale, ScreenRectangle scissorArea) implements PictureInPictureRenderState {
        @Override
        public ScreenRectangle bounds() {
            return PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
        }
    }

    private static final class Renderer extends PictureInPictureRenderer<State> {

        private Renderer(MultiBufferSource.BufferSource buffers) {
            super(buffers);
        }

        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected String getTextureLabel() {
            return "banira_manual_aui";
        }

        @Override
        protected void renderToTexture(State state, PoseStack ignored) {
            if (state.document.isDisposed() || state.generation != state.document.getRefreshGeneration()) return;
            var modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            try {
                modelView.identity();
                float range = Math.max(1024, Base.getFlatOverlayZ() + Base.getGuiItemDecorationZ() + 1);
                AuiServices.render().setProjectionMatrix(new Matrix4f().setOrtho(
                        0, state.x1 - state.x0, state.y1 - state.y0, 0, -range, range));
                Mask.resetDepth();
                try (Document.ContextScope context = Document.withContext(state.document)) {
                    if (state.foreground)
                        Base.drawOverlayDocument(new PoseStack(), state.document, Base.getFlatOverlayZ());
                    else Base.drawOverlayDocument(new PoseStack(), state.document);
                    Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
                } finally {
                    Mask.resetDepth();
                }
            } catch (RuntimeException | LinkageError failure) {
                FAILURES.put(state.document, failure);
                org.apache.logging.log4j.LogManager.getLogger(AuiDocumentRenderSubmission.class)
                        .warn("Manual AUI render failed; native fallback will resume on the next extraction", failure);
            } finally {
                modelView.popMatrix();
            }
        }
    }
}
