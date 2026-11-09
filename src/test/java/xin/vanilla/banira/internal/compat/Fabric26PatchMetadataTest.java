package xin.vanilla.banira.internal.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

public class Fabric26PatchMetadataTest {
    private JsonObject metadata() throws Exception {
        try (var jar = new JarFile(System.getProperty("banira.publication.jar"));
             var reader = new InputStreamReader(jar.getInputStream(jar.getJarEntry("fabric.mod.json")), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private boolean accepts(JsonArray predicates, String version) throws Exception {
        for (var predicate : predicates) {
            if (VersionPredicate.parse(predicate.getAsString()).test(Version.parse(version))) return true;
        }
        return false;
    }

    @Test
    public void formalArchiveAllowsExactlyTheThreeReleasedPatches() throws Exception {
        var versions = metadata().getAsJsonObject("depends").getAsJsonArray("minecraft");
        assertEquals(3, versions.size());
        assertEquals(List.of("26.1", "26.1.1", "26.1.2"),
                versions.asList().stream().map(value -> value.getAsString()).toList());
        for (String version : List.of("26.1", "26.1.1", "26.1.2")) assertTrue(version, accepts(versions, version));
    }

    @Test
    public void futurePatchesSnapshotsAndLaterDropsRemainRejected() throws Exception {
        var versions = metadata().getAsJsonObject("depends").getAsJsonArray("minecraft");
        for (String version : List.of("26.1.3", "26.1.99", "26.1.2-rc.1", "26.2", "26.3", "1.21.1")) {
            assertFalse(version, accepts(versions, version));
        }
    }

    @Test
    public void engineRemainsOptionalAndTheNativeEntryPointsRemainPresent() throws Exception {
        var root = metadata();
        assertFalse(root.getAsJsonObject("depends").has("apricityui"));
        assertTrue(root.getAsJsonObject("suggests").has("apricityui"));
        assertEquals(">=25", root.getAsJsonObject("depends").get("java").getAsString());
        assertTrue(root.getAsJsonObject("entrypoints").has("main"));
        assertTrue(root.getAsJsonObject("entrypoints").has("client"));
        assertEquals("banira_codex.classtweaker", root.get("accessWidener").getAsString());
    }
}
