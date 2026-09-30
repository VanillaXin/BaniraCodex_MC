package xin.vanilla.banira.api.script;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ScriptFactorySessionTest {
    public interface Counter { int next(); }
    public static final AtomicInteger INITIALIZATIONS = new AtomicInteger();
    public static final AtomicInteger CONSTRUCTIONS = new AtomicInteger();
    private static final String NS = "xin.vanilla.banira.generated.";

    private ScriptSession<ScriptFactory<Counter>> session(BlockingQueue<Runnable> owner) {
        return BaniraScripts.openFactorySession("factory", Counter.class, "1", ScriptLimits.defaults(), owner::add);
    }

    private ScriptSourceGroup group(String id, String className, String extra) {
        return new ScriptSourceGroup(id, NS + className, Collections.singletonMap(className + ".java",
                "package xin.vanilla.banira.generated; public class " + className + " implements "
                        + Counter.class.getCanonicalName() + " { private int value; public int next(){ return ++value; } " + extra + "}"));
    }

    private <T> PreparedScripts<T> prepare(ScriptSession<T> session, BlockingQueue<Runnable> owner,
                                          ScriptSourceGroup... groups) throws Exception {
        CompletableFuture<PreparedScripts<T>> future = session.prepareGroups(Arrays.asList(groups));
        Runnable callback = owner.poll(10, TimeUnit.SECONDS);
        if (callback != null) callback.run();
        return future.get(10, TimeUnit.SECONDS);
    }

    @Test public void createsIndependentInstancesWithoutInitializingDuringPreparation() throws Exception {
        INITIALIZATIONS.set(0);
        CONSTRUCTIONS.set(0);
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        try (ScriptSession<ScriptFactory<Counter>> session = session(owner)) {
            PreparedScripts<ScriptFactory<Counter>> prepared = prepare(session, owner, group("a", "Task",
                    "static { " + getClass().getName() + ".INITIALIZATIONS.incrementAndGet(); } public Task(){ "
                            + getClass().getName() + ".CONSTRUCTIONS.incrementAndGet(); }"));
            assertEquals(0, INITIALIZATIONS.get());
            assertEquals(0, CONSTRUCTIONS.get());
            assertTrue(session.publish(prepared));
            ScriptFactory<Counter> factory = session.active().get("a");
            assertEquals(NS + "Task", factory.entryType().getName());
            assertTrue(Counter.class.isAssignableFrom(factory.entryType()));
            assertEquals(0, INITIALIZATIONS.get());
            assertEquals(0, CONSTRUCTIONS.get());
            Counter a = factory.create();
            Counter b = factory.create();
            assertNotSame(a, b);
            assertEquals(1, a.next());
            assertEquals(2, a.next());
            assertEquals(1, b.next());
            assertEquals(1, INITIALIZATIONS.get());
            assertEquals(2, CONSTRUCTIONS.get());
        }
    }

    @Test public void compilesMutuallyReferencingSourcesAndReusesReorderedSnapshot() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        Map<String, String> files = new LinkedHashMap<>();
        files.put("Task.java", "package xin.vanilla.banira.generated; public class Task implements "
                + Counter.class.getCanonicalName() + " { public int next(){ return Helper.value(this); } public int base(){ return 42; }}");
        files.put("Helper.java", "package xin.vanilla.banira.generated; final class Helper { static int value(Task task){return task.base();}}");
        ScriptSourceGroup group = new ScriptSourceGroup("a", NS + "Task", files);
        files.clear();
        try (ScriptSession<ScriptFactory<Counter>> session = session(owner)) {
            PreparedScripts<ScriptFactory<Counter>> first = prepare(session, owner, group);
            ScriptFactory<Counter> factory = first.scripts().get("a");
            assertEquals(42, factory.create().next());
            assertTrue(session.publish(first));
            Map<String, String> reversed = new LinkedHashMap<>();
            reversed.put("Helper.java", group.getSourceFiles().get("Helper.java"));
            reversed.put("Task.java", group.getSourceFiles().get("Task.java"));
            PreparedScripts<ScriptFactory<Counter>> same = session.prepareGroups(Collections.singletonList(
                    new ScriptSourceGroup("a", NS + "Task", reversed))).get(1, TimeUnit.SECONDS);
            assertSame(factory, same.scripts().get("a"));
            assertTrue(owner.isEmpty());
            reversed.put("Helper.java", "package xin.vanilla.banira.generated; final class Helper { static int value(Task task){return 7;}}");
            PreparedScripts<ScriptFactory<Counter>> changed = prepare(session, owner, new ScriptSourceGroup("a", NS + "Task", reversed));
            assertNotSame(factory, changed.scripts().get("a"));
            assertEquals(7, changed.scripts().get("a").create().next());
        }
    }

    @Test public void rejectsOtherThreadsAndClosedFactories() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        ScriptSession<ScriptFactory<Counter>> session = session(owner);
        try {
            ScriptFactory<Counter> factory = prepare(session, owner, group("a", "Task", "")).scripts().get("a");
            assertFailure(CompletableFuture.supplyAsync(factory::create));
            assertFailure(CompletableFuture.supplyAsync(factory::entryType));
            assertEquals(1, factory.create().next());
            session.close();
            try { factory.create(); fail("Closed factory accepted"); }
            catch (IllegalStateException expected) { }
            try { factory.entryType(); fail("Closed factory type accepted"); }
            catch (IllegalStateException expected) { }
        } finally { session.close(); }
    }

    @Test public void rejectsDuplicateBinaryNamesAndHelperDiagnosticsKeepFileLocation() throws Exception {
        try (ScriptSession<ScriptFactory<Counter>> session = BaniraScripts.openFactorySession("duplicates", Counter.class,
                "1", ScriptLimits.defaults(), Runnable::run)) {
            Map<String, String> first = new HashMap<>(group("a", "TaskA", "").getSourceFiles());
            Map<String, String> second = new HashMap<>(group("b", "TaskB", "").getSourceFiles());
            String helper = "package xin.vanilla.banira.generated; class Shared {}";
            first.put("Shared.java", helper);
            second.put("Shared.java", helper);
            assertFailure(session.prepareGroups(Arrays.asList(new ScriptSourceGroup("a", NS + "TaskA", first),
                    new ScriptSourceGroup("b", NS + "TaskB", second))));
            first.put("Duplicate.java", helper);
            assertFailure(session.prepareGroups(Collections.singletonList(new ScriptSourceGroup("a", NS + "TaskA", first))));
            first.remove("Duplicate.java");
            first.put("Shared.java", "package xin.vanilla.banira.generated;\nclass Shared { int bad(){ return \"bad\"; }}");
            Throwable error = assertFailure(session.prepareGroups(Collections.singletonList(new ScriptSourceGroup("a", NS + "TaskA", first))));
            ScriptDiagnostic diagnostic = ((ScriptCompilationException) error).getDiagnostics().get(0);
            assertEquals("a", diagnostic.getScriptId());
            assertEquals("Shared.java", diagnostic.getFileName());
            assertEquals(2, diagnostic.getLine());
        }
    }

    @Test public void enforcesHelperFileCountAndUtf8ByteLimits() throws Exception {
        try (ScriptSession<ScriptFactory<Counter>> session = BaniraScripts.openFactorySession("limits", Counter.class, "1",
                new ScriptLimits(400, 600, 1), Runnable::run)) {
            Map<String, String> files = new HashMap<>(group("a", "Task", "").getSourceFiles());
            files.put("Helper.java", "package xin.vanilla.banira.generated; class Helper {}");
            assertFailure(session.prepareGroups(Collections.singletonList(new ScriptSourceGroup("a", NS + "Task", files))));
            files.remove("Helper.java");
            char[] chars = new char[150];
            Arrays.fill(chars, '\u4e00');
            files.put("Task.java", "/*" + new String(chars) + "*/");
            assertFailure(session.prepareGroups(Collections.singletonList(new ScriptSourceGroup("a", NS + "Task", files))));
        }
    }

    @Test public void closesQueuedPreparationAndRejectsForeignAndStaleCandidates() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        try (ScriptSession<ScriptFactory<Counter>> session = session(owner);
             ScriptSession<ScriptFactory<Counter>> other = session(owner)) {
            PreparedScripts<ScriptFactory<Counter>> first = prepare(session, owner, group("a", "Task", ""));
            PreparedScripts<ScriptFactory<Counter>> second = prepare(session, owner, group("b", "Other", ""));
            assertFalse(session.publish(first));
            assertFalse(other.publish(second));
            assertTrue(session.publish(second));
            CompletableFuture<?> queued = session.prepareGroups(Collections.singletonList(group("c", "Queued", "")));
            Runnable callback = owner.poll(10, TimeUnit.SECONDS);
            assertNotNull(callback);
            session.close();
            assertFailure(queued);
            callback.run();
            assertTrue(session.active().isEmpty());
        }
    }

    @Test public void constructorFailureIsDeferredAndDiagnosed() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        try (ScriptSession<ScriptFactory<Counter>> session = session(owner)) {
            ScriptFactory<Counter> factory = prepare(session, owner, group("broken", "Broken",
                    "public Broken(){ throw new IllegalStateException(\"broken\"); }")).scripts().get("broken");
            try { factory.create(); fail("Broken constructor accepted"); }
            catch (ScriptCompilationException expected) {
                assertEquals("broken", expected.getDiagnostics().get(0).getScriptId());
                assertEquals("instantiate", expected.getDiagnostics().get(0).getPhase());
            }
        }
    }

    @Test public void validatesFactoryEntrypointsWithoutRunningThem() throws Exception {
        try (ScriptSession<ScriptFactory<Counter>> session = BaniraScripts.openFactorySession("invalid", Counter.class,
                "1", ScriptLimits.defaults(), Runnable::run)) {
            for (String declaration : Arrays.asList(
                    "public abstract class Bad implements " + Counter.class.getCanonicalName() + " {}",
                    "public class Bad {}",
                    "public class Bad implements " + Counter.class.getCanonicalName() + " { private Bad(){} public int next(){return 1;} }")) {
                Throwable error = assertFailure(session.prepareGroups(Collections.singletonList(new ScriptSourceGroup("bad", NS + "Bad",
                        Collections.singletonMap("Bad.java", "package xin.vanilla.banira.generated; " + declaration)))));
                assertEquals("instantiate", ((ScriptCompilationException) error).getDiagnostics().get(0).getPhase());
            }
        }
    }

    @Test public void helperSourceCannotEscapeNamespaceOrBatchBudget() throws Exception {
        try (ScriptSession<ScriptFactory<Counter>> session = BaniraScripts.openFactorySession("limits", Counter.class,
                "1", new ScriptLimits(400, 600, 3), Runnable::run)) {
            Map<String, String> files = new LinkedHashMap<>(group("a", "Task", "").getSourceFiles());
            files.put("Helper.java", "package other; public class Helper {}");
            Throwable namespace = assertFailure(session.prepareGroups(Collections.singletonList(new ScriptSourceGroup("a", NS + "Task", files))));
            assertEquals("compile", ((ScriptCompilationException) namespace).getDiagnostics().get(0).getPhase());
            char[] padding = new char[280];
            Arrays.fill(padding, 'x');
            files.put("Helper.java", "package xin.vanilla.banira.generated; class Helper {} /*" + new String(padding) + "*/");
            files.put("Second.java", "package xin.vanilla.banira.generated; class Second {} /*" + new String(padding) + "*/");
            Throwable limit = assertFailure(session.prepareGroups(Collections.singletonList(new ScriptSourceGroup("a", NS + "Task", files))));
            assertEquals("validate", ((ScriptCompilationException) limit).getDiagnostics().get(0).getPhase());
        }
    }

    @Test public void pendingSnapshotAndFailedReloadDoNotReplaceHealthyFactory() throws Exception {
        BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
        try (ScriptSession<ScriptFactory<Counter>> session = session(owner)) {
            ScriptSourceGroup code = group("a", "Task", "");
            CompletableFuture<PreparedScripts<ScriptFactory<Counter>>> first = session.prepareGroups(Collections.singletonList(code));
            Runnable completion = owner.poll(10, TimeUnit.SECONDS);
            assertNotNull(completion);
            assertSame(first, session.prepareGroups(Collections.singletonList(code)));
            completion.run();
            assertTrue(session.publish(first.get(1, TimeUnit.SECONDS)));
            ScriptFactory<Counter> factory = session.active().get("a");
            assertFailure(session.prepareGroups(Collections.singletonList(group("a", "Task", "invalid;"))));
            assertSame(factory, session.active().get("a"));
            assertEquals(1, factory.create().next());
            assertFailure(session.prepare(null));
            assertFalse(session.publish(first.get()));
        }
    }

    private Throwable assertFailure(CompletableFuture<?> future) throws Exception {
        try { future.get(10, TimeUnit.SECONDS); fail("Expected failure"); return null; }
        catch (ExecutionException expected) { return expected.getCause(); }
        catch (CancellationException expected) { return expected; }
    }
}
