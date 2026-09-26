package xin.vanilla.banira.internal.neoforge.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.internal.config.*;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Dev-only checks through NeoForge file reloads and retained generated views. */
public final class NeoForgeConfigViewSmoke {
    private static final Gson JSON = new Gson();
    private static IntSupplier read;
    private static IntConsumer write;
    private static Object root;
    private static int cycles;
    private static boolean finished;
    private static boolean waiting;
    private static long started;
    private static Runnable unsubscribe;
    private static final java.util.concurrent.atomic.AtomicInteger reloads = new java.util.concurrent.atomic.AtomicInteger();

    private NeoForgeConfigViewSmoke() { }

    public static boolean step(boolean client) throws Exception {
        if (!BaniraNetworkSmokeStatus.enabled()) throw new IllegalStateException("Smoke disabled");
        if (finished) return true;
        if (started == 0) started = System.nanoTime();
        if (System.nanoTime() - started > java.util.concurrent.TimeUnit.SECONDS.toNanos(60)) {
            throw new IllegalStateException("NeoForge config reload timed out: cycles=" + cycles);
        }
        Class<?> type = client ? ClientConfig.class : CommonConfig.class;
        String key = client ? "notificationLogMaxEntries" : "help.helpInfoNumPerPage";
        ConfigHolder holder = NeoForgeConfigAdapter.getHolder(type);
        if (holder == null) throw new IllegalStateException("Missing config holder");
        if (read == null) {
            if (client) {
                ClientConfigView retained = ClientConfigView.get();
                root = retained;
                read = retained::notificationLogMaxEntries;
                write = retained::notificationLogMaxEntries;
            } else {
                CommonConfigView retained = CommonConfigView.get();
                root = retained;
                CommonConfigView.HelpView help = retained.help();
                read = help::helpInfoNumPerPage;
                write = help::helpInfoNumPerPage;
            }
        }
        Path file = BaniraPlatforms.get().configDir().resolve(holder.getConfigName() + ".toml");
        Path checkpoint = file.resolveSibling(holder.getConfigName() + ".generated-smoke.json");
        if ("phase-two".equals(BaniraNetworkSmokeStatus.phase())) {
            JsonObject expected = JSON.fromJson(new String(Files.readAllBytes(checkpoint), StandardCharsets.UTF_8), JsonObject.class);
            if (!expected.equals(snapshot(holder)) || read.getAsInt() != 61) {
                throw new IllegalStateException("Generated config changed after restart");
            }
            verifyReads(holder, root, "");
            BaniraNetworkSmokeStatus.append("PASS generated-config-view-restart values=" + expected.size());
            finished = true;
            return true;
        }
        if (!"phase-one".equals(BaniraNetworkSmokeStatus.phase())) throw new IllegalStateException("Unknown phase");
        if (cycles < 20) {
            if (unsubscribe == null) unsubscribe = holder.onReloaded(paths -> reloads.incrementAndGet());
            if (!waiting) {
                com.electronwill.nightconfig.core.CommentedConfig external = parse(file);
                external.set(key, 30 + cycles);
                Files.write(file, com.electronwill.nightconfig.toml.TomlFormat.instance().createWriter()
                        .writeToString(external).getBytes(StandardCharsets.UTF_8));
                waiting = true;
                return false;
            }
            if (read.getAsInt() != 30 + cycles || reloads.get() <= cycles) return false;
            verifyReads(holder, root, "");
            cycles++;
            waiting = false;
            return false;
        }
        write.accept(61);
        holder.save();
        if (read.getAsInt() != 61) throw new IllegalStateException("Retained write failed");
        com.electronwill.nightconfig.core.CommentedConfig disk = parse(file);
        for (String path : holder.valuePaths()) {
            if (!JSON.toJsonTree(holder.get(path)).equals(JSON.toJsonTree(normalizeDisk(holder.get(path), disk.get(path))))) {
                throw new IllegalStateException("TOML mismatch " + path);
            }
        }
        Files.write(checkpoint, JSON.toJson(snapshot(holder)).getBytes(StandardCharsets.UTF_8));
        BaniraNetworkSmokeStatus.append("PASS generated-config-view-reload cycles=20 values=" + holder.getDescriptors().size());
        unsubscribe.run();
        finished = true;
        return true;
    }

    private static Object normalizeDisk(Object expected, Object actual) {
        return expected instanceof Enum<?> && actual instanceof String
                ? ((Enum<?>) expected).name().equals(actual) ? expected : actual : actual;
    }

    private static com.electronwill.nightconfig.core.CommentedConfig parse(Path path) throws Exception {
        return new com.electronwill.nightconfig.toml.TomlParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    private static JsonObject snapshot(ConfigHolder holder) {
        JsonObject result = new JsonObject();
        for (String path : holder.valuePaths()) result.add(path, JSON.toJsonTree(holder.get(path)));
        return result;
    }

    private static void verifyReads(ConfigHolder holder, Object view, String prefix) throws Exception {
        for (Method method : view.getClass().getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 0 || method.getName().equals("handle")) continue;
            Object value = method.invoke(view);
            String path = prefix + method.getName();
            if (method.getReturnType().getEnclosingClass() == view.getClass()) {
                verifyReads(holder, value, path + ".");
            } else if (!holder.hasValue(path) || !JSON.toJsonTree(holder.get(path)).equals(JSON.toJsonTree(value))) {
                throw new IllegalStateException("Generated read mismatch " + path);
            }
        }
    }
}
