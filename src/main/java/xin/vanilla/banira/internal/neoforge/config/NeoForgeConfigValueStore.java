package xin.vanilla.banira.internal.neoforge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigValueStore;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigEditSnapshot;
import xin.vanilla.banira.common.config.ConfigCommitResult;
import xin.vanilla.banira.common.config.ConfigValueExpectation;
import xin.vanilla.banira.common.config.ConfigReadSnapshot;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class NeoForgeConfigValueStore implements ConfigValueStore {
    private final ModConfigSpec spec;
    private final Map<String, ModConfigSpec.ConfigValue<?>> values;

    @Nullable
    private ModConfig modConfig;
    private volatile NeoForgeConfigFile managedFile;
    private ConfigHolder holder;
    private long notifiedRevision;

    NeoForgeConfigValueStore(ModConfigSpec spec, Map<String, ModConfigSpec.ConfigValue<?>> values) {
        this.spec = spec;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    void bindModConfig(@Nullable ModConfig modConfig) {
        this.modConfig = modConfig;
    }

    void setHolder(ConfigHolder holder) { this.holder = holder; }

    void detach(NeoForgeConfigFile file) {
        if (managedFile == file) {
            managedFile = null;
            notifiedRevision = 0;
        }
    }

    NeoForgeConfigFile managedFile() { return managedFile; }

    CommentedFileConfig wrap(CommentedFileConfig file) {
        NeoForgeConfigFile managed = new NeoForgeConfigFile(file, spec, this::prepare);
        managedFile = managed;
        notifiedRevision = 0;
        return managed;
    }

    private void prepare(CommentedConfig candidate) {
        for (String path : values.keySet()) {
            ModConfigSpec.ValueSpec definition = valueSpec(path);
            if (!candidate.contains(path)) {
                if (managedFile != null && managedFile.hasLoaded()) throw new ParsingException("Missing config value at " + path);
                candidate.set(path, definition.getDefault());
            }
            if (!definition.test(candidate.get(path))) {
                throw new ParsingException("Invalid config value at " + path + ": " + candidate.get(path));
            }
            String comment = definition.getComment();
            if (comment != null) candidate.setComment(path, comment);
        }
    }

    @Override
    public Set<String> paths() {
        return values.keySet();
    }

    @Nullable
    @Override
    public Object get(String path) {
        synchronized (valueLock()) {
            ModConfigSpec.ConfigValue<?> value = values.get(path);
            Object result = value != null ? value.get() : null;
            return result instanceof java.util.List ? new java.util.ArrayList<>((java.util.List<?>) result) : result;
        }
    }

    @Override
    public boolean matchesStoredValue(String path, Object expected) {
        synchronized (valueLock()) {
            ModConfigSpec.ConfigValue<?> value = values.get(path);
            if (value == null) return false;
            return ConfigValueExpectation.storedEquals(value.get(), expected);
        }
    }

    @Override
    public BooleanSupplier prepareStoredMatch(Map<String, Object> expected, boolean allowEnumNames) {
        ConfigValueExpectation expectation = new ConfigValueExpectation(expected, allowEnumNames);
        ModConfigSpec.ConfigValue<?>[] handles = new ModConfigSpec.ConfigValue<?>[expectation.size()];
        for (int i = 0; i < handles.length; i++) {
            handles[i] = values.get(expectation.path(i));
            if (handles[i] == null) return () -> false;
        }
        return () -> {
            while (true) {
                Object lock = valueLock();
                synchronized (lock) {
                    // Wrapping a replacement file changes the monitor; never use a retired lock.
                    if (lock != valueLock()) continue;
                    if (lock instanceof NeoForgeConfigFile && !((NeoForgeConfigFile) lock).isOpen()) return false;
                    for (int i = 0; i < handles.length; i++) {
                        if (!expectation.matches(i, handles[i].get())) return false;
                    }
                    return true;
                }
            }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void set(String path, Object value) {
        NeoForgeConfigFile file = managedFile;
        if (file != null) {
            file.load();
            notifyExternalReload(file);
        }
        synchronized (valueLock()) {
            ModConfigSpec.ConfigValue configValue = values.get(path);
            if (configValue != null) {
                if (managedFile != null) managedFile.set(configValue.getPath(), value);
                else configValue.set(value);
            }
        }
    }

    private Object valueLock() {
        NeoForgeConfigFile file = managedFile;
        return file != null ? file : this;
    }


    @Override
    public ConfigEditSnapshot snapshotForEdit(Set<String> paths) {
        if (!values.keySet().containsAll(paths)) throw new IllegalArgumentException("Unknown config path");
        return requireManagedFile().snapshotForEdit(paths);
    }

    @Override
    public ConfigReadSnapshot snapshotForRead(Set<String> paths) {
        while (true) {
            Object lock = valueLock();
            synchronized (lock) {
                if (lock != valueLock()) continue;
                if (lock instanceof NeoForgeConfigFile && !((NeoForgeConfigFile) lock).isOpen())
                    throw new IllegalStateException("Config file is closed");
                Map<String, Object> captured = new LinkedHashMap<>();
                for (String path : paths) {
                    ModConfigSpec.ConfigValue<?> value = values.get(path);
                    if (value == null) throw new IllegalArgumentException("Unknown config path: " + path);
                    captured.put(path, value.get());
                }
                return ConfigReadSnapshot.of(captured);
            }
        }
    }

    @Override
    public ConfigCommitResult compareAndSetAll(ConfigEditSnapshot expected, Map<String, Object> changes) {
        changes.forEach((path, value) -> {
            if (!validate(path, value)) throw new IllegalArgumentException("Invalid config value: " + path);
        });
        return requireManagedFile().compareAndSetAll(expected, changes);
    }

    @Override
    public void setAll(Map<String, Object> changes) {
        NeoForgeConfigFile file = requireManagedFile();
        ConfigEditSnapshot snapshot = file.snapshotForEdit(changes.keySet());
        if (compareAndSetAll(snapshot, changes) == ConfigCommitResult.CONFLICT) {
            throw new IllegalStateException("Config changed during batch edit");
        }
    }

    private NeoForgeConfigFile requireManagedFile() {
        NeoForgeConfigFile file = managedFile;
        if (file == null) throw new IllegalStateException("Config file has not been bound");
        return file;
    }

    @Override
    public Class<?> valueClass(String path) {
        try {
            Object current = get(path);
            return current != null ? current.getClass() : Object.class;
        } catch (Throwable ignored) {
            return Object.class;
        }
    }

    @Nullable
    @Override
    public Object defaultValue(String path) {
        ModConfigSpec.ValueSpec valueSpec = valueSpec(path);
        return valueSpec != null ? valueSpec.getDefault() : null;
    }

    @Override
    public boolean validate(String path, Object value) {
        ModConfigSpec.ValueSpec valueSpec = valueSpec(path);
        return valueSpec != null && valueSpec.test(value);
    }

    @Override
    public void save() {
        if (managedFile != null) {
            managedFile.save();
        } else if (modConfig != null) {
            if (modConfig.getLoadedConfig() != null) modConfig.getLoadedConfig().save();
        }
        if (managedFile != null) notifyExternalReload(managedFile);
    }

    void acceptReload() {
        if (holder == null) return;
        synchronized (holder) {
            NeoForgeConfigFile file = managedFile;
            // Native events and a local write can both observe the same external revision.
            if (file != null) notifyExternalReload(file);
            else holder.acceptExternalReload();
        }
    }

    private void notifyExternalReload(NeoForgeConfigFile file) {
        long revision = file.externalRevision();
        if (holder != null && revision != notifiedRevision) {
            notifiedRevision = revision;
            // No file monitor is held while subscribers run, including reentrant configuration readers.
            holder.acceptExternalReload();
        }
    }

    @Nullable
    private ModConfigSpec.ValueSpec valueSpec(String path) {
        ModConfigSpec.ConfigValue<?> value = values.get(path);
        if (value == null) {
            return null;
        }
        return spec.getSpec().get(value.getPath());
    }
}
