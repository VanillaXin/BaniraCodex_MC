package xin.vanilla.banira.internal.server.dev;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.banira.api.event.BaniraEvents;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** 真实专服内验证 Banira 事件、配置热重载、玩家数据、文件服务与 Spark 报告。 */
public final class BaniraNetworkSmokeServerRunner {
    private static final int WORKLOAD_OPERATIONS_PER_TICK = 512;
    private static final String[] WORKLOAD_MOD_IDS = {
            "network_smoke_0", "network_smoke_1", "network_smoke_2", "network_smoke_3",
            "network_smoke_4", "network_smoke_5", "network_smoke_6", "network_smoke_7"
    };
    private static boolean ready;
    private static boolean finished;
    private static boolean failed;
    private static boolean eventVerified;
    private static boolean reloadRequested;
    private static int eventTicks;
    private static int shutdownTicks;
    private static ReflectiveSparkProfile spark;
    private static BaniraNetworkSmokeWorkload workload;

    private BaniraNetworkSmokeServerRunner() {
    }

    public static void register() {
        if (!BaniraNetworkSmokeStatus.enabled()) return;
        BaniraEvents.Server.onTick(event -> eventTicks++);
        BaniraEvents.Server.onTick(event -> onTick());
    }

    private static void onTick() {
        try {
            MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
            if (server == null || !server.isRunning()) return;
            if (failed) {
                shutdownWhenIdle(server);
                return;
            }
            if (spark != null && spark.writeWhenComplete()) {
                BaniraNetworkSmokeStatus.append("PASS spark-report-written");
            }
            if (finished) {
                shutdownWhenIdle(server);
                return;
            }
            if (!ready) {
                ready = true;
                BaniraNetworkSmokeStatus.append("PASS server-ready");
            }
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty() || eventTicks < 2) return;
            ServerPlayer player = players.get(0);
            if ("phase-one".equals(BaniraNetworkSmokeStatus.phase())) firstPhase(server, player);
            else if ("phase-two".equals(BaniraNetworkSmokeStatus.phase())) secondPhase(player);
            else throw new IllegalStateException("Unknown network smoke phase " + BaniraNetworkSmokeStatus.phase());
        } catch (Throwable error) {
            if (failed) return;
            failed = true;
            finished = true;
            BaniraNetworkSmokeStatus.append("FAIL server " + error);
            throw new IllegalStateException("Banira network smoke server failed", error);
        }
    }

    private static void firstPhase(MinecraftServer server, ServerPlayer player) throws Exception {
        if (spark != null) {
            runSustainedWorkload(player);
            return;
        }
        if (!eventVerified) {
            eventVerified = true;
            BaniraNetworkSmokeStatus.append("PASS event-bridge");
        }
        if (!reloadRequested) {
            Path config = CustomConfig.getConfigDirectory().resolve(CustomConfig.FILE_NAME);
            Files.createDirectories(config.getParent());
            Files.write(config, ("{\"player\":{},\"server\":{\"virtual_permission\":{},\"help_num_per_page\":23,"
                    + "\"virtual_op_permission\":4,\"default_language\":\"en_us\"}}").getBytes(StandardCharsets.UTF_8));
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON);
            reloadRequested = true;
            return;
        }
        if (CustomConfig.getHelpNumPerPage() != 23) return;
        BaniraNetworkSmokeStatus.append("PASS config-hot-reload");
        BaniraCodex.playerDataManager.getOrCreate(player.getUUID()).putString("network_smoke", "persisted");
        BaniraCodex.playerDataManager.saveToDisk(player.getUUID());
        if (!"persisted".equals(BaniraCodex.playerDataManager.getOrCreate(player.getUUID()).getString("network_smoke"))) {
            throw new IllegalStateException("Player data was not written");
        }
        BaniraNetworkSmokeStatus.append("PASS player-data-file");
        Path file = CustomConfig.getConfigDirectory().resolve("network-smoke.txt");
        Files.write(file, "Banira network smoke".getBytes(StandardCharsets.UTF_8));
        if (!"Banira network smoke".equals(new String(Files.readAllBytes(file), StandardCharsets.UTF_8))) {
            throw new IllegalStateException("Managed file operation failed");
        }
        BaniraNetworkSmokeStatus.append("PASS file-operation");
        spark = ReflectiveSparkProfile.start(server);
        workload = new BaniraNetworkSmokeWorkload(eventTicks);
        BaniraNetworkSmokeStatus.append("PASS spark-profiler-active");
        BaniraNetworkSmokeStatus.append("START sustained-core-workload");
    }

    private static void runSustainedWorkload(ServerPlayer player) {
        if (workload == null) throw new IllegalStateException("Missing sustained workload state");
        for (int slot = 0; slot < WORKLOAD_OPERATIONS_PER_TICK; slot++) {
            BaniraCodex.playerDataManager.getOrCreate(player.getUUID(), WORKLOAD_MOD_IDS[slot % WORKLOAD_MOD_IDS.length])
                    .putInt("workload_tick", workload.elapsedTicksAt(eventTicks));
            if (CustomConfig.getHelpNumPerPage() != 23) {
                throw new IllegalStateException("Hot-reloaded configuration changed during workload");
            }
        }
        if (workload.shouldPollConfigAt(eventTicks)) {
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON);
        }
        if (!workload.completeAt(eventTicks)) return;
        BaniraNetworkSmokeStatus.append("PASS sustained-core-workload");
        BaniraNetworkSmokeStatus.append("FINISHED phase-one");
        finished = true;
    }

    private static void secondPhase(ServerPlayer player) {
        if (!"persisted".equals(BaniraCodex.playerDataManager.getOrCreate(player.getUUID()).getString("network_smoke"))) {
            throw new IllegalStateException("Player data did not survive restart");
        }
        BaniraNetworkSmokeStatus.append("PASS persisted-player-data");
        BaniraNetworkSmokeStatus.append("FINISHED phase-two");
        finished = true;
    }

    private static void shutdownWhenIdle(MinecraftServer server) {
        if (server.getPlayerList().getPlayerCount() > 0) {
            shutdownTicks = 0;
            return;
        }
        if (++shutdownTicks >= 40) {
            BaniraNetworkSmokeStatus.append("PASS server-shutdown");
            server.halt(false);
        }
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
                builderType.getMethod("completeAfter", long.class, TimeUnit.class).invoke(builder, 20L, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                builderType.getMethod("threadDumper", dumperType).invoke(builder,
                        plugin.getClass().getMethod("getDefaultThreadDumper").invoke(plugin));
                Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                builderType.getMethod("threadGrouper", grouperType).invoke(builder, grouperType.getField("BY_POOL").get(null));
                Object sampler = method(builderType, "start", 0).invoke(builder);
                method(sampler.getClass(), "start", 0).invoke(sampler);
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
