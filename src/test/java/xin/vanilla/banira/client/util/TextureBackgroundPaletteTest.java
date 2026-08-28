package xin.vanilla.banira.client.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextureBackgroundPaletteTest {

    @Test
    public void isolatedDecorationPixelDoesNotBecomeATextBackground() {
        int[] pixels = {
                0xFFF1F7EE, 0xFFF0F6ED, 0xFFF2F8EF, 0xFFF1F7EE, 0xFFF0F6ED,
                0xFFF2F8EF, 0xFFF1F7EE, 0xFFF0F6ED, 0xFFF2F8EF, 0xFF111111
        };

        int[] palette = TextureUtils.representativeBackgroundColors(pixels);

        assertEquals(1, palette.length);
    }

    @Test
    public void twoSubstantialBackgroundRegionsAreBothRetained() {
        int[] pixels = {
                0xFFF1F7EE, 0xFFF0F6ED, 0xFFF2F8EF, 0xFFF1F7EE, 0xFFF0F6ED,
                0xFF334455, 0xFF344556, 0xFF334455, 0xFF344556, 0xFF334455
        };

        int[] palette = TextureUtils.representativeBackgroundColors(pixels);

        assertEquals(2, palette.length);
    }

    @Test
    public void translucentBackgroundIncludesDarkAndLightCompositedExtremes() {
        int[] pixels = {
                0xB3FFFFFF, 0xB3FFFFFF, 0xB3FFFFFF, 0xB3FFFFFF
        };

        int[] palette = TextureUtils.representativeBackgroundColors(pixels);

        assertEquals(2, palette.length);
    }
}
