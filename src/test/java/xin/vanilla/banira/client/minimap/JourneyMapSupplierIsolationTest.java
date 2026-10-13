package xin.vanilla.banira.client.minimap;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.Test;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class JourneyMapSupplierIsolationTest {
    private MinimapLine line(String name, java.util.function.Supplier<EnumNotificationHudHost> host) {
        return new MinimapLine(new ResourceLocation("isolation", name), Component.literal(name), () -> Component.literal(name), host, 200);
    }
    private List<String> slots(MinimapLine broken, MinimapLine healthy) throws Exception {
        Class<?> plugin = Class.forName("xin.vanilla.banira.internal.forge.compat.minimap.JourneyMapNotificationPlugin");
        Method method = plugin.getDeclaredMethod("appendDisplaySlots", List.class, Consumer.class);
        method.setAccessible(true);
        List<String> result = new ArrayList<>();
        method.invoke(null, Arrays.asList(broken, healthy), (Consumer<String>) result::add);
        return result;
    }
    @Test public void throwingHostDoesNotSuppressFollowingCountdownSlot() throws Exception {
        MinimapLine bad = line("broken", () -> {throw new IllegalStateException("supplier failure");});
        MinimapLine healthy = line("countdown", () -> EnumNotificationHudHost.AUTO);
        assertEquals(Arrays.asList("countdown"), slots(bad, healthy));
    }
    @Test public void linkageFailureDoesNotSuppressFollowingCountdownSlot() throws Exception {
        MinimapLine bad = line("broken", () -> {throw new NoClassDefFoundError("optional module");});
        MinimapLine healthy = line("countdown", () -> EnumNotificationHudHost.JOURNEYMAP);
        assertEquals(Arrays.asList("countdown"), slots(bad, healthy));
    }
    @Test public void inactivePreferenceStillDoesNotAllocateNativeSlot() throws Exception {
        assertEquals(Arrays.asList("countdown"), slots(line("other", () -> EnumNotificationHudHost.XAERO),
                line("countdown", () -> EnumNotificationHudHost.JOURNEYMAP)));
    }

    @Test public void automaticOwnerExpiryIsAnIndependentRefreshInput() throws Exception {
        org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        new org.objectweb.asm.ClassReader(getClass().getResourceAsStream(
                "/xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin.class")).accept(node, 0);
        assertTrue(node.fields.stream().anyMatch(f -> f.name.equals("previousAutomaticOwner")));
        org.objectweb.asm.tree.MethodNode refresh = node.methods.stream().filter(m -> m.name.equals("refreshLayout")).findFirst().get();
        boolean selector = false;
        for (org.objectweb.asm.tree.AbstractInsnNode i : refresh.instructions) if (i instanceof org.objectweb.asm.tree.MethodInsnNode) {
            org.objectweb.asm.tree.MethodInsnNode call = (org.objectweb.asm.tree.MethodInsnNode) i;
            selector |= call.owner.endsWith("/MinimapLineRegistry") && call.name.equals("selectedHost");
        }
        assertTrue(selector);
    }
}
