package xin.vanilla.banira.api.script;
import xin.vanilla.banira.internal.script.DefaultScriptSession;
import java.util.concurrent.Executor;
/** Compiles trusted local Java code. This API is not a security sandbox. */
public final class BaniraScripts {
    private BaniraScripts() { }
    public static <T> ScriptSession<T> open(String ownerId, Class<T> contract, String apiVersion,
                                           ScriptLimits limits, Executor ownerExecutor) {
        return new DefaultScriptSession<>(ownerId, contract, apiVersion, limits, ownerExecutor);
    }
}
