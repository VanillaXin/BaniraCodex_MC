package xin.vanilla.banira.common.util;

import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Bootstrap;
import net.minecraftforge.registries.ForgeRegistries;
import org.junit.Test;
import xin.vanilla.banira.internal.forge.platform.ForgeBaniraRegistryService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.BaniraRegistryService;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class RegistryKeyReuseTest {
    @Test
    public void nativeRegistryKeysAreReusedWithoutStringRoundTrips() {
        Bootstrap.bootStrap();
        BaniraPlatforms.install(new TestBaniraPlatform().registryService(new ForgeBaniraRegistryService()));
        try {
            assertSame(ForgeRegistries.ITEMS.getKey(Items.APPLE), ItemUtils.getItemRegistry(Items.APPLE));
            assertSame(ForgeRegistries.ENTITIES.getKey(EntityType.PIG), EntityUtils.getEntityRegistry(EntityType.PIG));
            assertEquals("minecraft:apple", ItemUtils.getItemRegistryString(Items.APPLE));
            assertEquals("minecraft:pig", EntityUtils.getEntityRegistryString(EntityType.PIG));
        } finally {
            BaniraPlatforms.install(new TestBaniraPlatform());
        }
    }

    @Test
    public void stringOnlyServicesStillNormalizeIdsAndHandleInvalidOrMissingKeys() {
        Bootstrap.bootStrap();
        String[] id = {"apple"};
        BaniraRegistryService service = new StringRegistryService(id);
        BaniraPlatforms.install(new TestBaniraPlatform().registryService(service));
        try {
            assertEquals(new ResourceLocation("minecraft:apple"), ItemUtils.getItemRegistry(Items.APPLE));
            assertEquals(new ResourceLocation("minecraft:apple"), EntityUtils.getEntityRegistry(EntityType.PIG));
            id[0] = "INVALID id";
            assertNull(ItemUtils.getItemRegistry(Items.APPLE));
            assertNull(EntityUtils.getEntityRegistry(EntityType.PIG));
            id[0] = null;
            assertNull(ItemUtils.getItemRegistry(Items.APPLE));
            assertNull(EntityUtils.getEntityRegistry(EntityType.PIG));
        } finally {
            BaniraPlatforms.install(new TestBaniraPlatform());
        }
    }

    private static final class StringRegistryService implements BaniraRegistryService {
        private final String[] id;

        StringRegistryService(String[] id) {
            this.id = id;
        }

        public String itemKey(Object item) {
            return id[0];
        }

        public String entityTypeKey(Object entityType) {
            return id[0];
        }

        public String blockKey(Object block) {
            return null;
        }

        public Object block(String id) {
            return null;
        }

        public Collection<?> blocks() {
            return Collections.emptyList();
        }

        public Object item(String id) {
            return null;
        }

        public Collection<?> items() {
            return Collections.emptyList();
        }

        public Collection<String> itemTagIds(Object item) {
            return Collections.emptyList();
        }

        public Object entityType(String id) {
            return null;
        }

        public Collection<?> entityTypes() {
            return Collections.emptyList();
        }

        public String effectKey(Object effect) {
            return null;
        }

        public Object effect(String id) {
            return null;
        }

        public Collection<?> effects() {
            return Collections.emptyList();
        }

        public Object biome(String id) {
            return null;
        }

        public Collection<String> biomeIds() {
            return Collections.emptyList();
        }
    }
}
