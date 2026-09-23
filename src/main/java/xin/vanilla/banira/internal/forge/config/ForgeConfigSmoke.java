package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;
import xin.vanilla.banira.internal.config.CommonConfigView;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Dev-only checks through the actual Forge watcher, spec, holder and restart lifecycle. */
public final class ForgeConfigSmoke {
    private static final Map<String, ModConfig> CONFIGS = new ConcurrentHashMap<>();
    private static final AtomicInteger RELOADS = new AtomicInteger();
    private static int stage;
    private static int cycles;
    private static int invalidTicks;
    private static long started;
    private static byte[] validBytes;
    private static Runnable unsubscribe;
    private static java.util.function.IntSupplier retainedRead;
    private static java.util.function.IntConsumer retainedWrite;

    private ForgeConfigSmoke() { }

    public static void register(IEventBus bus) {
        if (!BaniraNetworkSmokeStatus.enabled()) return;
        bus.addListener((ModConfigEvent.Loading event) -> CONFIGS.put(event.getConfig().getFileName(), event.getConfig()));
    }

    public static boolean tick(boolean client) throws Exception {
        if (stage == 4) return true;
        if (started == 0) started = System.nanoTime();
        if (System.nanoTime() - started > java.util.concurrent.TimeUnit.SECONDS.toNanos(45)) {
            throw new IllegalStateException("Forge config smoke timed out: stage=" + stage + ", cycles=" + cycles);
        }
        String name = client ? "banira_codex-client.toml" : "banira_codex-common.toml";
        String key = client ? "notificationLogMaxEntries" : "help.helpInfoNumPerPage";
        ModConfig mod = CONFIGS.get(name);
        if (mod == null) throw new IllegalStateException("Missing Forge config " + name);
        if (!(mod.getConfigData() instanceof ForgeConfigFile)) throw new IllegalStateException("Transactional config handler not active");
        ForgeConfigFile file = (ForgeConfigFile) mod.getConfigData();
        if (!mod.getSpec().isCorrect(file)) throw new IllegalStateException("Config spec bridge not active");
        ConfigHolder holder = ForgeConfigAdapter.getHolder(client ? ClientConfig.class : CommonConfig.class);
        if (retainedRead == null) {
            if (client) {
                ClientConfigView retained = ClientConfigView.get();
                retainedRead = retained::notificationLogMaxEntries;
                retainedWrite = retained::notificationLogMaxEntries;
            } else {
                CommonConfigView.HelpView retained = CommonConfigView.get().help();
                retainedRead = retained::helpInfoNumPerPage;
                retainedWrite = retained::helpInfoNumPerPage;
            }
        }
        Path checkpoint = file.getNioPath().resolveSibling(name + ".smoke-expected");
        if ("phase-two".equals(BaniraNetworkSmokeStatus.phase())) {
            CommentedConfig expected = new TomlParser().parse(new String(Files.readAllBytes(checkpoint), StandardCharsets.UTF_8));
            if (!expected.valueMap().equals(snapshot(file).valueMap())) throw new IllegalStateException("Complete config snapshot changed after restart");
            if (retainedRead.getAsInt() != ((Number) expected.get(key)).intValue()) throw new IllegalStateException("Generated config view changed after restart");
            BaniraNetworkSmokeStatus.append("PASS generated-config-view-restart file=" + name);
            BaniraNetworkSmokeStatus.append("PASS forge-config-restart file=" + name);
            stage = 4;
            return true;
        }
        if (stage == 0) {
            unsubscribe = holder.onReloaded(paths -> {
                holder.get(key);
                RELOADS.incrementAndGet();
            });
            editExternally(file, holder, key);
            stage = 1;
            return false;
        }
        if (stage == 1) {
            if (((Number) holder.get(key)).intValue() != 30 + cycles || RELOADS.get() <= cycles) return false;
            if (retainedRead.getAsInt() != 30 + cycles) throw new IllegalStateException("Retained generated view missed external reload");
            if (!Arrays.asList("minecraft:arrow", "tick, clazz -> tick >= 5").equals(file.get("smokeUnknown.items"))) {
                throw new IllegalStateException("Unknown category or comma expression was lost");
            }
            if (++cycles < 20) {
                editExternally(file, holder, key);
                return false;
            }
            validBytes = Files.readAllBytes(file.getNioPath());
            Files.write(file.getNioPath(), "broken = [\n".getBytes(StandardCharsets.UTF_8));
            stage = 2;
            return false;
        }
        if (stage == 2) {
            if (++invalidTicks < 30) return false;
            int before = ((Number) holder.get(key)).intValue();
            try { file.load(); throw new IllegalStateException("Invalid config was accepted"); }
            catch (ParsingException expected) { }
            try { holder.save(); throw new IllegalStateException("Invalid external file was overwritten"); }
            catch (ParsingException expected) { }
            if (before != ((Number) holder.get(key)).intValue()) throw new IllegalStateException("Invalid load changed cached value");
            if (!"broken = [\n".equals(new String(Files.readAllBytes(file.getNioPath()), StandardCharsets.UTF_8))) {
                throw new IllegalStateException("Invalid file was replaced");
            }
            Files.write(file.getNioPath(), validBytes);
            stage = 3;
        }
        file.load();
        retainedWrite.accept(61);
        holder.save();
        if (retainedRead.getAsInt() != 61 || ((Number) holder.get(key)).intValue() != 61) throw new IllegalStateException("Generated view write missed current holder");
        Files.write(checkpoint, TomlFormat.instance().createWriter().writeToString(snapshot(file)).getBytes(StandardCharsets.UTF_8));
        unsubscribe.run();
        stage = 4;
        BaniraNetworkSmokeStatus.append("PASS forge-config-transaction cycles=" + cycles + " reloads=" + RELOADS.get() + " file=" + name);
        BaniraNetworkSmokeStatus.append("PASS generated-config-view-reload cycles=" + cycles + " file=" + name);
        return true;
    }

    private static void editExternally(ForgeConfigFile file, ConfigHolder holder, String key) throws Exception {
        holder.set(key, 21);
        holder.save();
        CommentedConfig external = snapshot(file);
        external.set(key, 30 + cycles);
        external.set("smokeUnknown.items", Arrays.asList("minecraft:arrow", "tick, clazz -> tick >= 5"));
        Files.write(file.getNioPath(), TomlFormat.instance().createWriter().writeToString(external).getBytes(StandardCharsets.UTF_8));
    }

    private static CommentedConfig snapshot(ForgeConfigFile file) {
        return new TomlParser().parse(TomlFormat.instance().createWriter().writeToString(file));
    }
}
