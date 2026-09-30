package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigValueStore;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigEditSnapshot;
import xin.vanilla.banira.common.config.ConfigCommitResult;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class ForgeConfigBackend implements ConfigValueStore {
    private final ForgeConfigSpec spec;
    private final Map<String, ForgeConfigSpec.ConfigValue<?>> values;

    @Nullable
    private ModConfig modConfig;
    private volatile ForgeConfigFile managedFile;
    private ConfigHolder holder;
    private long notifiedRevision;

    ForgeConfigBackend(ForgeConfigSpec spec, Map<String, ForgeConfigSpec.ConfigValue<?>> values) {
        this.spec = spec;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    void setModConfig(@Nullable ModConfig modConfig) {
        this.modConfig = modConfig;
    }

    void setHolder(ConfigHolder holder) { this.holder = holder; }

    ForgeConfigReloadGate reloadGate(Runnable callback) {
        ForgeConfigFile file = managedFile;
        return new ForgeConfigReloadGate(file::hasExternalChange, callback);
    }

    CommentedFileConfig wrap(CommentedFileConfig file) {
        ForgeConfigFile managed = new ForgeConfigFile(file, spec, this::prepare);
        managedFile = managed;
        notifiedRevision = 0;
        return managed;
    }

    private void prepare(CommentedConfig candidate) {
        for (String path : values.keySet()) {
            ForgeConfigSpec.ValueSpec definition = valueSpec(path);
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
            ForgeConfigSpec.ConfigValue<?> value = values.get(path);
            Object result = value != null ? value.get() : null;
            return result instanceof java.util.List ? new java.util.ArrayList<>((java.util.List<?>) result) : result;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void set(String path, Object value) {
        ForgeConfigFile file = managedFile;
        if (file != null) {
            file.load();
            notifyExternalReload(file);
        }
        synchronized (valueLock()) {
            ForgeConfigSpec.ConfigValue configValue = values.get(path);
            if (configValue != null) {
                configValue.set(value);
            }
        }
    }

    private Object valueLock() {
        ForgeConfigFile file = managedFile;
        return file != null ? file : this;
    }

    @Override
    public ConfigEditSnapshot snapshotForEdit(Set<String> paths) {
        if (!values.keySet().containsAll(paths)) throw new IllegalArgumentException("Unknown config path");
        return requireManagedFile().snapshotForEdit(paths);
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
        ForgeConfigFile file = requireManagedFile();
        ConfigEditSnapshot snapshot = file.snapshotForEdit(changes.keySet());
        if (compareAndSetAll(snapshot, changes) == ConfigCommitResult.CONFLICT) {
            throw new IllegalStateException("Config changed during batch edit");
        }
    }

    private ForgeConfigFile requireManagedFile() {
        ForgeConfigFile file = managedFile;
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
        ForgeConfigSpec.ValueSpec valueSpec = valueSpec(path);
        return valueSpec != null ? valueSpec.getDefault() : null;
    }

    @Override
    public boolean validate(String path, Object value) {
        ForgeConfigSpec.ValueSpec valueSpec = valueSpec(path);
        return valueSpec != null && valueSpec.test(value);
    }

    @Override
    public void save() {
        if (modConfig != null) {
            modConfig.save();
        } else if (managedFile != null) {
            managedFile.save();
        }
        if (managedFile != null) notifyExternalReload(managedFile);
    }

    void acceptReload() {
        if (holder == null) return;
        synchronized (holder) {
            ForgeConfigFile file = managedFile;
            // Native events and a local write can both observe the same external revision.
            if (file != null) notifyExternalReload(file);
            else holder.acceptExternalReload();
        }
    }

    private void notifyExternalReload(ForgeConfigFile file) {
        long revision = file.externalRevision();
        if (holder != null && revision != notifiedRevision) {
            notifiedRevision = revision;
            // No file monitor is held while subscribers run, including reentrant configuration readers.
            holder.acceptExternalReload();
        }
    }

    @Nullable
    private ForgeConfigSpec.ValueSpec valueSpec(String path) {
        ForgeConfigSpec.ConfigValue<?> value = values.get(path);
        if (value == null) {
            return null;
        }
        return spec.getSpec().get(value.getPath());
    }
}
