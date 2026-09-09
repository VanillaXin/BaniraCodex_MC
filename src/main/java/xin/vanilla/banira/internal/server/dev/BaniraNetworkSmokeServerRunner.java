package xin.vanilla.banira.internal.server.dev;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.banira.api.event.BaniraEvents;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeProfilePlan;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** 真实专服内验证 Banira 事件、配置热重载、玩家数据、文件操作与 Spark 归档。 */
public final class BaniraNetworkSmokeServerRunner {
    private static final int WORKLOAD_OPERATIONS_PER_TICK = 512;
    private static final String[] WORKLOAD_MOD_IDS = {
            "network_smoke_0", "network_smoke_1", "network_smoke_2", "network_smoke_3",
            "network_smoke_4", "network_smoke_5", "network_smoke_6", "network_smoke_7"
    };
    private static BaniraNetworkSmokeWorkload workload;
    private static boolean coreWorkloadReported;
    private static long coreOperations;
    private static long coreTicks;
    private static long coreTotalNanos;
    private static long coreMaxNanos;
    private static final long SPARK_SAMPLE_SECONDS = 20L;
    private static int eventTicks;
    private static boolean ready;
    private static boolean finished;
    private static boolean failed;
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
    private static long sustainedStartedAt;
    private static ReflectiveSparkProfile spark;

    private BaniraNetworkSmokeServerRunner() { }

    public static void register() {
        if (!BaniraNetworkSmokeStatus.enabled()) return;
        BaniraEvents.Server.onTick(event -> eventTicks++);
        BaniraEvents.Server.onTick(event -> onTick(BaniraServer.currentAs(MinecraftServer.class)));
    }

    private static void onTick(MinecraftServer server) {
        try {
            if (server == null || !server.isRunning()) return;
            if (failed) { shutdownWhenIdle(server); return; }
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
            if (failed) return;
            failed = true;
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
            runCoreWorkload(player);
            if (System.nanoTime() - sustainedStartedAt > TimeUnit.SECONDS.toNanos(60)) {
                throw new IllegalStateException("Sustained workload timed out: cycles=" + sustainedCycles
                        + ", reloads=" + completedConfigReloads + ", pending=" + pendingHelpNumPerPage);
            }
            if (BaniraNetworkSmokeProfilePlan.isCycleDue(eventTicks, lastSustainedWorkloadTick)) {
                runSustainedWorkload(player);
                lastSustainedWorkloadTick = eventTicks;
            }
            if (BaniraNetworkSmokeProfilePlan.shouldContinue(sparkReportWritten, sustainedCycles)) return;
            if (pendingHelpNumPerPage >= 0 || completedConfigReloads < 10 || !coreWorkloadReported) return;
            java.util.Properties checkpoint = new java.util.Properties();
            checkpoint.setProperty("player", player.getUUID().toString());
            checkpoint.setProperty("cycle", Integer.toString(sustainedCycles));
            checkpoint.setProperty("helpNumPerPage", Integer.toString(CustomConfig.getHelpNumPerPage()));
            try (java.io.Writer writer = Files.newBufferedWriter(
                    CustomConfig.getConfigDirectory().resolve("network-smoke-checkpoint.properties"), StandardCharsets.UTF_8)) {
                checkpoint.store(writer, "Network smoke restart checkpoint");
            }
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
        BaniraCodex.playerDataManager.getOrCreate(player.getUUID()).putString("network_smoke", "persisted");
        BaniraCodex.playerDataManager.saveToDisk(player.getUUID());
        if (!"persisted".equals(BaniraCodex.playerDataManager.loadFromDisk(player.getUUID()).getString("network_smoke"))) throw new IllegalStateException("Player data was not written");
        BaniraNetworkSmokeStatus.append("PASS player-data-file");
        Path file = CustomConfig.getConfigDirectory().resolve("network-smoke.txt");
        Files.write(file, "Banira network smoke".getBytes(StandardCharsets.UTF_8));
        if (!"Banira network smoke".equals(new String(Files.readAllBytes(file), StandardCharsets.UTF_8))) throw new IllegalStateException("Managed file operation failed");
        BaniraNetworkSmokeStatus.append("PASS file-operation");
        if (!sustainedWorkloadStarted) {
            sustainedWorkloadStarted = true;
            sustainedStartedAt = System.nanoTime();
            lastSustainedWorkloadTick = eventTicks;
            spark = ReflectiveSparkProfile.start(server);
            workload = new BaniraNetworkSmokeWorkload(eventTicks);
            BaniraNetworkSmokeStatus.append("PASS spark-profiler-active");
            BaniraNetworkSmokeStatus.append("START sustained-core-workload");
            return;
        }
    }

    private static void runCoreWorkload(ServerPlayer player) {
        if (coreWorkloadReported) return;
        if (workload == null) throw new IllegalStateException("Missing sustained workload state");
        long startedAt = System.nanoTime();
        int expectedHelp = CustomConfig.getHelpNumPerPage();
        if (expectedHelp != 23 && expectedHelp != 24) throw new IllegalStateException("Unexpected workload config");
        for (int slot = 0; slot < WORKLOAD_OPERATIONS_PER_TICK; slot++) {
            BaniraCodex.playerDataManager.getOrCreate(player.getUUID(), WORKLOAD_MOD_IDS[slot % WORKLOAD_MOD_IDS.length])
                    .putInt("workload_tick", workload.elapsedTicksAt(eventTicks));
            if (CustomConfig.getHelpNumPerPage() != expectedHelp) {
                throw new IllegalStateException("Configuration changed inside one workload tick");
            }
        }
        if (workload.shouldPollConfigAt(eventTicks)) {
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON);
        }
        long elapsed = System.nanoTime() - startedAt;
        coreTicks++;
        coreOperations += WORKLOAD_OPERATIONS_PER_TICK;
        coreTotalNanos += elapsed;
        coreMaxNanos = Math.max(coreMaxNanos, elapsed);
        if (workload.completeAt(eventTicks)) {
            coreWorkloadReported = true;
            BaniraNetworkSmokeStatus.append("PASS sustained-core-workload ticks=" + coreTicks
                    + " operations=" + coreOperations + " average-ns=" + coreTotalNanos / coreTicks
                    + " max-ns=" + coreMaxNanos);
        }
    }

    private static void runSustainedWorkload(ServerPlayer player) throws Exception {
        long startedAt = System.nanoTime();
        int cycle = ++sustainedCycles;
        if (pendingHelpNumPerPage >= 0 && CustomConfig.getHelpNumPerPage() == pendingHelpNumPerPage) {
            pendingHelpNumPerPage = -1;
            completedConfigReloads++;
        }
        if ((BaniraNetworkSmokeProfilePlan.shouldContinue(sparkReportWritten, cycle) || completedConfigReloads < 10)
                && BaniraNetworkSmokeProfilePlan.shouldScheduleConfigReload(pendingHelpNumPerPage >= 0, cycle)) {
            int helpNumPerPage = CustomConfig.getHelpNumPerPage() == 23 ? 24 : 23;
            Path config = CustomConfig.getConfigDirectory().resolve(CustomConfig.FILE_NAME);
            Files.write(config, ("{\"player\":{},\"server\":{\"virtual_permission\":{},\"help_num_per_page\":" + helpNumPerPage
                    + ",\"virtual_op_permission\":4,\"default_language\":\"en_us\"}}").getBytes(StandardCharsets.UTF_8));
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON);
            pendingHelpNumPerPage = helpNumPerPage;
        }
        BaniraCodex.playerDataManager.getOrCreate(player.getUUID()).putInt("network_smoke_cycle", cycle);
        BaniraCodex.playerDataManager.saveToDisk(player.getUUID());
        if (BaniraCodex.playerDataManager.loadFromDisk(player.getUUID()).getInt("network_smoke_cycle") != cycle) {
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

    private static void secondPhase(ServerPlayer player) throws Exception {
        if (!xin.vanilla.banira.common.util.PlayerUtils.isRemoteClientModInstalled(player, BaniraCodex.MODID)) return;
        java.util.Properties checkpoint = new java.util.Properties();
        try (java.io.Reader reader = Files.newBufferedReader(
                CustomConfig.getConfigDirectory().resolve("network-smoke-checkpoint.properties"), StandardCharsets.UTF_8)) {
            checkpoint.load(reader);
        }
        int expectedCycle = Integer.parseInt(checkpoint.getProperty("cycle"));
        int expectedHelp = Integer.parseInt(checkpoint.getProperty("helpNumPerPage"));
        if (!player.getUUID().toString().equals(checkpoint.getProperty("player"))
                || expectedCycle < BaniraNetworkSmokeProfilePlan.MINIMUM_CYCLES
                || BaniraCodex.playerDataManager.loadFromDisk(player.getUUID()).getInt("network_smoke_cycle") != expectedCycle
                || !"persisted".equals(BaniraCodex.playerDataManager.getOrCreate(player.getUUID()).getString("network_smoke"))) {
            throw new IllegalStateException("Final player data cycle did not survive restart");
        }
        if (CustomConfig.getHelpNumPerPage() != expectedHelp) throw new IllegalStateException("Final config reload did not survive restart");
        sendVanillaNotificationBatch(player);
        BaniraNetworkSmokeStatus.append("PASS persisted-final-cycle cycle=" + expectedCycle + " help=" + expectedHelp);
        BaniraNetworkSmokeStatus.append("PASS persisted-player-data");
        BaniraNetworkSmokeStatus.append("FINISHED phase-two");
        finished = true;
    }

    private static void sendVanillaNotificationBatch(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        String previous = CustomConfig.getPlayerNotificationReceiveMode(uuid);
        CustomConfig.setPlayerNotificationReceiveMode(uuid, CustomConfig.notificationReceiveModeVanillaMessage);
        try {
            java.util.List<xin.vanilla.banira.common.data.Component> entries = new java.util.ArrayList<>();
            for (int index = 0; index < xin.vanilla.banira.internal.dev.BaniraNetworkSmokeNotificationFixture.ENTRIES; index++) {
                entries.add(xin.vanilla.banira.BaniraComponent.get().literal(
                        xin.vanilla.banira.internal.dev.BaniraNetworkSmokeNotificationFixture.text(index)).color(0xFF00FF00));
            }
            xin.vanilla.banira.common.util.MessageUtils.sendNotificationBatch(player,
                    xin.vanilla.banira.BaniraComponent.get().literal(
                            xin.vanilla.banira.internal.dev.BaniraNetworkSmokeNotificationFixture.PREFIX),
                    entries, xin.vanilla.banira.BaniraComponent.get().literal("|"),
                    xin.vanilla.banira.common.enums.EnumNotificationStyle.SUCCESS, "banira_codex.network_smoke");
            BaniraNetworkSmokeStatus.append("PASS vanilla-notification-batch-submitted entries=256 client-installed=true");
        } finally {
            CustomConfig.setPlayerNotificationReceiveMode(uuid, previous);
        }
    }

    private static void shutdownWhenIdle(MinecraftServer server) {
        if (server.getPlayerList().getPlayerCount() > 0) { shutdownTicks = 0; return; }
        if (++shutdownTicks >= 40) { BaniraNetworkSmokeStatus.append("PASS server-shutdown"); server.halt(false); }
    }

    /** Spark 没有稳定的跨加载器导出 API，烟测仅反射调用其原生 sampler。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class ReflectiveSparkProfile {
        private final Object plugin;
        private final Object platform;
        private final Object sampler;
        private final Future<?> future;
        private final Path reportPath;
        private boolean written;

        private ReflectiveSparkProfile(Object plugin, Object platform, Object sampler, Future<?> future, Path reportPath) {
            this.plugin = plugin;
            this.platform = platform;
            this.sampler = sampler;
            this.future = future;
            this.reportPath = reportPath;
        }

        private static ReflectiveSparkProfile start(MinecraftServer server) {
            try {
                String configured = System.getProperty("banira.networkSmoke.sparkReport", "").trim();
                if (configured.isEmpty()) throw new IllegalStateException("Missing banira.networkSmoke.sparkReport");
                Object plugin = serverPlugin(server);
                Class<?> pluginBase = plugin.getClass().getSuperclass();
                Field platformField = pluginBase.getDeclaredField("platform");
                platformField.setAccessible(true);
                Object platform = platformField.get(plugin);
                Field gameThreadField = pluginBase.getDeclaredField("threadDumper");
                gameThreadField.setAccessible(true);
                Object gameThreadDumper = gameThreadField.get(plugin);
                gameThreadDumper.getClass().getMethod("ensureSetup").invoke(gameThreadDumper);
                ClassLoader loader = platform.getClass().getClassLoader();
                Class<?> builderType = Class.forName("me.lucko.spark.common.sampler.SamplerBuilder", true, loader);
                Object builder = builderType.getConstructor().newInstance();
                builderType.getMethod("samplingInterval", double.class).invoke(builder, 10.0D);
                builderType.getMethod("completeAfter", long.class, TimeUnit.class).invoke(builder, SPARK_SAMPLE_SECONDS, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                builderType.getMethod("threadDumper", dumperType).invoke(builder,
                        plugin.getClass().getMethod("getDefaultThreadDumper").invoke(plugin));
                Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                builderType.getMethod("threadGrouper", grouperType).invoke(builder, grouperType.getField("BY_POOL").get(null));
                Object sampler = method(builderType, "start", 0).invoke(builder);
                Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                return new ReflectiveSparkProfile(plugin, platform, sampler, future, Paths.get(configured).toAbsolutePath());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start Spark sampler for network smoke", error);
            }
        }

        private boolean writeWhenComplete() {
            if (written || !future.isDone()) return false;
            try {
                ClassLoader loader = sampler.getClass().getClassLoader();
                Class<?> propsType = Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader);
                Class<?> platformInfoType = Class.forName("me.lucko.spark.common.platform.PlatformInfo", true, loader);
                Class<?> commandSenderType = Class.forName("me.lucko.spark.common.command.sender.CommandSender", true, loader);
                Class<?> orderType = Class.forName("me.lucko.spark.common.sampler.ThreadNodeOrder", true, loader);
                Class<?> mergeType = Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
                Class<?> lookupType = Class.forName("me.lucko.spark.common.util.ClassSourceLookup", true, loader);
                Class<?> senderType = Class.forName("me.lucko.spark.fabric.FabricCommandSender", true, loader);
                Constructor<?> senderConstructor = senderType.getConstructors()[0];
                Object sender = senderConstructor.newInstance(BaniraServer.currentAs(MinecraftServer.class), plugin);
                Object props = propsType.getConstructor(platformInfoType, commandSenderType, java.util.Comparator.class,
                                String.class, mergeType, lookupType)
                        .newInstance(plugin.getClass().getMethod("getPlatformInfo").invoke(plugin), sender,
                                orderType.getField("BY_TIME").get(null), "Banira network smoke", mergeMode(),
                                plugin.getClass().getMethod("createClassSourceLookup").invoke(plugin));
                Object proto = method(sampler.getClass(), "toProto", 1).invoke(sampler, props);
                byte[] bytes = (byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (bytes.length == 0) throw new IllegalStateException("Spark report was empty");
                Files.createDirectories(reportPath.getParent());
                Files.write(reportPath, bytes);
                written = true;
                plugin.getClass().getMethod("disable").invoke(plugin);
                return true;
            } catch (ReflectiveOperationException | java.io.IOException error) {
                throw new IllegalStateException("Unable to write Spark report", error);
            }
        }

        private static Object serverPlugin(MinecraftServer server) throws ReflectiveOperationException {
            Class<?> modType = Class.forName("me.lucko.spark.fabric.FabricSparkMod");
            Field mod = modType.getDeclaredField("mod");
            mod.setAccessible(true);
            Class<?> pluginType = Class.forName("me.lucko.spark.fabric.plugin.FabricServerSparkPlugin");
            Object plugin = pluginType.getConstructor(modType, MinecraftServer.class).newInstance(mod.get(null), server);
            pluginType.getMethod("enable").invoke(plugin);
            return plugin;
        }

        private static Object mergeMode() {
            try {
                ClassLoader loader = ReflectiveSparkProfile.class.getClassLoader();
                Class<?> disambiguator = Class.forName("me.lucko.spark.common.util.MethodDisambiguator", true, loader);
                Class<?> merge = Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
                return merge.getMethod("sameMethod", disambiguator).invoke(null, disambiguator.getConstructor().newInstance());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create Spark merge mode", error);
            }
        }

        private static Method method(Class<?> type, String name, int parameters) {
            for (Method candidate : type.getMethods()) {
                if (candidate.getName().equals(name) && candidate.getParameterCount() == parameters) return candidate;
            }
            throw new IllegalStateException("Missing Spark method " + type.getName() + '#' + name);
        }
    }
}
