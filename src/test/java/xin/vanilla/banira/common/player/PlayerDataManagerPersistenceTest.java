package xin.vanilla.banira.common.player;

import net.minecraft.nbt.CompoundTag;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.util.NBTUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PlayerDataManagerPersistenceTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String suffix;

    @After
    public void removeManager() {
        if (suffix != null) {
            PlayerDataManager.removeInstance(suffix);
        }
    }

    @Test
    public void repeatedReadOnlySavesDoNotRewriteCompressedPlayerData() throws Exception {
        Path root = temporaryFolder.newFolder("playerdata").toPath();
        suffix = "test_" + UUID.randomUUID().toString().replace("-", "");
        UUID playerUuid = UUID.randomUUID();
        PlayerDataManager manager = PlayerDataManager.getOrCreateInstance(
                () -> root, "test_mod", suffix
        );

        manager.getOrCreate(playerUuid).putString("value", "initial");
        manager.saveToDisk(playerUuid);

        Path playerFile = root.resolve(suffix).resolve(playerUuid + ".nbt");
        assertTrue(Files.isRegularFile(playerFile));
        FileTime unchangedMarker = FileTime.fromMillis(1_000L);
        Files.setLastModifiedTime(playerFile, unchangedMarker);

        for (int i = 0; i < 5_000; i++) {
            manager.getOrCreate(playerUuid);
            manager.saveToDisk(playerUuid);
        }
        for (int i = 0; i < 5_000; i++) {
            manager.getOrCreate(playerUuid);
            manager.saveAll();
        }
        assertEquals(unchangedMarker, Files.getLastModifiedTime(playerFile));

        manager.getOrCreate(playerUuid).putString("value", "changed");
        manager.saveToDisk(playerUuid);
        assertNotEquals(unchangedMarker, Files.getLastModifiedTime(playerFile));
        CompoundTag persisted = NBTUtils.readCompressed(playerFile.toFile());
        assertEquals("changed", persisted.getCompound("test_mod").getString("value"));
    }
}
