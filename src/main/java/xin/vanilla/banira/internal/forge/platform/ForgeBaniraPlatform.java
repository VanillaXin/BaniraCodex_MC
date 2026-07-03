package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.client.util.BaniraKeyBindings;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.client.gui.component.Notification;
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
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Forge 1.20.1 的 platform 适配层；公共 API 只依赖这里，不直接触碰 Forge/FML 类型。
 */
public final class ForgeBaniraPlatform implements BaniraPlatform {
    private static final String NETWORK_PROTOCOL_VERSION = "1";

    private static final BaniraPathService PATHS = new ForgePathService();
    private static final BaniraConfigService CONFIGS = new ForgeConfigService();
    private static final BaniraNetworkService NETWORK = new ForgeNetworkService();
    private static final BaniraRegistryService REGISTRIES = new ForgeRegistryService();
    private static final BaniraInputService INPUT = new ForgeInputService();
    private static final BaniraNotificationService NOTIFICATIONS = new ForgeNotificationService();

    @Nonnull
    @Override
    public String loaderType() {
        return "forge";
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
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getMod())
                .filter(Objects::nonNull)
                .map(Object::getClass)
                .orElseThrow(() -> new IllegalStateException("No loaded mod main class for mod id: " + modId));
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

    private static final class ForgePathService implements BaniraPathService {
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

    private static final class ForgeConfigService implements BaniraConfigService {
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
            return holder == null ? null : new ForgeConfigHandle(holder);
        }
    }

    private static final class ForgeConfigHandle implements BaniraConfigHandle {
        private final ConfigHolder holder;

        private ForgeConfigHandle(ConfigHolder holder) {
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

    private static final class ForgeNetworkService implements BaniraNetworkService {
        private static SimpleChannel defaultChannel;

        @Nonnull
        @Override
        public NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier) {
            SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(identifier.getNamespace(), channelName),
                    () -> NETWORK_PROTOCOL_VERSION,
                    clientVersion -> true,
                    serverVersion -> true
            );
            if (defaultChannel == null) {
                defaultChannel = channel;
            }
            return new ForgeNetworkPacketRegistrar(channel);
        }

        @Override
        public void sendToServer(@Nonnull BaniraNetworkPacket packet) {
            if (defaultChannel != null) {
                defaultChannel.sendToServer(packet);
            }
        }

        @Override
        public void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
            if (defaultChannel != null && player instanceof ServerPlayer serverPlayer) {
                defaultChannel.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
            }
        }

        @Override
        public boolean hasDefaultChannel() {
            return defaultChannel != null;
        }

        @Override
        public boolean hasLocalChannel(@Nonnull String channelId) {
            return false;
        }

        @Override
        public boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
            return player instanceof ServerPlayer;
        }
    }

    private static final class ForgeNetworkPacketRegistrar implements NetworkPacketRegistrar {
        private final SimpleChannel channel;

        private ForgeNetworkPacketRegistrar(SimpleChannel channel) {
            this.channel = channel;
        }

        @Override
        public <MSG extends xin.vanilla.banira.common.api.INetworkPacket> void register(
                int packetId,
                Class<MSG> packetClass,
                java.util.function.BiConsumer<MSG, BaniraPacketBuffer> encoder,
                java.util.function.Function<BaniraPacketBuffer, MSG> decoder,
                java.util.function.BiConsumer<MSG, BaniraNetworkContext> handler) {
            // Forge 20.1 仍使用 SimpleChannel；转换只允许停留在 adapter 内部。
            channel.registerMessage(
                    packetId,
                    packetClass,
                    (packet, buffer) -> encoder.accept(packet, new ForgePacketBuffer(buffer)),
                    buffer -> decoder.apply(new ForgePacketBuffer(buffer)),
                    (packet, context) -> handler.accept(packet, new ForgeNetworkContext(context))
            );
        }
    }

    private static final class ForgePacketBuffer implements BaniraPacketBuffer {
        private final FriendlyByteBuf delegate;

        private ForgePacketBuffer(FriendlyByteBuf delegate) {
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
            delegate.writeResourceLocation(new ResourceLocation(Objects.requireNonNull(value, "value").asString()));
        }
    }

    private static final class ForgeNetworkContext implements BaniraNetworkContext {
        private final Supplier<NetworkEvent.Context> delegate;

        private ForgeNetworkContext(Supplier<NetworkEvent.Context> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void enqueueWork(Runnable work) {
            delegate.get().enqueueWork(work);
        }

        @Override
        public void markHandled() {
            delegate.get().setPacketHandled(true);
        }

        @Override
        public boolean isClientSide() {
            return delegate.get().getDirection().getReceptionSide().isClient();
        }

        @Override
        public boolean isServerSide() {
            return delegate.get().getDirection().getReceptionSide().isServer();
        }

        @Nullable
        @Override
        public Object sender() {
            return delegate.get().getSender();
        }
    }

    private static final class ForgeRegistryService implements BaniraRegistryService {
        @Nullable
        @Override
        public String blockKey(@Nullable Object block) {
            return key(BuiltInRegistries.BLOCK.getKey((net.minecraft.world.level.block.Block) block));
        }

        @Nullable
        @Override
        public Object block(@Nullable String id) {
            return id == null ? null : BuiltInRegistries.BLOCK.get(resourceLocation(id));
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
            return id == null ? null : BuiltInRegistries.ITEM.get(resourceLocation(id));
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
            return id == null ? null : BuiltInRegistries.ENTITY_TYPE.get(resourceLocation(id));
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
            return id == null ? null : BuiltInRegistries.MOB_EFFECT.get(resourceLocation(id));
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

        private static ResourceLocation resourceLocation(String id) {
            return new ResourceLocation(id);
        }
    }

    private static final class ForgeInputService implements BaniraInputService {
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
            // Forge 的 RegisterKeyMappingsEvent 事件会调用 BaniraKeyBindings.flushPendingRegistrations(event)。
        }
    }

    private static final class ForgeNotificationService implements BaniraNotificationService {
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
