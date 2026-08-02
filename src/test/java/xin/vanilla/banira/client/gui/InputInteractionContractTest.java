package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 键盘表单、组合键捕获和长菜单必须保留统一交互边界。 */
public class InputInteractionContractTest {
    private static final Path MAIN = Paths.get("src/main/java/xin/vanilla/banira");

    @Test
    public void tabCyclesFieldsAndCapturedChordCommitsOnRelease() throws Exception {
        String form = source("client/gui/InputFormScreen.java");
        String capture = source("client/gui/widget/KeyCaptureInputWidget.java");
        String tags = source("client/gui/widget/TagListEditorWidget.java");
        assertTrue(form.contains("GLFW_KEY_TAB"));
        assertTrue(form.contains("focusAdjacentInput"));
        assertTrue(form.contains("scrollFieldIntoView"));
        assertTrue(capture.contains("protected boolean onKeyRelease"));
        assertTrue(capture.contains("capturedKeys"));
        assertTrue(capture.contains("pressedKeys.isEmpty()"));
        assertTrue(capture.contains("GLFWKeyUtils.getKeyDisplayString"));
        assertTrue(tags.contains("onCaptured(value -> confirmAddFromInput())"));
        assertTrue(tags.contains("onCaptured(value -> commitInlineEdit())"));
    }

    @Test
    public void formsForwardExplicitLengthAndTitleTooltipMetadata() throws Exception {
        String form = source("client/gui/InputFormScreen.java");
        assertTrue(form.contains("private int maxLength = 256"));
        assertTrue(form.contains("inputWidget.maxLength(widget.maxLength())"));
        assertTrue(form.contains("dd.maxLength(widget.maxLength())"));
        assertTrue(form.contains("titleLabel.tooltip(titleTooltip)"));
        assertTrue(form.contains("StringUtils.isNotNullOrEmpty(widget.regex())"));
    }

    @Test
    public void popupDefaultsToTwoThirdsScreenHeight() throws Exception {
        String popup = source("client/gui/widget/PopupOption.java");
        String screen = source("client/gui/BaniraScreen.java");
        assertTrue(popup.contains("screenHeight * 2 / 3"));
        assertTrue(popup.contains("addScrollOffset"));
        assertTrue(screen.contains("this.popupOption.addScrollOffset(delta)"));
    }

    @Test
    public void selectedDropdownPreviewUsesDisplayLabels() throws Exception {
        String dropdown = source("client/gui/widget/DropdownSelectWidget.java");
        String preview = source("client/gui/widget/DropdownPreviewOverlayWidget.java");
        assertTrue(dropdown.contains("String displayLabelForValue"));
        assertTrue(preview.contains("parent.displayLabelForValue(items.get(i))"));
    }

    private static String source(String relative) throws Exception {
        return new String(Files.readAllBytes(MAIN.resolve(relative)), StandardCharsets.UTF_8);
    }
}
