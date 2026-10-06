package xin.vanilla.banira.api.script;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One entrypoint and its mutually dependent source files, compiled together.
 */
public final class ScriptSourceGroup {
    private final String id;
    private final String entryClassName;
    private final Map<String, String> sourceFiles;

    public ScriptSourceGroup(String id, String entryClassName, Map<String, String> sourceFiles) {
        this.id = Objects.requireNonNull(id, "id");
        this.entryClassName = Objects.requireNonNull(entryClassName, "entryClassName");
        TreeMap<String, String> snapshot = new TreeMap<>();
        Objects.requireNonNull(sourceFiles, "sourceFiles").forEach((name, source) ->
                snapshot.put(Objects.requireNonNull(name, "filename"), Objects.requireNonNull(source, "source")));
        this.sourceFiles = Collections.unmodifiableMap(snapshot);
    }

    public String getId() {
        return id;
    }

    public String getEntryClassName() {
        return entryClassName;
    }

    public Map<String, String> getSourceFiles() {
        return sourceFiles;
    }
}
