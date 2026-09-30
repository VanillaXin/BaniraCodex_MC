package xin.vanilla.banira.api.script;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
/**
 * One owner's compilation lifecycle. Close when its world or owner stops.
 * Never block the owner executor waiting for prepare: constructors run there.
 * Retaining returned candidates or instances also retains generated class loaders.
 */
public interface ScriptSession<T> extends AutoCloseable {
    CompletableFuture<PreparedScripts<T>> prepare(List<ScriptSource> sources);
    default CompletableFuture<PreparedScripts<T>> prepareGroups(List<ScriptSourceGroup> sources) {
        CompletableFuture<PreparedScripts<T>> result = new CompletableFuture<>();
        result.completeExceptionally(new UnsupportedOperationException("Source groups are not supported by this session"));
        return result;
    }
    boolean publish(PreparedScripts<T> candidate);
    Map<String, T> active();
    @Override void close();
}
