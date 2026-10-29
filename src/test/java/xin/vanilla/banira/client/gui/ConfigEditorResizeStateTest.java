package xin.vanilla.banira.client.gui;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xin.vanilla.banira.client.gui.widget.InputWidget;
import xin.vanilla.banira.internal.client.ConfigEditorContentTreeBuilder;
import xin.vanilla.banira.internal.client.ConfigEditorState;
import xin.vanilla.banira.internal.client.ui.UiViewState;
import xin.vanilla.banira.internal.client.ui.page.ConfigUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigEditorResizeStateTest {
    @Before
    public void setUp() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @After
    public void tearDown() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void captureWithoutFocusedInputClearsPreviousFocusAndSelection() throws Exception {
        ConfigEditorScreen screen = screen();
        UiViewState state = new UiViewState();
        state.selection("config:values/item:0/input", 1, 4);
        screen.captureUiState(state);
        assertEquals("", state.focusedField());
        assertEquals(0, state.selectionStart());
        assertEquals(0, state.selectionEnd());
    }

    @Test
    public void captureRetainsCurrentSearchFocusAndSelection() throws Exception {
        ConfigEditorScreen screen = screen();
        InputWidget input = allocate(InputWidget.class);
        set(input, "focused", true);
        set(input, "cursorPosition", 2);
        set(input, "highlightPos", 6);
        set(screen, "searchInput", input);
        UiViewState state = new UiViewState();
        state.selection("config:values/item:0/input", 1, 4);
        screen.captureUiState(state);
        assertEquals("search", state.focusedField());
        assertEquals(2, Math.min(state.selectionStart(), state.selectionEnd()));
        assertEquals(6, Math.max(state.selectionStart(), state.selectionEnd()));
    }

    @Test
    public void nativeResizeCapturesInteractionStateBeforeRebuildingWidgets() throws Exception {
        ClassNode type = new ClassNode();
        try (InputStream input = ConfigEditorScreen.class.getResourceAsStream("ConfigEditorScreen.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        MethodNode resize = type.methods.stream().filter(method -> method.name.equals("resize")
                && method.desc.equals("(Lnet/minecraft/client/Minecraft;II)V")).findFirst().orElse(null);
        assertNotNull("Native resize must capture current focus, selection and scroll before init", resize);
        List<String> calls = new ArrayList<>();
        int superResize = -1;
        for (AbstractInsnNode instruction : resize.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            calls.add(call.name);
            if (call.name.equals("resize") && call.getOpcode() == Opcodes.INVOKESPECIAL) superResize = calls.size() - 1;
            assertFalse("Resize must not write configuration", call.name.equals("save") || call.name.equals("sync"));
            assertFalse("Resize must retain the session", call.owner.endsWith("/ConfigUiSession") && call.name.equals("<init>"));
        }
        int capture = calls.indexOf("captureUiState"), prepare = calls.indexOf("prepareUiState");
        assertTrue("Capture and prepare must precede native widget initialization",
                capture >= 0 && prepare > capture && superResize > prepare);
    }

    private static ConfigEditorScreen screen() throws Exception {
        ConfigEditorScreen screen = allocate(ConfigEditorScreen.class);
        ConfigEditorState editor = new ConfigEditorState(null);
        set(screen, "uiSession", allocate(ConfigUiSession.class));
        set(screen, "editorState", editor);
        set(screen, "contentTreeBuilder", new ConfigEditorContentTreeBuilder(screen, null, null, editor, () -> {
        }, 20, 2));
        return screen;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        for (Class<?> owner = target.getClass(); owner != null; owner = owner.getSuperclass()) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }
}
