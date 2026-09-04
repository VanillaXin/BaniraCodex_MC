package xin.vanilla.banira.internal.dependency;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Locks optional inventory integrations out of the production dependency graph. */
public final class OptionalIntegrationDependencyContractTest {
    private static final List<String> OPTIONAL_ARTIFACTS = Arrays.asList(
            "curse.maven:jei-238222",
            "curse.maven:architectury-api-419699",
            "curse.maven:ftb-library-forge-404465"
    );

    @Test
    public void compilesOptionalInventoryIntegrationsWithoutPublishingThem() throws IOException {
        String build = new String(Files.readAllBytes(Paths.get("build.gradle")), "UTF-8");
        for (String artifact : OPTIONAL_ARTIFACTS) {
            assertFalse("Optional integration must not use implementation: " + artifact,
                    build.contains("implementation fg.deobf(\"" + artifact));
            boolean compileOnly = build.contains("compileOnly fg.deobf(\"" + artifact);
            boolean runtimeOnly = build.contains("runtimeOnly fg.deobf(\"" + artifact);
            assertEquals("Optional integration must use matching compile and development runtime scopes: " + artifact,
                    compileOnly, runtimeOnly);
        }
    }
}
