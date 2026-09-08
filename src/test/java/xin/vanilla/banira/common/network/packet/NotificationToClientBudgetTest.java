package xin.vanilla.banira.common.network.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class NotificationToClientBudgetTest {
    private static boolean bootstrapped;

    public static synchronized void bootstrapGame() throws Exception {
        if (bootstrapped) return;
        // Forge 52 item components dispatch a mod-bus event during vanilla bootstrap.
        // Supply an empty headless loading context, not a game or a mocked codec.
        var loading = net.minecraftforge.fml.loading.LoadingModList.of(
                java.util.List.of(), java.util.List.of(), null);
        loading.setBrokenFiles(java.util.List.of());
        setField(net.minecraftforge.fml.loading.FMLLoader.class, "loadingModList", loading);
        cpw.mods.modlauncher.api.IModuleLayerManager layers = layer -> java.util.Optional.of(ModuleLayer.boot());
        setField(net.minecraftforge.fml.loading.FMLLoader.class, "moduleLayerManager", layers);
        var mods = net.minecraftforge.fml.ModList.of(java.util.List.of(), java.util.List.of());
        var setLoadedMods = net.minecraftforge.fml.ModList.class.getDeclaredMethod("setLoadedMods", java.util.List.class);
        setLoadedMods.setAccessible(true);
        setLoadedMods.invoke(mods, java.util.List.of());
        net.minecraftforge.fml.ModLoader.get();
        setField(net.minecraftforge.fml.ModLoader.class, "loadingStateValid", true);
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        bootstrapped = true;
    }

    private static void setField(Class<?> type, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    @BeforeClass
    public static void installPlatform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void rejectsOversizedComponentBeforeTransportIsCalled() {
        Component component = text(repeat("\u4e2d", 6000));
        assertTrue(component.toJson().toString().length() < 16384);
        assertThrows(IllegalArgumentException.class, () -> new NotificationToClient(component));
    }

    @Test
    public void exactUtf8BoundaryRoundTripsWithRealFriendlyByteBuf() {
        String seed = "\u4e2d\ud83c\udf38\"\\\n";
        int overhead = new NotificationToClient(text(seed)).componentJson().getBytes(StandardCharsets.UTF_8).length;
        Component exact = text(seed + repeat("x", 16384 - overhead));
        NotificationToClient packet = new NotificationToClient(exact);
        assertEquals(16384, packet.componentJson().getBytes(StandardCharsets.UTF_8).length);
        FriendlyByteBuf nativeBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BaniraPacketBuffer buffer = adapt(nativeBuffer);
            packet.toBytes(buffer);
            NotificationToClient decoded = new NotificationToClient(buffer);
            assertEquals(packet.componentJson(), decoded.componentJson());
            assertEquals(packet.typeId(), decoded.typeId());
            assertEquals(0, nativeBuffer.readableBytes());
        } finally {
            nativeBuffer.release();
        }
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationToClient(text(exact.text() + "x")));
    }

    @Test
    public void rejectsOversizedTypeEvenForShortComponent() {
        assertThrows(IllegalArgumentException.class, () -> new NotificationToClient(text("ok"),
                EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, 5000, EnumNotificationStyle.NORMAL,
                repeat("\u4e2d", 43)));
    }

    public static BaniraPacketBuffer adapt(FriendlyByteBuf buffer) {
        return (BaniraPacketBuffer) Proxy.newProxyInstance(BaniraPacketBuffer.class.getClassLoader(),
                new Class<?>[]{BaniraPacketBuffer.class}, (proxy, method, args) ->
                        FriendlyByteBuf.class.getMethod(method.getName(), method.getParameterTypes()).invoke(buffer, args));
    }

    private static Component text(String value) {
        return BaniraComponent.get().literal(value).languageCode("zh_cn");
    }

    public static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }
}
