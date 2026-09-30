package xin.vanilla.banira.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.neoforge.common.NeoForge;
import xin.vanilla.banira.api.BaniraEnvironment;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeProfilePlan;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 自动加入独立专服，并在第一阶段采集真实 Banira 界面的客户端 Spark 报告。 */
public final class BaniraNetworkSmokeClientRunner {
    private static int ticks;
    private static boolean notificationReady;
    private static boolean connected;
    private static final NetworkSmokeClientState state = new NetworkSmokeClientState();
    private static boolean finished;
    private static boolean uiOpened;
    private static BaniraNetworkSmokeScreen uiScreen;
    private static int uiCycles;
    private static int lastUiCycleTick;
    private static long uiWorkloadTotalNanos;
    private static long uiWorkloadMaxNanos;
    private static boolean uiWorkloadReported;
    private static long serverWaitStartedAt;
    private static String loginView = "";
    private static ReflectiveClientSparkProfile spark;
    private static boolean chatListenerRegistered;
    private static final java.util.List<String> vanillaDetails = new java.util.ArrayList<>();
    private static int vanillaPages;

    private BaniraNetworkSmokeClientRunner() {
    }

    public static void tick(Minecraft client) {
        if (!BaniraNetworkSmokeStatus.enabled() || finished) return;
        if ("tooltip-ui".equals(BaniraNetworkSmokeStatus.phase())) {
            if (client.getOverlay() != null || ++ticks < 20) return;
            try {
                if (TooltipTransitionSmoke.tick(client)) {
                    finished = true;
                    BaniraNetworkSmokeStatus.append("FINISHED tooltip-ui");
                    client.stop();
                }
            } catch (Exception error) {
                fail(client, "tooltip UI smoke failed: " + error);
            }
            return;
        }
        if (client.player == null) {
            String view = "screen=" + (client.screen == null ? "none" : client.screen.getClass().getName())
                    + " overlay=" + (client.getOverlay() == null ? "none" : client.getOverlay().getClass().getName());
            if (!view.equals(loginView)) {
                BaniraNetworkSmokeStatus.append("INFO login-view " + view);
                loginView = view;
            }
        }
        if (!chatListenerRegistered) {
            NeoForge.EVENT_BUS.addListener(BaniraNetworkSmokeClientRunner::receiveVanillaNotification);
            chatListenerRegistered = true;
        }
        if (!connected && client.getOverlay() != null) {
            ticks = 0;
            return;
        }
        if (!connected) {
            if (++ticks < 20) return;
            String host = System.getProperty("banira.networkSmoke.host", "127.0.0.1");
            int port = Integer.getInteger("banira.networkSmoke.port", 25579);
            ServerData server = new ServerData("Banira Network Smoke", host + ':' + port, ServerData.Type.OTHER);
            ConnectScreen.startConnecting(client.screen, client, ServerAddress.parseString(server.ip), server, false, null);
            connected = true;
            ticks = 0;
            return;
        }
        if (client.player == null || client.getSingleplayerServer() != null) {
            if (++ticks > 1400) fail(client, "remote login timed out " + loginView);
            return;
        }
        if (state.markRemoteLogin()) {
            if (!Boolean.parseBoolean(System.getProperty("banira.devInventoryIntegrations", "true"))) {
                if (BaniraEnvironment.isModLoaded("jei")) {
                    fail(client, "JEI is loaded while development inventory integrations are disabled");
                    return;
                }
                BaniraNetworkSmokeStatus.append("PASS optional-inventory-absent");
            }
            ticks = 0;
            BaniraNetworkSmokeStatus.append("PASS remote-login");
        }
        try {
            if (!NotificationRegionSmoke.tick(client)) return;
            if (!BaniraNetworkSmokeStatus.notificationsOnly()
                    && !xin.vanilla.banira.internal.neoforge.config.NeoForgeConfigViewSmoke.step(true)) return;
            if (!NotificationReadSmoke.tick(client)) return;
            if (BaniraNetworkSmokeStatus.notificationsOnly() && !notificationReady) {
                notificationReady = true;
                BaniraNetworkSmokeStatus.append("PASS notification-client-ready");
            }
        } catch (Exception error) {
            fail(client, "notification/config smoke failed: " + error);
            return;
        }
        ticks++;
        if (!BaniraNetworkSmokeStatus.notificationsOnly() && "phase-one".equals(BaniraNetworkSmokeStatus.phase())) {
            runClientUiWorkload(client);
            if (spark == null || !spark.written() || !uiWorkloadReported) {
                if (ticks > 900) fail(client, "client UI Spark profile/workload timed out cycles=" + uiCycles
                        + " report-written=" + (spark != null && spark.written()) + " " + uiScreen.workloadSummary());
                return;
            }
        }
        if (!serverFinished(client)) return;
        if ("phase-two".equals(BaniraNetworkSmokeStatus.phase())) {
            int expected = xin.vanilla.banira.internal.dev.BaniraNetworkSmokeNotificationFixture.ENTRIES;
            if (vanillaDetails.size() < expected) {
                if (ticks > 600) fail(client, "native notification details timed out: " + vanillaDetails.size());
                return;
            }
            if (vanillaDetails.size() != expected || vanillaPages < 2) {
                fail(client, "native notification batch count invalid");
                return;
            }
            for (int index = 0; index < expected; index++) {
                if (!xin.vanilla.banira.internal.dev.BaniraNetworkSmokeNotificationFixture.text(index).equals(vanillaDetails.get(index))) {
                    fail(client, "native notification detail changed/reordered at " + index);
                    return;
                }
            }
            BaniraNetworkSmokeStatus.append("PASS vanilla-notification-details-client entries=" + expected + " pages=" + vanillaPages);
        }
        finished = true;
        BaniraNetworkSmokeStatus.append("FINISHED " + BaniraNetworkSmokeStatus.phase());
        client.stop();
    }

    private static void receiveVanillaNotification(net.neoforged.neoforge.client.event.ClientChatReceivedEvent.System event) {
        String message = event.getMessage().getString();
        String prefix = xin.vanilla.banira.internal.dev.BaniraNetworkSmokeNotificationFixture.PREFIX;
        if (!message.startsWith(prefix)) return;
        vanillaPages++;
        java.util.Collections.addAll(vanillaDetails, message.substring(prefix.length()).split("\\|", -1));
    }

    private static void runClientUiWorkload(Minecraft client) {
        if (uiScreen == null) {
            uiScreen = new BaniraNetworkSmokeScreen();
            client.setScreen(uiScreen);
        }
        if (!uiOpened && client.screen == uiScreen) {
            BaniraNetworkSmokeStatus.append("PASS client-ui-opened");
            uiOpened = true;
        }
        if (client.screen == uiScreen
                && BaniraNetworkSmokeProfilePlan.isCycleDue(ticks, lastUiCycleTick)) {
            long startedAt = System.nanoTime();
            uiScreen.runNetworkSmokeCycle(++uiCycles);
            long elapsed = System.nanoTime() - startedAt;
            uiWorkloadTotalNanos += elapsed;
            uiWorkloadMaxNanos = Math.max(uiWorkloadMaxNanos, elapsed);
            lastUiCycleTick = ticks;
        }
        if (spark == null && ticks >= 20) {
            spark = ReflectiveClientSparkProfile.start();
            BaniraNetworkSmokeStatus.append("PASS client-ui-spark-profiler-active");
        }
        if (spark != null && spark.writeWhenComplete()) {
            BaniraNetworkSmokeStatus.append("PASS client-ui-spark-report-written");
        }
        if (!uiWorkloadReported && client.screen == uiScreen && spark != null
                && !BaniraNetworkSmokeProfilePlan.shouldContinue(spark.written(), uiCycles)
                && uiScreen.workloadVerified()) {
            long averageNanos = uiCycles == 0 ? 0L : uiWorkloadTotalNanos / uiCycles;
            BaniraNetworkSmokeStatus.append("PASS client-ui-sustained-workload cycles=" + uiCycles
                    + " average-ns=" + averageNanos + " max-ns=" + uiWorkloadMaxNanos
                    + " " + uiScreen.workloadSummary());
            uiWorkloadReported = true;
        }
    }

    private static boolean serverFinished(Minecraft client) {
        if (serverWaitStartedAt == 0L) serverWaitStartedAt = System.nanoTime();
        String configured = System.getProperty("banira.networkSmoke.serverStatus", "").trim();
        if (configured.isEmpty()) {
            fail(client, "missing server status path");
            return false;
        }
        String expected = "FINISHED " + BaniraNetworkSmokeStatus.phase();
        try {
            Path status = Paths.get(configured);
            if (Files.isRegularFile(status)) {
                List<String> lines = Files.readAllLines(status, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.startsWith("FAIL ")) {
                        fail(client, "server reported " + line);
                        return false;
                    }
                }
                if (lines.contains(expected)) {
                    BaniraNetworkSmokeStatus.append("PASS server-finished " + BaniraNetworkSmokeStatus.phase());
                    return true;
                }
            }
        } catch (java.io.IOException | java.nio.file.InvalidPathException error) {
            fail(client, "unable to read server status: " + error);
            return false;
        }
        if (System.nanoTime() - serverWaitStartedAt > TimeUnit.SECONDS.toNanos(90)) {
            fail(client, "timed out waiting for server " + expected);
        }
        return false;
    }

    private static void fail(Minecraft client, String reason) {
        finished = true;
        BaniraNetworkSmokeStatus.append("FAIL client " + reason);
        client.stop();
    }

    /** 复用 NeoForge 已注册的客户端 Spark 插件，只启动独立采样器。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class ReflectiveClientSparkProfile {
        private final Object sampler;
        private final Future<?> future;
        private final Object platform;
        private final Object plugin;
        private final Path report;
        private boolean written;

        private ReflectiveClientSparkProfile(Object sampler, Future<?> future, Object platform, Object plugin, Path report) {
            this.sampler = sampler;
            this.future = future;
            this.platform = platform;
            this.plugin = plugin;
            this.report = report;
        }

        private static ReflectiveClientSparkProfile start() {
            try {
                Object plugin = plugin();
                ClassLoader loader = plugin.getClass().getClassLoader();
                Class<?> base = base(plugin);
                Field platformField = base.getDeclaredField("platform");
                platformField.setAccessible(true);
                Object platform = platformField.get(plugin);
                Class<?> builderType = Class.forName("me.lucko.spark.common.sampler.SamplerBuilder", true, loader);
                Object builder = builderType.getConstructor().newInstance();
                builderType.getMethod("samplingInterval", double.class).invoke(builder, 4.0D);
                builderType.getMethod("completeAfter", long.class, TimeUnit.class).invoke(builder, 20L, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                builderType.getMethod("threadDumper", dumperType).invoke(builder, threadDumper(plugin, base, dumperType));
                Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                configureThreadGrouper(builderType, builder, grouperType);
                Object sampler = method(builderType, "start", 1).invoke(builder, platform);
                Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                String configured = System.getProperty("banira.networkSmoke.clientSparkReport", "").trim();
                if (configured.isEmpty()) throw new IllegalStateException("Missing client Spark report path");
                return new ReflectiveClientSparkProfile(sampler, future, platform, plugin, Paths.get(configured).toAbsolutePath());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start client Spark sampler for network smoke", error);
            }
        }

        private boolean written() {
            return written;
        }

        private boolean writeWhenComplete() {
            if (written || !future.isDone()) return false;
            try {
                ClassLoader loader = plugin.getClass().getClassLoader();
                Object proto = exportProto(loader);
                byte[] bytes = (byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (bytes.length == 0) throw new IllegalStateException("Client Spark report was empty");
                Files.createDirectories(report.getParent());
                Files.write(report, bytes);
                written = true;
                return true;
            } catch (ReflectiveOperationException | java.io.IOException error) {
                throw new IllegalStateException("Unable to write client Spark report", error);
            }
        }

        private Object exportProto(ClassLoader loader) throws ReflectiveOperationException {
            try {
                Class<?> propsType = Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader);
                Object props = propsType.getConstructor().newInstance();
                Class<?> senderData = Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
                Object creator = senderData.getConstructor(String.class, UUID.class).newInstance("Banira client UI smoke", null);
                propsType.getMethod("creator", senderData).invoke(props, creator);
                propsType.getMethod("comment", String.class).invoke(props, "Banira client UI smoke");
                configureExportMerge(propsType, props, loader);
                propsType.getMethod("classSourceLookup", Supplier.class).invoke(props, (Supplier<Object>) () -> classSourceLookup());
                return method(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                Class<?> sender = Class.forName("me.lucko.spark.common.command.sender.CommandSender", true, loader);
                Class<?> order = Class.forName("me.lucko.spark.common.sampler.ThreadNodeOrder", true, loader);
                return method(sampler.getClass(), "toProto", 6).invoke(sampler, platform, commandSender(sender, loader),
                        order.getField("BY_TIME").get(null), "Banira client UI smoke", mergeMode(loader), classSourceLookup());
            }
        }

        private static void configureExportMerge(Class<?> propsType, Object props, ClassLoader loader) throws ReflectiveOperationException {
            try {
                Class<?> strategyType = Class.forName("me.lucko.spark.common.sampler.java.MergeStrategy", true, loader);
                propsType.getMethod("mergeStrategy", strategyType).invoke(props, strategyType.getField("SAME_METHOD").get(null));
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                propsType.getMethod("mergeMode", Supplier.class).invoke(props, (Supplier<Object>) () -> mergeModeUnchecked(loader));
            }
        }

        private Object classSourceLookup() {
            try {
                return base(plugin).getMethod("createClassSourceLookup").invoke(plugin);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create Spark class source lookup", error);
            }
        }

        private static Object mergeModeUnchecked(ClassLoader loader) {
            try {
                return mergeMode(loader);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create Spark merge mode", error);
            }
        }

        private static Object mergeMode(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> disambiguator = Class.forName("me.lucko.spark.common.util.MethodDisambiguator", true, loader);
            Class<?> merge = Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
            return merge.getMethod("sameMethod", disambiguator).invoke(null, disambiguator.getConstructor().newInstance());
        }

        private static Object threadDumper(Object plugin, Class<?> base, Class<?> dumperType) throws ReflectiveOperationException {
            try {
                Field gameThread = base.getDeclaredField("threadDumper");
                gameThread.setAccessible(true);
                Object value = gameThread.get(plugin);
                if (value != null) {
                    value.getClass().getMethod("ensureSetup").invoke(value);
                    return base.getMethod("getDefaultThreadDumper").invoke(plugin);
                }
            } catch (NoSuchFieldException | java.lang.reflect.InvocationTargetException ignored) {
                // Some Spark releases do not expose an initialized game-thread dumper on the client plugin.
            }
            return dumperType.getField("ALL").get(null);
        }

        private static void configureThreadGrouper(Class<?> builderType, Object builder, Class<?> grouperType) throws ReflectiveOperationException {
            Object byPool = grouperType.getField("BY_POOL").get(null);
            for (Method candidate : builderType.getMethods()) {
                if (!candidate.getName().equals("threadGrouper") || candidate.getParameterCount() != 1) continue;
                Class<?> parameterType = candidate.getParameterTypes()[0];
                if (parameterType.isInstance(byPool)) {
                    candidate.invoke(builder, byPool);
                    return;
                }
                if (Supplier.class.isAssignableFrom(parameterType)) {
                    Supplier<Object> supplier = byPool instanceof Supplier ? (Supplier<Object>) byPool : () -> byPool;
                    candidate.invoke(builder, supplier);
                    return;
                }
            }
            throw new NoSuchMethodException("Missing compatible Spark threadGrouper method");
        }

        private static Object commandSender(Class<?> senderType, ClassLoader loader) throws ReflectiveOperationException {
            Class<?> data = Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
            return Proxy.newProxyInstance(senderType.getClassLoader(), new Class<?>[]{senderType}, (proxy, method, args) -> {
                String name = method.getName();
                if ("getName".equals(name)) return "Banira client UI smoke";
                if ("getUniqueId".equals(name)) return null;
                if ("hasPermission".equals(name)) return true;
                if ("sendMessage".equals(name)) return null;
                if ("toData".equals(name)) return data.getConstructor(String.class, UUID.class).newInstance("Banira client UI smoke", null);
                if ("toString".equals(name)) return "Banira client UI smoke";
                if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                if ("equals".equals(name)) return proxy == args[0];
                return null;
            });
        }

        private static Object plugin() throws ReflectiveOperationException {
            Field listeners = NeoForge.EVENT_BUS.getClass().getDeclaredField("listeners");
            listeners.setAccessible(true);
            Object values = listeners.get(NeoForge.EVENT_BUS);
            for (Object candidate : ((Map<?, ?>) values).keySet()) {
                if (candidate != null && candidate.getClass().getName().equals("me.lucko.spark.neoforge.plugin.NeoForgeClientSparkPlugin")) return candidate;
            }
            throw new IllegalStateException("Spark client plugin was not registered");
        }

        private static Class<?> base(Object plugin) {
            Class<?> type = plugin.getClass();
            while (type != null && !type.getName().equals("me.lucko.spark.neoforge.plugin.NeoForgeSparkPlugin")) type = type.getSuperclass();
            if (type == null) throw new IllegalStateException("Spark base plugin was not found");
            return type;
        }

        private static Method method(Class<?> type, String name, int parameters) {
            for (Method candidate : type.getMethods()) {
                if (candidate.getName().equals(name) && candidate.getParameterCount() == parameters) return candidate;
            }
            throw new IllegalStateException("Missing Spark method " + type.getName() + '#' + name);
        }
    }
}
