package xin.vanilla.banira.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import xin.vanilla.banira.internal.DebugScreen;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 自动加入独立专服，确保通用服务端能力经过真实远端连接。 */
public final class BaniraNetworkSmokeClientRunner {
    private static int ticks;
    private static boolean connected;
    private static boolean finished;
    private static boolean uiOpened;
    private static ReflectiveClientSparkProfile spark;

    private BaniraNetworkSmokeClientRunner() {
    }

    public static void tick(Minecraft client) {
        if (!BaniraNetworkSmokeStatus.enabled() || finished) return;
        if (!connected && ++ticks >= 20) {
            String host = System.getProperty("banira.networkSmoke.host", "127.0.0.1");
            int port = Integer.getInteger("banira.networkSmoke.port", 25579);
            ServerData server = new ServerData("Banira Network Smoke", host + ':' + port, ServerData.Type.OTHER);
            ConnectScreen.startConnecting(client.screen, client, ServerAddress.parseString(host + ':' + port), server, false, null);
            connected = true;
            ticks = 0;
            return;
        }
        if (client.player == null || client.getSingleplayerServer() != null) {
            if (ticks > 1400) fail(client, "remote login timed out");
            else ticks++;
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
            spark = ReflectiveClientSparkProfile.start(client);
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

    /** Spark 的客户端 API 随版本变动，仅在 dev-only smoke 中通过反射采集并导出报告。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class ReflectiveClientSparkProfile {
        private final Object plugin;
        private final Object platform;
        private final Object sampler;
        private final Future<?> future;
        private final Path reportPath;
        private boolean written;

        private ReflectiveClientSparkProfile(Object plugin, Object platform, Object sampler, Future<?> future, Path reportPath) {
            this.plugin = plugin;
            this.platform = platform;
            this.sampler = sampler;
            this.future = future;
            this.reportPath = reportPath;
        }

        private static ReflectiveClientSparkProfile start(Minecraft client) {
            try {
                String configured = System.getProperty("banira.networkSmoke.clientSparkReport", "").trim();
                if (configured.isEmpty()) throw new IllegalStateException("Missing banira.networkSmoke.clientSparkReport");
                Class<?> modType = Class.forName("me.lucko.spark.fabric.FabricSparkMod");
                Field mod = modType.getDeclaredField("mod");
                mod.setAccessible(true);
                Object modInstance = mod.get(null);
                Class<?> pluginType = Class.forName("me.lucko.spark.fabric.plugin.FabricClientSparkPlugin");
                Object plugin = pluginType.getConstructor(modType, Minecraft.class).newInstance(modInstance, client);
                pluginType.getMethod("enable").invoke(plugin);
                Object platform = platform(plugin);
                ClassLoader loader = platform.getClass().getClassLoader();
                Class<?> builderType = Class.forName("me.lucko.spark.common.sampler.SamplerBuilder", true, loader);
                Object builder = builderType.getConstructor().newInstance();
                Object sampler = startSampler(plugin, platform, pluginType, builderType, builder, loader);
                Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                return new ReflectiveClientSparkProfile(plugin, platform, sampler, future, Paths.get(configured).toAbsolutePath());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start client Spark sampler for network smoke", error);
            }
        }

        private static Object startSampler(Object plugin, Object platform, Class<?> pluginType, Class<?> builderType,
                                           Object builder, ClassLoader loader) throws ReflectiveOperationException {
            Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
            builderType.getMethod("completeAfter", long.class, TimeUnit.class).invoke(builder, 20L, TimeUnit.SECONDS);
            builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
            Object dumper;
            try {
                dumper = pluginType.getMethod("getDefaultThreadDumper").invoke(plugin);
            } catch (java.lang.reflect.InvocationTargetException ignored) {
                dumper = dumperType.getField("ALL").get(null);
            }
            builderType.getMethod("threadDumper", dumperType).invoke(builder, dumper);
            Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
            threadGrouper(builderType, builder, grouperType);
            try {
                Object samplerContainer = platform.getClass().getMethod("getSamplerContainer").invoke(platform);
                method(samplerContainer.getClass(), "stopActiveSampler", 1).invoke(samplerContainer, true);
                Class<?> modeType = Class.forName("me.lucko.spark.common.sampler.SamplerMode", true, loader);
                Object executionMode = Enum.valueOf((Class) modeType, "EXECUTION");
                builderType.getMethod("mode", modeType).invoke(builder, executionMode);
                builderType.getMethod("samplingInterval", double.class).invoke(builder,
                        ((Number) modeType.getMethod("defaultInterval").invoke(executionMode)).doubleValue());
                Object sampler = method(builderType, "start", 1).invoke(builder, platform);
                method(samplerContainer.getClass(), "setActiveSampler", 1).invoke(samplerContainer, sampler);
                return sampler;
            } catch (NoSuchMethodException ignored) {
                builderType.getMethod("samplingInterval", double.class).invoke(builder, 4.0D);
                Object sampler = method(builderType, "start", 0).invoke(builder);
                return sampler;
            }
        }

        private boolean written() {
            return written;
        }

        private boolean writeWhenComplete() {
            if (written || !future.isDone()) return false;
            try {
                ClassLoader loader = sampler.getClass().getClassLoader();
                Object proto = exportProto(loader);
                byte[] bytes = (byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (bytes.length == 0) throw new IllegalStateException("Client Spark report was empty");
                Files.createDirectories(reportPath.getParent());
                Files.write(reportPath, bytes);
                plugin.getClass().getMethod("disable").invoke(plugin);
                written = true;
                return true;
            } catch (ReflectiveOperationException | java.io.IOException error) {
                throw new IllegalStateException("Unable to write client Spark report", error);
            }
        }

        private static Object platform(Object plugin) throws ReflectiveOperationException {
            Field platform = plugin.getClass().getSuperclass().getDeclaredField("platform");
            platform.setAccessible(true);
            return platform.get(plugin);
        }

        private static Object classSourceLookup(Object platform) {
            try {
                return platform.getClass().getMethod("createClassSourceLookup").invoke(platform);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create client Spark class source lookup", error);
            }
        }

        private static void threadGrouper(Class<?> builderType, Object builder, Class<?> grouperType)
                throws ReflectiveOperationException {
            Object grouper = grouperType.getField("BY_POOL").get(null);
            try {
                builderType.getMethod("threadGrouper", Supplier.class).invoke(builder, grouper);
            } catch (NoSuchMethodException ignored) {
                builderType.getMethod("threadGrouper", grouperType).invoke(builder, grouper);
            }
        }

        private Object exportProto(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> propsType = Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader);
            try {
                Object props = propsType.getConstructor().newInstance();
                Class<?> senderDataType = Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
                Object creator = senderDataType.getConstructor(String.class, java.util.UUID.class)
                        .newInstance("Banira client UI smoke", null);
                propsType.getMethod("creator", senderDataType).invoke(props, creator);
                Class<?> strategyType = Class.forName("me.lucko.spark.common.sampler.java.MergeStrategy", true, loader);
                propsType.getMethod("mergeStrategy", strategyType).invoke(props, strategyType.getField("SAME_METHOD").get(null));
                propsType.getMethod("classSourceLookup", Supplier.class).invoke(props,
                        (Supplier<Object>) () -> classSourceLookup(platform));
                return method(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                try {
                    Object props = propsType.getConstructor().newInstance();
                    Class<?> senderDataType = Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
                    Object creator = senderDataType.getConstructor(String.class, java.util.UUID.class)
                            .newInstance("Banira client UI smoke", null);
                    propsType.getMethod("creator", senderDataType).invoke(props, creator);
                    propsType.getMethod("comment", String.class).invoke(props, "Banira client UI smoke");
                    propsType.getMethod("mergeMode", Supplier.class).invoke(props,
                            (Supplier<Object>) () -> legacyMergeModeUnchecked(loader));
                    propsType.getMethod("classSourceLookup", Supplier.class).invoke(props,
                            (Supplier<Object>) () -> classSourceLookup(platform));
                    return method(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props);
                } catch (NoSuchMethodException noSupplierProps) {
                    return exportLegacyProto(loader, propsType);
                }
            }
        }

        private Object exportLegacyProto(ClassLoader loader, Class<?> propsType) throws ReflectiveOperationException {
                Class<?> platformInfo = Class.forName("me.lucko.spark.common.platform.PlatformInfo", true, loader);
                Class<?> sender = Class.forName("me.lucko.spark.common.command.sender.CommandSender", true, loader);
                Class<?> order = Class.forName("me.lucko.spark.common.sampler.ThreadNodeOrder", true, loader);
                Class<?> merge = Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
                Class<?> lookup = Class.forName("me.lucko.spark.common.util.ClassSourceLookup", true, loader);
                Object commandSender = legacyCommandSender(sender, senderData(loader));
                Object props = propsType.getConstructor(platformInfo, sender, java.util.Comparator.class, String.class, merge, lookup)
                        .newInstance(plugin.getClass().getMethod("getPlatformInfo").invoke(plugin), commandSender,
                                order.getField("BY_TIME").get(null), "Banira client UI smoke", legacyMergeMode(loader),
                                plugin.getClass().getMethod("createClassSourceLookup").invoke(plugin));
                return method(sampler.getClass(), "toProto", 1).invoke(sampler, props);
        }

        private static Object legacyCommandSender(Class<?> senderType, final Class<?> senderDataType) {
            return Proxy.newProxyInstance(senderType.getClassLoader(), new Class<?>[]{senderType}, (proxy, method, args) -> {
                String name = method.getName();
                if ("getName".equals(name)) return "Banira client UI smoke";
                if ("getUniqueId".equals(name)) return null;
                if ("hasPermission".equals(name)) return true;
                if ("sendMessage".equals(name)) return null;
                if ("toData".equals(name)) return senderDataType.getConstructor(String.class, UUID.class)
                        .newInstance("Banira client UI smoke", null);
                if ("toString".equals(name)) return "Banira client UI smoke";
                if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                if ("equals".equals(name)) return proxy == args[0];
                return null;
            });
        }

        private static Class<?> senderData(ClassLoader loader) {
            try {
                return Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
            } catch (ClassNotFoundException error) {
                throw new IllegalStateException("Unable to load legacy Spark sender metadata", error);
            }
        }

        private static Object legacyMergeModeUnchecked(ClassLoader loader) {
            try {
                return legacyMergeMode(loader);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create legacy Spark merge mode", error);
            }
        }

        private static Object legacyMergeMode(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> disambiguator = Class.forName("me.lucko.spark.common.util.MethodDisambiguator", true, loader);
            Class<?> merge = Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
            Object methodDisambiguator = disambiguator.getConstructor().newInstance();
            return merge.getMethod("sameMethod", disambiguator).invoke(null, methodDisambiguator);
        }

        private static Method method(Class<?> type, String name, int parameters) {
            for (Method candidate : type.getMethods()) {
                if (candidate.getName().equals(name) && candidate.getParameterCount() == parameters) return candidate;
            }
            throw new IllegalStateException("Missing Spark method " + type.getName() + '#' + name);
        }
    }
}
