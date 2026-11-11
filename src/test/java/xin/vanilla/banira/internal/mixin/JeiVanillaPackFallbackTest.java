package xin.vanilla.banira.internal.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class JeiVanillaPackFallbackTest {
    @BeforeClass
    public static void version() {
        SharedConstants.tryDetectVersion();
    }

    @Test
    public void constructsServerDataPackWithForgeHiddenFlag() throws Exception {
        verifyPack(true, true, Pack.Position.BOTTOM);
    }

    @Test
    public void preservesVisiblePackAndLazyResources() throws Exception {
        verifyPack(false, false, Pack.Position.TOP);
    }

    private static void verifyPack(boolean hidden, boolean required, Pack.Position position) throws Exception {
        String name = "xin.vanilla.banira.internal.mixin.compat.jei.VanillaClientRecipeLoaderMixin";
        assertNotNull("JEI fallback must replace the incompatible PackConstructor",
                JeiVanillaPackFallbackTest.class.getClassLoader().getResource(name.replace('.', '/') + ".class"));
        Class<?> mixin = Class.forName(name);
        Method replacement = mixin.getDeclaredMethod("banira$forgePackConstructor", Pack.PackConstructor.class);
        replacement.setAccessible(true);
        Pack.PackConstructor broken = (id, title, originalRequired, resources, metadata, originalPosition, source, ignored) -> {
            throw new AssertionError("The incompatible JEI constructor must not be invoked");
        };
        Pack.PackConstructor factory = (Pack.PackConstructor) replacement.invoke(null, broken);
        AtomicInteger opened = new AtomicInteger();
        Supplier<PackResources> resources = () -> {
            opened.incrementAndGet();
            return null;
        };
        Component title = Component.literal("Vanilla server data");
        Component description = Component.literal("Server recipes");
        PackMetadataSection metadata = new PackMetadataSection(description, 10);
        Pack pack = factory.create("vanilla", title, required, resources, metadata,
                position, PackSource.BUILT_IN, hidden);
        assertEquals("vanilla", pack.getId());
        assertEquals(title, pack.getTitle());
        assertEquals(description, pack.getDescription());
        assertEquals(required, pack.isRequired());
        assertEquals(position, pack.getDefaultPosition());
        assertEquals(PackSource.BUILT_IN, pack.getPackSource());
        assertEquals(PackCompatibility.COMPATIBLE, pack.getCompatibility());
        assertEquals(hidden, pack.isHidden());
        assertEquals(0, opened.get());
        assertNull(pack.open());
        assertEquals(1, opened.get());
    }
}
