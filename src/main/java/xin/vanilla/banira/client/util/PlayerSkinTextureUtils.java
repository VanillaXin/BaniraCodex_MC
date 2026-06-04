package xin.vanilla.banira.client.util;

import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.EnvironmentUtils;
import xin.vanilla.banira.common.util.PlayerUtils;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client-side helpers for slicing player skin resources into GUI textures.
 */
public final class PlayerSkinTextureUtils {

    private PlayerSkinTextureUtils() {
    }

    /**
     * Returns the front face and hat-layer textures for a player skin.
     */
    @Nullable
    public static Texture[] headFaceTextures(@Nullable ResourceLocation skin) {
        if (!EnvironmentUtils.isClient() || skin == null) {
            return null;
        }
        KeyValue<Integer, Integer> wh = TextureUtils.resolveTextureSizeForDraw(skin);
        int tw = wh.key();
        int th = wh.val();
        if (tw <= 0 || th <= 0) {
            tw = 64;
            th = 64;
        }
        int uFace = skinTemplateU(8, tw);
        int vFace = skinTemplateV(8, th);
        int uHat = skinTemplateU(40, tw);
        int side = skinTemplateSize(8, tw);
        Texture base = Texture.of(skin, tw, th).u0(uFace).v0(vFace).uWidth(side).vHeight(side);
        Texture overlay = Texture.of(skin, tw, th).u0(uHat).v0(vFace).uWidth(side).vHeight(side);
        return new Texture[]{base, overlay};
    }

    @Nullable
    public static Texture[] headFaceTextures(@Nullable UUID uuid) {
        return headFaceTextures(PlayerUtils.getPlayerSkin(uuid));
    }

    private static int skinTemplateU(int uStd, int texW) {
        return Math.round(uStd * (texW / 64f));
    }

    private static int skinTemplateV(int vStd, int texH) {
        if (texH < 64) {
            return vStd;
        }
        return Math.round(vStd * (texH / 64f));
    }

    private static int skinTemplateSize(int sizeStd, int texW) {
        return Math.round(sizeStd * (texW / 64f));
    }
}
