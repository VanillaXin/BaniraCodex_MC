package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.api.client.hud.BaniraHudBounds;
import xin.vanilla.banira.api.client.hud.BaniraHudEvents;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderContext;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderEvent;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.api.client.hud.HudRenderPhase;
import xin.vanilla.banira.api.client.input.BaniraKeyCodes;
import xin.vanilla.banira.api.client.notification.BaniraNotifications;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;
import xin.vanilla.banira.api.client.render.BaniraDrawHandle;
import xin.vanilla.banira.api.event.BaniraCommonSetupEvent;
import xin.vanilla.banira.api.event.BaniraEventRegistration;
import xin.vanilla.banira.api.event.BaniraLifecycle;
import xin.vanilla.banira.api.notification.BaniraNotificationTypes;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraInputService;
import xin.vanilla.banira.platform.BaniraNetworkPacket;
import xin.vanilla.banira.platform.BaniraNetworkService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * 最小子 mod 调用契约：这里只使用推荐公开 API，避免 19.2 Forge/Fabric 分支再次分叉。
 */
public class ChildModApiUsageContractTest {
    private static final String MOD_ID = "demo_child";

    @Test
    public void childModStyleApiUsageCompilesAndRoutesThroughStableFacades() {
        RecordingConfigService configs = new RecordingConfigService();
        RecordingNetworkService network = new RecordingNetworkService();
        RecordingInputService input = new RecordingInputService();
        BaniraPlatforms.install(new TestBaniraPlatform()
                .loaderType("forge")
                .minecraftVersion("1.19.2")
                .client(true)
                .mod(MOD_ID, DemoChildMod.class)
                .configService(configs)
                .networkService(network)
                .inputService(input));

        DemoChildMod.commonInit();
        DemoChildMod.clientInit();
        DemoChildMod.sendHandshake();

        assertSame(DemoConfig.class, configs.registeredConfig);
        assertEquals(MOD_ID, configs.registeredModId);
        assertEquals("forge", BaniraEnvironment.loaderType());
        assertEquals("1.19.2", BaniraEnvironment.minecraftVersion());
        assertEquals("vanilla.xin", BaniraDataPaths.rootDirectoryName());
        assertEquals("key.demo_child.action", input.handle.descriptionId());
        assertEquals(BaniraKeyCodes.KEY_G, input.handle.defaultKey());
        assertSame(DemoPacket.INSTANCE, network.serverPacket);
        assertTrue(BaniraModPresence.hasRegistration(MOD_ID));
        assertTrue(BaniraModPresence.announcedModIds().contains(MOD_ID));
        assertTrue(BaniraNotificationTypes.sortedSnapshot().contains("demo_child:progress"));

        DemoChildMod.disableCallbacks();
        BaniraModPresence.unregister(MOD_ID);
    }

    @Test
    public void childModExperienceOverlayCallbackCanCancelAndDraw() {
        AtomicBoolean drawn = new AtomicBoolean(false);
        AtomicBoolean active = new AtomicBoolean(true);

        BaniraHudEvents.onExperiencePreRender(event -> {
            if (active.get() && DemoChildMod.shouldReplaceExperienceBar()) {
                event.cancel();
                event.context().draw().progressBar(event.bounds(), 0.5F, 0x55000000, 0xFF7BD88F);
                drawn.set(true);
            }
        });

        BaniraHudRenderEvent event = new BaniraHudRenderEvent(
                HudRenderPhase.PRE,
                HudOverlayElement.EXPERIENCE_BAR,
                new BaniraHudRenderContext(new BaniraDrawContext(new NoopDrawHandle(), 320, 180, 0.0F), 320, 180, 0.0F),
                BaniraHudBounds.of(69, 151, 182, 5),
                true
        );

        BaniraHudEvents.dispatchPre(event);
        active.set(false);

        assertTrue(event.canceled());
        assertTrue(drawn.get());
    }

    public static final class DemoChildMod {
        private static BaniraKeyHandle actionKey;
        private static BaniraEventRegistration commonSetupRegistration;
        private static boolean callbacksEnabled;

        private DemoChildMod() {
        }

        public static void commonInit() {
            BaniraConfigs.register(DemoConfig.class, MOD_ID);
            BaniraModPresence.register(MOD_ID);
            BaniraNotificationTypes.register("demo_child:progress");
            if (commonSetupRegistration != null) {
                commonSetupRegistration.unregister();
            }
            commonSetupRegistration = BaniraLifecycle.onCommonSetup(event -> event.enqueueWork(DemoChildMod::onCommonSetup));
            BaniraLifecycle.dispatchCommonSetup(BaniraCommonSetupEvent.immediate());
            assertNotNull(DemoLang.INSTANCE.getKey(EnumI18nType.WORD, "ready"));
        }

        public static void clientInit() {
            callbacksEnabled = true;
            actionKey = BaniraInput.registerKey(
                    BaniraInput.spec(MOD_ID, "action")
                            .defaultKey(BaniraKeyCodes.KEY_G)
                            .category(BaniraInput.defaultCategory(MOD_ID))
            );
            BaniraClientEvents.Client.onKeyPressedPre(event -> {
                if (callbacksEnabled && BaniraKeyCodes.matchesExactModifiers(event.modifiers(), BaniraKeyCodes.MOD_CONTROL)) {
                    event.cancel();
                }
            });
            BaniraClientEvents.Client.onRenderOverlayPre(event -> {
            });
            BaniraNotifications.show(BaniraComponent.get().literal("demo"));
        }

        public static void sendHandshake() {
            BaniraNetwork.sendToServer(DemoPacket.INSTANCE);
        }

        public static boolean shouldReplaceExperienceBar() {
            return actionKey == null || actionKey.isDown();
        }

        public static void disableCallbacks() {
            callbacksEnabled = false;
            if (commonSetupRegistration != null) {
                commonSetupRegistration.unregister();
                commonSetupRegistration = null;
            }
        }

        private static void onCommonSetup() {
        }
    }

    @Config(name = "demo_child", type = ConfigScope.CLIENT)
    public static final class DemoConfig {
        @ConfigEntry(key = "show_marker", category = "hud")
        public static boolean showMarker = true;

        @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
        public static int durationTicks = 60;

        private DemoConfig() {
        }
    }

    public static final class DemoLang extends Translator {
        public static final DemoLang INSTANCE = new DemoLang();

        private DemoLang() {
            super(MOD_ID, DemoLang.class);
        }
    }

    public enum DemoPacket implements NetworkPacket {
        INSTANCE
    }

    private static final class RecordingConfigService implements BaniraConfigService {
        private Class<?> registeredConfig;
        private String registeredModId;

        @Override
        public <T> void register(Class<T> configClass, String modId) {
            this.registeredConfig = configClass;
            this.registeredModId = modId;
        }

        @Override
        public <T> T view(Class<?> configClass, Class<T> viewClass) {
            throw new UnsupportedOperationException("view");
        }

        @Override
        public @Nullable BaniraConfigHandle handle(Class<?> configClass) {
            return null;
        }
    }

    private static final class RecordingNetworkService implements BaniraNetworkService {
        private BaniraNetworkPacket serverPacket;

        @Override
        public @Nonnull NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier) {
            throw new UnsupportedOperationException("registrar");
        }

        @Override
        public void sendToServer(@Nonnull BaniraNetworkPacket packet) {
            this.serverPacket = packet;
        }

        @Override
        public void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
        }

        @Override
        public boolean hasDefaultChannel() {
            return true;
        }

        @Override
        public boolean hasLocalChannel(@Nonnull String channelId) {
            return true;
        }

        @Override
        public boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
            return true;
        }
    }

    private static final class RecordingInputService implements BaniraInputService {
        private RecordingKeyHandle handle;

        @Override
        public @Nonnull BaniraKeyHandle register(@Nonnull BaniraKeySpec spec) {
            this.handle = new RecordingKeyHandle(spec);
            return handle;
        }

        @Override
        public boolean isKeyDown(int keyCode) {
            return false;
        }

        @Override
        public boolean isMouseDown(int button) {
            return false;
        }

        @Override
        public void flushPendingRegistrations() {
        }
    }

    private static final class RecordingKeyHandle implements BaniraKeyHandle {
        private final BaniraKeySpec spec;

        private RecordingKeyHandle(BaniraKeySpec spec) {
            this.spec = spec;
        }

        @Override
        public @Nonnull String descriptionId() {
            return BaniraInput.descriptionId(spec.modId(), spec.suffix());
        }

        @Override
        public @Nonnull String category() {
            return spec.category() == null ? BaniraInput.defaultCategory(spec.modId()) : spec.category();
        }

        @Override
        public int defaultKey() {
            return spec.defaultKey();
        }

        @Override
        public boolean isDown() {
            return true;
        }

        @Override
        public boolean consumeClick() {
            return false;
        }
    }

    private static final class NoopDrawHandle implements BaniraDrawHandle {
        @Override
        public void fill(int x, int y, int width, int height, int argb) {
        }

        @Override
        public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        }

        @Override
        public void roundedRect(int x, int y, int width, int height, int argb, int radius) {
        }

        @Override
        public void text(@Nonnull String text, int x, int y, int argb, boolean shadow) {
        }

        @Override
        public void texture(@Nonnull String textureId, int x, int y, int width, int height,
                            float u, float v, int textureWidth, int textureHeight) {
        }
    }
}
