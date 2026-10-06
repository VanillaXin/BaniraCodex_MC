package xin.vanilla.banira.api.script;

import lombok.Getter;

@Getter
public final class ScriptDiagnostic {
    private final String scriptId;
    private final String fileName;
    private final int line;
    private final int column;
    private final String phase;
    private final String message;

    public ScriptDiagnostic(String scriptId, String fileName, int line, int column, String phase, String message) {
        this.scriptId = scriptId;
        this.fileName = fileName;
        this.line = line;
        this.column = column;
        this.phase = phase;
        this.message = message;
    }
}
