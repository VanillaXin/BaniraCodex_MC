package xin.vanilla.banira.internal.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.DistExecutor;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.internal.forge.client.ForgeBaniraClientService;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.List;
import java.util.UUID;

/**
 * Internal client facade; dedicated servers use the noop service without loading Minecraft client classes.
 */
public final class BaniraClientAccess {
    private static final BaniraClientService NOOP = BaniraClientService.noop();

    private BaniraClientAccess() {
    }

    public static BaniraClientService service() {
        if (!BaniraPlatforms.isInstalled() || !BaniraPlatforms.get().isClient()) {
            return NOOP;
        }
        return ClientServiceHolder.SERVICE;
    }

    public static PlayerEntity localPlayer() {
        return service().localPlayer();
    }

    public static UUID localPlayerUuid() {
        return service().localPlayerUuid();
    }

    public static String onlinePlayerName(UUID uuid) {
        return service().onlinePlayerName(uuid);
    }

    public static PlayerEntity playerByUuid(UUID uuid) {
        return service().playerByUuid(uuid);
    }

    public static ResourceLocation playerSkin(UUID uuid) {
        return service().playerSkin(uuid);
    }

    public static List<ITextComponent> itemTooltip(ItemStack stack, PlayerEntity player, boolean advanced) {
        return service().itemTooltip(stack, player, advanced);
    }

    public static String selectedLanguageCode() {
        return service().selectedLanguageCode();
    }

    public static boolean hasResource(ResourceLocation location) {
        return service().hasResource(location);
    }

    public static void bindTexture(ResourceLocation location) {
        service().bindTexture(location);
    }

    public static long windowHandle() {
        return service().windowHandle();
    }

    public static boolean isWindowActive() {
        return service().isWindowActive();
    }

    public static boolean hasScreen() {
        return service().hasScreen();
    }

    public static void runOnClientThread(Runnable action) {
        service().runOnClientThread(action);
    }

    public static String clipboard() {
        return service().clipboard();
    }

    public static void clipboard(String value) {
        service().clipboard(value);
    }

    public static double guiScale() {
        return service().guiScale();
    }

    public static KeyValue<Integer, Integer> screenSize() {
        return service().screenSize();
    }

    public static KeyValue<Integer, Integer> guiScaledSize() {
        return service().guiScaledSize();
    }

    public static KeyValue<Integer, Integer> guiPixelSize() {
        return service().guiPixelSize();
    }

    public static int drawText(Object nativeContext, String text, int x, int y, int argb, boolean shadow) {
        return service().drawText(nativeContext, text, x, y, argb, shadow);
    }

    public static int textWidth(String text) {
        return service().textWidth(text);
    }

    public static int lineHeight() {
        return service().lineHeight();
    }

    public static void fill(Object nativeContext, int x, int y, int width, int height, int argb) {
        service().fill(nativeContext, x, y, width, height, argb);
    }

    public static void blit(Object nativeContext, ResourceLocation texture, int x, int y, double u, double v, int width, int height, int textureWidth, int textureHeight) {
        service().blit(nativeContext, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void pushTransform(Object nativeContext) {
        service().pushTransform(nativeContext);
    }

    public static void popTransform(Object nativeContext) {
        service().popTransform(nativeContext);
    }

    public static void translate(Object nativeContext, double x, double y, double z) {
        service().translate(nativeContext, x, y, z);
    }

    public static void scale(Object nativeContext, float x, float y, float z) {
        service().scale(nativeContext, x, y, z);
    }

    private static final class ClientServiceHolder {
        private static final BaniraClientService SERVICE = DistExecutor.safeRunForDist(
                () -> ForgeBaniraClientService::new,
                () -> BaniraClientService::noop
        );
    }
}
