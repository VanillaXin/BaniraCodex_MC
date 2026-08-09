package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.*;

/**
 * 测试用 platform，可按需覆盖少量字段，避免单测重复实现整套加载器适配。
 */
public final class TestBaniraPlatform implements BaniraPlatform {
    private String loaderType = "test";
    private String minecraftVersion = "0.0";
    private boolean client;
    private boolean dedicatedServer = true;
    private boolean development = true;
    private final Set<String> loadedMods = new HashSet<>();
    private final Map<String, Class<?>> modMainClasses = new HashMap<>();
    private final Map<Class<?>, String> modIdsByMainClass = new HashMap<>();
    private final Map<UUID, String> usernames = new HashMap<>();
    private BaniraConfigService configService = NoopConfigService.INSTANCE;
    private BaniraServerService serverService = new BaniraServerService() {
        @Override
        public Object current() {
            return null;
        }

        @Override
        public boolean isRunning() {
            return false;
        }
    };
    private BaniraPlayerDataService playerDataService;
    private BaniraPermissionService permissionService = new BaniraPermissionService() {
        @Override
        public boolean hasVanillaPermission(Object player, int permissionLevel) {
            return false;
        }

        @Override
        public boolean hasVirtualPermission(Object player, String permissionKey) {
            return false;
        }
    };
    private BaniraNetworkService networkService = NoopNetworkService.INSTANCE;
    private BaniraRegistryService registryService = NoopRegistryService.INSTANCE;
    private BaniraNotificationService notificationService = NoopNotificationService.INSTANCE;
    private BaniraLogoService logoService = NoopLogoService.INSTANCE;
    private Path configDir = Path.of("config");
    private BaniraPathService pathService = new BaniraPathService() {
        @Override
        public @Nonnull String rootDirectoryName() {
            return "vanilla.xin";
        }

        @Override
        public @Nonnull Path gameConfigPath() {
            return configDir;
        }

        @Override
        public @Nonnull Path configPath() {
            return gameConfigPath().resolve(rootDirectoryName());
        }

        @Override
        public @Nonnull Path worldDataPath() {
            return Path.of("build", "test-world", rootDirectoryName());
        }

        @Override
        public @Nonnull Path playerDataPath() {
            return worldDataPath().resolve("playerdata");
        }

        @Override
        public @Nonnull Path vanillaPlayerDataPath() {
            return Path.of("build", "test-world", "playerdata");
        }
    };

    public TestBaniraPlatform loaderType(String value) {
        this.loaderType = Objects.requireNonNull(value, "loaderType");
        return this;
    }

    public TestBaniraPlatform minecraftVersion(String value) {
        this.minecraftVersion = Objects.requireNonNull(value, "minecraftVersion");
        return this;
    }

    public TestBaniraPlatform client(boolean value) {
        this.client = value;
        this.dedicatedServer = !value;
        return this;
    }

    public TestBaniraPlatform dedicatedServer(boolean value) {
        this.dedicatedServer = value;
        this.client = !value;
        return this;
    }

    public TestBaniraPlatform development(boolean value) {
        this.development = value;
        return this;
    }

    public TestBaniraPlatform mod(String modId, Class<?> mainClass) {
        loadedMods.add(Objects.requireNonNull(modId, "modId"));
        modMainClasses.put(modId, Objects.requireNonNull(mainClass, "mainClass"));
        modIdsByMainClass.put(mainClass, modId);
        return this;
    }

    public TestBaniraPlatform modIdFromMainClass(Class<?> mainClass, String modId) {
        modIdsByMainClass.put(Objects.requireNonNull(mainClass, "mainClass"), Objects.requireNonNull(modId, "modId"));
        return this;
    }

    public TestBaniraPlatform username(UUID uuid, String username) {
        usernames.put(Objects.requireNonNull(uuid, "uuid"), username);
        return this;
    }

    public TestBaniraPlatform configService(BaniraConfigService value) {
        this.configService = Objects.requireNonNull(value, "configService");
        return this;
    }

    public TestBaniraPlatform serverService(BaniraServerService value) {
        this.serverService = Objects.requireNonNull(value, "serverService");
        return this;
    }

    public TestBaniraPlatform playerDataService(BaniraPlayerDataService value) {
        this.playerDataService = Objects.requireNonNull(value, "playerDataService");
        return this;
    }

    public TestBaniraPlatform permissionService(BaniraPermissionService value) {
        this.permissionService = Objects.requireNonNull(value, "permissionService");
        return this;
    }

    public TestBaniraPlatform networkService(BaniraNetworkService value) {
        this.networkService = Objects.requireNonNull(value, "networkService");
        return this;
    }

    public TestBaniraPlatform registryService(BaniraRegistryService value) {
        this.registryService = Objects.requireNonNull(value, "registryService");
        return this;
    }

    public TestBaniraPlatform notificationService(BaniraNotificationService value) {
        this.notificationService = Objects.requireNonNull(value, "notificationService");
        return this;
    }

    public TestBaniraPlatform logoService(BaniraLogoService value) {
        this.logoService = Objects.requireNonNull(value, "logoService");
        return this;
    }

    public TestBaniraPlatform configDir(Path value) {
        this.configDir = Objects.requireNonNull(value, "configDir");
        return this;
    }

    public TestBaniraPlatform pathService(BaniraPathService value) {
        this.pathService = Objects.requireNonNull(value, "pathService");
        return this;
    }

    @Override
    public @Nonnull String loaderType() {
        return loaderType;
    }

    @Override
    public @Nonnull String minecraftVersion() {
        return minecraftVersion;
    }

    @Override
    public boolean isClient() {
        return client;
    }

    @Override
    public boolean isDedicatedServer() {
        return dedicatedServer;
    }

    @Override
    public boolean isDevelopment() {
        return development;
    }

    @Override
    public boolean isModLoaded(@Nonnull String modId) {
        return loadedMods.contains(modId);
    }

    @Override
    public @Nonnull String modDisplayName(@Nonnull String modId) {
        return modId;
    }

    @Override
    public String lastKnownUsername(@Nonnull UUID uuid) {
        return usernames.get(uuid);
    }

    @Override
    public @Nonnull String modIdFromMainClass(@Nonnull Class<?> modMainClass) {
        return modIdsByMainClass.getOrDefault(modMainClass, "test");
    }

    @Override
    public @Nonnull Class<?> modMainClass(@Nonnull String modId) {
        return modMainClasses.getOrDefault(modId, TestBaniraPlatform.class);
    }

    @Override
    public @Nonnull Path configDir() {
        return configDir;
    }

    @Override
    public @Nonnull BaniraPathService pathService() {
        return pathService;
    }

    @Override
    public @Nonnull BaniraConfigService configService() {
        return configService;
    }

    @Override
    public @Nonnull BaniraServerService serverService() {
        return serverService;
    }

    @Override
    public @Nonnull BaniraPlayerDataService playerDataService() {
        if (playerDataService == null) {
            throw new IllegalStateException("No player data service configured for this test");
        }
        return playerDataService;
    }

    @Override
    public @Nonnull BaniraPermissionService permissionService() {
        return permissionService;
    }

    @Override
    public @Nonnull BaniraNetworkService networkService() {
        return networkService;
    }

    @Override
    public @Nonnull BaniraRegistryService registryService() {
        return registryService;
    }

    @Override
    public @Nonnull BaniraInputService inputService() {
        return NoopInputService.INSTANCE;
    }

    @Override
    public @Nonnull BaniraNotificationService notificationService() {
        return notificationService;
    }

    @Override
    public @Nonnull BaniraLogoService logoService() {
        return logoService;
    }
}
