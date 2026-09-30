package xin.vanilla.banira.internal.script;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.SimpleCompiler;
import xin.vanilla.banira.api.script.*;
import java.util.*;

final class JaninoCompiler {
    static final String NAMESPACE = "xin.vanilla.banira.generated.";

    Map<String, byte[]> compile(List<ScriptSource> sources, ClassLoader parent) {
        Map<String, byte[]> bytecodes = new HashMap<>();
        for (ScriptSource source : sources) {
            try {
                SimpleCompiler compiler = new SimpleCompiler();
                compiler.setParentClassLoader(parent);
                compiler.setSourceVersion(8);
                compiler.setTargetVersion(8);
                compiler.cook(source.getFileName(), source.getSource());
                for (Map.Entry<String, byte[]> entry : compiler.getBytecodes().entrySet()) {
                    byte[] bytes = entry.getValue();
                    if (!entry.getKey().startsWith(NAMESPACE) || bytes.length < 8 || bytes[6] != 0 || bytes[7] != 52) {
                        throw new IllegalArgumentException("Generated classes must use the script namespace and Java 8");
                    }
                    if (bytecodes.put(entry.getKey(), bytes) != null) {
                        throw new IllegalArgumentException("Duplicate generated class: " + entry.getKey());
                    }
                }
                if (!bytecodes.containsKey(source.getClassName())) {
                    throw new IllegalArgumentException("Missing entrypoint: " + source.getClassName());
                }
            } catch (CompileException error) {
                Location at = error.getLocation();
                throw new ScriptCompilationException(new ScriptDiagnostic(source.getId(), source.getFileName(),
                        at == null ? -1 : at.getLineNumber(), at == null ? -1 : at.getColumnNumber(),
                        "compile", error.getMessage()), error);
            } catch (Exception | LinkageError error) {
                throw failure(source, "compile", error);
            }
        }
        return bytecodes;
    }

    <T> Map<String, T> instantiate(List<ScriptSource> sources, Map<String, byte[]> bytecodes, Class<T> contract) {
        ClassLoader loader = new ClassLoader(contract.getClassLoader()) {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] code = bytecodes.get(name);
                if (code == null) throw new ClassNotFoundException(name);
                return defineClass(name, code, 0, code.length);
            }
        };
        Map<String, T> instances = new LinkedHashMap<>();
        for (ScriptSource source : sources) {
            try {
                Class<?> type = loader.loadClass(source.getClassName());
                if (type.getClassLoader() != loader || !contract.isAssignableFrom(type)) {
                    throw new IllegalArgumentException("Entrypoint does not implement the session contract");
                }
                instances.put(source.getId(), contract.cast(type.getConstructor().newInstance()));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                throw failure(source, "instantiate", error);
            }
        }
        return Collections.unmodifiableMap(instances);
    }

    private ScriptCompilationException failure(ScriptSource source, String phase, Throwable error) {
        return new ScriptCompilationException(new ScriptDiagnostic(source.getId(), source.getFileName(),
                -1, -1, phase, error.toString()), error);
    }
}
