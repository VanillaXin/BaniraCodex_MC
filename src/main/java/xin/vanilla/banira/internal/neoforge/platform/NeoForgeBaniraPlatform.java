package xin.vanilla.banira.internal.neoforge.platform;

import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.IModFile;
import org.objectweb.asm.Type;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.util.BaniraKeyBindings;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.platform.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * NeoForge 1.21.1 的 platform 适配层；公共 API 不直接暴露 NeoForge/FML 类型。
 */
public final class NeoForgeBaniraPlatform implements BaniraPlatform {

    private static final BaniraPathService PATHS = new NeoForgePathService();
    private static final BaniraConfigService CONFIGS = new NeoForgeConfigService();
    private static final BaniraNetworkService NETWORK = new NeoForgeNetworkService();
    private static final BaniraRegistryService REGISTRIES = new NeoForgeRegistryService();
    private static final BaniraInputService INPUT = new NeoForgeInputService();
    private static final BaniraNotificationService NOTIFICATIONS = new NeoForgeNotificationService();

    private static final Map<String, List<PendingPayloadRegistration<?>>> PENDING_PAYLOADS = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public String loaderType() {
        return "neoforge";
    }

    @Nonnull
    @Override
    public String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.production;
    }

    @Override
    public boolean isModLoaded(@Nonnull String modId) {
        return !modId.isEmpty() && ModList.get().isLoaded(modId);
    }

    @Nonnull
    @Override
    public String modDisplayName(@Nonnull String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    @Nullable
    @Override
    public String lastKnownUsername(@Nonnull UUID uuid) {
        return UsernameCache.getLastKnownUsername(uuid);
    }

    @Nonnull
    @Override
    public String modIdFromMainClass(@Nonnull Class<?> modMainClass) {
        Mod annotation = modMainClass.getAnnotation(Mod.class);
        if (annotation == null || annotation.value().trim().isEmpty()) {
            throw new IllegalArgumentException("Class must be annotated with @Mod: " + modMainClass.getName());
        }
        return annotation.value();
    }

    @Nonnull
    @Override
    public Class<?> modMainClass(@Nonnull String modId) {
        return resolveModMainClass(modId);
    }

    @Nonnull
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Nonnull
    @Override
    public BaniraPathService pathService() {
        return PATHS;
    }

    @Nonnull
    @Override
    public BaniraConfigService configService() {
        return CONFIGS;
    }

    @Nonnull
    @Override
    public BaniraNetworkService networkService() {
        return NETWORK;
    }

    @Nonnull
    @Override
    public BaniraRegistryService registryService() {
        return REGISTRIES;
    }

    @Nonnull
    @Override
    public BaniraInputService inputService() {
        return INPUT;
    }

    @Nonnull
    @Override
    public BaniraNotificationService notificationService() {
        return NOTIFICATIONS;
    }

    /**
     * NeoForge 的自定义 payload 必须在注册事件中集中声明；子 mod 通过公共 registrar 先登记到这里。
     */
    public static void registerPendingPayloads(@Nonnull RegisterPayloadHandlersEvent event) {
        for (Map.Entry<String, List<PendingPayloadRegistration<?>>> entry : PENDING_PAYLOADS.entrySet()) {
            PayloadRegistrar registrar = event.registrar(entry.getKey()).optional();
            for (PendingPayloadRegistration<?> registration : entry.getValue()) {
                registration.register(registrar);
            }
        }
    }

    private static Class<?> resolveModMainClass(String modId) {
        try {
            IModInfo modInfo = ModList.get().getModContainerById(modId)
                    .orElseThrow(() -> new IllegalStateException("No mod container for id: " + modId))
                    .getModInfo();
            IModFile modFile = modInfo.getOwningFile().getFile();
            ModFileScanData scan = modFile.getScanResult();
            if (scan == null) {
                throw new IllegalStateException("No ModFileScanData for mod id: " + modId);
            }
            Type modAnnotationType = Type.getType(Mod.class);
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
                if (!modAnnotationType.equals(annotation.annotationType())) {
                    continue;
                }
                if (!modIdMatchesAnnotation(modId, annotation.annotationData())) {
                    continue;
                }
                return Class.forName(annotation.clazz().getClassName(), false, loader);
            }
            throw new IllegalStateException("No @Mod class in scan data for mod id: " + modId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load @Mod class for mod id: " + modId, e);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to resolve @Mod main class for mod id: " + modId, t);
        }
    }

    private static boolean modIdMatchesAnnotation(String modId, @Nullable Map<String, ?> annotationData) {
        if (annotationData == null) {
            return false;
        }
        Object value = annotationData.get("value");
        if (value instanceof String s) {
            return modId.equals(s);
        }
        if (value instanceof String[] arr && arr.length == 1) {
            return modId.equals(arr[0]);
        }
        if (value instanceof List<?> list && list.size() == 1 && list.get(0) instanceof String s) {
            return modId.equals(s);
        }
        return false;
    }

    private static final class NeoForgePathService implements BaniraPathService {
        @Override
        public String rootDirectoryName() {
            return BaniraCodex.VANILLA_XIN;
        }

        @Override
        public Path configPath() {
            return BaniraCodex.BANIRA_CONFIG_PATH.get();
        }

        @Override
        public Path worldDataPath() {
            return BaniraCodex.BANIRA_WORLD_DATA_PATH.get();
        }

        @Override
        public Path playerDataPath() {
            return BaniraCodex.BANIRA_PLAYER_DATA_PATH.get();
        }

        @Override
        public Path vanillaPlayerDataPath() {
            return BaniraCodex.serverInstance().key().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR);
        }
    }

    private static final class NeoForgeConfigService implements BaniraConfigService {
        @Override
        public <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
            ForgeConfigAdapter.register(configClass, modId);
        }

        @Nonnull
        @Override
        public <T> T view(@Nonnull Class<?> configClass, @Nonnull Class<T> viewClass) {
            Object view = ForgeConfigAdapter.get(configClass);
            return viewClass.cast(view);
        }

        @Nullable
        @Override
        public BaniraConfigHandle handle(@Nonnull Class<?> configClass) {
            ConfigHolder holder = ForgeConfigAdapter.getHolder(configClass);
            return holder == null ? null : new NeoForgeConfigHandle(holder);
        }
    }

    private static final class NeoForgeConfigHandle implements BaniraConfigHandle {
        private final ConfigHolder holder;

        private NeoForgeConfigHandle(ConfigHolder holder) {
            this.holder = holder;
        }

        @Override
        public String getModId() {
            return holder.getModId();
        }

        @Override
        public String getConfigName() {
            return holder.getConfigName();
        }

        @Override
        public void save() {
            holder.save();
        }

        @Override
        public <T> T get(String path) {
            return holder.get(path);
        }

        @Override
        public void set(String path, Object value) {
            holder.set(path, value);
        }

        @Override
        public Set<String> valuePaths() {
            return holder.getValueMap().keySet();
        }

        @Override
        public boolean hasValue(String path) {
            return holder.getValueMap().containsKey(path);
        }

        @Nullable
        @Override
        public String findValuePath(String key) {
            if (key == null || key.isEmpty()) {
                return null;
            }
            if (hasValue(key)) {
                return key;
            }
            for (String path : holder.getValueMap().keySet()) {
                if (path.endsWith("." + key) || path.equals(key)) {
                    return path;
                }
            }
            return null;
        }

        @Override
        public Class<?> valueClass(String path) {
            Object value = get(path);
            if (value != null) {
                return value.getClass();
            }
            Object def = defaultValue(path);
            return def != null ? def.getClass() : Object.class;
        }

        @Nullable
        @Override
        public Object defaultValue(String path) {
            ConfigEntryDescriptor descriptor = holder.getDescriptor(path);
            return descriptor == null ? null : descriptor.getDefaultValue();
        }

        @Override
        public boolean validate(String path, Object value) {
            ConfigEntryDescriptor descriptor = holder.getDescriptor(path);
            return descriptor != null && holder.getValueMap().containsKey(path);
        }

        @Override
        public boolean setIfValid(String path, Object value) {
            if (!validate(path, value)) {
                return false;
            }
            set(path, value);
            return true;
        }
    }

    private static final class NeoForgeNetworkService implements BaniraNetworkService {
        @Nonnull
        @Override
        public NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier) {
            return new NeoForgeNetworkPacketRegistrar(channelName);
        }

        @Override
        public void sendToServer(@Nonnull BaniraNetworkPacket packet) {
            PacketUtils.sendPacketToServer((xin.vanilla.banira.common.api.INetworkPacket) packet);
        }

        @Override
        public void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketUtils.sendPacketToPlayer((xin.vanilla.banira.common.api.INetworkPacket) packet, serverPlayer);
            }
        }

        @Override
        public boolean hasDefaultChannel() {
            return true;
        }

        @Override
        public boolean hasLocalChannel(@Nonnull String channelId) {
            return false;
        }

        @Override
        public boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
            if (player instanceof ServerPlayer serverPlayer) {
                return PacketUtils.hasChannel(serverPlayer, ResourceLocation.parse(channelId));
            }
            return false;
        }
    }

    private static final class NeoForgeNetworkPacketRegistrar implements NetworkPacketRegistrar {
        private final String channelName;

        private NeoForgeNetworkPacketRegistrar(String channelName) {
            this.channelName = channelName;
        }

        @Override
        public <MSG extends xin.vanilla.banira.common.api.INetworkPacket> void register(
                int packetId,
                Class<MSG> packetClass,
                BiConsumer<MSG, BaniraPacketBuffer> encoder,
                Function<BaniraPacketBuffer, MSG> decoder,
                BiConsumer<MSG, BaniraNetworkContext> handler) {
            CustomPacketPayload.Type<MSG> type = payloadType(packetClass);
            PendingPayloadRegistration<MSG> registration = new PendingPayloadRegistration<>(
                    type,
                    new NeoForgeStreamCodec<>(encoder, decoder),
                    (packet, context) -> handler.accept(packet, new NeoForgeNetworkContext(context))
            );
            PENDING_PAYLOADS.computeIfAbsent(channelName, key -> Collections.synchronizedList(new ArrayList<>()))
                    .add(registration);
        }

        @SuppressWarnings("unchecked")
        private static <MSG extends xin.vanilla.banira.common.api.INetworkPacket> CustomPacketPayload.Type<MSG> payloadType(Class<MSG> packetClass) {
            try {
                Field field = packetClass.getField("TYPE");
                Object value = field.get(null);
                if (value instanceof CustomPacketPayload.Type<?> type) {
                    return (CustomPacketPayload.Type<MSG>) type;
                }
            } catch (ReflectiveOperationException ignored) {
                // 公开 registrar 约定 NeoForge payload 类提供 public static TYPE。
            }
            throw new IllegalArgumentException("NeoForge packet class must expose public static TYPE: " + packetClass.getName());
        }
    }

    private record PendingPayloadRegistration<MSG extends xin.vanilla.banira.common.api.INetworkPacket>(
            CustomPacketPayload.Type<MSG> type,
            StreamCodec<RegistryFriendlyByteBuf, MSG> codec,
            IPayloadHandler<MSG> handler) {
        private void register(PayloadRegistrar registrar) {
            registrar.playBidirectional(type, codec, handler);
        }
    }

    private static final class NeoForgeStreamCodec<MSG extends xin.vanilla.banira.common.api.INetworkPacket>
            implements StreamCodec<RegistryFriendlyByteBuf, MSG> {
        private final BiConsumer<MSG, BaniraPacketBuffer> encoder;
        private final Function<BaniraPacketBuffer, MSG> decoder;

        private NeoForgeStreamCodec(BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                    Function<BaniraPacketBuffer, MSG> decoder) {
            this.encoder = encoder;
            this.decoder = decoder;
        }

        @Nonnull
        @Override
        public MSG decode(@Nonnull RegistryFriendlyByteBuf buffer) {
            return decoder.apply(new NeoForgePacketBuffer(buffer));
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull MSG packet) {
            encoder.accept(packet, new NeoForgePacketBuffer(buffer));
        }
    }

    private static final class NeoForgePacketBuffer implements BaniraPacketBuffer {
        private final FriendlyByteBuf delegate;

        private NeoForgePacketBuffer(FriendlyByteBuf delegate) {
            this.delegate = delegate;
        }

        @Override
        public String readUtf() {
            return delegate.readUtf();
        }

        @Override
        public String readUtf(int maxLength) {
            return delegate.readUtf(maxLength);
        }

        @Override
        public void writeUtf(String value) {
            delegate.writeUtf(value);
        }

        @Override
        public void writeUtf(String value, int maxLength) {
            delegate.writeUtf(value, maxLength);
        }

        @Override
        public int readInt() {
            return delegate.readInt();
        }

        @Override
        public void writeInt(int value) {
            delegate.writeInt(value);
        }

        @Override
        public int readVarInt() {
            return delegate.readVarInt();
        }

        @Override
        public void writeVarInt(int value) {
            delegate.writeVarInt(value);
        }

        @Override
        public long readLong() {
            return delegate.readLong();
        }

        @Override
        public void writeLong(long value) {
            delegate.writeLong(value);
        }

        @Override
        public boolean readBoolean() {
            return delegate.readBoolean();
        }

        @Override
        public void writeBoolean(boolean value) {
            delegate.writeBoolean(value);
        }

        @Override
        public BaniraIdentifier readIdentifier() {
            return BaniraIdentifier.parse(delegate.readResourceLocation().toString());
        }

        @Override
        public void writeIdentifier(BaniraIdentifier value) {
            BaniraIdentifier identifier = Objects.requireNonNull(value, "value");
            delegate.writeResourceLocation(ResourceLocation.fromNamespaceAndPath(identifier.getNamespace(), identifier.getPath()));
        }
    }

    private static final class NeoForgeNetworkContext implements BaniraNetworkContext {
        private final IPayloadContext delegate;

        private NeoForgeNetworkContext(IPayloadContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public void enqueueWork(Runnable work) {
            delegate.enqueueWork(work);
        }

        @Override
        public void markHandled() {
        }

        @Override
        public boolean isClientSide() {
            return delegate.flow() == PacketFlow.CLIENTBOUND;
        }

        @Override
        public boolean isServerSide() {
            return delegate.flow() == PacketFlow.SERVERBOUND;
        }

        @Nullable
        @Override
        public Object sender() {
            return delegate.player();
        }
    }

    private static final class NeoForgeRegistryService implements BaniraRegistryService {
        @Nullable
        @Override
        public String blockKey(@Nullable Object block) {
            return key(BuiltInRegistries.BLOCK.getKey((net.minecraft.world.level.block.Block) block));
        }

        @Nullable
        @Override
        public Object block(@Nullable String id) {
            return id == null ? null : BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        }

        @Nonnull
        @Override
        public Collection<?> blocks() {
            return Collections.unmodifiableCollection(BuiltInRegistries.BLOCK.stream().toList());
        }

        @Nullable
        @Override
        public String itemKey(@Nullable Object item) {
            return key(BuiltInRegistries.ITEM.getKey((net.minecraft.world.item.Item) item));
        }

        @Nullable
        @Override
        public Object item(@Nullable String id) {
            return id == null ? null : BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        }

        @Nonnull
        @Override
        public Collection<?> items() {
            return Collections.unmodifiableCollection(BuiltInRegistries.ITEM.stream().toList());
        }

        @Nonnull
        @Override
        public Collection<String> itemTagIds(@Nullable Object item) {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public String entityTypeKey(@Nullable Object entityType) {
            return key(BuiltInRegistries.ENTITY_TYPE.getKey((net.minecraft.world.entity.EntityType<?>) entityType));
        }

        @Nullable
        @Override
        public Object entityType(@Nullable String id) {
            return id == null ? null : BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id));
        }

        @Nonnull
        @Override
        public Collection<?> entityTypes() {
            return Collections.unmodifiableCollection(BuiltInRegistries.ENTITY_TYPE.stream().toList());
        }

        @Nullable
        @Override
        public String effectKey(@Nullable Object effect) {
            if (effect instanceof net.minecraft.world.effect.MobEffect mobEffect) {
                return key(BuiltInRegistries.MOB_EFFECT.getKey(mobEffect));
            }
            return null;
        }

        @Nullable
        @Override
        public Object effect(@Nullable String id) {
            return id == null ? null : BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(id));
        }

        @Nonnull
        @Override
        public Collection<?> effects() {
            return Collections.unmodifiableCollection(BuiltInRegistries.MOB_EFFECT.stream().toList());
        }

        @Nullable
        @Override
        public Object biome(@Nullable String id) {
            return null;
        }

        @Nonnull
        @Override
        public Collection<String> biomeIds() {
            return Collections.emptyList();
        }

        @Nullable
        private static String key(@Nullable ResourceLocation location) {
            return location == null ? null : location.toString();
        }
    }

    private static final class NeoForgeInputService implements BaniraInputService {
        @Nonnull
        @Override
        public BaniraKeyHandle register(@Nonnull BaniraKeySpec spec) {
            String category = spec.category() != null ? spec.category() : BaniraKeyBindings.defaultCategory(spec.modId());
            KeyMapping mapping = BaniraKeyBindings.register(spec.modId(), spec.suffix(), spec.defaultKey(), category);
            String descriptionId = BaniraKeyBindings.descriptionId(spec.modId(), spec.suffix());
            int defaultKey = spec.defaultKey();
            return new BaniraKeyHandle() {
                @Nonnull
                @Override
                public String descriptionId() {
                    return descriptionId;
                }

                @Nonnull
                @Override
                public String category() {
                    return category;
                }

                @Override
                public int defaultKey() {
                    return defaultKey;
                }

                @Override
                public boolean isDown() {
                    return mapping.isDown();
                }

                @Override
                public boolean consumeClick() {
                    return mapping.consumeClick();
                }
            };
        }

        @Override
        public boolean isKeyDown(int keyCode) {
            return InputStateManager.isKeyPressing(keyCode);
        }

        @Override
        public boolean isMouseDown(int button) {
            return InputStateManager.isMousePressing(button);
        }

        @Override
        public void flushPendingRegistrations() {
            // NeoForge 的 RegisterKeyMappingsEvent 事件会调用 BaniraKeyBindings.flushPendingRegistrations(event)。
        }
    }

    private static final class NeoForgeNotificationService implements BaniraNotificationService {
        @Override
        public void show(@Nonnull Component component) {
            NotificationManager.get().addNotification(Notification.ofComponent(component));
        }

        @Override
        public void show(@Nonnull NotificationData notification) {
            NotificationManager.get().addNotification(Notification.fromData(notification));
        }

        @Override
        public void show(@Nonnull NotificationData notification, boolean fromNetwork) {
            NotificationManager.get().addNotification(Notification.fromData(notification, fromNetwork), fromNetwork);
        }
    }
}
