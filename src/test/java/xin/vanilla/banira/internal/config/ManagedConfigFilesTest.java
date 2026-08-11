package xin.vanilla.banira.internal.config;

import org.junit.After;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ManagedConfigFilesTest {
    @After
    public void cleanUp() {
        ManagedConfigFiles.clearForTests();
    }

    @Test
    public void reloadsExternalChangesButIgnoresOwnWrites() throws Exception {
        Path file = Files.createTempFile("banira-config", ".json");
        Files.write(file, "one".getBytes(StandardCharsets.UTF_8));
        AtomicInteger reloads = new AtomicInteger();
        ManagedConfigFiles.register(file, ManagedConfigFiles.Scope.CLIENT, reloads::incrementAndGet);

        Files.write(file, "two".getBytes(StandardCharsets.UTF_8));
        ManagedConfigFiles.pollNowForTests(ManagedConfigFiles.Scope.CLIENT);
        assertEquals(1, reloads.get());

        Files.write(file, "three".getBytes(StandardCharsets.UTF_8));
        ManagedConfigFiles.markWritten(file);
        ManagedConfigFiles.pollNowForTests(ManagedConfigFiles.Scope.CLIENT);
        assertEquals(1, reloads.get());
    }
}
