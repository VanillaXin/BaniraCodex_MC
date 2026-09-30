package xin.vanilla.banira.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLEnvironment;
import xin.vanilla.banira.api.script.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
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
            finish(client, "FAIL " + error + "\n");
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
