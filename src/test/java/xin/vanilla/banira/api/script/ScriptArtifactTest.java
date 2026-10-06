package xin.vanilla.banira.api.script;

import org.junit.Assume;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntPredicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

/**
 * Runs the shipped compiler without Minecraft or the development dependency classpath.
 */
public class ScriptArtifactTest {
    @Test
    public void shippedJarCompilesAloneAndBesideAnOlderCompiler() throws Exception {
        String artifact = System.getProperty("banira.scriptArtifact");
        Assume.assumeNotNull(artifact);
        File jar = new File(artifact);
        Set<String> names = new HashSet<>();
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                assertTrue("Duplicate " + name, names.add(name));
                assertFalse("Unrelocated dependency " + name, name.startsWith("org/codehaus/"));
                assertFalse("Unexpected dependency " + name, name.startsWith("net/minecraft/") || name.startsWith("mezz/jei/"));
                if (name.endsWith(".class")) {
                    try (DataInputStream input = new DataInputStream(zip.getInputStream(zip.getEntry(name)))) {
                        assertEquals(0xCAFEBABE, input.readInt());
                        input.readUnsignedShort();
                        int major = input.readUnsignedShort();
                        int ceiling = name.startsWith("xin/vanilla/banira/internal/shaded/") ? 52
                                : (int) Double.parseDouble(System.getProperty("java.class.version"));
                        assertTrue("Unsupported Java version " + major + ": " + name, major >= 45 && major <= ceiling);
                    }
                }
            }
        }
        assertTrue(names.contains("META-INF/licenses/janino-LICENSE.txt"));
        assertTrue(names.contains("META-INF/services/xin.vanilla.banira.internal.shaded.commons.compiler.ICompilerFactory"));
        exercise(new URL[]{jar.toURI().toURL()});
        List<URL> collision = new ArrayList<>();
        collision.add(jar.toURI().toURL());
        for (String file : System.getProperty("banira.scriptCollision").split(File.pathSeparator)) {
            collision.add(new File(file).toURI().toURL());
        }
        exercise(collision.toArray(new URL[0]));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void exercise(URL[] files) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(files, null)) {
            Class<?> api = loader.loadClass("xin.vanilla.banira.api.script.BaniraScripts");
            Class<?> limits = loader.loadClass("xin.vanilla.banira.api.script.ScriptLimits");
            Class<?> source = loader.loadClass("xin.vanilla.banira.api.script.ScriptSource");
            Class<?> sessionType = loader.loadClass("xin.vanilla.banira.api.script.ScriptSession");
            Class<?> candidateType = loader.loadClass("xin.vanilla.banira.api.script.PreparedScripts");
            Object session = api.getMethod("open", String.class, Class.class, String.class, limits, Executor.class)
                    .invoke(null, "artifact", IntPredicate.class, "1", limits.getMethod("defaults").invoke(null), (Executor) Runnable::run);
            try {
                Object code = source.getConstructor(String.class, String.class, String.class, String.class).newInstance(
                        "test", "xin.vanilla.banira.generated.ArtifactRule", "ArtifactRule.java",
                        "package xin.vanilla.banira.generated; public class ArtifactRule implements java.util.function.IntPredicate {"
                                + " public boolean test(int value) { return value / 2 == 2; }}");
                CompletableFuture<?> future = (CompletableFuture<?>) sessionType.getMethod("prepare", List.class)
                        .invoke(session, Collections.singletonList(code));
                assertEquals(true, sessionType.getMethod("publish", candidateType).invoke(session, future.get(10, TimeUnit.SECONDS)));
                Map<String, IntPredicate> active = (Map<String, IntPredicate>) sessionType.getMethod("active").invoke(session);
                assertTrue(active.get("test").test(5));
                assertFalse(active.get("test").test(6));
                Class factory = loader.loadClass("xin.vanilla.banira.internal.shaded.commons.compiler.ICompilerFactory");
                Object provider = ServiceLoader.load(factory, loader).iterator().next();
                assertTrue(provider.getClass().getName().startsWith("xin.vanilla.banira.internal.shaded."));
            } finally {
                ((AutoCloseable) session).close();
            }
            Class<?> groupType = loader.loadClass("xin.vanilla.banira.api.script.ScriptSourceGroup");
            Class<?> factoryType = loader.loadClass("xin.vanilla.banira.api.script.ScriptFactory");
            BlockingQueue<Runnable> owner = new LinkedBlockingQueue<>();
            Object factorySession = api.getMethod("openFactorySession", String.class, Class.class, String.class, limits, Executor.class)
                    .invoke(null, "artifact-factory", IntPredicate.class, "1", limits.getMethod("defaults").invoke(null), (Executor) owner::add);
            try {
                Map<String, String> sources = new LinkedHashMap<>();
                sources.put("Task.java", "package xin.vanilla.banira.generated; public class Task implements java.util.function.IntPredicate {"
                        + " public boolean test(int value){return Helper.accept(this, value);} public int limit(){return 5;} }");
                sources.put("Helper.java", "package xin.vanilla.banira.generated; class Helper {"
                        + " static boolean accept(Task task, int value){return value >= task.limit();} }");
                Object group = groupType.getConstructor(String.class, String.class, Map.class).newInstance(
                        "test", "xin.vanilla.banira.generated.Task", sources);
                CompletableFuture<?> prepared = (CompletableFuture<?>) sessionType.getMethod("prepareGroups", List.class)
                        .invoke(factorySession, Collections.singletonList(group));
                Runnable completion = owner.poll(10, TimeUnit.SECONDS);
                assertNotNull(completion);
                completion.run();
                Object candidate = prepared.get(1, TimeUnit.SECONDS);
                assertEquals(true, sessionType.getMethod("publish", candidateType).invoke(factorySession, candidate));
                Object factory = ((Map<?, ?>) sessionType.getMethod("active").invoke(factorySession)).get("test");
                IntPredicate first = (IntPredicate) factoryType.getMethod("create").invoke(factory);
                IntPredicate second = (IntPredicate) factoryType.getMethod("create").invoke(factory);
                assertNotSame(first, second);
                assertTrue(first.test(5));
                assertFalse(second.test(4));
            } finally {
                ((AutoCloseable) factorySession).close();
            }
        }
    }
}
