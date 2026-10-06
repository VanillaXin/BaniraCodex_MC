package xin.vanilla.banira.api.script;

import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ScriptSessionTest {
    public interface Rule {
        boolean test(int value);
    }

    public interface Cost {
        double calculate(double distance);
    }

    public static final AtomicInteger CONSTRUCTIONS = new AtomicInteger();

    private ScriptSession<Rule> session(Executor owner) {
        return BaniraScripts.open("test", Rule.class, "1", ScriptLimits.defaults(), owner);
    }

    private ScriptSource source(String id, String body) {
        return new ScriptSource(id, "xin.vanilla.banira.generated.Rule" + id, id + ".java",
                "package xin.vanilla.banira.generated;\npublic class Rule" + id + " implements "
                        + Rule.class.getCanonicalName() + " {\npublic boolean test(int value) { " + body + " }\n}");
    }

    private PreparedScripts<Rule> prepare(ScriptSession<Rule> session, ScriptSource source) throws Exception {
        return session.prepare(Collections.singletonList(source)).get(10, TimeUnit.SECONDS);
    }

    @Test
    public void compilesBooleanAndNumericContractsWithoutPublishingAutomatically() throws Exception {
        try (ScriptSession<Rule> rules = session(Runnable::run)) {
            PreparedScripts<Rule> candidate = prepare(rules, source("A", "return value >= 5;"));
            assertTrue(rules.active().isEmpty());
            assertTrue(rules.publish(candidate));
            assertTrue(rules.active().get("A").test(5));
            assertFalse(rules.active().get("A").test(4));
            try {
                rules.active().clear();
                fail("Mutable active map");
            } catch (UnsupportedOperationException expected) {
            }
        }
        try (ScriptSession<Cost> costs = BaniraScripts.open("cost", Cost.class, "1", ScriptLimits.defaults(), Runnable::run)) {
            ScriptSource source = new ScriptSource("cost", "xin.vanilla.banira.generated.Cost", "cost.java",
                    "package xin.vanilla.banira.generated; public class Cost implements "
                            + Cost.class.getCanonicalName() + " { public double calculate(double distance) { return distance * .25; }}");
            assertTrue(costs.publish(costs.prepare(Collections.singletonList(source)).get(10, TimeUnit.SECONDS)));
            assertEquals(2.5, costs.active().get("cost").calculate(10), 0);
        }
    }

    @Test
    public void reportsSourceLocationAndKeepsActiveOnCompileError() throws Exception {
        try (ScriptSession<Rule> session = session(Runnable::run)) {
            assertTrue(session.publish(prepare(session, source("A", "return true;"))));
            Rule old = session.active().get("A");
            try {
                prepare(session, source("A", "return \"05\" == 5;"));
                fail("Illegal comparison compiled");
            } catch (ExecutionException expected) {
                ScriptCompilationException error = (ScriptCompilationException) expected.getCause();
                ScriptDiagnostic diagnostic = error.getDiagnostics().get(0);
                assertEquals("A", diagnostic.getScriptId());
                assertEquals("A.java", diagnostic.getFileName());
                assertEquals("compile", diagnostic.getPhase());
                assertTrue(diagnostic.getLine() >= 3);
                assertTrue(diagnostic.getColumn() > 0);
            }
            assertSame(old, session.active().get("A"));
        }
    }

    @Test
    public void sameSnapshotReusesInstancesButSessionsNeverShareThem() throws Exception {
        try (ScriptSession<Rule> first = session(Runnable::run); ScriptSession<Rule> second = session(Runnable::run)) {
            ScriptSource source = source("A", "return value / 2 == 2;");
            first.publish(prepare(first, source));
            Rule existing = first.active().get("A");
            first.publish(prepare(first, source));
            assertSame(existing, first.active().get("A"));
            second.publish(prepare(second, source));
            assertNotSame(existing, second.active().get("A"));
            assertNotSame(existing.getClass().getClassLoader(), second.active().get("A").getClass().getClassLoader());
        }
    }

    @Test
    public void rejectsStaleAndForeignCandidatesAndClosesActive() throws Exception {
        try (ScriptSession<Rule> session = session(Runnable::run); ScriptSession<Rule> other = session(Runnable::run)) {
            PreparedScripts<Rule> first = prepare(session, source("A", "return true;"));
            PreparedScripts<Rule> second = prepare(session, source("A", "return false;"));
            assertFalse(session.publish(first));
            assertFalse(other.publish(second));
            assertTrue(session.publish(second));
            session.close();
            assertTrue(session.active().isEmpty());
            assertFalse(session.publish(second));
            assertFailure(session.prepare(Collections.singletonList(source("A", "return true;"))));
        }
    }

    @Test
    public void ownerThreadConstructsAndInputListIsSnapshotted() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        try (ScriptSession<Rule> session = session(owner::add)) {
            CONSTRUCTIONS.set(0);
            ScriptSource source = new ScriptSource("A", "xin.vanilla.banira.generated.RuleA", "A.java",
                    "package xin.vanilla.banira.generated; public class RuleA implements " + Rule.class.getCanonicalName()
                            + " { public RuleA() { " + ScriptSessionTest.class.getName()
                            + ".CONSTRUCTIONS.incrementAndGet(); } public boolean test(int value) { return true; }}");
            List<ScriptSource> mutable = new ArrayList<>(Collections.singletonList(source));
            CompletableFuture<PreparedScripts<Rule>> result = session.prepare(mutable);
            mutable.clear();
            Runnable completion = owner.poll(10, TimeUnit.SECONDS);
            assertNotNull(completion);
            assertFalse(result.isDone());
            assertEquals(0, CONSTRUCTIONS.get());
            completion.run();
            assertTrue(session.publish(result.get(1, TimeUnit.SECONDS)));
            assertEquals(1, CONSTRUCTIONS.get());
        }
    }

    @Test
    public void closeDiscardsQueuedConstructionAndCompletesFuture() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        ScriptSession<Rule> session = session(owner::add);
        CompletableFuture<PreparedScripts<Rule>> result = session.prepare(Collections.singletonList(source("A", "return true;")));
        Runnable completion = owner.poll(10, TimeUnit.SECONDS);
        assertNotNull(completion);
        session.close();
        assertFailure(result);
        completion.run();
        assertTrue(session.active().isEmpty());
    }

    @Test
    public void newerRequestInvalidatesAlreadyQueuedOwnerCallback() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        try (ScriptSession<Rule> session = session(owner::add)) {
            CompletableFuture<PreparedScripts<Rule>> first = session.prepare(Collections.singletonList(source("A", "return true;")));
            Runnable old = owner.poll(10, TimeUnit.SECONDS);
            assertNotNull(old);
            CompletableFuture<PreparedScripts<Rule>> second = session.prepare(Collections.singletonList(source("A", "return false;")));
            Runnable latest = owner.poll(10, TimeUnit.SECONDS);
            assertNotNull(latest);
            latest.run();
            old.run();
            assertFailure(first);
            assertTrue(session.publish(second.get(1, TimeUnit.SECONDS)));
            assertFalse(session.active().get("A").test(0));
        }
    }

    @Test
    public void rejectsLimitsDuplicatesAndInvalidEntrypoints() throws Exception {
        try (ScriptSession<Rule> session = BaniraScripts.open("test", Rule.class, "1",
                new ScriptLimits(400, 600, 2), Runnable::run)) {
            ScriptSource valid = source("A", "return true;");
            assertFailure(session.prepare(Arrays.asList(valid, valid)));
            assertFailure(session.prepare(Arrays.asList(valid, source("B", "return true;"), source("C", "return true;"))));
            assertFailure(session.prepare(Collections.singletonList(source("A", "return true; /*" + repeat('x', 400) + "*/"))));
            List<ScriptSource> oversized = Arrays.asList(source("A", "return true; /*" + repeat('x', 150) + "*/"),
                    source("B", "return true; /*" + repeat('x', 150) + "*/"));
            assertTrue(oversized.stream().mapToInt(s -> s.getSource().length()).sum() > 600);
            assertFailure(session.prepare(oversized));
            assertFailure(session.prepare(Collections.singletonList(new ScriptSource("x", "other.Wrong", "x.java",
                    "package other; public class Wrong {}"))));
        }
    }

    @Test
    public void reportsWrongContractAndConstructorFailure() throws Exception {
        try (ScriptSession<Rule> session = session(Runnable::run)) {
            for (String code : Arrays.asList(
                    "package xin.vanilla.banira.generated; public class Wrong {}",
                    "package xin.vanilla.banira.generated; public class Wrong implements " + Rule.class.getCanonicalName()
                            + " { public Wrong() { throw new IllegalStateException(\"broken\"); } public boolean test(int n) {return true;} }")) {
                try {
                    session.prepare(Collections.singletonList(new ScriptSource("bad", "xin.vanilla.banira.generated.Wrong",
                            "bad.java", code))).get(10, TimeUnit.SECONDS);
                    fail("Accepted invalid entrypoint");
                } catch (ExecutionException expected) {
                    assertEquals("instantiate", ((ScriptCompilationException) expected.getCause()).getDiagnostics().get(0).getPhase());
                }
            }
        }
    }

    @Test
    public void rejectedOwnerExecutorCompletesFuture() throws Exception {
        try (ScriptSession<Rule> session = session(task -> {
            throw new RejectedExecutionException("stopped");
        })) {
            assertFailure(session.prepare(Collections.singletonList(source("A", "return true;"))));
        }
    }

    @Test
    public void supersededFutureCallbacksRunOutsideSessionLock() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        try (ScriptSession<Rule> session = session(owner::add)) {
            CompletableFuture<PreparedScripts<Rule>> first = session.prepare(Collections.singletonList(source("A", "return true;")));
            assertNotNull(owner.poll(10, TimeUnit.SECONDS));
            AtomicReference<Exception> blocked = new AtomicReference<>();
            first.whenComplete((value, error) -> {
                try {
                    CompletableFuture.runAsync(session::close).get(1, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    blocked.set(failure);
                }
            });
            session.prepare(Collections.singletonList(source("B", "return false;")));
            assertNull("Future callback was invoked with session lock held", blocked.get());
            assertTrue(session.active().isEmpty());
        }
    }

    @Test
    public void identicalPendingSnapshotKeepsItsQueuedConstructor() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        try (ScriptSession<Rule> session = session(owner::add)) {
            ScriptSource source = source("A", "return true;");
            CompletableFuture<PreparedScripts<Rule>> first = session.prepare(Collections.singletonList(source));
            Runnable completion = owner.poll(10, TimeUnit.SECONDS);
            assertNotNull(completion);
            CompletableFuture<PreparedScripts<Rule>> second = session.prepare(Collections.singletonList(source));
            assertSame(first, second);
            completion.run();
            assertTrue(session.publish(second.get(1, TimeUnit.SECONDS)));
            assertTrue(owner.isEmpty());
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sameNamedContractsFromDifferentLoadersStayIsolated() throws Exception {
        URL location = ScriptContractFixture.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader firstLoader = new URLClassLoader(new URL[]{location}, null);
             URLClassLoader secondLoader = new URLClassLoader(new URL[]{location}, null)) {
            Class firstType = firstLoader.loadClass(ScriptContractFixture.class.getName());
            Class secondType = secondLoader.loadClass(ScriptContractFixture.class.getName());
            assertNotSame(firstType, secondType);
            ScriptSource code = new ScriptSource("A", "xin.vanilla.banira.generated.Isolated", "Isolated.java",
                    "package xin.vanilla.banira.generated; public class Isolated implements " + firstType.getName()
                            + " { public boolean test(int value) { return value > 0; }}");
            try (ScriptSession first = BaniraScripts.open("same", firstType, "1", ScriptLimits.defaults(), Runnable::run);
                 ScriptSession second = BaniraScripts.open("same", secondType, "1", ScriptLimits.defaults(), Runnable::run)) {
                first.publish((PreparedScripts) first.prepare(Collections.singletonList(code)).get(10, TimeUnit.SECONDS));
                second.publish((PreparedScripts) second.prepare(Collections.singletonList(code)).get(10, TimeUnit.SECONDS));
                Object a = first.active().get("A");
                Object b = second.active().get("A");
                assertTrue(firstType.isInstance(a));
                assertFalse(firstType.isInstance(b));
                assertTrue(secondType.isInstance(b));
                assertFalse(secondType.isInstance(a));
                assertEquals(true, firstType.getMethod("test", int.class).invoke(a, 1));
            }
        }
    }

    @Test
    public void defaultLimitsAndUtf8SizeAreEnforced() throws Exception {
        ScriptLimits limits = ScriptLimits.defaults();
        assertEquals(256 * 1024, limits.getSourceBytes());
        assertEquals(8 * 1024 * 1024, limits.getBatchBytes());
        assertEquals(128, limits.getScriptCount());
        try (ScriptSession<Rule> session = session(Runnable::run)) {
            ScriptSource oversized = source("A", "return true; /*" + repeat('\u4e00', 100000) + "*/");
            assertTrue(oversized.getSource().length() < limits.getSourceBytes());
            assertFailure(session.prepare(Collections.singletonList(oversized)));
            PreparedScripts<Rule> stale = prepare(session, source("A", "return true;"));
            assertFailure(session.prepare(null));
            assertFalse(session.publish(stale));
        }
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static void assertFailure(CompletableFuture<?> future) throws Exception {
        try {
            future.get(10, TimeUnit.SECONDS);
            fail("Expected failure");
        } catch (ExecutionException | CancellationException expected) {
        }
    }
}
