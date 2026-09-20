package xin.vanilla.banira.internal.fabric.config;

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

/** Dev-only proof of Fabric's file-backed re-registration and retained generated views. */
public final class FabricConfigViewSmoke {
    private static final Gson JSON = new Gson();
    private static IntSupplier read;
    private static IntConsumer write;
    private static Object root;
    private static int cycles;
    private static boolean finished;

    private FabricConfigViewSmoke() { }

    public static boolean step(boolean client) throws Exception {
        if (!BaniraNetworkSmokeStatus.enabled()) throw new IllegalStateException("Smoke disabled");
        if (finished) return true;
        Class<?> type = client ? ClientConfig.class : CommonConfig.class;
        String key = client ? "notificationLogMaxEntries" : "help.helpInfoNumPerPage";
        ConfigHolder holder = FabricConfigAdapter.getHolder(type);
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
            Object previous = holder.get(key);
            FabricConfigValueStore external = new FabricConfigValueStore(file, holder.getDescriptors());
            external.set(key, 30 + cycles);
            external.save();
            FabricConfigAdapter.register(type, "banira_codex");
            ConfigHolder replacement = FabricConfigAdapter.getHolder(type);
            if (replacement == holder || read.getAsInt() != 30 + cycles || !previous.equals(holder.get(key))) {
                throw new IllegalStateException("Retained view failed to follow replacement");
            }
            verifyReads(replacement, root, "");
            cycles++;
            return false;
        }
        write.accept(61);
        holder.save();
        if (read.getAsInt() != 61) throw new IllegalStateException("Retained write failed");
        FabricConfigValueStore disk = new FabricConfigValueStore(file, holder.getDescriptors());
        for (String path : holder.valuePaths()) {
            if (!JSON.toJsonTree(holder.get(path)).equals(JSON.toJsonTree(disk.get(path)))) {
                throw new IllegalStateException("TOML mismatch " + path);
            }
        }
        Files.write(checkpoint, JSON.toJson(snapshot(holder)).getBytes(StandardCharsets.UTF_8));
        BaniraNetworkSmokeStatus.append("PASS generated-config-view-rebind cycles=20 values=" + holder.getDescriptors().size());
        finished = true;
        return true;
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
