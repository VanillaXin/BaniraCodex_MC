package xin.vanilla.banira.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.effect.MobEffectInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.internal.client.BaniraClientEventHub;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.IIdentifier;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public final class TextureUtils {
    private TextureUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 药水图标文件夹路径
     */
    public static final String DEFAULT_EFFECT_DIR = "textures/mob_effect/";

    private static final Map<ResourceLocation, NativeImage> CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, KeyValue<Integer, Integer>> TEXTURE_SIZE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Texture, NinePatchInfo> NINE_PATCH_CACHE = new ConcurrentHashMap<>();

    /**
     * 由 {@link #loadCustomTexture} 注册到 {@link TextureManager} 的外部文件动态纹理；{@link #clearAll()} 时 {@link TextureManager#release}，{@link #getTextureImage} 据此读取像素。
     */
    private static final Set<ResourceLocation> REGISTERED_DYNAMIC_TEXTURE_LOCATIONS = ConcurrentHashMap.newKeySet();

    private static String normalizeTexturePath(String name) {
        String n = name.replace('\\', '/');
        if (n.startsWith("./")) {
            n = n.substring(2);
        }
        return n;
    }

    private static boolean looksLikeWindowsDrivePath(String normalized) {
        return normalized.length() >= 2 && normalized.charAt(1) == ':' && Character.isLetter(normalized.charAt(0));
    }

    public static ResourceLocation loadCustomTexture(IIdentifier factory, String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return factory.empty();
        }
        String normalized = normalizeTexturePath(name);
        String safePath = getSafeTexturePath(normalized);
        ResourceLocation rl = factory.create(safePath);
        ResourceManager resourceManager = mc.getResourceManager();
        TextureManager textureManager = mc.getTextureManager();

        // region 资源包纹理
        if (resourceManager.getResource(rl).isPresent()) {
            return rl;
        }
        if (!looksLikeWindowsDrivePath(normalized) && normalized.indexOf(':') >= 0) {
            ResourceLocation parsed = ResourceLocation.tryParse(normalized);
            if (parsed != null && resourceManager.getResource(parsed).isPresent()) {
                return parsed;
            }
        }
        // endregion 资源包纹理

        // region 外部文件动态纹理
        File textureFile = new File(normalized);
        if (textureFile.isFile()) {
            try {
                textureFile = textureFile.getCanonicalFile();
            } catch (IOException ignored) {
            }
            ResourceLocation externalRl = factory.create("dynamic/ext_" + Integer.toHexString(textureFile.getAbsolutePath().hashCode()));
            synchronized (TextureUtils.class) {
                if (REGISTERED_DYNAMIC_TEXTURE_LOCATIONS.contains(externalRl)) {
                    return externalRl;
                }
                try (InputStream inputStream = Files.newInputStream(textureFile.toPath())) {
                    NativeImage nativeImage = NativeImage.read(inputStream);
                    DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                    textureManager.register(externalRl, dynamicTexture);
                    REGISTERED_DYNAMIC_TEXTURE_LOCATIONS.add(externalRl);
                    return externalRl;
                } catch (IOException e) {
                    LOGGER.warn("Failed to load texture from file: {}", textureFile.getAbsolutePath());
                    LOGGER.error(e);
                    return factory.empty();
                }
            }
        }
        // endregion 外部文件动态纹理

        LOGGER.warn("Texture not found in resources or external: {}", name);
        return factory.empty();
    }

    public static String getSafeTexturePath(String path) {
        return path.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }

    public static boolean isTextureAvailable(ResourceLocation location) {
        if (location == null) {
            return false;
        }
        if (MissingTextureAtlasSprite.getLocation().equals(location)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return false;
        }
        TextureManager textureManager = mc.getTextureManager();
        AbstractTexture miss = MissingTextureAtlasSprite.getTexture();

        if (REGISTERED_DYNAMIC_TEXTURE_LOCATIONS.contains(location)) {
            AbstractTexture t = textureManager.getTexture(location);
            if (t == miss) {
                return false;
            }
            if (!RenderSystem.isOnRenderThreadOrInit()) {
                return true;
            }
            return t.getId() != -1;
        }

        ResourceManager resourceManager = mc.getResourceManager();
        if (resourceManager.getResource(location).isEmpty()) {
            return false;
        }

        if (!RenderSystem.isOnRenderThreadOrInit()) {
            return true;
        }

        AbstractTexture texture = textureManager.getTexture(location);
        if (texture == miss) {
            return false;
        }
        return texture.getId() != -1;
    }

    /**
     * 获取药水效果图标
     */
    public static ResourceLocation getEffectTexture(IIdentifier factory, MobEffectInstance effectInstance) {
        ResourceLocation effectIcon;
        ResourceLocation registryName = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect());
        if (registryName != null) {
            effectIcon = factory.create(registryName.getNamespace(), DEFAULT_EFFECT_DIR + registryName.getPath() + ".png");
        } else {
            effectIcon = null;
        }
        return effectIcon;
    }

    /**
     * 九宫格信息
     */
    public static class NinePatchInfo {
        /**
         * 纹理宽度
         */
        public final int texWidth;
        /**
         * 纹理高度
         */
        public final int texHeight;
        /**
         * 水平分割点列表
         */
        public final int[] horizontalDivisions;
        /**
         * 垂直分割点列表
         */
        public final int[] verticalDivisions;
        /**
         * 水平方向每个区域是否可拉伸
         */
        public final boolean[] horizontalStretchable;
        /**
         * 垂直方向每个区域是否可拉伸
         */
        public final boolean[] verticalStretchable;
        /**
         * 右参考线高度
         */
        public final int rightGuideHeight;
        /**
         * 右参考线上内边距
         */
        public final int rightGuideTopPadding;
        /**
         * 右参考线下内边距
         */
        public final int rightGuideBottomPadding;
        /**
         * 下参考线左内边距
         */
        public final int bottomGuideLeftPadding;
        /**
         * 下参考线右内边距
         */
        public final int bottomGuideRightPadding;
        /**
         * 文字颜色（从最右下角像素点解析，ARGB格式）
         */
        public final int textColor;

        public NinePatchInfo(int texWidth, int texHeight,
                             int[] horizontalDivisions, int[] verticalDivisions,
                             boolean[] horizontalStretchable, boolean[] verticalStretchable,
                             int rightGuideHeight, int rightGuideTopPadding, int rightGuideBottomPadding,
                             int bottomGuideLeftPadding, int bottomGuideRightPadding, int textColor) {
            this.texWidth = texWidth;
            this.texHeight = texHeight;
            this.horizontalDivisions = horizontalDivisions;
            this.verticalDivisions = verticalDivisions;
            this.horizontalStretchable = horizontalStretchable;
            this.verticalStretchable = verticalStretchable;
            this.rightGuideHeight = rightGuideHeight;
            this.rightGuideTopPadding = rightGuideTopPadding;
            this.rightGuideBottomPadding = rightGuideBottomPadding;
            this.bottomGuideLeftPadding = bottomGuideLeftPadding;
            this.bottomGuideRightPadding = bottomGuideRightPadding;
            this.textColor = textColor;
        }
    }

    public static void clearAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            TextureManager tm = mc.getTextureManager();
            for (ResourceLocation loc : REGISTERED_DYNAMIC_TEXTURE_LOCATIONS) {
                tm.release(loc);
            }
        }
        REGISTERED_DYNAMIC_TEXTURE_LOCATIONS.clear();
        for (NativeImage img : CACHE.values()) {
            try {
                img.close();
            } catch (Exception ignored) {
            }
        }
        CACHE.clear();
        TEXTURE_SIZE_CACHE.clear();
        NINE_PATCH_CACHE.clear();
    }

    /**
     * 从资源中加载纹理并转换为 NativeImage。
     *
     * @param texture 纹理的 ResourceLocation
     */
    public static NativeImage getTextureImage(ResourceLocation texture) {
        // 优先从缓存中获取
        if (CACHE.containsKey(texture)) {
            return CACHE.get(texture);
        }
        if (REGISTERED_DYNAMIC_TEXTURE_LOCATIONS.contains(texture)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return null;
            }
            AbstractTexture gpuTexture = mc.getTextureManager().getTexture(texture);
            if (gpuTexture instanceof DynamicTexture dt) {
                return dt.getPixels();
            }
            return null;
        }
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            if (resourceManager.getResource(texture).isEmpty()) {
                return null;
            }
            Resource resource = resourceManager.getResource(texture).get();
            try (InputStream inputStream = resource.open()) {
                NativeImage nativeImage = NativeImage.read(inputStream);
                CACHE.put(texture, nativeImage);
                return nativeImage;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to load texture: {}", texture);
            return null;
        }
    }

    /**
     * 获取纹理的宽高
     */
    public static KeyValue<Integer, Integer> getTextureSize(ResourceLocation texture) {
        KeyValue<Integer, Integer> size = new KeyValue<>(0, 0);
        if (TEXTURE_SIZE_CACHE.containsKey(texture)) {
            size = TEXTURE_SIZE_CACHE.get(texture);
        } else {
            NativeImage textureImage = getTextureImage(texture);
            if (textureImage != null) {
                size.key(textureImage.getWidth()).value(textureImage.getHeight());
            }
            TEXTURE_SIZE_CACHE.put(texture, size);
        }
        return size;
    }

    /**
     * 解析用于绘制的纹理尺寸：优先资源/缓存图像，失败时从已上传的 GPU 纹理查询（适用于玩家皮肤等不在资源包中的纹理）
     */
    public static KeyValue<Integer, Integer> resolveTextureSizeForDraw(ResourceLocation texture) {
        KeyValue<Integer, Integer> cached = TEXTURE_SIZE_CACHE.get(texture);
        if (cached != null && cached.key() > 0 && cached.val() > 0) {
            return cached;
        }
        NativeImage textureImage = getTextureImage(texture);
        if (textureImage != null) {
            KeyValue<Integer, Integer> size = new KeyValue<>(textureImage.getWidth(), textureImage.getHeight());
            TEXTURE_SIZE_CACHE.put(texture, size);
            return size;
        }
        KeyValue<Integer, Integer> gpu = tryGetGpuTextureSize(texture);
        if (gpu != null && gpu.key() > 0 && gpu.val() > 0) {
            TEXTURE_SIZE_CACHE.put(texture, gpu);
            return gpu;
        }
        if (cached != null) {
            return cached;
        }
        KeyValue<Integer, Integer> zero = new KeyValue<>(0, 0);
        TEXTURE_SIZE_CACHE.put(texture, zero);
        return zero;
    }

    @Nullable
    private static KeyValue<Integer, Integer> tryGetGpuTextureSize(ResourceLocation location) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }
        net.minecraft.client.renderer.texture.AbstractTexture gpuTexture = mc.getTextureManager().getTexture(location);
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
            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (w <= 0 || h <= 0) {
                return null;
            }
            return new KeyValue<>(w, h);
        } finally {
            RenderSystem.bindTexture(prev[0]);
        }
    }

    /**
     * 解析.9.png格式的纹理
     *
     * @param texture 纹理对象
     * @return 九宫格信息，如果不是.9.png格式或解析失败则返回null
     */
    public static NinePatchInfo parseNinePatch(Texture texture) {
        if (texture == null || texture.location() == null) {
            return null;
        }

        // 优先从缓存中获取
        if (NINE_PATCH_CACHE.containsKey(texture)) {
            return NINE_PATCH_CACHE.get(texture);
        }

        NativeImage image = getTextureImage(texture.location());
        if (image == null) {
            return null;
        }

        // 使用 Texture 中指定的范围
        int textureStartX = texture.u0();
        int textureStartY = texture.v0();
        int textureWidth = texture.uWidth();
        int textureHeight = texture.vHeight();
        int textureEndX = textureStartX + textureWidth - 1;
        int textureEndY = textureStartY + textureHeight - 1;

        // 确保范围有效
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        if (textureStartX < 0 || textureStartY < 0 ||
                textureEndX >= imageWidth || textureEndY >= imageHeight ||
                textureWidth < 3 || textureHeight < 3) {
            return null;
        }

        // 黑色像素的阈值
        final int BLACK_THRESHOLD = 0x80; // 128

        // 内容区域的边界（相对于纹理范围的偏移）
        int contentStartX = textureStartX + 1;
        int contentEndX = textureEndX - 1;
        int contentStartY = textureStartY + 1;
        int contentEndY = textureEndY - 1;
        int contentWidth = contentEndX - contentStartX + 1;
        int contentHeight = contentEndY - contentStartY + 1;

        // 解析水平引导线
        // 找出所有分割点和每个区域是否可拉伸
        List<Integer> horizontalDivs = new ArrayList<>();
        List<Boolean> horizontalStretch = new ArrayList<>();

        // 首先检查第一个像素，确定起始状态
        int firstPixel = image.getPixelRGBA(contentStartX, textureStartY);
        Color firstColor = Color.abgr(firstPixel);
        boolean firstIsBlack = !firstColor.isEmpty() && firstColor.red() < BLACK_THRESHOLD &&
                firstColor.green() < BLACK_THRESHOLD && firstColor.blue() < BLACK_THRESHOLD;

        // 添加起始分割点
        horizontalDivs.add(0);
        horizontalStretch.add(firstIsBlack);

        boolean lastWasBlack = firstIsBlack;
        for (int x = contentStartX + 1; x <= contentEndX; x++) {
            int pixel = image.getPixelRGBA(x, textureStartY);
            Color color = Color.abgr(pixel);

            boolean isBlack = !color.isEmpty() && color.red() < BLACK_THRESHOLD &&
                    color.green() < BLACK_THRESHOLD && color.blue() < BLACK_THRESHOLD;

            // 检测到状态变化
            if (isBlack != lastWasBlack) {
                // 添加分割点
                horizontalDivs.add(x - contentStartX);
                // 记录新区域是否可拉伸
                horizontalStretch.add(isBlack);
                lastWasBlack = isBlack;
            }
        }

        // 确保最后一个分割点是内容区域的结束位置
        if (horizontalDivs.get(horizontalDivs.size() - 1) != contentWidth) {
            horizontalDivs.add(contentWidth);
        }

        // 解析垂直引导线
        // 找出所有分割点和每个区域是否可拉伸
        List<Integer> verticalDivs = new ArrayList<>();
        List<Boolean> verticalStretch = new ArrayList<>();

        // 首先检查第一个像素，确定起始状态
        int firstVPixel = image.getPixelRGBA(textureStartX, contentStartY);
        Color firstVColor = Color.fromAbgr(firstVPixel);

        boolean firstVIsBlack = !firstVColor.isEmpty() && firstVColor.red() < BLACK_THRESHOLD &&
                firstVColor.green() < BLACK_THRESHOLD && firstVColor.blue() < BLACK_THRESHOLD;

        // 添加起始分割点
        verticalDivs.add(0);
        verticalStretch.add(firstVIsBlack);

        lastWasBlack = firstVIsBlack;
        for (int y = contentStartY + 1; y <= contentEndY; y++) {
            int pixel = image.getPixelRGBA(textureStartX, y);
            Color color = Color.fromAbgr(pixel);
            boolean isBlack = !color.isEmpty() && color.red() < BLACK_THRESHOLD &&
                    color.green() < BLACK_THRESHOLD && color.blue() < BLACK_THRESHOLD;

            // 检测到状态变化
            if (isBlack != lastWasBlack) {
                // 添加分割点
                verticalDivs.add(y - contentStartY);
                // 记录新区域是否可拉伸
                verticalStretch.add(isBlack);
                lastWasBlack = isBlack;
            }
        }

        // 确保最后一个分割点是内容区域的结束位置
        if (verticalDivs.get(verticalDivs.size() - 1) != contentHeight) {
            verticalDivs.add(contentHeight);
        }

        // 若没有找到任何分割点，使整个区域可拉伸
        if (horizontalDivs.size() < 2) {
            horizontalDivs.clear();
            horizontalStretch.clear();
            horizontalDivs.add(0);
            horizontalDivs.add(contentWidth);
            horizontalStretch.add(true);
        }
        if (verticalDivs.size() < 2) {
            verticalDivs.clear();
            verticalStretch.clear();
            verticalDivs.add(0);
            verticalDivs.add(contentHeight);
            verticalStretch.add(true);
        }

        // 解析右参考线
        // 右参考线用于确定文字显示区域的高度
        // 右参考线中，黑色像素段表示内容区域，非黑色区域表示内边距
        int rightGuideHeight = 0;
        int rightGuideTopPadding = 0;
        int rightGuideBottomPadding = 0;
        int topmostBlackY = -1;
        int bottommostBlackY = -1;

        // 扫描整个右参考线，找到所有黑色像素段的最上和最下边界
        for (int y = contentStartY; y <= contentEndY; y++) {
            int pixel = image.getPixelRGBA(textureEndX, y);
            Color color = Color.fromAbgr(pixel);
            boolean isBlack = !color.isEmpty() && color.red() < BLACK_THRESHOLD &&
                    color.green() < BLACK_THRESHOLD && color.blue() < BLACK_THRESHOLD;
            if (isBlack) {
                if (topmostBlackY == -1) {
                    topmostBlackY = y;
                }
                bottommostBlackY = y; // 持续更新最下边界
                rightGuideHeight++;
            }
        }

        // 计算内边距
        if (topmostBlackY != -1) {
            // 上内边距 = 第一个黑色像素的位置 - 内容区域起始位置
            rightGuideTopPadding = topmostBlackY - contentStartY;
        }
        if (bottommostBlackY != -1) {
            // 下内边距 = 内容区域结束位置 - 最后一个黑色像素的位置
            rightGuideBottomPadding = contentEndY - bottommostBlackY;
        }

        // 解析下参考线
        // 下参考线用于确定文字显示区域的左右内边距
        // 下参考线中，黑色像素段表示内容区域，非黑色区域表示内边距
        int bottomGuideLeftPadding = 0;
        int bottomGuideRightPadding = 0;
        int leftmostBlackX = -1;
        int rightmostBlackX = -1;

        // 扫描整个下参考线，找到所有黑色像素段的最左和最右边界
        for (int x = contentStartX; x <= contentEndX; x++) {
            int pixel = image.getPixelRGBA(x, textureEndY);
            Color color = Color.fromAbgr(pixel);
            boolean isBlack = !color.isEmpty() && color.red() < BLACK_THRESHOLD &&
                    color.green() < BLACK_THRESHOLD && color.blue() < BLACK_THRESHOLD;

            if (isBlack) {
                if (leftmostBlackX == -1) {
                    leftmostBlackX = x;
                }
                rightmostBlackX = x; // 持续更新最右边界
            }
        }

        // 计算内边距
        if (leftmostBlackX != -1) {
            // 左内边距 = 第一个黑色像素的位置 - 内容区域起始位置
            bottomGuideLeftPadding = leftmostBlackX - contentStartX;
        }
        if (rightmostBlackX != -1) {
            // 右内边距 = 内容区域结束位置 - 最后一个黑色像素的位置
            bottomGuideRightPadding = contentEndX - rightmostBlackX;
        }

        // 解析最右下角像素点作为文字颜色
        int textColor = 0x00FFFFFF;
        int bottomRightPixel = image.getPixelRGBA(textureEndX, textureEndY);
        Color bottomRightColor = Color.abgr(bottomRightPixel);
        if (!bottomRightColor.isEmpty()) {
            textColor = bottomRightColor.getArgb();
        }

        // 转换为数组
        int[] hDivs = horizontalDivs.stream().mapToInt(i -> i).toArray();
        int[] vDivs = verticalDivs.stream().mapToInt(i -> i).toArray();
        boolean[] hStretch = new boolean[horizontalStretch.size()];
        for (int i = 0; i < horizontalStretch.size(); i++) {
            hStretch[i] = horizontalStretch.get(i);
        }
        boolean[] vStretch = new boolean[verticalStretch.size()];
        for (int i = 0; i < verticalStretch.size(); i++) {
            vStretch[i] = verticalStretch.get(i);
        }

        // 使用纹理范围的尺寸
        NinePatchInfo info = new NinePatchInfo(textureWidth, textureHeight, hDivs, vDivs, hStretch, vStretch,
                rightGuideHeight, rightGuideTopPadding, rightGuideBottomPadding,
                bottomGuideLeftPadding, bottomGuideRightPadding, textColor);
        NINE_PATCH_CACHE.put(texture, info);
        return info;
    }

    /**
     * 当资源（纹理）被重载后调用，由客户端事件处理器通过 {@link BaniraClientEventHub.Client} 触发。
     */
    public static void resourceReloadEvent() {
        clearAll();
        LOGGER.debug("Cleared texture cache");
    }
}
