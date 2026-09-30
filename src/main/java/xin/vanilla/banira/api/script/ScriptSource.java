package xin.vanilla.banira.api.script;
import lombok.Getter;
import java.util.Objects;
@Getter
public final class ScriptSource {
    private final String id;
    private final String className;
    private final String fileName;
    private final String source;
    public ScriptSource(String id, String className, String fileName, String source) {
        this.id = Objects.requireNonNull(id, "id");
        this.className = Objects.requireNonNull(className, "className");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.source = Objects.requireNonNull(source, "source");
    }
}
