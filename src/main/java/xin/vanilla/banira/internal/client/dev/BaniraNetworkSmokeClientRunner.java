package xin.vanilla.banira.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.common.MinecraftForge;
import xin.vanilla.banira.internal.DebugScreen;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** 自动加入独立专服，并在第一阶段采集真实 Banira 界面的客户端 Spark 报告。 */
public final class BaniraNetworkSmokeClientRunner {
    private static int ticks;
    private static boolean connected;
    private static boolean finished;
    private static boolean uiOpened;
    private static ReflectiveClientSparkProfile spark;

    private BaniraNetworkSmokeClientRunner() {
    }

    public static void tick() {
        if (!BaniraNetworkSmokeStatus.enabled() || finished) return;
        Minecraft client = Minecraft.getInstance();
        if (!connected && ++ticks >= 20) {
            String host = System.getProperty("banira.networkSmoke.host", "127.0.0.1");
            int port = Integer.getInteger("banira.networkSmoke.port", 25579);
            ServerData server = new ServerData("Banira Network Smoke", host + ':' + port, ServerData.Type.OTHER);
            ConnectScreen.startConnecting(client.screen, client, ServerAddress.parseString(server.ip), server, false, null);
            connected = true;
            ticks = 0;
            return;
        }
        if (client.player == null || client.getSingleplayerServer() != null) {
            if (++ticks > 1400) fail(client, "remote login timed out");
            return;
        }
        if (ticks == 0) BaniraNetworkSmokeStatus.append("PASS remote-login");
        ticks++;
        if ("phase-one".equals(BaniraNetworkSmokeStatus.phase())) {
            runClientUiWorkload(client);
            if (spark == null || !spark.written()) {
                if (ticks > 900) fail(client, "client UI Spark profile timed out");
                return;
            }
        }
        if (ticks >= 500) {
            finished = true;
            BaniraNetworkSmokeStatus.append("FINISHED " + BaniraNetworkSmokeStatus.phase());
            client.stop();
        }
    }

    private static void runClientUiWorkload(Minecraft client) {
        if (!uiOpened) {
            uiOpened = true;
            client.setScreen(new DebugScreen());
            BaniraNetworkSmokeStatus.append("PASS client-ui-opened");
        }
        if (spark == null && ticks >= 20) {
            spark = ReflectiveClientSparkProfile.start();
            BaniraNetworkSmokeStatus.append("PASS client-ui-spark-profiler-active");
        }
        if (spark != null && spark.writeWhenComplete()) {
            BaniraNetworkSmokeStatus.append("PASS client-ui-spark-report-written");
        }
    }

    private static void fail(Minecraft client, String reason) {
        finished = true;
        BaniraNetworkSmokeStatus.append("FAIL client " + reason);
        client.stop();
    }

    /** Forge 1.16 的 Spark 已注册客户端插件，不重新实例化插件，仅启动独立采样器。 */
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
                builderType.getMethod("threadGrouper", grouperType).invoke(builder, grouperType.getField("BY_POOL").get(null));
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
                Class<?> sender = Class.forName("me.lucko.spark.common.command.sender.CommandSender", true, loader);
                Class<?> order = Class.forName("me.lucko.spark.common.sampler.ThreadNodeOrder", true, loader);
                Class<?> disambiguator = Class.forName("me.lucko.spark.common.util.MethodDisambiguator", true, loader);
                Class<?> merge = Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
                Object mergeMode = merge.getMethod("sameMethod", disambiguator).invoke(null, disambiguator.getConstructor().newInstance());
                Object lookup = base(plugin).getMethod("createClassSourceLookup").invoke(plugin);
                Object proto = method(sampler.getClass(), "toProto", 6).invoke(sampler, platform, commandSender(sender, loader),
                        order.getField("BY_TIME").get(null), "Banira client UI smoke", mergeMode, lookup);
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

        private static Object threadDumper(Object plugin, Class<?> base, Class<?> dumperType) throws ReflectiveOperationException {
            try {
                Field gameThread = base.getDeclaredField("threadDumper");
                gameThread.setAccessible(true);
                Object value = gameThread.get(plugin);
                if (value != null) {
                    value.getClass().getMethod("ensureSetup").invoke(value);
                    return base.getMethod("getDefaultThreadDumper").invoke(plugin);
                }
            } catch (java.lang.reflect.InvocationTargetException ignored) {
                // The 1.6 client plugin can be registered before its game-thread dumper is initialized.
            }
            return dumperType.getField("ALL").get(null);
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
            Field listeners = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("listeners");
            listeners.setAccessible(true);
            Object values = listeners.get(MinecraftForge.EVENT_BUS);
            for (Object candidate : ((Map<?, ?>) values).keySet()) {
                if (candidate != null && candidate.getClass().getName().equals("me.lucko.spark.forge.plugin.ForgeClientSparkPlugin")) return candidate;
            }
            throw new IllegalStateException("Spark client plugin was not registered");
        }

        private static Class<?> base(Object plugin) {
            Class<?> type = plugin.getClass();
            while (type != null && !type.getName().equals("me.lucko.spark.forge.plugin.ForgeSparkPlugin")) type = type.getSuperclass();
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
