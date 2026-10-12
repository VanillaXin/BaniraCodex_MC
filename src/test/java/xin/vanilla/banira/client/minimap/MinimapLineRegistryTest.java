package xin.vanilla.banira.client.minimap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class MinimapLineRegistryTest {
    private static final String REGISTRY = "xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry";
    private static final String LINE = "xin.vanilla.banira.api.client.hud.MinimapLine";
    private static final EnumNotificationHudHost XAERO = EnumNotificationHudHost.XAERO;

    private Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Missing minimap information-line implementation: " + name, e);
        }
    }

    private Object registry() throws Exception {
        return type(REGISTRY).getConstructor().newInstance();
    }

    private Object line(String id, int order, EnumNotificationHudHost host, Supplier<Component> text) throws Exception {
        return type(LINE).getConstructor(ResourceLocation.class, Component.class,
                Supplier.class, Supplier.class, int.class).newInstance(
                new ResourceLocation("fixture", id), Component.literal(id), text,
                (Supplier<EnumNotificationHudHost>) () -> host, order);
    }

    private Object line(String id, int order, String text) throws Exception {
        return line(id, order, EnumNotificationHudHost.AUTO, () -> Component.literal(text));
    }

    private Object call(Object target, String name, Object... args) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof Exception) throw (Exception) e.getCause();
                    throw new AssertionError(e.getCause());
                }
            }
        }
        throw new AssertionError("Missing minimap method " + name);
    }

    @SuppressWarnings("unchecked")
    private List<Object> snapshot(Object registry, EnumNotificationHudHost host, long now) throws Exception {
        return (List<Object>) call(registry, "snapshot", host, now);
    }

    private List<String> ids(List<Object> rows) throws Exception {
        List<String> result = new ArrayList<>();
        for (Object row : rows) result.add(call(row, "id").toString());
        return result;
    }

    @Test
    public void duplicateIdKeepsFirstRegistration() throws Exception {
        Object registry = registry();
        assertEquals(true, call(registry, "register", line("a", 10, "first")));
        assertEquals(false, call(registry, "register", line("a", 20, "second")));
        assertEquals(1, ((List<?>) call(registry, "registrations")).size());
        call(registry, "seen", XAERO, 0L);
        assertEquals("first", ((Component) call(snapshot(registry, XAERO, 0L).get(0), "text")).getString());
    }

    @Test
    public void orderedSnapshotSkipsEmptyLines() throws Exception {
        Object registry = registry();
        call(registry, "register", line("z", 200, "z"));
        call(registry, "register", line("b", 100, "b"));
        call(registry, "register", line("a", 100, "a"));
        call(registry, "register", line("empty", 50, ""));
        call(registry, "seen", XAERO, 0L);
        assertEquals(Arrays.asList("fixture:a", "fixture:b", "fixture:z"), ids(snapshot(registry, XAERO, 0L)));
    }

    @Test
    public void automaticSelectionUsesPriorityAndExpiresAfterFiveHundredMilliseconds() throws Exception {
        Object registry = registry();
        call(registry, "register", line("a", 100, "a"));
        assertTrue(snapshot(registry, XAERO, 0L).isEmpty());
        call(registry, "seen", EnumNotificationHudHost.JOURNEYMAP, 0L);
        call(registry, "seen", XAERO, 0L);
        assertEquals(1, snapshot(registry, XAERO, 500_000_000L).size());
        assertTrue(snapshot(registry, EnumNotificationHudHost.JOURNEYMAP, 500_000_000L).isEmpty());
        assertTrue(snapshot(registry, XAERO, 500_000_001L).isEmpty());
    }

    @Test
    public void explicitHostDoesNotFallBackToAnotherMap() throws Exception {
        Object registry = registry();
        call(registry, "register", line("a", 1, EnumNotificationHudHost.JOURNEYMAP,
                () -> Component.literal("a")));
        call(registry, "seen", XAERO, 0L);
        assertTrue(snapshot(registry, XAERO, 0L).isEmpty());
        call(registry, "seen", EnumNotificationHudHost.JOURNEYMAP, 0L);
        assertEquals(1, snapshot(registry, EnumNotificationHudHost.JOURNEYMAP, 0L).size());
    }

    @Test
    public void standaloneAndNullPreferenceProduceNoMapRows() throws Exception {
        Object registry = registry();
        call(registry, "register", line("standalone", 1, EnumNotificationHudHost.STANDALONE,
                () -> Component.literal("a")));
        call(registry, "register", line("null", 2, null, () -> Component.literal("b")));
        call(registry, "seen", XAERO, 0L);
        assertTrue(snapshot(registry, XAERO, 0L).isEmpty());
        assertTrue(snapshot(registry, EnumNotificationHudHost.STANDALONE, 0L).isEmpty());
    }

    @Test
    public void observersReceiveExistingAndLateRegistrationsOnce() throws Exception {
        Object registry = registry();
        call(registry, "register", line("a", 1, "a"));
        List<Object> observed = new ArrayList<>();
        call(registry, "onRegistration", (Consumer<Object>) observed::add);
        call(registry, "register", line("b", 2, "b"));
        call(registry, "register", line("a", 1, "duplicate"));
        assertEquals(Arrays.asList("fixture:a", "fixture:b"), ids(observed));
    }

    @Test
    public void brokenObserverDoesNotBlockOtherObserversOrRows() throws Exception {
        Object registry = registry();
        call(registry, "onRegistration", (Consumer<Object>) row -> { throw new IllegalStateException("fixture"); });
        List<Object> observed = new ArrayList<>();
        call(registry, "onRegistration", (Consumer<Object>) observed::add);
        call(registry, "register", line("a", 1, "a"));
        assertEquals(Arrays.asList("fixture:a"), ids(observed));
        call(registry, "seen", XAERO, 0L);
        assertEquals(1, snapshot(registry, XAERO, 0L).size());
    }

    @Test
    public void brokenAndNullTextSuppliersDoNotBlockHealthyRows() throws Exception {
        Object registry = registry();
        call(registry, "register", line("broken", 1, XAERO,
                () -> { throw new IllegalStateException("fixture"); }));
        call(registry, "register", line("null", 2, XAERO, () -> null));
        call(registry, "register", line("healthy", 3, "ok"));
        call(registry, "seen", XAERO, 0L);
        assertEquals(Arrays.asList("fixture:healthy"), ids(snapshot(registry, XAERO, 0L)));
    }

    @Test
    public void textChangesDoNotRebuildLayoutButVisibilityChangesDo() throws Exception {
        Object registry = registry();
        AtomicReference<String> text = new AtomicReference<>("00:10");
        call(registry, "register", line("a", 1, XAERO, () -> Component.literal(text.get())));
        call(registry, "seen", XAERO, 0L);
        List<Object> before = snapshot(registry, XAERO, 0L);
        long revision = ((Number) call(registry, "layoutRevision")).longValue();
        text.set("00:09");
        assertEquals("00:09", ((Component) call(snapshot(registry, XAERO, 1L).get(0), "text")).getString());
        assertEquals(revision, ((Number) call(registry, "layoutRevision")).longValue());
        assertEquals("00:10", ((Component) call(before.get(0), "text")).getString());
        text.set("");
        assertTrue(snapshot(registry, XAERO, 2L).isEmpty());
        assertTrue(((Number) call(registry, "layoutRevision")).longValue() > revision);
    }

    @Test
    public void requestedIdsDetectVisibilityWithoutClaimingHiddenMap() throws Exception {
        Object registry = registry();
        AtomicReference<String> text = new AtomicReference<>("00:10");
        call(registry, "register", line("bad", 0, XAERO, () -> { throw new IllegalStateException("fixture"); }));
        call(registry, "register", line("a", 1, XAERO, () -> Component.literal(text.get())));
        call(registry, "register", line("elsewhere", 2, EnumNotificationHudHost.JOURNEYMAP,
                () -> Component.literal("elsewhere")));
        assertEquals(Arrays.asList(new ResourceLocation("fixture", "a")), call(registry, "requestedIds", XAERO));
        assertTrue(snapshot(registry, XAERO, 0L).isEmpty());
        text.set("");
        assertEquals(new ArrayList<>(), call(registry, "requestedIds", XAERO));
    }

    @Test
    public void snapshotFreezesMutableTextAndReturnsDefensiveCopies() throws Exception {
        Object registry = registry();
        net.minecraft.network.chat.MutableComponent mutable = Component.literal("first");
        call(registry, "register", line("a", 1, XAERO, () -> mutable));
        call(registry, "seen", XAERO, 0L);
        Object row = snapshot(registry, XAERO, 0L).get(0);
        mutable.getSiblings().add(Component.literal(" changed"));
        assertEquals("first", ((Component) call(row, "text")).getString());
        ((Component) call(row, "text")).getSiblings().add(Component.literal(" mutation"));
        assertEquals("first", ((Component) call(row, "text")).getString());
    }

    @Test
    public void snapshotsAndRegistrationsCannotBeMutated() throws Exception {
        Object registry = registry();
        call(registry, "register", line("a", 1, "a"));
        call(registry, "seen", XAERO, 0L);
        try {
            snapshot(registry, XAERO, 0L).clear();
            fail("snapshot was mutable");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, snapshot(registry, XAERO, 0L).size());
        }
        try {
            ((List<?>) call(registry, "registrations")).clear();
            fail("registrations were mutable");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, ((List<?>) call(registry, "registrations")).size());
        }
    }

    @Test
    public void worldExitClearsHostsButKeepsRegistrations() throws Exception {
        Object registry = registry();
        call(registry, "register", line("a", 1, "a"));
        call(registry, "seen", XAERO, 0L);
        assertEquals(1, snapshot(registry, XAERO, 0L).size());
        call(registry, "resetHosts");
        assertTrue(snapshot(registry, XAERO, 0L).isEmpty());
        assertEquals(1, ((List<?>) call(registry, "registrations")).size());
        call(registry, "seen", XAERO, 1L);
        assertEquals(1, snapshot(registry, XAERO, 1L).size());
    }

    @Test
    public void supplierMayRegisterWithoutChangingCurrentSnapshot() throws Exception {
        Object registry = registry();
        Object late = line("late", 2, "late");
        call(registry, "register", line("first", 1, XAERO, () -> {
            try {
                call(registry, "register", late);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return Component.literal("first");
        }));
        call(registry, "seen", XAERO, 0L);
        assertEquals(Arrays.asList("fixture:first"), ids(snapshot(registry, XAERO, 0L)));
        assertEquals(Arrays.asList("fixture:first", "fixture:late"), ids(snapshot(registry, XAERO, 1L)));
    }

    @Test
    public void negativeNanoTimesAndNormalWrapDoNotHideLiveHost() throws Exception {
        Object registry = registry();
        call(registry, "register", line("a", 1, "a"));
        call(registry, "seen", XAERO, Long.MAX_VALUE - 100L);
        assertEquals(1, snapshot(registry, XAERO, Long.MIN_VALUE + 100L).size());
        call(registry, "seen", XAERO, -100L);
        assertEquals(1, snapshot(registry, XAERO, 100L).size());
        assertTrue(snapshot(registry, XAERO, -101L).isEmpty());
    }

    @Test
    public void sharedRegistryHasNoMapOrMinecraftClientClassDependency() throws Exception {
        for (String name : Arrays.asList(REGISTRY, LINE,
                "xin.vanilla.banira.api.client.hud.BaniraMinimapLines",
                "xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot")) {
            type(name);
            try (InputStream input = getClass().getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
                assertNotNull(input);
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, 0);
                for (MethodNode method : node.methods) for (AbstractInsnNode instruction : method.instructions) {
                    String owner = instruction instanceof MethodInsnNode ? ((MethodInsnNode) instruction).owner
                            : instruction instanceof TypeInsnNode ? ((TypeInsnNode) instruction).desc : "";
                    assertFalse(owner, owner.startsWith("xaero/") || owner.startsWith("journeymap/")
                            || owner.startsWith("dev/ftb/") || owner.startsWith("net/minecraft/client/"));
                }
            }
        }
    }
}
