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

import static org.junit.Assert.assertEquals;
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
                if (line.startsWith("import net.minecraft.")
                        || line.startsWith("import net.minecraftforge.")
                        || line.startsWith("import xin.vanilla.banira.internal.")) {
                    violations.add(location(file, i + 1) + " " + line);
                }
            }
        });
        assertNoViolations("Public api/platform imports must stay loader-neutral.", violations);
    }

    @Test
    public void stableServerRuntimeFacadeDoesNotExposeMinecraftTypes() throws Exception {
        Path facade = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api", "BaniraServer.java"));
        if (!Files.exists(facade)) {
            fail("Server runtime access must stay in api.BaniraServer.");
        }
        String source = new String(Files.readAllBytes(facade), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "net.minecraft.", violations, "BaniraServer must use neutral server handles.");
        addIfContains(source, "xin.vanilla.banira.internal.", violations, "BaniraServer must delegate through the platform contract.");
        assertNoViolations("Server runtime access should stay loader-neutral.", violations);

        Class<?> service = Class.forName("xin.vanilla.banira.platform.BaniraServerService");
        assertEquals(service, BaniraPlatform.class.getMethod("serverService").getReturnType());
    }

    @Test
    public void stablePlayerDataFacadeDoesNotExposeMinecraftTypes() throws Exception {
        Path facade = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api", "BaniraPlayerData.java"));
        if (!Files.exists(facade)) {
            fail("Player data access must stay in api.BaniraPlayerData.");
        }
        String source = new String(Files.readAllBytes(facade), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "net.minecraft.", violations, "BaniraPlayerData must use neutral data handles.");
        addIfContains(source, "xin.vanilla.banira.internal.", violations, "BaniraPlayerData must delegate through the platform contract.");
        assertNoViolations("Player data access should stay loader-neutral.", violations);

        Class<?> service = Class.forName("xin.vanilla.banira.platform.BaniraPlayerDataService");
        assertEquals(service, BaniraPlatform.class.getMethod("playerDataService").getReturnType());
    }

    @Test
    public void rootPlatformDoesNotHideInternalDefaults() throws IOException {
        Path platform = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "platform", "BaniraPlatform.java"));
        String source = new String(Files.readAllBytes(platform), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "default String minecraftVersion", violations, "minecraftVersion must be implemented per branch.");
        addIfContains(source, "default BaniraPathService", violations, "pathService must be implemented by the loader adapter.");
        addIfContains(source, "default BaniraInputService", violations, "inputService must be implemented by the loader adapter.");
        addIfContains(source, "xin.vanilla.banira.internal", violations, "root platform must not depend on internal packages.");
        assertNoViolations("BaniraPlatform should be a pure contract.", violations);
    }

    @Test
    public void networkServiceDoesNotExposeLegacyIdentifierFactory() throws IOException {
        Path networkService = MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "platform", "BaniraNetworkService.java"));
        String source = new String(Files.readAllBytes(networkService), StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();
        addIfContains(source, "common.util.IIdentifier", violations, "Network service registration must use api.BaniraIdentifier.");
        addIfContains(source, " IIdentifier ", violations, "Network service registration must not expose IIdentifier.");
        assertNoViolations("Network service registration should stay loader-neutral.", violations);
    }

    @Test
    public void sharedCommonAndClientPackagesDoNotImportLoaderApis() throws IOException {
        List<String> violations = new ArrayList<>();
        forEachSharedPackageFile(file -> {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("import net.minecraftforge.")
                        || line.startsWith("import net.fabricmc.")
                        || line.startsWith("import net.neoforged.")
                        || line.startsWith("import xin.vanilla.banira.internal.forge.")
                        || line.startsWith("import xin.vanilla.banira.internal.fabric.")
                        || line.startsWith("import xin.vanilla.banira.internal.neoforge.")) {
                    violations.add(location(file, i + 1) + " " + line);
                }
            }
        });
        assertNoViolations("Shared common/client packages must keep loader APIs in internal adapters.", violations);
    }

    private static void forEachPublicApiFile(ThrowingPathConsumer consumer) throws IOException {
        forEachJavaFile(MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "api")), consumer);
        forEachJavaFile(MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "platform")), consumer);
    }

    private static void forEachSharedPackageFile(ThrowingPathConsumer consumer) throws IOException {
        forEachJavaFile(MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "common")), consumer);
        forEachJavaFile(MAIN_SOURCE.resolve(Paths.get("xin", "vanilla", "banira", "client")), consumer);
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
