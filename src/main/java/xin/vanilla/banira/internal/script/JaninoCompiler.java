package xin.vanilla.banira.internal.script;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.util.resource.*;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import xin.vanilla.banira.api.script.*;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class JaninoCompiler {
    static final String NAMESPACE = "xin.vanilla.banira.generated.";

    Map<String, byte[]> compile(List<ScriptSourceGroup> groups, ClassLoader parent) {
        Map<String, byte[]> bytecodes = new HashMap<>();
        for (ScriptSourceGroup group : groups) {
            try {
                // JDK contracts use the bootstrap loader (null); Janino requires an actual loader object.
                ClassLoader lookup = parent == null ? new ClassLoader(null) { } : parent;
                Compiler compiler = new Compiler(ResourceFinder.EMPTY_RESOURCE_FINDER, new ClassLoaderIClassLoader(lookup));
                compiler.setSourceVersion(8);
                compiler.setTargetVersion(8);
                compiler.setSourceCharset(StandardCharsets.UTF_8);
                compiler.setDebugSource(true);
                compiler.setDebugLines(true);
                Map<String, byte[]> output = new HashMap<>();
                compiler.setClassFileCreator(new ResourceCreator() {
                    private final MapResourceCreator delegate = new MapResourceCreator(output);
                    private final Set<String> written = new HashSet<>();
                    @Override public OutputStream createResource(String name) {
                        if (!written.add(name)) throw new IllegalArgumentException("Duplicate generated class: " + name);
                        return delegate.createResource(name);
                    }
                    @Override public boolean deleteResource(String name) { return delegate.deleteResource(name); }
                });
                compiler.setClassFileFinder(ResourceFinder.EMPTY_RESOURCE_FINDER);
                List<Resource> resources = new ArrayList<>();
                group.getSourceFiles().forEach((name, source) -> resources.add(new StringResource(name, source)));
                compiler.compile(resources.toArray(new Resource[0]));
                for (Map.Entry<String, byte[]> entry : output.entrySet()) {
                    String path = entry.getKey();
                    String name = path.substring(0, path.length() - ".class".length()).replace('/', '.');
                    byte[] bytes = entry.getValue();
                    if (!name.startsWith(NAMESPACE) || bytes.length < 8 || bytes[6] != 0 || bytes[7] != 52) {
                        throw new IllegalArgumentException("Generated classes must use the script namespace and Java 8");
                    }
                    if (bytecodes.put(name, bytes) != null) {
                        throw new IllegalArgumentException("Duplicate generated class: " + name);
                    }
                }
                if (!output.containsKey(group.getEntryClassName().replace('.', '/') + ".class")) {
                    throw new IllegalArgumentException("Missing entrypoint: " + group.getEntryClassName());
                }
            } catch (CompileException error) {
                Location at = error.getLocation();
                throw new ScriptCompilationException(new ScriptDiagnostic(group.getId(),
                        at == null || at.getFileName() == null ? entryFile(group) : at.getFileName(),
                        at == null ? -1 : at.getLineNumber(), at == null ? -1 : at.getColumnNumber(),
                        "compile", error.getMessage()), error);
            } catch (Exception | LinkageError error) {
                throw failure(group, "compile", error);
            }
        }
        return bytecodes;
    }

    <T> Map<String, ScriptFactory<T>> factories(List<ScriptSourceGroup> groups, Map<String, byte[]> bytecodes,
                                               Class<T> contract, Runnable checkAccess) {
        ClassLoader loader = new ClassLoader(contract.getClassLoader()) {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] code = bytecodes.get(name);
                if (code == null) throw new ClassNotFoundException(name);
                return defineClass(name, code, 0, code.length);
            }
        };
        Map<String, ScriptFactory<T>> factories = new LinkedHashMap<>();
        for (ScriptSourceGroup group : groups) {
            try {
                Class<?> type = Class.forName(group.getEntryClassName(), false, loader);
                if (type.getClassLoader() != loader || !contract.isAssignableFrom(type)
                        || !Modifier.isPublic(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())) {
                    throw new IllegalArgumentException("Entrypoint must be a public concrete implementation of the contract");
                }
                Constructor<? extends T> constructor = type.asSubclass(contract).getConstructor();
                factories.put(group.getId(), () -> {
                    checkAccess.run();
                    try {
                        T instance = constructor.newInstance();
                        checkAccess.run();
                        return instance;
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                        throw failure(group, "instantiate", error);
                    }
                });
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                throw failure(group, "instantiate", error);
            }
        }
        return Collections.unmodifiableMap(factories);
    }

    private static String entryFile(ScriptSourceGroup group) {
        String simple = group.getEntryClassName().substring(group.getEntryClassName().lastIndexOf('.') + 1) + ".java";
        for (String file : group.getSourceFiles().keySet()) {
            if (file.equals(simple) || file.endsWith("/" + simple)) return file;
        }
        return group.getSourceFiles().isEmpty() ? "" : group.getSourceFiles().keySet().iterator().next();
    }

    private static ScriptCompilationException failure(ScriptSourceGroup group, String phase, Throwable error) {
        return new ScriptCompilationException(new ScriptDiagnostic(group.getId(), entryFile(group),
                -1, -1, phase, error.toString()), error);
    }
}
