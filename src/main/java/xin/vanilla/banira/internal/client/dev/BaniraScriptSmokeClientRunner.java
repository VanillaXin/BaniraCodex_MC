package xin.vanilla.banira.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLEnvironment;
import xin.vanilla.banira.api.script.*;
import xin.vanilla.banira.common.config.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntPredicate;

/** Development-only, bounded compiler/owner-thread probe; no world or UI input needed. */
public final class BaniraScriptSmokeClientRunner {
    private static ScriptSession<IntPredicate> session;
    private static CompletableFuture<PreparedScripts<IntPredicate>> pending;
    private static IntPredicate active;
    private static long started;
    private static boolean finished;

    private BaniraScriptSmokeClientRunner() { }

    public static void tick() {
        if (FMLEnvironment.production || !Boolean.getBoolean("banira.scriptSmoke") || finished) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getOverlay() != null) return;
        try {
            if ("config".equals(System.getProperty("banira.scriptSmoke.mode"))) {
                verifyConfigTransaction();
                finish(client, "PASS config-transaction-native-values-guard-restore\nFINISHED config-client\n");
                return;
            }
            if (session == null) {
                started = System.nanoTime();
                session = BaniraScripts.open("banira_codex", IntPredicate.class, "1", ScriptLimits.defaults(), client::execute);
                String code = "package xin.vanilla.banira.generated; public class ClientProbe implements java.util.function.IntPredicate {"
                        + " public ClientProbe() { if (!Thread.currentThread().getName().equals(\"Render thread\"))"
                        + " throw new IllegalStateException(\"Wrong owner thread\"); }"
                        + " public boolean test(int value) { return value == 42; }}";
                pending = session.prepare(Collections.singletonList(new ScriptSource("probe",
                        "xin.vanilla.banira.generated.ClientProbe", "ClientProbe.java", code)));
            } else if (pending.isDone()) {
                if (active == null) {
                    if (!session.publish(pending.join())) throw new IllegalStateException("Publish rejected");
                    active = session.active().get("probe");
                    if (!active.test(42) || active.test(0)) throw new IllegalStateException("Incorrect result");
                    pending = session.prepare(Collections.singletonList(new ScriptSource("probe",
                            "xin.vanilla.banira.generated.ClientProbe", "ClientProbe.java",
                            "package xin.vanilla.banira.generated; public class ClientProbe { broken }")));
                } else {
                    if (!pending.isCompletedExceptionally() || session.active().get("probe") != active) {
                        throw new IllegalStateException("Failed reload replaced active rules");
                    }
                    finish(client, "PASS compile-owner-thread-failure-isolation\nFINISHED script-client\n");
                }
            } else if (System.nanoTime() - started > 30_000_000_000L) {
                throw new IllegalStateException("Script smoke timed out");
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(client, "FAIL " + error + "\n");
        }
    }

    private static void verifyConfigTransaction() throws ReflectiveOperationException {
        ConfigHolder holder = ConfigRegistry.get("banira_codex-client");
        if (holder == null) throw new IllegalStateException("Client config is not registered");
        LinkedHashSet<String> paths = new LinkedHashSet<>(Arrays.asList("notificationMergeWindowMs", "notificationBurstThreshold"));
        ConfigEditSnapshot before = holder.snapshotForEdit(paths);
        AtomicInteger saved = new AtomicInteger();
        Runnable unsubscribe = holder.onSaved(changes -> saved.incrementAndGet());
        try {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("notificationMergeWindowMs", 1947);
            changes.put("notificationBurstThreshold", 7);
            if (holder.compareAndSetAll(before, changes, ConfigEditOrigin.UI) != ConfigCommitResult.APPLIED
                    || !Integer.valueOf(1947).equals(holder.get("notificationMergeWindowMs"))
                    || !Integer.valueOf(7).equals(holder.get("notificationBurstThreshold"))
                    || saved.get() != 1) throw new IllegalStateException("Native batch did not apply together");
            ConfigEditSnapshot applied = holder.snapshotForEdit(paths);
            Runnable unguard = holder.onEdit((origin, values) -> {
                if (origin == ConfigEditOrigin.REMOTE) throw new IllegalArgumentException("Rejected by smoke guard");
            });
            try {
                changes.put("notificationBurstThreshold", 8);
                try {
                    holder.setAll(changes, ConfigEditOrigin.REMOTE);
                    throw new IllegalStateException("Rejected batch was saved");
                } catch (IllegalArgumentException expected) {
                    if (!Arrays.equals(applied.getSourceBytes(), holder.snapshotForEdit(paths).getSourceBytes())) {
                        throw new IllegalStateException("Guard changed config file");
                    }
                }
            } finally { unguard.run(); }
            verifyEditorRejection(holder, paths);
        } finally {
            try {
                ConfigEditSnapshot current = holder.snapshotForEdit(paths);
                if (holder.compareAndSetAll(current, before.getValues(), ConfigEditOrigin.LOCAL_MIGRATION) == ConfigCommitResult.CONFLICT) {
                    throw new IllegalStateException("Cannot restore client smoke config");
                }
            } finally { unsubscribe.run(); }
        }
    }

    private static void verifyEditorRejection(ConfigHolder holder, LinkedHashSet<String> paths)
            throws ReflectiveOperationException {
        ConfigEditSnapshot before = holder.snapshotForEdit(paths);
        AtomicInteger rejected = new AtomicInteger();
        Runnable unguard = holder.onEdit((origin, values) -> {
            if (origin == ConfigEditOrigin.UI) {
                rejected.incrementAndGet();
                throw new IllegalArgumentException("Rejected UI edit by smoke guard");
            }
        });
        xin.vanilla.banira.client.gui.ConfigEditorScreen screen =
                new xin.vanilla.banira.client.gui.ConfigEditorScreen(holder, null);
        Minecraft client = Minecraft.getInstance();
        screen.init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        try {
            java.lang.reflect.Method save = screen.getClass().getDeclaredMethod("saveConfig");
            save.setAccessible(true);
            save.invoke(screen);
            if (rejected.get() != 1 || !Arrays.equals(before.getSourceBytes(), holder.snapshotForEdit(paths).getSourceBytes())) {
                throw new IllegalStateException("UI rejection bypassed guard or changed disk");
            }
        } finally {
            unguard.run();
            screen.removed();
        }
    }

    private static void finish(Minecraft client, String result) {
        finished = true;
        if (session != null) session.close();
        session = null;
        pending = null;
        active = null;
        try {
            Path status = Paths.get(System.getProperty("banira.scriptSmoke.status"));
            Files.createDirectories(status.toAbsolutePath().getParent());
            Files.write(status, result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) { throw new IllegalStateException("Cannot write script smoke status", error); }
        finally { client.stop(); }
    }
}
