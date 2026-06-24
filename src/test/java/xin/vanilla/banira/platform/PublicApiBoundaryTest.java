package xin.vanilla.banira.platform;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

public class PublicApiBoundaryTest {
    private static final Path MAIN_SOURCE = Paths.get("src", "main", "java");

    @Test
    public void publicApiDoesNotImportMinecraftForgeOrInternalTypes() throws IOException {
        List<String> violations = new ArrayList<>();
        forEachPublicApiFile(file -> {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.contains("net.minecraft.")
                        || line.contains("net.minecraftforge.")
                        || line.contains("net.fabricmc.")
                        || line.contains("xin.vanilla.banira.internal.")) {
                    violations.add(location(file, i + 1) + " " + line);
                }
            }
        });
        assertNoViolations("Public api/platform imports must stay loader-neutral.", violations);
    }

    @Test
    public void rootPlatformDoesNotHideInternalDefaults() throws IOException {
        Path platform = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "platform", "BaniraPlatform.java"));
        String source = new String(Files.readAllBytes(platform), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "default String minecraftVersion", violations, "minecraftVersion must be implemented per branch.");
        addIfContains(source, "default BaniraPathService", violations, "pathService must be implemented by the loader adapter.");
        addIfContains(source, "default BaniraInputService", violations, "inputService must be implemented by the loader adapter.");
        addIfContains(source, "default BaniraNotificationService", violations, "notificationService must be implemented by the loader adapter.");
        addIfContains(source, "xin.vanilla.banira.internal", violations, "root platform must not depend on internal packages.");
        assertNoViolations("BaniraPlatform should be a pure contract.", violations);
    }

    @Test
    public void legacyClientInputUtilityDoesNotReturnToPublicPackages() {
        Path legacy = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "client", "util", "GLFWKeyUtils.java"));
        if (Files.exists(legacy)) {
            fail("GLFWKeyUtils must stay internal. Public key helpers belong in api.client.input.BaniraKeyCodes.");
        }
    }

    @Test
    public void baniraScreenDoesNotExposeInputStateManager() throws IOException {
        Path screen = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "client", "gui", "BaniraScreen.java"));
        String source = new String(Files.readAllBytes(screen), StandardCharsets.UTF_8);
        if (source.contains("protected final InputStateManager inputState")) {
            fail("BaniraScreen.inputState() must expose BaniraInputState, not the internal InputStateManager implementation.");
        }
    }

    @Test
    public void inputStateManagerDoesNotReturnToClientUtil() {
        Path legacy = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "client", "util", "InputStateManager.java"));
        if (Files.exists(legacy)) {
            fail("InputStateManager must stay internal. Public input state belongs in api.client.input.BaniraInputState.");
        }
    }

    @Test
    public void environmentUtilsStaysCompatibilityFacade() throws IOException {
        Path environment = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "common", "util", "EnvironmentUtils.java"));
        String source = new String(Files.readAllBytes(environment), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "Banira.platform()", violations, "EnvironmentUtils must delegate to api.BaniraEnvironment.");
        addIfContains(source, "FabricLoader", violations, "EnvironmentUtils must not depend on loader APIs.");
        assertNoViolations("New environment API belongs in api.BaniraEnvironment.", violations);
    }

    @Test
    public void packetUtilsUsesNetworkFacadeForSimpleSending() throws IOException {
        Path packetUtils = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "common", "util", "PacketUtils.java"));
        String source = new String(Files.readAllBytes(packetUtils), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "networkService().sendToServer", violations, "PacketUtils simple send must delegate to api.BaniraNetwork.");
        addIfContains(source, "networkService().hasDefaultChannel", violations, "PacketUtils channel checks must delegate to api.BaniraNetwork.");
        addIfContains(source, "networkService().hasLocalChannel", violations, "PacketUtils channel checks must delegate to api.BaniraNetwork.");
        addIfContains(source, "networkService().hasPlayerChannel", violations, "PacketUtils channel checks must delegate to api.BaniraNetwork.");
        if (!source.contains("BaniraNetwork.sendToPlayer(msg, player)")) {
            violations.add("PacketUtils sendPacketToPlayer must delegate to api.BaniraNetwork.");
        }
        assertNoViolations("New network API belongs in api.BaniraNetwork.", violations);
    }

    @Test
    public void baniraConfigDelegatesToStableApiFacade() throws IOException {
        Path facade = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api", "BaniraConfigs.java"));
        if (!Files.exists(facade)) {
            fail("Config registration facade must stay in api.BaniraConfigs.");
        }
        Path legacy = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "common", "config", "BaniraConfig.java"));
        String source = new String(Files.readAllBytes(legacy), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "Banira.platform()", violations, "common.config.BaniraConfig must delegate to api.BaniraConfigs.");
        if (!source.contains("BaniraConfigs.register") || !source.contains("BaniraConfigs.handle")) {
            violations.add("common.config.BaniraConfig must remain a compatibility facade over api.BaniraConfigs.");
        }
        assertNoViolations("New config API belongs in api.BaniraConfigs.", violations);
    }

    @Test
    public void virtualPermissionTypeKeepsStableApiFacade() throws IOException {
        Path facade = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api", "permission", "BaniraVirtualPermission.java"));
        if (!Files.exists(facade)) {
            fail("Virtual permission type must stay in api.permission.BaniraVirtualPermission.");
        }
        Path legacy = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "common", "api", "IVirtualPermissionType.java"));
        String legacySource = new String(Files.readAllBytes(legacy), StandardCharsets.UTF_8);
        Path manager = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "common", "util", "VirtualPermissionManager.java"));
        String managerSource = new String(Files.readAllBytes(manager), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        if (!legacySource.contains("extends BaniraVirtualPermission")) {
            violations.add("IVirtualPermissionType must remain a compatibility alias over api.permission.BaniraVirtualPermission.");
        }
        if (!managerSource.contains("BaniraVirtualPermission")) {
            violations.add("VirtualPermissionManager must accept the stable api.permission.BaniraVirtualPermission type.");
        }
        assertNoViolations("Virtual permission type belongs in api.permission.", violations);
    }

    @Test
    public void modLoadedPresenceKeepsStableApiFacade() throws IOException {
        Path facade = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api", "BaniraModPresence.java"));
        if (!Files.exists(facade)) {
            fail("Client optional mod presence facade must stay in api.BaniraModPresence.");
        }
        Path legacy = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "common", "network", "ModLoadedPresence.java"));
        String source = new String(Files.readAllBytes(legacy), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        if (!source.contains("registerInternal") || !source.contains("announcedModIdsInternal")) {
            violations.add("ModLoadedPresence must expose internal methods for api.BaniraModPresence delegation.");
        }
        assertNoViolations("Mod presence registration belongs in api.BaniraModPresence.", violations);
    }

    @Test
    public void notificationTypeRegistriesKeepStableApiFacades() throws IOException {
        Path serverFacade = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api", "notification", "BaniraNotificationTypes.java"));
        Path clientFacade = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api", "client", "notification", "BaniraClientNotificationTypes.java"));
        if (!Files.exists(serverFacade) || !Files.exists(clientFacade)) {
            fail("Notification type registration must stay in api.notification and api.client.notification facades.");
        }
        Path serverRegistry = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "common", "notification", "ServerNotificationTypeRegistry.java"));
        Path clientRegistry = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "client", "notification", "NotificationTypeRegistry.java"));
        String serverSource = new String(Files.readAllBytes(serverRegistry), StandardCharsets.UTF_8);
        String clientSource = new String(Files.readAllBytes(clientRegistry), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        if (!serverSource.contains("registerInternal") || !serverSource.contains("sortedSnapshotInternal")) {
            violations.add("Server notification type registry must expose internal methods for api.notification delegation.");
        }
        if (!clientSource.contains("registerInternal") || !clientSource.contains("knownTypesSortedInternal")) {
            violations.add("Client notification type registry must expose internal methods for api.client.notification delegation.");
        }
        assertNoViolations("Notification type registration belongs in Banira notification type facades.", violations);
    }

    @Test
    public void logoModifierDoesNotReturnToClientUtil() {
        Path legacy = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "client", "util", "LogoModifier.java"));
        if (Files.exists(legacy)) {
            fail("LogoModifier must stay internal. Runtime logo mutation is loader-specific implementation detail.");
        }
    }

    @Test
    public void fabricClientEntrypointDoesNotOwnOverlayImplementation() throws IOException {
        Path entrypoint = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "internal", "fabric", "client", "FabricBaniraCodexClient.java"));
        if (!Files.exists(entrypoint)) {
            return;
        }
        String source = new String(Files.readAllBytes(entrypoint), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "client.util.NotificationManager", violations, "Fabric client entrypoint must use BaniraClientOverlayBridge for notifications.");
        addIfContains(source, "client.gui.quickaction.QuickActionOverlay", violations, "Fabric client entrypoint must use BaniraClientOverlayBridge for quick actions.");
        addIfContains(source, "InputStateManager", violations, "Fabric client entrypoint must use BaniraClientInputBridge for input state.");
        addIfContains(source, "BaniraMouseEvent", violations, "Fabric client entrypoint must use BaniraClientInputBridge for mouse events.");
        addIfContains(source, "BaniraKeyboardEvent", violations, "Fabric client entrypoint must use BaniraClientInputBridge for keyboard events.");
        assertNoViolations("Loader entrypoints should only adapt native events.", violations);
    }

    private static void forEachPublicApiFile(ThrowingPathConsumer consumer) throws IOException {
        forEachJavaFile(MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api")), consumer);
        forEachJavaFile(MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "platform")), consumer);
    }

    private static void forEachJavaFile(Path root, ThrowingPathConsumer consumer) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : (Iterable<Path>) stream.filter(path -> path.toString().endsWith(".java"))::iterator) {
                consumer.accept(path);
            }
        }
    }

    private static void addIfContains(String source, String needle, List<String> violations, String message) {
        if (source.contains(needle)) {
            violations.add(message);
        }
    }

    private static String location(Path file, int line) {
        return MAIN_SOURCE.relativize(file) + ":" + line;
    }

    private static void assertNoViolations(String message, List<String> violations) {
        if (!violations.isEmpty()) {
            fail(message + System.lineSeparator() + String.join(System.lineSeparator(), violations));
        }
    }

    private interface ThrowingPathConsumer {
        void accept(Path path) throws IOException;
    }
}
