package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.common.data.KeyValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.UUID;

/**
 * 客户端运行时访问点，集中隔离 Minecraft 单例和窗口句柄。
 */
public final class BaniraClientRuntime {
    private static Field itemColorsField;

    private BaniraClientRuntime() {
    }

    public static void execute(@Nonnull Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    public static long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    public static boolean isWindowActive() {
        return Minecraft.getInstance().isWindowActive();
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static ResourceManager resourceManager() {
        return Minecraft.getInstance().getResourceManager();
    }

    public static TextureManager textureManager() {
        return Minecraft.getInstance().getTextureManager();
    }

    public static ItemRenderer itemRenderer() {
        return Minecraft.getInstance().getItemRenderer();
    }

    public static ItemColors itemColors() {
        ItemRenderer renderer = itemRenderer();
        try {
            if (itemColorsField == null) {
                for (Field field : ItemRenderer.class.getDeclaredFields()) {
                    if (field.getType() == ItemColors.class) {
                        field.setAccessible(true);
                        itemColorsField = field;
                        break;
                    }
                }
            }
            return itemColorsField == null ? null : (ItemColors) itemColorsField.get(renderer);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    public static String serverIp() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server != null ? server.ip : "";
    }

    public static boolean hasConnection() {
        return Minecraft.getInstance().getConnection() != null;
    }

    public static boolean chatLinksEnabled() {
        return Minecraft.getInstance().options.chatLinks().get();
    }

    public static void showGameInfo(@Nonnull net.minecraft.network.chat.Component message, @Nonnull UUID sender) {
        Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }

    @Nullable
    public static Level level() {
        return Minecraft.getInstance().level;
    }

    @Nullable
    public static String selectedLanguageCode() {
        if (Minecraft.getInstance().getLanguageManager() == null
                || Minecraft.getInstance().getLanguageManager().getSelected() == null) {
            return null;
        }
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }

    public static String clipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    public static void clipboard(@Nullable String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text == null ? "" : text);
    }

    @Nullable
    public static Screen currentScreen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    @Nullable
    public static LocalPlayer localPlayer() {
        return Minecraft.getInstance().player;
    }

    /**
     * 供共享代码读取玩家时使用中立返回类型，避免 dedicated server 解析客户端类描述符。
     */
    @Nullable
    public static Player player() {
        return Minecraft.getInstance().player;
    }

    @Nullable
    public static Player levelPlayer(@Nullable UUID uuid) {
        if (uuid == null || Minecraft.getInstance().level == null) {
            return null;
        }
        return Minecraft.getInstance().level.getPlayerByUUID(uuid);
    }

    /**
     * 从当前客户端连接缓存中查询玩家名；服务端/离线状态下返回 null。
     */
    @Nullable
    public static String onlinePlayerName(@Nullable UUID uuid) {
        LocalPlayer player = localPlayer();
        if (player == null || player.connection == null || uuid == null) {
            return null;
        }
        return player.connection.getOnlinePlayers().stream()
                .filter(info -> info.getProfile().getId().equals(uuid))
                .findFirst()
                .map(info -> info.getProfile().getName())
                .orElse(null);
    }

    @Nullable
    public static ResourceLocation onlinePlayerSkin(@Nullable UUID uuid) {
        LocalPlayer player = localPlayer();
        if (player == null || player.connection == null || uuid == null) {
            return null;
        }
        return player.connection.getOnlinePlayers().stream()
                .filter(info -> info.getProfile().getId().equals(uuid))
                .findFirst()
                .map(info -> info.getSkin().texture())
                .orElse(null);
    }

    /**
     * 当前 GUI 逻辑尺寸。若打开了 Screen，优先使用 Screen 尺寸以贴近控件布局。
     */
    public static KeyValue<Integer, Integer> screenSize() {
        Screen screen = currentScreen();
        if (screen != null) {
            return new KeyValue<>(screen.width, screen.height);
        }
        return guiScaledSize();
    }

    public static KeyValue<Integer, Integer> guiScaledSize() {
        Minecraft mc = Minecraft.getInstance();
        return new KeyValue<>(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    public static KeyValue<Integer, Integer> windowSize() {
        Minecraft mc = Minecraft.getInstance();
        return new KeyValue<>(mc.getWindow().getWidth(), mc.getWindow().getHeight());
    }

    public static double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static double scaledMouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / Math.max(1, (double) mc.getWindow().getScreenWidth());
    }

    public static double scaledMouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / Math.max(1, (double) mc.getWindow().getScreenHeight());
    }

    public static boolean leftMouseDown() {
        return GLFW.glfwGetMouseButton(windowHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
}
