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
