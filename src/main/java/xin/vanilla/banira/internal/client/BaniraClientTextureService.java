package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import xin.vanilla.banira.common.data.KeyValue;

import java.io.InputStream;

/**
 * Version-sensitive client texture access used by the public texture utility facade.
 */
public final class BaniraClientTextureService {
    private BaniraClientTextureService() {
    }

    public static boolean isClientReady() {
        return Minecraft.getInstance() != null;
    }

    public static void registerDynamicTexture(ResourceLocation location, NativeImage image) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        textureManager.register(location, new DynamicTexture(image));
    }

    public static void releaseTexture(ResourceLocation location) {
        if (isClientReady() && location != null) {
            Minecraft.getInstance().getTextureManager().release(location);
        }
    }

    public static boolean isTextureAvailable(ResourceLocation location, boolean hasResource) {
        if (!isClientReady() || location == null || MissingTextureAtlasSprite.getLocation().equals(location)) {
            return false;
        }
        net.minecraft.client.renderer.texture.AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
        return texture != null ? texture.getId() != -1 : hasResource;
    }

    public static NativeImage dynamicTextureImage(ResourceLocation texture) {
        if (!isClientReady() || texture == null) {
            return null;
        }
        net.minecraft.client.renderer.texture.AbstractTexture gpuTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        return gpuTexture instanceof DynamicTexture ? ((DynamicTexture) gpuTexture).getPixels() : null;
    }

    public static NativeImage resourceTextureImage(ResourceLocation texture) {
        if (!isClientReady() || texture == null) {
            return null;
        }
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            try (InputStream inputStream = resource.getInputStream()) {
                return NativeImage.read(inputStream);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    public static KeyValue<Integer, Integer> gpuTextureSize(ResourceLocation location) {
        if (!isClientReady() || location == null) {
            return null;
        }
        net.minecraft.client.renderer.texture.AbstractTexture gpuTexture = Minecraft.getInstance().getTextureManager().getTexture(location);
        if (gpuTexture == null) {
            return null;
        }
        int tid = gpuTexture.getId();
        if (tid == -1) {
            return null;
        }
        int[] prev = new int[1];
        GL11.glGetIntegerv(GL11.GL_TEXTURE_BINDING_2D, prev);
        try {
            RenderSystem.bindTexture(tid);
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            return width > 0 && height > 0 ? new KeyValue<>(width, height) : null;
        } finally {
            RenderSystem.bindTexture(prev[0]);
        }
    }
}
