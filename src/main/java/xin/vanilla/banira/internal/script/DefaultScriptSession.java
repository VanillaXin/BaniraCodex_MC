package xin.vanilla.banira.internal.script;

import xin.vanilla.banira.api.script.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

public final class DefaultScriptSession<T> implements ScriptSession<T> {
    private final String ownerId;
    private final String apiVersion;
    private final ScriptLimits limits;
    private Class<?> contract;
    private final boolean factoryMode;
    private final FactoryAccess factoryAccess = new FactoryAccess();
    private Executor ownerExecutor;
    private long requestId;
    private Request request;
    private Candidate<T> candidate;
    private String activeHash;
    private volatile Map<String, T> active = Collections.emptyMap();
    private boolean closed;

    public DefaultScriptSession(String ownerId, Class<T> contract, String apiVersion,
                                ScriptLimits limits, Executor ownerExecutor) {
        this(ownerId, contract, apiVersion, limits, ownerExecutor, false);
    }

    public static <C> ScriptSession<ScriptFactory<C>> factories(String ownerId, Class<C> contract,
            String apiVersion, ScriptLimits limits, Executor ownerExecutor) {
        return new DefaultScriptSession<>(ownerId, contract, apiVersion, limits, ownerExecutor, true);
    }

    private DefaultScriptSession(String ownerId, Class<?> contract, String apiVersion,
                                 ScriptLimits limits, Executor ownerExecutor, boolean factoryMode) {
        if (ownerId == null || ownerId.trim().isEmpty() || apiVersion == null || apiVersion.isEmpty()) {
            throw new IllegalArgumentException("An owner and API version are required");
        }
        if (!Objects.requireNonNull(contract, "contract").isInterface() || !Modifier.isPublic(contract.getModifiers())) {
            throw new IllegalArgumentException("Script contract must be a public interface");
        }
        this.ownerId = ownerId;
        this.apiVersion = apiVersion;
        this.contract = contract;
        this.factoryMode = factoryMode;
        this.limits = Objects.requireNonNull(limits, "limits");
        this.ownerExecutor = Objects.requireNonNull(ownerExecutor, "ownerExecutor");
    }

    @Override public CompletableFuture<PreparedScripts<T>> prepare(List<ScriptSource> sources) {
        try {
            if (sources == null) return prepareGroups(null);
            List<ScriptSourceGroup> groups = new ArrayList<>();
            for (ScriptSource source : sources) {
                groups.add(new ScriptSourceGroup(source.getId(), source.getClassName(),
                        Collections.singletonMap(source.getFileName(), source.getSource())));
            }
            return prepareGroups(groups);
        } catch (RuntimeException error) {
            // Invalid legacy input must also invalidate an earlier candidate.
            return prepareGroups(null);
        }
    }

    @Override public CompletableFuture<PreparedScripts<T>> prepareGroups(List<ScriptSourceGroup> sources) {
        Request old;
        CompletableFuture<PreparedScripts<T>> result;
        synchronized (this) {
            if (closed) return failed(new IllegalStateException("Script session is closed"));
            List<ScriptSourceGroup> snapshot = null;
            String hash = null;
            RuntimeException invalid = null;
            try {
                snapshot = validate(sources);
                hash = fingerprint(snapshot);
            } catch (RuntimeException error) { invalid = error; }
            if (invalid == null && request != null && !request.future.isDone() && hash.equals(request.hash)) {
                return request.future;
            }
            ++requestId;
            old = request;
            if (old != null) old.release();
            request = null;
            ScriptCompileQueue.SHARED.cancel(this);
            Candidate<T> previous = candidate;
            candidate = null;
            try {
                if (invalid != null) throw invalid;
                Map<String, T> cached = hash.equals(activeHash) ? active
                        : previous != null && hash.equals(previous.hash) ? previous.scripts : null;
                if (cached != null) {
                    candidate = new Candidate<>(requestId, hash, cached);
                    result = CompletableFuture.completedFuture(candidate);
                } else {
                    Request next = new Request(requestId, hash, snapshot, contract);
                    request = next;
                    // Session lifecycle completes superseded futures outside its monitor.
                    ScriptCompileQueue.SHARED.submit(this, () -> compile(next), () -> {});
                    result = next.future;
                }
            } catch (RuntimeException error) {
                if (request != null) request.release();
                request = null;
                result = failed(diagnostic(error, invalid == null ? "queue" : "validate"));
            }
        }
        cancel(old);
        return result;
    }

    private void compile(Request next) {
        try {
            Class<?> type;
            List<ScriptSourceGroup> sources;
            synchronized (this) {
                if (!current(next)) return;
                type = next.type;
                sources = next.sources;
            }
            Map<String, byte[]> compiled = new JaninoCompiler().compile(sources, type.getClassLoader());
            Executor executor;
            synchronized (this) {
                if (!current(next)) return;
                next.bytecodes = compiled;
                executor = ownerExecutor;
            }
            executor.execute(() -> instantiate(next));
        } catch (Exception | LinkageError error) { reject(next, error); }
    }

    private void instantiate(Request next) {
        try {
            List<ScriptSourceGroup> sources;
            Map<String, byte[]> bytecodes;
            Class<?> type;
            synchronized (this) {
                if (!current(next)) return;
                sources = next.sources;
                bytecodes = next.bytecodes;
                type = next.type;
            }
            if (factoryMode) factoryAccess.bindOwner();
            Map<String, T> values = materialize(sources, bytecodes, type);
            Candidate<T> prepared;
            synchronized (this) {
                if (!current(next)) return;
                prepared = new Candidate<>(next.id, next.hash, values);
                candidate = prepared;
                request = null;
                next.release();
            }
            next.future.complete(prepared);
        } catch (Exception | LinkageError error) { reject(next, error); }
    }

    @Override public synchronized boolean publish(PreparedScripts<T> prepared) {
        if (closed || prepared == null || prepared != candidate || candidate.id != requestId) return false;
        active = candidate.scripts;
        activeHash = candidate.hash;
        candidate = null;
        return true;
    }

    @Override public Map<String, T> active() { return active; }

    @SuppressWarnings("unchecked")
    private Map<String, T> materialize(List<ScriptSourceGroup> sources, Map<String, byte[]> bytecodes, Class<?> type) {
        Map<String, T> values = new LinkedHashMap<>();
        // Constructor/static factory fix the output type; keep the mode-dependent cast at this boundary.
        new JaninoCompiler().factories(sources, bytecodes, type, factoryAccess::check).forEach((id, factory) ->
                values.put(id, (T) (factoryMode ? factory : factory.create())));
        return Collections.unmodifiableMap(values);
    }

    @Override public void close() {
        Request old;
        synchronized (this) {
            if (closed) return;
            closed = true;
            factoryAccess.closed = true;
            ++requestId;
            old = request;
            request = null;
            if (old != null) old.release();
            ScriptCompileQueue.SHARED.cancel(this);
            candidate = null;
            active = Collections.emptyMap();
            activeHash = null;
            ownerExecutor = null;
            contract = null;
        }
        cancel(old);
    }

    private boolean current(Request next) {
        return !closed && request == next && requestId == next.id && !next.future.isDone();
    }

    private void cancel(Request next) {
        if (next != null) next.future.completeExceptionally(new CancellationException("Script request superseded or closed"));
    }

    private void reject(Request next, Throwable error) {
        synchronized (this) {
            if (request == next) request = null;
            next.release();
        }
        next.future.completeExceptionally(diagnostic(error, "schedule"));
    }

    private Throwable diagnostic(Throwable error, String phase) {
        if (error instanceof ScriptCompilationException) return error;
        return new ScriptCompilationException(new ScriptDiagnostic("", "", -1, -1, phase, error.toString()), error);
    }

    private List<ScriptSourceGroup> validate(List<ScriptSourceGroup> sources) {
        if (sources == null || sources.size() > limits.getScriptCount()) {
            throw new IllegalArgumentException("Script count exceeds limit");
        }
        List<ScriptSourceGroup> snapshot = new ArrayList<>(sources);
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        long bytes = 0;
        int files = 0;
        for (ScriptSourceGroup source : snapshot) {
            Objects.requireNonNull(source, "source");
            if (!source.getId().matches("[A-Za-z0-9_-]{1,128}") || !ids.add(source.getId())
                    || !source.getEntryClassName().startsWith(JaninoCompiler.NAMESPACE)
                    || !source.getEntryClassName().matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+")
                    || !names.add(source.getEntryClassName())) {
                throw new IllegalArgumentException("Invalid or duplicate script ID/entrypoint");
            }
            if (source.getSourceFiles().isEmpty() || source.getSourceFiles().size() > limits.getScriptCount() - files) {
                throw new IllegalArgumentException("Source file count exceeds limit or group is empty");
            }
            files += source.getSourceFiles().size();
            for (Map.Entry<String, String> file : source.getSourceFiles().entrySet()) {
                if (file.getKey().isEmpty() || file.getKey().length() > 256 || file.getValue().length() > limits.getSourceBytes()) {
                    throw new IllegalArgumentException("Invalid filename or source exceeds byte limit");
                }
                int size = file.getValue().getBytes(StandardCharsets.UTF_8).length;
                bytes += size;
                if (size > limits.getSourceBytes() || bytes > limits.getBatchBytes()) {
                    throw new IllegalArgumentException("Script source byte limit exceeded");
                }
            }
        }
        return Collections.unmodifiableList(snapshot);
    }

    private String fingerprint(List<ScriptSourceGroup> sources) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hashPart(digest, ownerId);
            hashPart(digest, apiVersion);
            hashPart(digest, factoryMode ? "factory" : "instance");
            for (ScriptSourceGroup source : sources) {
                hashPart(digest, source.getId());
                hashPart(digest, source.getEntryClassName());
                hashPart(digest, Integer.toString(source.getSourceFiles().size()));
                source.getSourceFiles().forEach((name, code) -> { hashPart(digest, name); hashPart(digest, code); });
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    private void hashPart(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte) (bytes.length >>> shift));
        digest.update(bytes);
    }

    private static <R> CompletableFuture<R> failed(Throwable error) {
        CompletableFuture<R> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }

    private final class Request {
        final long id;
        final String hash;
        final CompletableFuture<PreparedScripts<T>> future = new CompletableFuture<>();
        List<ScriptSourceGroup> sources;
        Class<?> type;
        Map<String, byte[]> bytecodes;
        Request(long id, String hash, List<ScriptSourceGroup> sources, Class<?> type) {
            this.id = id; this.hash = hash; this.sources = sources; this.type = type;
        }
        void release() { sources = null; type = null; bytecodes = null; }
    }

    private static final class Candidate<T> implements PreparedScripts<T> {
        final long id;
        final String hash;
        final Map<String, T> scripts;
        Candidate(long id, String hash, Map<String, T> scripts) { this.id = id; this.hash = hash; this.scripts = scripts; }
        @Override public Map<String, T> scripts() { return scripts; }
    }

    private static final class FactoryAccess {
        volatile boolean closed;
        private Thread owner;
        synchronized void bindOwner() {
            if (owner == null) owner = Thread.currentThread();
            check();
        }
        void check() {
            if (closed) throw new IllegalStateException("Script session is closed");
            if (owner != null && owner != Thread.currentThread()) {
                throw new IllegalStateException("Script instances must be created on the owner thread");
            }
        }
    }
}
