package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.BaniraPlayerDataService;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertSame;

public class BaniraPlayerDataTest {
    @Test
    public void readsAndWritesTypedDataWithoutNativeApiTypes() {
        UUID uuid = UUID.randomUUID();
        Object stored = new Object();
        AtomicReference<Object> written = new AtomicReference<>();
        BaniraPlatforms.install(new TestBaniraPlatform().playerDataService(new BaniraPlayerDataService() {
            @Override
            public Object getOrCreate(UUID playerUuid, String modId) {
                return stored;
            }

            @Override
            public void put(UUID playerUuid, String modId, Object data) {
                written.set(data);
            }
        }));

        assertSame(stored, BaniraPlayerData.getOrCreate(uuid, "child_mod", Object.class));
        BaniraPlayerData.put(uuid, "child_mod", stored);
        assertSame(stored, written.get());
    }
}
