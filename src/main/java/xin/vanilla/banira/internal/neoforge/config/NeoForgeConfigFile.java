package xin.vanilla.banira.internal.neoforge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.utils.CommentedConfigWrapper;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import net.neoforged.neoforge.common.ModConfigSpec;
import xin.vanilla.banira.common.config.ConfigCommitResult;
import xin.vanilla.banira.common.config.ConfigEditSnapshot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;

/**
 * Keeps Forge's file lifecycle while isolating parsing and synchronous disk commits.
 */
public final class NeoForgeConfigFile extends CommentedConfigWrapper<CommentedConfig> implements CommentedFileConfig {
    private final CommentedFileConfig bootstrap;
    private final ModConfigSpec spec;
    private final Consumer<CommentedConfig> validator;
    private final ConfigWriter writer;
    private byte[] acceptedBytes;
    private boolean closed;
    private long externalRevision;
    private Consumer<CommentedConfig> nativePublisher = candidate -> {
    };

    synchronized void nativePublisher(Consumer<CommentedConfig> publisher) {
        nativePublisher = publisher;
    }

    NeoForgeConfigFile(CommentedFileConfig bootstrap, ModConfigSpec spec, Consumer<CommentedConfig> validator) {
        this(bootstrap, spec, validator, TomlFormat.instance().createWriter());
    }

    NeoForgeConfigFile(CommentedFileConfig bootstrap, ModConfigSpec spec, Consumer<CommentedConfig> validator,
                       ConfigWriter writer) {
        super(TomlFormat.instance().createConfig(LinkedHashMap::new));
        this.bootstrap = bootstrap;
        this.spec = spec;
        this.validator = validator;
        this.writer = writer;
    }

    synchronized boolean hasExternalChange() {
        return !closed && !Arrays.equals(acceptedBytes, readBytes());
    }

    synchronized long externalRevision() {
        return externalRevision;
    }

    synchronized boolean hasLoaded() {
        return acceptedBytes != null;
    }

    synchronized boolean isOpen() {
        return !closed;
    }

    public void saveOnUnload() {
        try {
            save();
        } catch (ParsingException | WritingException exception) {
            org.apache.logging.log4j.LogManager.getLogger().warn("Leaving externally changed config untouched while unloading {}", getNioPath(), exception);
        }
    }

    public boolean belongsTo(ModConfigSpec owner) {
        return owner == spec;
    }

    @Override
    public synchronized void load() {
        requireOpen();
        if (acceptedBytes == null && !Files.exists(getNioPath())) {
            // Preserve Forge's defaultconfig and missing-file policy before the first publication.
            bootstrap.load();
        }
        byte[] bytes = readBytes();
        if (!Arrays.equals(bytes, acceptedBytes)) {
            CommentedConfig candidate = parse(bytes);
            if (acceptedBytes == null) {
                CommentedConfig original = new TomlParser().parse(new String(bytes, StandardCharsets.UTF_8));
                ConfigWriter comparison = TomlFormat.instance().createWriter();
                if (!comparison.writeToString(candidate).equals(comparison.writeToString(original))) {
                    commit(candidate, bytes);
                    return;
                }
            }
            publishExternal(candidate, bytes);
        }
    }

    @Override
    public synchronized void save() {
        requireLoaded();
        byte[] disk = readBytes();
        if (!Arrays.equals(disk, acceptedBytes)) {
            // A delayed save must not turn an external edit back into an older in-memory snapshot.
            publishExternal(parse(disk), disk);
            return;
        }
        // Every mutation commits synchronously; an unchanged save has nothing left to flush.
    }

    private CommentedConfig parse(byte[] bytes) {
        CommentedConfig candidate = new TomlParser().parse(new String(bytes, StandardCharsets.UTF_8));
        try {
            validator.accept(candidate);
        } catch (ParsingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ParsingException("Invalid config structure in " + getNioPath(), exception);
        }
        return candidate;
    }

    private CommentedConfig candidateForEdit() {
        requireLoaded();
        byte[] disk = readBytes();
        if (!Arrays.equals(disk, acceptedBytes))
            throw new WritingException("External config change must be reloaded before editing: " + getNioPath());
        return copy(config);
    }

    private void commit(CommentedConfig candidate, byte[] expectedBytes) {
        validator.accept(candidate);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(getNioPath().toAbsolutePath().getParent(), ".banira-config-", ".tmp");
            writer.write(candidate, temporary, WritingMode.REPLACE, StandardCharsets.UTF_8);
            byte[] written = Files.readAllBytes(temporary);
            if (!Arrays.equals(expectedBytes, readBytes())) {
                throw new EditConflict("Config changed during save; keeping external file: " + getNioPath());
            }
            Files.move(temporary, getNioPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            temporary = null;
            publish(candidate, written);
        } catch (IOException exception) {
            throw new WritingException("Cannot commit config " + getNioPath(), exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Never replace the original file merely to remove an abandoned staging file.
                }
            }
        }
    }

    private void publish(CommentedConfig candidate, byte[] bytes) {
        config.clear();
        config.clearComments();
        config.putAll(candidate);
        config.putAllComments(candidate);
        acceptedBytes = bytes;
        nativePublisher.accept(copy(config));
        spec.afterReload();
    }

    private void publishExternal(CommentedConfig candidate, byte[] bytes) {
        boolean reloaded = acceptedBytes != null;
        publish(candidate, bytes);
        if (reloaded) externalRevision++;
    }

    private byte[] readBytes() {
        try {
            return Files.readAllBytes(getNioPath());
        } catch (IOException exception) {
            throw new ParsingException("Cannot read config " + getNioPath(), exception);
        }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Config is closed: " + getNioPath());
    }

    private void requireLoaded() {
        requireOpen();
        if (acceptedBytes == null) throw new IllegalStateException("Config has not been loaded: " + getNioPath());
    }

    public synchronized ConfigEditSnapshot snapshotForEdit(Set<String> paths) {
        requireLoaded();
        byte[] disk = readBytes();
        if (!Arrays.equals(disk, acceptedBytes)) {
            throw new IllegalStateException("Reload externally changed config before taking an edit snapshot");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String path : paths) {
            if (!config.contains(path)) throw new IllegalArgumentException("Unknown config path: " + path);
            values.put(path, copyValue(config.get(path)));
        }
        return new ConfigEditSnapshot(this, getNioPath().getFileName().toString(), disk, values);
    }

    public synchronized ConfigCommitResult compareAndSetAll(ConfigEditSnapshot expected, Map<String, Object> changes) {
        requireLoaded();
        if (!expected.belongsTo(this)) throw new IllegalArgumentException("Snapshot belongs to a different file");
        if (!expected.getValues().keySet().containsAll(changes.keySet())) {
            throw new IllegalArgumentException("Edit includes paths outside its snapshot");
        }
        byte[] original = expected.getSourceBytes();
        if (!Arrays.equals(original, acceptedBytes) || !Arrays.equals(original, readBytes())) {
            return ConfigCommitResult.CONFLICT;
        }
        CommentedConfig candidate = copy(config);
        boolean changed = false;
        for (Map.Entry<String, Object> edit : changes.entrySet()) {
            Object previous = candidate.set(edit.getKey(), copyValue(edit.getValue()));
            changed |= !Objects.deepEquals(previous, edit.getValue());
        }
        if (!changed) return ConfigCommitResult.UNCHANGED;
        try {
            commit(candidate, original);
            return ConfigCommitResult.APPLIED;
        } catch (EditConflict conflict) {
            return ConfigCommitResult.CONFLICT;
        }
    }

    private static final class EditConflict extends WritingException {
        EditConflict(String message) {
            super(message);
        }
    }

    @Override
    public synchronized <T> T set(List<String> path, Object value) {
        CommentedConfig candidate = candidateForEdit();
        T previous = candidate.set(path, copyValue(value));
        if (Objects.deepEquals(previous, value)) return previous;
        commit(candidate, acceptedBytes);
        return previous;
    }

    @Override
    public synchronized boolean add(List<String> path, Object value) {
        CommentedConfig candidate = candidateForEdit();
        boolean added = candidate.add(path, copyValue(value));
        if (added) commit(candidate, acceptedBytes);
        return added;
    }

    @Override
    public synchronized <T> T remove(List<String> path) {
        CommentedConfig candidate = candidateForEdit();
        T previous = candidate.remove(path);
        commit(candidate, acceptedBytes);
        return previous;
    }

    @Override
    public synchronized void clear() {
        candidateForEdit();
        commit(createSubConfig(), acceptedBytes);
    }

    @Override
    public synchronized String setComment(List<String> path, String comment) {
        CommentedConfig candidate = candidateForEdit();
        String previous = candidate.setComment(path, comment);
        commit(candidate, acceptedBytes);
        return previous;
    }

    @Override
    public synchronized String removeComment(List<String> path) {
        CommentedConfig candidate = candidateForEdit();
        String previous = candidate.removeComment(path);
        commit(candidate, acceptedBytes);
        return previous;
    }

    @Override
    public synchronized void clearComments() {
        CommentedConfig candidate = candidateForEdit();
        candidate.clearComments();
        commit(candidate, acceptedBytes);
    }

    @Override
    public synchronized void putAll(UnmodifiableConfig values) {
        CommentedConfig candidate = candidateForEdit();
        candidate.putAll(copy(values));
        commit(candidate, acceptedBytes);
    }

    @Override
    public synchronized void addAll(UnmodifiableConfig values) {
        CommentedConfig candidate = candidateForEdit();
        candidate.addAll(copy(values));
        commit(candidate, acceptedBytes);
    }

    @Override
    public synchronized void removeAll(UnmodifiableConfig values) {
        CommentedConfig candidate = candidateForEdit();
        candidate.removeAll(values);
        commit(candidate, acceptedBytes);
    }

    @Override
    public synchronized void putAllComments(UnmodifiableCommentedConfig comments) {
        CommentedConfig candidate = candidateForEdit();
        candidate.putAllComments(comments);
        commit(candidate, acceptedBytes);
    }

    @Override
    public synchronized void putAllComments(Map<String, UnmodifiableCommentedConfig.CommentNode> comments) {
        CommentedConfig candidate = candidateForEdit();
        candidate.putAllComments(comments);
        commit(candidate, acceptedBytes);
    }

    @Override
    public synchronized Map<String, UnmodifiableCommentedConfig.CommentNode> getComments() {
        return copy(config).getComments();
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T getRaw(List<String> path) {
        return (T) copyValue(config.getRaw(path));
    }

    @Override
    public synchronized boolean contains(List<String> path) {
        return config.contains(path);
    }

    @Override
    public synchronized boolean isNull(List<String> path) {
        return config.isNull(path);
    }

    @Override
    public synchronized int size() {
        return config.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public synchronized String getComment(List<String> path) {
        return config.getComment(path);
    }

    @Override
    public synchronized boolean containsComment(List<String> path) {
        return config.containsComment(path);
    }

    @Override
    public synchronized Map<String, Object> valueMap() {
        return copy(config).valueMap();
    }

    @Override
    public synchronized Map<String, String> commentMap() {
        return new LinkedHashMap<>(config.commentMap());
    }

    @Override
    public synchronized Set<? extends CommentedConfig.Entry> entrySet() {
        return copy(config).entrySet();
    }

    @Override
    public com.electronwill.nightconfig.core.concurrent.ConcurrentCommentedConfig createSubConfig() {
        return new com.electronwill.nightconfig.core.concurrent.SynchronizedConfig(TomlFormat.instance(), LinkedHashMap::new);
    }

    @Override
    public synchronized <R> R bulkCommentedRead(java.util.function.Function<? super UnmodifiableCommentedConfig, R> action) {
        return action.apply(copy(config).unmodifiable());
    }

    @Override
    public synchronized <R> R bulkCommentedUpdate(java.util.function.Function<? super CommentedConfig, R> action) {
        CommentedConfig candidate = candidateForEdit();
        R result = action.apply(candidate);
        commit(candidate, acceptedBytes);
        return result;
    }

    @Override
    public File getFile() {
        return bootstrap.getFile();
    }

    @Override
    public Path getNioPath() {
        return bootstrap.getNioPath();
    }

    @Override
    public synchronized void close() {
        closed = true;
        bootstrap.close();
    }

    static CommentedConfig copy(UnmodifiableConfig source) {
        CommentedConfig result = TomlFormat.instance().createConfig(LinkedHashMap::new);
        source.valueMap().forEach((key, value) -> result.valueMap().put(key, copyValue(value)));
        if (source instanceof UnmodifiableCommentedConfig) result.putAllComments((UnmodifiableCommentedConfig) source);
        return result;
    }

    private static Object copyValue(Object value) {
        if (value instanceof Config) return copy((Config) value);
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) result.add(copyValue(item));
            return result;
        }
        if (value instanceof Map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach((key, item) -> result.put(key, copyValue(item)));
            return result;
        }
        return value;
    }
}
