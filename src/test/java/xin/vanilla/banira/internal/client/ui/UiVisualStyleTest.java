package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.util.ColorUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UiVisualStyleTest {
    private static Map<String, Integer> palette(BaniraColorConfig theme) {
        Map<String, Integer> result = new HashMap<>();
        for (String entry : UiColorBridge.tokens(theme).split(";")) {
            String[] pair = entry.split(":", 2);
            if (pair.length == 2)
                result.put(pair[0], Color.rgba((int) Long.parseLong(pair[1].substring(1), 16)).getArgb());
        }
        return result;
    }

    @Test
    public void nativeTokensPreserveTheirRgbaEncoding() {
        assertEquals("#12345678", UiColorBridge.css(0x78123456));
        Map<String, Integer> colors = palette(new BaniraColorConfig().bgSurface(0xff102030).accent(0xffabcdef));
        assertEquals(Integer.valueOf(0xff102030), colors.get("--surface"));
        assertEquals(Integer.valueOf(0xffabcdef), colors.get("--accent"));
    }

    @Test
    public void neutralPageSurfacesRetainSeasonalAccentAndNativePalette() {
        for (int accent : new int[]{0xffed8438, 0xff6fa16a, 0xffcc6c98, 0xff8c9ed6}) {
            BaniraColorConfig theme = new BaniraColorConfig().bgPrimary(0xfffff4e8).bgSurface(0xffffe8d4)
                    .textPrimary(0xff684932).accent(accent);
            Map<String, Integer> colors = palette(theme);
            for (String role : new String[]{"--canvas", "--panel", "--panel-soft", "--line"}) {
                assertNotNull(role, colors.get(role));
                int value = colors.get(role);
                int red = value >> 16 & 255, green = value >> 8 & 255, blue = value & 255;
                assertTrue(role, Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) <= 14);
            }
            assertEquals(Integer.valueOf(accent), colors.get("--accent"));
            assertEquals(Integer.valueOf(0xffffe8d4), colors.get("--surface"));
            assertEquals(0xfffff4e8, theme.bgPrimary());
        }
    }

    @Test
    public void pagePaletteFollowsLightAndDarkThemeWithoutLosingContrast() {
        for (int background : new int[]{0xfffaf7ef, 0xff171b1c}) {
            BaniraColorConfig theme = new BaniraColorConfig().bgPrimary(background).accent(0xffce739b).textPrimary(0xff9a7b6c);
            Map<String, Integer> colors = palette(theme);
            assertNotNull(colors.get("--ink"));
            boolean dark = ColorUtils.getBrightnessFromArgb(background) < .5f;
            assertEquals(dark, ColorUtils.getBrightnessFromArgb(colors.get("--canvas")) < .5f);
            assertEquals((int) colors.get("--ink"), ColorUtils.ensureReadableTextArgb(colors.get("--ink"), colors.get("--panel")));
            assertEquals((int) colors.get("--action-text"), ColorUtils.ensureReadableTextArgb(colors.get("--action-text"), colors.get("--action")));
        }
    }

    @Test
    public void buttonsAndIconSlotsHaveStableCenteredGeometry() throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/shared.css")) {
            assertNotNull(input);
            String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(css.contains(".row-icon"));
            assertTrue(css.contains(".game-icon"));
            assertTrue(css.matches("(?s).*height:\\s*22px.*"));
            assertTrue(css.matches("(?s).*justify-content:\\s*center.*"));
            assertTrue(css.contains(".selector-selection #selected_icon"));
        }
    }

    @Test
    public void sharedControlsUseDedicatedPageRolesAndCompactStableSizes() throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/shared.css")) {
            assertNotNull(input);
            String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String role : new String[]{"--canvas", "--panel", "--panel-soft", "--line", "--ink", "--muted", "--selected", "--action", "--action-text"}) {
                assertTrue(role, css.contains("var(" + role));
            }
            assertTrue(css.contains(".primary:hover"));
            assertTrue(css.contains(".row > button:hover"));
            assertTrue(css.matches("(?s).*min-height:\\s*20px.*"));
            assertTrue(css.matches("(?s).*letter-spacing:\\s*0.*"));
            assertFalse(css.contains("@import"));
            assertFalse(css.contains("@font-face"));
            assertFalse(css.contains("box-shadow:"));
        }
    }

    @Test
    public void primaryButtonsPreferWhiteWithoutLosingAccentHueOrContrast() {
        for (int accent : new int[]{0xffee7fa8, 0xff65c978, 0xfff59a4c, 0xff78bdf7, 0xff202020}) {
            BaniraColorConfig theme = new BaniraColorConfig().accent(accent);
            Map<String, Integer> colors = palette(theme);
            assertEquals(Integer.valueOf(0xffffffff), colors.get("--action-text"));
            assertEquals(0xffffffff, ColorUtils.ensureReadableTextArgb(0xffffffff, colors.get("--action")));
            assertEquals(0xffffffff, ColorUtils.ensureReadableTextArgb(0xffffffff, colors.get("--action-hover")));
            assertEquals(accent, theme.accent());
            assertEquals(Integer.valueOf(accent), colors.get("--accent"));
        }
    }

    @Test
    public void primaryLabelsAndDisabledLabelsUseTheirOwnForeground() throws Exception {
        String css = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/assets/apricityui/apricity/banira_codex/shared.css"), StandardCharsets.UTF_8);
        assertTrue(css.contains(".primary>*{color:var(--action-text,#ffffff)}"));
        assertTrue(css.contains(".primary:disabled>*{color:var(--muted,#637168)}"));
        assertTrue(css.contains(".sakura-ui .primary:hover"));
        assertTrue(css.contains("var(--action,#b74777)"));
    }

    @Test
    public void sharedStyleBudgetSurvivesWindowsCheckout() throws Exception {
        int bytes = 0;
        for (var entry : UiBoundaryChecks.ownResources("banira_codex").entrySet())
            if (entry.getKey().endsWith(".css")) {
                String text = new String(entry.getValue(), StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\n", "\r\n");
                bytes += text.getBytes(StandardCharsets.UTF_8).length;
            }
        assertTrue("Shared CSS must fit 16KiB after CRLF checkout", bytes <= 16384);
    }
}
