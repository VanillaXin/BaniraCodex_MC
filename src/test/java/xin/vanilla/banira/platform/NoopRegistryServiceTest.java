package xin.vanilla.banira.platform;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NoopRegistryServiceTest {
    @Test
    public void noopRegistryReturnsEmptyValues() {
        assertNull(NoopRegistryService.INSTANCE.blockKey(null));
        assertNull(NoopRegistryService.INSTANCE.item(null));
        assertTrue(NoopRegistryService.INSTANCE.blocks().isEmpty());
        assertTrue(NoopRegistryService.INSTANCE.itemTagIds(null).isEmpty());
        assertTrue(NoopRegistryService.INSTANCE.biomeIds().isEmpty());
    }
}
