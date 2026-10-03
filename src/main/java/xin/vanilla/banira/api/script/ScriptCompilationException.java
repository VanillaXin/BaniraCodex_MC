package xin.vanilla.banira.api.script;

import java.util.Collections;
import java.util.List;

public final class ScriptCompilationException extends RuntimeException {
    private final List<ScriptDiagnostic> diagnostics;

    public ScriptCompilationException(ScriptDiagnostic diagnostic, Throwable cause) {
        super(diagnostic.getMessage(), cause);
        this.diagnostics = Collections.singletonList(diagnostic);
    }

    public List<ScriptDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
