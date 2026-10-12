package xin.vanilla.banira.internal.client.compat.jei;

import org.junit.Test;
import java.lang.reflect.Method;
import java.util.Optional;
import static org.junit.Assert.*;

public class JeiRuntimeAccessTest {
    private Class<?> helper() {
        try { return Class.forName("xin.vanilla.banira.internal.client.compat.jei.JeiRuntimeAccess"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("Missing JEI runtime compatibility boundary", missing); }
    }
    private boolean enabled(Object config) throws Exception {
        return (Boolean) helper().getMethod("lookupHistoryEnabled", Object.class).invoke(null, config);
    }
    public interface OldConfig { boolean isLookupHistoryEnabled(); }
    private static final class Old implements OldConfig {
        boolean enabled;
        public boolean isLookupHistoryEnabled() { return enabled; }
    }
    public interface Value<T> { T get(); }
    private static final class Box implements Value<Boolean> {
        boolean enabled;
        public Boolean get() { return enabled; }
    }
    public interface NewConfig { Value<Boolean> lookupHistoryEnabled(); }
    private static final class New implements NewConfig {
        final Box value = new Box();
        public Value<Boolean> lookupHistoryEnabled() { return value; }
    }
    public static final class Direct { public boolean lookupHistoryEnabled() { return true; } }
    public static final class Wrong { public String lookupHistoryEnabled() { return "true"; } }
    public static final class Broken { public boolean isLookupHistoryEnabled() { throw new IllegalStateException("fixture"); } }
    @Test public void oldBooleanGetterReadsLiveValue() throws Exception {
        Old value = new Old(); assertFalse(enabled(value)); value.enabled = true; assertTrue(enabled(value));
    }
    @Test public void newConfigValueReadsLiveValueThroughPublicInterfaces() throws Exception {
        New config = new New(); assertFalse(enabled(config)); config.value.enabled = true; assertTrue(enabled(config));
        config.value.enabled = false; assertFalse(enabled(config));
    }
    @Test public void directUnprefixedBooleanIsAccepted() throws Exception { assertTrue(enabled(new Direct())); }
    @Test public void absentNullInvalidAndFailingConfigDoNotCrash() throws Exception {
        assertFalse(enabled(null)); assertFalse(enabled(new Object())); assertFalse(enabled(new Wrong())); assertFalse(enabled(new Broken()));
    }
    private Object input(ClassLoader loader) throws Exception {
        return helper().getMethod("mouseInput", ClassLoader.class).invoke(null,loader);
    }
    @Test public void movedAndLegacyInputFactoriesCreateExecuteMouseClicks() throws Exception {
        for (String pkg : new String[]{"mezz.jei.common.input","mezz.jei.gui.input"}) {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("jei-input-fixture");
            try {
                java.util.List<String> files = new java.util.ArrayList<>();
                String api = "mezz.jei.api.gui.inputs";
                java.util.Map<String,String> sources = new java.util.LinkedHashMap<>();
                sources.put(api+".IJeiUserInput","package "+api+"; public interface IJeiUserInput {}");
                sources.put(pkg+".InputType","package "+pkg+"; public enum InputType { EXECUTE, SIMULATE }");
                sources.put(pkg+".UserInput","package "+pkg+"; public class UserInput implements "+api+".IJeiUserInput {"
                        +" public double x,y; public int button; public InputType type;"
                        +" public static java.util.Optional<UserInput> fromVanilla(double x,double y,int button,InputType type) {"
                        +" UserInput input=new UserInput();input.x=x;input.y=y;input.button=button;input.type=type;return java.util.Optional.of(input); } }");
                for (java.util.Map.Entry<String,String> source : sources.entrySet()) {
                    java.nio.file.Path file = dir.resolve(source.getKey().replace('.','/')+".java");
                    java.nio.file.Files.createDirectories(file.getParent());
                    java.nio.file.Files.write(file,source.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    files.add(file.toString());
                }
                java.util.List<String> args = new java.util.ArrayList<>(java.util.Arrays.asList("-encoding","UTF-8","-d",dir.toString()));
                args.addAll(files);
                javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
                assertNotNull(compiler);
                assertEquals(0,compiler.run(null,null,null,args.toArray(new String[0])));
                try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{dir.toUri().toURL()},null)) {
                    Object value = input(loader);assertNotNull(value);
                    Class<?> type=value.getClass();
                    assertEquals(pkg+".UserInput",type.getName());
                    assertEquals(0,type.getField("button").getInt(value));
                    assertEquals(0,type.getField("x").getDouble(value),0);
                    assertEquals(0,type.getField("y").getDouble(value),0);
                    assertEquals("EXECUTE",type.getField("type").get(value).toString());
                }
            } finally {
                try (java.util.stream.Stream<java.nio.file.Path> files=java.nio.file.Files.walk(dir)) {
                    files.sorted(java.util.Comparator.reverseOrder()).forEach(path->{try{java.nio.file.Files.delete(path);}catch(java.io.IOException e){throw new IllegalStateException(e);}});
                }
            }
        }
    }
    @Test public void unavailableFactoryReturnsNull() throws Exception {
        assertNull(input(new ClassLoader(null) {}));
    }
}
