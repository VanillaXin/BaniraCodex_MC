package xin.vanilla.banira.internal.fabric.platform;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class FabricRegistryValueContractTest {
    @BeforeClass
    public static void bootstrap() {
        xin.vanilla.banira.test.NativeBootstrap.initialize();
    }

    @Test
    public void platformReturnsRegistryValuesInsteadOfOptionalHolders() {
        var registry = FabricBaniraRegistryService.INSTANCE;
        assertSame(Items.DIAMOND, registry.item("minecraft:diamond"));
        assertSame(Blocks.STONE, registry.block("minecraft:stone"));
        assertSame(EntityType.PIG, registry.entityType("minecraft:pig"));
        assertSame(MobEffects.STRENGTH.value(), registry.effect("minecraft:strength"));
        assertEquals("minecraft:diamond", registry.itemKey(registry.item("minecraft:diamond")));
        assertEquals("minecraft:strength", registry.effectKey(registry.effect("minecraft:strength")));
    }

    @Test
    public void malformedIdsRemainAbsentAndUnknownEffectsNeverSelectADefault() {
        var registry = FabricBaniraRegistryService.INSTANCE;
        for (String id : new String[]{null, "Bad ID", "minecraft:INVALID"}) {
            assertNull(registry.item(id));
            assertNull(registry.block(id));
            assertNull(registry.entityType(id));
            assertNull(registry.effect(id));
        }
        assertNull(registry.effect("banira_test:missing"));
    }
}
