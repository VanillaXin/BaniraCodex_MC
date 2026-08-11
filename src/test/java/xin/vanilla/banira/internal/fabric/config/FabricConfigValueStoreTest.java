package xin.vanilla.banira.internal.fabric.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FabricConfigValueStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void stringListsRoundTripEscapedCommasAndBackslashes() throws Exception {
        File file = temporaryFolder.newFile("strings.properties");
        List<ConfigEntryDescriptor> descriptors = List.of(descriptor(
                "names",
                ConfigEntryDescriptor.ConfigValueType.STRING_LIST,
                List.of("default")
        ));
        FabricConfigValueStore store = new FabricConfigValueStore(file.toPath(), descriptors);

        store.set("names", List.of("a,b", "c\\d", "plain"));
        store.save();

        FabricConfigValueStore reloaded = new FabricConfigValueStore(file.toPath(), descriptors);
        assertEquals(List.of("a,b", "c\\d", "plain"), reloaded.get("names"));
    }

    @Test
    public void listValidationRejectsInvalidElementsAndNormalizesAcceptedValues() throws Exception {
        File file = temporaryFolder.newFile("ints.properties");
        List<ConfigEntryDescriptor> descriptors = List.of(ConfigEntryDescriptor.builder()
                .path("ints")
                .displayName("Ints")
                .tooltip(List.of())
                .valueType(ConfigEntryDescriptor.ConfigValueType.INTEGER_LIST)
                .defaultValue(List.of(1))
                .minValue(1)
                .maxValue(3)
                .build());
        FabricConfigValueStore store = new FabricConfigValueStore(file.toPath(), descriptors);

        store.set("ints", List.of(2, "3"));
        assertEquals(List.of(2, 3), store.get("ints"));

        store.set("ints", List.of(2, 4));
        assertEquals(List.of(2, 3), store.get("ints"));
    }

    @Test
    public void reloadFromDiskReplacesValuesAndRestoresRemovedKeys() throws Exception {
        File file = temporaryFolder.newFile("reload.toml");
        List<ConfigEntryDescriptor> descriptors = List.of(descriptor(
                "value", ConfigEntryDescriptor.ConfigValueType.STRING, "default"));
        FabricConfigValueStore store = new FabricConfigValueStore(file.toPath(), descriptors);

        Files.write(file.toPath(), "value = \"disk\"\n".getBytes(StandardCharsets.UTF_8));
        store.reloadFromDisk();
        assertEquals("disk", store.get("value"));

        Files.write(file.toPath(), new byte[0]);
        store.reloadFromDisk();
        assertEquals("default", store.get("value"));
    }

    private static ConfigEntryDescriptor descriptor(String path, ConfigEntryDescriptor.ConfigValueType type, Object defaultValue) {
        return ConfigEntryDescriptor.builder()
                .path(path)
                .displayName(path)
                .tooltip(List.of())
                .valueType(type)
                .defaultValue(defaultValue)
                .build();
    }
}
