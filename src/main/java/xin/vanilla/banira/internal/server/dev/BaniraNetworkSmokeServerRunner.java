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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
            if (!BaniraNetworkSmokeStatus.notificationsOnly()
                    && !xin.vanilla.banira.internal.fabric.config.FabricConfigViewSmoke.step(false)) return;
            if (!ready) { ready = true; BaniraNetworkSmokeStatus.append("PASS server-ready"); }
            if (server.getPlayerList().getPlayers().isEmpty() || eventTicks < 2) return;
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            if (BaniraNetworkSmokeStatus.notificationsOnly()) {
                if (eventTicks % 10 != 0 || !BaniraNetworkSmokeStatus.notificationClientReady()) return;
                if (!xin.vanilla.banira.common.util.PlayerUtils.isRemoteClientModInstalled(player, xin.vanilla.banira.api.Banira.MOD_ID)) return;
                if ("phase-two".equals(BaniraNetworkSmokeStatus.phase())) sendVanillaNotificationBatch(player);
                BaniraNetworkSmokeStatus.append("PASS notification-only-server");
                BaniraNetworkSmokeStatus.append("FINISHED " + BaniraNetworkSmokeStatus.phase());
                finished = true;
                return;
            }
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
            spark = ReflectiveSparkProfile.start();
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
        private final Object platform;
        private final Object sampler;
        private final Future<?> future;
        private final Path reportPath;
        private boolean written;

        private ReflectiveSparkProfile(Object platform, Object sampler, Future<?> future, Path reportPath) {
            this.platform = platform;
            this.sampler = sampler;
            this.future = future;
            this.reportPath = reportPath;
        }

        private static ReflectiveSparkProfile start() {
            try {
                String configured = System.getProperty("banira.networkSmoke.sparkReport", "").trim();
                if (configured.isEmpty()) throw new IllegalStateException("Missing banira.networkSmoke.sparkReport");
                Object platform = platform();
                Object plugin = serverPlugin();
                ClassLoader loader = platform.getClass().getClassLoader();
                Object samplerContainer = platform.getClass().getMethod("getSamplerContainer").invoke(platform);
                method(samplerContainer.getClass(), "stopActiveSampler", 1).invoke(samplerContainer, true);
                Class<?> builderType = Class.forName("me.lucko.spark.common.sampler.SamplerBuilder", true, loader);
                Object builder = builderType.getConstructor().newInstance();
                Class<?> modeType = Class.forName("me.lucko.spark.common.sampler.SamplerMode", true, loader);
                Object executionMode = Enum.valueOf((Class) modeType, "EXECUTION");
                builderType.getMethod("mode", modeType).invoke(builder, executionMode);
                builderType.getMethod("samplingInterval", double.class).invoke(builder,
                        ((Number) modeType.getMethod("defaultInterval").invoke(executionMode)).doubleValue());
                builderType.getMethod("completeAfter", long.class, TimeUnit.class).invoke(builder, SPARK_SAMPLE_SECONDS, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                builderType.getMethod("threadDumper", dumperType).invoke(builder,
                        plugin.getClass().getMethod("getDefaultThreadDumper").invoke(plugin));
                Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                builderType.getMethod("threadGrouper", Supplier.class).invoke(builder, grouperType.getField("BY_POOL").get(null));
                Object sampler = method(builderType, "start", 1).invoke(builder, platform);
                method(samplerContainer.getClass(), "setActiveSampler", 1).invoke(samplerContainer, sampler);
                Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                return new ReflectiveSparkProfile(platform, sampler, future, Paths.get(configured).toAbsolutePath());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start Spark sampler for network smoke", error);
            }
        }

        private boolean writeWhenComplete() {
            if (written || !future.isDone()) return false;
            try {
                ClassLoader loader = sampler.getClass().getClassLoader();
                Class<?> propsType = Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader);
                Object props = propsType.getConstructor().newInstance();
                Class<?> senderDataType = Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
                Object creator = senderDataType.getConstructor(String.class, java.util.UUID.class)
                        .newInstance("Banira network smoke", null);
                propsType.getMethod("creator", senderDataType).invoke(props, creator);
                Class<?> strategyType = Class.forName("me.lucko.spark.common.sampler.java.MergeStrategy", true, loader);
                propsType.getMethod("mergeStrategy", strategyType)
                        .invoke(props, strategyType.getField("SAME_METHOD").get(null));
                propsType.getMethod("classSourceLookup", Supplier.class).invoke(props,
                        (Supplier<Object>) () -> classSourceLookup(platform));
                Object proto = method(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props);
                byte[] bytes = (byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (bytes.length == 0) throw new IllegalStateException("Spark report was empty");
                Files.createDirectories(reportPath.getParent());
                Files.write(reportPath, bytes);
                written = true;
                return true;
            } catch (ReflectiveOperationException | java.io.IOException error) {
                throw new IllegalStateException("Unable to write Spark report", error);
            }
        }

        private static Object platform() throws ReflectiveOperationException {
            Object plugin = serverPlugin();
            Field platformField = plugin.getClass().getSuperclass().getDeclaredField("platform");
            platformField.setAccessible(true);
            return platformField.get(plugin);
        }

        private static Object serverPlugin() throws ReflectiveOperationException {
            Class<?> modType = Class.forName("me.lucko.spark.fabric.FabricSparkMod");
            Field mod = modType.getDeclaredField("mod");
            mod.setAccessible(true);
            Field plugin = modType.getDeclaredField("activeServerPlugin");
            plugin.setAccessible(true);
            return plugin.get(mod.get(null));
        }

        private static Object classSourceLookup(Object platform) {
            try {
                return platform.getClass().getMethod("createClassSourceLookup").invoke(platform);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create Spark class source lookup", error);
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
