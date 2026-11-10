package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class InputFormRenderMutationTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.util.SharedConstants.getCurrentVersion();
        net.minecraft.util.registry.Bootstrap.bootStrap();
    }

    @Test
    public void openingCompletionDuringUpdateDoesNotMutateTheCurrentFrame() throws Exception {
        Form screen = form();
        AtomicInteger drawn = new AtomicInteger();
        AtomicBoolean added = new AtomicBoolean();
        Probe overlay = new Probe(() -> {
        }, drawn);
        screen.addOverlayWidget(new Probe(() -> {
            if (added.compareAndSet(false, true)) screen.addOverlayWidget(overlay);
        }, new AtomicInteger()));
        screen.drawFrame();
        assertEquals(0, drawn.get());
        screen.drawFrame();
        assertEquals(1, drawn.get());
    }

    @Test
    public void closingCompletionDuringUpdateDoesNotInvalidateTheIterator() throws Exception {
        Form screen = form();
        AtomicInteger drawn = new AtomicInteger();
        Probe overlay = new Probe(() -> {
        }, drawn);
        screen.addOverlayWidget(new Probe(() -> screen.removeOverlayWidget(overlay), new AtomicInteger()));
        screen.addOverlayWidget(overlay);
        screen.drawFrame();
        int first = drawn.get();
        screen.drawFrame();
        assertEquals(first, drawn.get());
        assertFalse(screen.widgets().contains(overlay));
    }

    private static Form form() throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field instance = unsafe.getDeclaredField("theUnsafe");
        instance.setAccessible(true);
        Form form = (Form) unsafe.getMethod("allocateInstance", Class.class).invoke(instance.get(null), Form.class);
        set(form, "widgets", new ArrayList<>());
        set(form, "widgetMap", new HashMap<>());
        set(form, "renderWidgetSnapshot", new ArrayList<>());
        set(form, "renderWidgetSnapshotDirty", true);
        return form;
    }

    private static void set(Form form, String name, Object value) throws Exception {
        Field field = BaniraScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(form, value);
    }

    private static final class Form extends InputFormScreen {
        private Form() {
            super(new InputFormScreen.Args());
        }

        public BaniraColorConfig getEffectiveTheme() {
            return BaniraColorConfig.forSeason(EnumSeason.SPRING);
        }

        void drawFrame() {
            super.renderWidgets(null, 0);
        }
    }

    private static final class Probe extends BaseWidget {
        private final Runnable onUpdate;
        private final AtomicInteger renders;

        Probe(Runnable onUpdate, AtomicInteger renders) {
            super(null, new ScreenCoordinate(0, 0, 10, 10));
            this.onUpdate = onUpdate;
            this.renders = renders;
        }

        public void update() {
            onUpdate.run();
        }

        public void render(MatrixStack graphics, float partialTicks) {
            renders.incrementAndGet();
        }
    }
}
