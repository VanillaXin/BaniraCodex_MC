package xin.vanilla.banira.common.config;

import java.security.*;
import java.util.*;

/** Local-only file revision used for backups and compare-and-commit edits. */
public final class ConfigEditSnapshot {
    private final Object owner;
    private final String fileName;
    private final byte[] sourceBytes;
    private final String revision;
    private final Map<String, Object> values;

    public ConfigEditSnapshot(Object owner, String fileName, byte[] sourceBytes, Map<String, Object> values) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.sourceBytes = sourceBytes.clone();
        this.values = immutableValues(values);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(this.sourceBytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            revision = hex.toString();
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    public boolean belongsTo(Object owner) { return this.owner == owner; }
    public String getFileName() { return fileName; }
    public byte[] getSourceBytes() { return sourceBytes.clone(); }
    public String getRevision() { return revision; }
    public Map<String, Object> getValues() { return values; }

    static Map<String, Object> immutableValues(Map<String, Object> values) {
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "path"), freeze(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object freeze(Object value) {
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<?>) value) copy.add(freeze(item));
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach((key, item) -> copy.put(key, freeze(item)));
            return Collections.unmodifiableMap(copy);
        }
        return value;
    }
}
