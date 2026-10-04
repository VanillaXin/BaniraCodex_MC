package xin.vanilla.banira.api.script;
import lombok.Getter;
@Getter
public final class ScriptLimits {
    private final int sourceBytes;
    private final int batchBytes;
    private final int scriptCount;
    public ScriptLimits(int sourceBytes, int batchBytes, int scriptCount) {
        if (sourceBytes < 1 || batchBytes < sourceBytes || scriptCount < 1) {
            throw new IllegalArgumentException("Invalid script limits");
        }
        this.sourceBytes = sourceBytes;
        this.batchBytes = batchBytes;
        this.scriptCount = scriptCount;
    }
    public static ScriptLimits defaults() { return new ScriptLimits(256 * 1024, 8 * 1024 * 1024, 128); }
}
