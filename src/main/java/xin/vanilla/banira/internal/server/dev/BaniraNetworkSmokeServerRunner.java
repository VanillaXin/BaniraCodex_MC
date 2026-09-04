package xin.vanilla.banira.internal.server.dev;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import xin.vanilla.banira.api.event.BaniraEvents;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeProfilePlan;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 真实专服内验证 Banira 事件、配置热重载、玩家数据、文件操作与 Spark 归档。 */
public final class BaniraNetworkSmokeServerRunner {
    private static final long SPARK_SAMPLE_SECONDS = 3L;
    private static int eventTicks;
    private static boolean ready;
    private static boolean finished;
    private static boolean configReloadRequested;
    private static boolean eventVerified;
    private static boolean sustainedWorkloadStarted;
    private static boolean sparkReportWritten;
    private static int shutdownTicks;
    private static int sustainedCycles;
    private static int lastSustainedWorkloadTick;
    private static int pendingHelpNumPerPage = -1;
    private static int completedConfigReloads;
    private static long sustainedWorkloadTotalNanos;
    private static long sustainedWorkloadMaxNanos;
    private static ReflectiveSparkProfile spark;

    private BaniraNetworkSmokeServerRunner() { }

    public static void register() {
        if (!BaniraNetworkSmokeStatus.enabled()) return;
        BaniraEvents.Server.onTick(event -> eventTicks++);
        BaniraEvents.Server.onTick(event -> onTick(event.serverAs(MinecraftServer.class)));
    }

    private static void onTick(MinecraftServer server) {
        try {
            if (server == null || !server.isRunning()) return;
            if (spark != null && spark.writeWhenComplete()) {
                sparkReportWritten = true;
                BaniraNetworkSmokeStatus.append("PASS spark-report-written");
            }
            if (finished) { shutdownWhenIdle(server); return; }
            if (!ready) { ready = true; BaniraNetworkSmokeStatus.append("PASS server-ready"); }
            if (server.getPlayerList().getPlayers().isEmpty() || eventTicks < 2) return;
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            if ("phase-one".equals(BaniraNetworkSmokeStatus.phase())) firstPhase(server, player);
            else if ("phase-two".equals(BaniraNetworkSmokeStatus.phase())) secondPhase(player);
            else throw new IllegalStateException("Unknown smoke phase " + BaniraNetworkSmokeStatus.phase());
        } catch (Throwable error) {
            finished = true;
            BaniraNetworkSmokeStatus.append("FAIL server " + error);
            throw new IllegalStateException("Banira network smoke server failed", error);
        }
    }

    private static void firstPhase(MinecraftServer server, ServerPlayer player) throws Exception {
        if (!eventVerified) {
            BaniraNetworkSmokeStatus.append("PASS event-bridge");
            eventVerified = true;
        }
        if (sustainedWorkloadStarted) {
            if (BaniraNetworkSmokeProfilePlan.isCycleDue(eventTicks, lastSustainedWorkloadTick)) {
                runSustainedWorkload(player);
                lastSustainedWorkloadTick = eventTicks;
            }
            if (BaniraNetworkSmokeProfilePlan.shouldContinue(sparkReportWritten, sustainedCycles)) return;
            long averageNanos = sustainedCycles == 0 ? 0L : sustainedWorkloadTotalNanos / sustainedCycles;
            BaniraNetworkSmokeStatus.append("PASS sustained-workload cycles=" + sustainedCycles
                    + " config-reloads=" + completedConfigReloads
                    + " average-ns=" + averageNanos + " max-ns=" + sustainedWorkloadMaxNanos);
            BaniraNetworkSmokeStatus.append("FINISHED phase-one");
            finished = true;
            return;
        }
        if (!configReloadRequested) {
            Path config = CustomConfig.getConfigDirectory().resolve(CustomConfig.FILE_NAME);
            Files.createDirectories(config.getParent());
            Files.write(config, ("{\"player\":{},\"server\":{\"virtual_permission\":{},\"help_num_per_page\":23,"
                    + "\"virtual_op_permission\":4,\"default_language\":\"en_us\"}}").getBytes(StandardCharsets.UTF_8));
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON);
            configReloadRequested = true;
            return;
        }
        if (CustomConfig.getHelpNumPerPage() != 23) return;
        BaniraNetworkSmokeStatus.append("PASS config-hot-reload");
        BaniraServerRuntime.playerDataManager().getOrCreate(player.getUUID()).putString("network_smoke", "persisted");
        BaniraServerRuntime.playerDataManager().saveToDisk(player.getUUID());
        if (!"persisted".equals(BaniraServerRuntime.playerDataManager().getOrCreate(player.getUUID()).getString("network_smoke"))) throw new IllegalStateException("Player data was not written");
        BaniraNetworkSmokeStatus.append("PASS player-data-file");
        Path file = CustomConfig.getConfigDirectory().resolve("network-smoke.txt");
        Files.write(file, "Banira network smoke".getBytes(StandardCharsets.UTF_8));
        if (!"Banira network smoke".equals(new String(Files.readAllBytes(file), StandardCharsets.UTF_8))) throw new IllegalStateException("Managed file operation failed");
        BaniraNetworkSmokeStatus.append("PASS file-operation");
        if (!sustainedWorkloadStarted) {
            sustainedWorkloadStarted = true;
            lastSustainedWorkloadTick = eventTicks;
            spark = ReflectiveSparkProfile.start(server);
            BaniraNetworkSmokeStatus.append("PASS spark-profiler-active");
            return;
        }
    }

    private static void runSustainedWorkload(ServerPlayer player) throws Exception {
        long startedAt = System.nanoTime();
        int cycle = ++sustainedCycles;
        if (pendingHelpNumPerPage >= 0 && CustomConfig.getHelpNumPerPage() == pendingHelpNumPerPage) {
            pendingHelpNumPerPage = -1;
            completedConfigReloads++;
        }
        if (BaniraNetworkSmokeProfilePlan.shouldScheduleConfigReload(pendingHelpNumPerPage >= 0, cycle)) {
            int helpNumPerPage = CustomConfig.getHelpNumPerPage() == 23 ? 24 : 23;
            Path config = CustomConfig.getConfigDirectory().resolve(CustomConfig.FILE_NAME);
            Files.write(config, ("{\"player\":{},\"server\":{\"virtual_permission\":{},\"help_num_per_page\":" + helpNumPerPage
                    + ",\"virtual_op_permission\":4,\"default_language\":\"en_us\"}}").getBytes(StandardCharsets.UTF_8));
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON);
            pendingHelpNumPerPage = helpNumPerPage;
        }
        BaniraServerRuntime.playerDataManager().getOrCreate(player.getUUID()).putInt("network_smoke_cycle", cycle);
        BaniraServerRuntime.playerDataManager().saveToDisk(player.getUUID());
        if (BaniraServerRuntime.playerDataManager().getOrCreate(player.getUUID()).getInt("network_smoke_cycle") != cycle) {
            throw new IllegalStateException("Player data cycle was not persisted");
        }
        Path file = CustomConfig.getConfigDirectory().resolve("network-smoke-cycle.txt");
        String content = "Banira network smoke cycle " + cycle + "\n" + player.getUUID();
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        if (!content.equals(new String(Files.readAllBytes(file), StandardCharsets.UTF_8))) {
            throw new IllegalStateException("Managed file cycle was not persisted");
        }
        long elapsed = System.nanoTime() - startedAt;
        sustainedWorkloadTotalNanos += elapsed;
        sustainedWorkloadMaxNanos = Math.max(sustainedWorkloadMaxNanos, elapsed);
    }

    private static void secondPhase(ServerPlayer player) {
        if (!"persisted".equals(BaniraServerRuntime.playerDataManager().getOrCreate(player.getUUID()).getString("network_smoke"))) throw new IllegalStateException("Player data did not survive restart");
        BaniraNetworkSmokeStatus.append("PASS persisted-player-data");
        BaniraNetworkSmokeStatus.append("FINISHED phase-two");
        finished = true;
    }

    private static void shutdownWhenIdle(MinecraftServer server) {
        if (server.getPlayerList().getPlayerCount() > 0) { shutdownTicks = 0; return; }
        if (++shutdownTicks >= 40) { BaniraNetworkSmokeStatus.append("PASS server-shutdown"); server.halt(false); }
    }

    private static final class ReflectiveSparkProfile {
        private final Object sampler, platform, plugin;
        private final Future<?> future;
        private final MinecraftServer server;
        private final Path report;
        private final long startedAt = System.nanoTime();
        private boolean written;
        private boolean stopRequested;
        private ReflectiveSparkProfile(Object sampler, Future<?> future, Object platform, Object plugin, MinecraftServer server, Path report) { this.sampler=sampler; this.future=future; this.platform=platform; this.plugin=plugin; this.server=server; this.report=report; }
        private static ReflectiveSparkProfile start(MinecraftServer server) {
            try {
                Object plugin = plugin(); ClassLoader loader = plugin.getClass().getClassLoader(); Class<?> base = base(plugin);
                Field platformField = base.getDeclaredField("platform"); platformField.setAccessible(true); Object platform = platformField.get(plugin);
                Class<?> builderType = Class.forName("me.lucko.spark.common.sampler.SamplerBuilder", true, loader); Object builder = builderType.getConstructor().newInstance();
                builderType.getMethod("samplingInterval", double.class).invoke(builder, 10.0D); builderType.getMethod("completeAfter", long.class, TimeUnit.class).invoke(builder, SPARK_SAMPLE_SECONDS, TimeUnit.SECONDS); builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumper = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                Class<?> gameThread = Class.forName("me.lucko.spark.common.sampler.ThreadDumper$GameThread", true, loader);
                Object gameThreadDumper = gameThread.getConstructor().newInstance();
                gameThread.getMethod("setThread", Thread.class).invoke(gameThreadDumper, Thread.currentThread());
                builderType.getMethod("threadDumper", dumper).invoke(builder, gameThread.getMethod("get").invoke(gameThreadDumper));
                Class<?> grouper = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader); builderType.getMethod("threadGrouper", grouper).invoke(builder, grouper.getField("BY_POOL").get(null));
                Object container = platform.getClass().getMethod("getSamplerContainer").invoke(platform);
                // Spark starts its configured background profiler with the server. The smoke owns this short, exportable sample.
                method(container.getClass(), "stopActiveSampler", 1).invoke(container, false);
                Object sampler = method(builderType, "start", 1).invoke(builder, platform); Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                method(container.getClass(), "setActiveSampler", 1).invoke(container, sampler);
                String path = System.getProperty("banira.networkSmoke.sparkReport", "").trim(); if (path.isEmpty()) throw new IllegalStateException("Missing Spark report path");
                return new ReflectiveSparkProfile(sampler, future, platform, plugin, server, Paths.get(path).toAbsolutePath());
            } catch (ReflectiveOperationException error) { throw new IllegalStateException("Unable to start Spark sampler", error); }
        }
        private boolean writeWhenComplete() {
            if (!stopRequested && System.nanoTime() - startedAt >= TimeUnit.SECONDS.toNanos(SPARK_SAMPLE_SECONDS)) {
                try {
                    method(sampler.getClass(), "stop", 1).invoke(sampler, false);
                    stopRequested = true;
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Unable to stop Spark sampler", error);
                }
            }
            // Spark 1.10 leaves its future incomplete after an explicit stop(false), even though its data is exportable.
            if (written || (!stopRequested && !future.isDone())) return false;
            try {
                ClassLoader loader=plugin.getClass().getClassLoader(); Class<?> propsType=Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader); Object props=propsType.getConstructor().newInstance();
                Class<?> senderData=Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader); propsType.getMethod("creator", senderData).invoke(props, senderData.getConstructor(String.class, java.util.UUID.class).newInstance("Banira network smoke", null));
                Class<?> disambiguator=Class.forName("me.lucko.spark.common.util.MethodDisambiguator", true, loader); Class<?> merge=Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
                Supplier<Object> mergeMode=() -> createMergeMode(merge, disambiguator); propsType.getMethod("mergeMode", Supplier.class).invoke(props, mergeMode);
                propsType.getMethod("classSourceLookup", Supplier.class).invoke(props, (Supplier<Object>) () -> createClassSourceLookup(plugin));
                Object proto=method(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props); byte[] bytes=(byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (bytes.length == 0) throw new IllegalStateException("Spark report was empty"); Files.createDirectories(report.getParent()); Files.write(report, bytes); written=true; return true;
            } catch (ReflectiveOperationException | java.io.IOException error) { throw new IllegalStateException("Unable to write Spark report", error); }
        }
        private static Object plugin() throws ReflectiveOperationException { Field field=MinecraftForge.EVENT_BUS.getClass().getDeclaredField("listeners"); field.setAccessible(true); Object listeners=field.get(MinecraftForge.EVENT_BUS); for(Object candidate:((Map<?,?>)listeners).keySet()) if(candidate!=null && candidate.getClass().getName().equals("me.lucko.spark.forge.plugin.ForgeServerSparkPlugin")) return candidate; throw new IllegalStateException("Spark server plugin was not registered"); }
        private static Class<?> base(Object plugin) { Class<?> type=plugin.getClass(); while(type!=null && !type.getName().equals("me.lucko.spark.forge.plugin.ForgeSparkPlugin")) type=type.getSuperclass(); if(type==null) throw new IllegalStateException("Spark base plugin was not found"); return type; }
        private static Object createMergeMode(Class<?> merge, Class<?> disambiguator) { try { return merge.getMethod("sameMethod", disambiguator).invoke(null, disambiguator.getConstructor().newInstance()); } catch (ReflectiveOperationException error) { throw new IllegalStateException("Unable to create Spark merge mode", error); } }
        private static Object createClassSourceLookup(Object plugin) { try { return plugin.getClass().getMethod("createClassSourceLookup").invoke(plugin); } catch (ReflectiveOperationException error) { throw new IllegalStateException("Unable to create Spark class source lookup", error); } }
        private static Method method(Class<?> type,String name,int count) { for(Method method:type.getMethods()) if(method.getName().equals(name) && method.getParameterCount()==count) return method; throw new IllegalStateException("Missing Spark method "+name); }
    }
}
