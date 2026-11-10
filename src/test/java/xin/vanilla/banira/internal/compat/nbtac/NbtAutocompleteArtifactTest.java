package xin.vanilla.banira.internal.compat.nbtac;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class NbtAutocompleteArtifactTest {
    @org.junit.Rule
    public org.junit.rules.TemporaryFolder sandbox = new org.junit.rules.TemporaryFolder();

    @Test
    public void publishedArtifactExposesThePublicComponentParserWithoutModernApi() throws Exception {
        try (URLClassLoader loader = officialLoader()) {
            try {
                Class.forName("net.mt1006.nbtac.api.NBTacAPI", false, loader);
                fail("The pinned Forge 1.3.10 artifact must exercise the legacy component path");
            } catch (ClassNotFoundException expected) {
            }
            String root = "com.mt1006.nbt_ac.autocomplete.";
            Class<?> manager = Class.forName(root + "NbtSuggestionManager", false, loader);
            var method = manager.getMethod("finishSuggestions", Class.forName(root + "SuggestionList", false, loader),
                    SuggestionsBuilder.class, Class.forName(root + "CustomTagParser$Suggestion", false, loader), int.class);
            assertEquals(loader, manager.getClassLoader());
            assertTrue(Modifier.isPublic(method.getModifiers()));
            assertTrue(Modifier.isStatic(method.getModifiers()));
            assertTrue(CompletableFuture.class.isAssignableFrom(method.getReturnType()));
            assertNotNull(NbtAutocomplete.bind(loader, "item/minecraft:custom_data"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void officialParserCompletesCustomDataAndPreservesTheSuffix() throws Exception {
        xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.bootstrapGame();
        try (AutoCloseable client = headlessClientDirectory(); URLClassLoader loader = officialLoader();
             AutoCloseable platform = headlessUpstreamPlatform(loader)) {
            String root = "com.mt1006.nbt_ac.autocomplete.";
            Class<?> suggestion = Class.forName(root + "suggestions.NbtSuggestion", false, loader);
            Class<?> type = Class.forName(root + "suggestions.NbtSuggestion$Type", false, loader);
            var constructor = suggestion.getConstructor(String.class, type);
            Object component = constructor.newInstance("minecraft:custom_data", type.getField("COMPOUND").get(null));
            Object tag = constructor.newInstance("banira_flag", type.getField("INT").get(null));
            Object fields = suggestion.getMethod("getSubcompound").invoke(component);
            fields.getClass().getMethod("add", suggestion).invoke(fields, tag);
            Map<String, Object> components = (Map<String, Object>) Class.forName(root + "DataComponentManager", false, loader)
                    .getField("componentMap").get(null);
            Object previous = components.put("item/minecraft:custom_data", component);
            var ready = Class.forName(root + "loader.Loader", false, loader).getField("finished");
            boolean wasReady = ready.getBoolean(null);
            ready.setBoolean(null, true);
            try {
                assertEquals(loader, component.getClass().getClassLoader());
                var provider = NbtAutocomplete.bind(loader, "item/minecraft:custom_data", "minecraft:diamond_sword");
                assertNotNull(provider);
                String input = "{ban,keep:1}";
                Class<?> listType = Class.forName(root + "SuggestionList", false, loader);
                Class<?> parserClass = Class.forName(root + "CustomTagParser", false, loader);
                Class<?> parserType = Class.forName(root + "CustomTagParser$Type", false, loader);
                Object list = listType.getConstructor().newInstance();
                Object parser = parserClass.getConstructor(String.class, parserType)
                        .newInstance(input.substring(0, 4), parserType.getField("COMPONENT").get(null));
                Object parsed = parserClass.getMethod("read", listType, suggestion, String.class)
                        .invoke(parser, list, component, "minecraft:diamond_sword");
                int offset = (int) parserClass.getMethod("getCursor").invoke(parser);
                var future = (CompletableFuture<?>) Class.forName(root + "NbtSuggestionManager", false, loader)
                        .getMethod("finishSuggestions", listType, SuggestionsBuilder.class,
                                Class.forName(root + "CustomTagParser$Suggestion", false, loader), int.class)
                        .invoke(null, list, new SuggestionsBuilder(input.substring(0, 4), 0), parsed, offset);
                var raw = (com.mojang.brigadier.suggestion.Suggestions) future.get(3, TimeUnit.SECONDS);
                assertFalse("The upstream public component path must execute successfully", raw.isEmpty());
                var result = provider.complete(input, 4).get(3, TimeUnit.SECONDS);
                assertFalse("The real upstream parser must return a custom-data candidate", result.isEmpty());
                var completed = result.stream().filter(value -> value.text().contains("banira_flag")).findFirst().orElseThrow();
                assertEquals(1, completed.start());
                assertEquals(4, completed.end());
                assertTrue(completed.apply(input).endsWith(",keep:1}"));
                assertFalse(result.stream().anyMatch(value -> value.text().contains("Damage")));
            } finally {
                ready.setBoolean(null, wasReady);
                if (previous == null) components.remove("item/minecraft:custom_data");
                else components.put("item/minecraft:custom_data", previous);
            }
        }
    }

    private URLClassLoader officialLoader() throws Exception {
        String path = System.getProperty("banira.nbtacApiJar");
        Assume.assumeNotNull(path);
        return new URLClassLoader(new URL[]{new File(path).toURI().toURL()}, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("net.mt1006.nbtac.") && !name.startsWith("com.mt1006.nbt_ac."))
                    return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) loaded = findClass(name);
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        };
    }

    private AutoCloseable headlessClientDirectory() throws Exception {
        Class<?> minecraft = net.minecraft.client.Minecraft.class;
        var singleton = java.util.Arrays.stream(minecraft.getDeclaredFields())
                .filter(field -> field.getType() == minecraft && Modifier.isStatic(field.getModifiers()))
                .findFirst().orElseThrow();
        singleton.setAccessible(true);
        Object previous = singleton.get(null);
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        var access = unsafeType.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Object client = unsafeType.getMethod("allocateInstance", Class.class).invoke(access.get(null), minecraft);
        var directory = minecraft.getDeclaredField("gameDirectory");
        directory.setAccessible(true);
        directory.set(client, sandbox.newFolder("nbtac-context"));
        singleton.set(null, client);
        return () -> singleton.set(null, previous);
    }

    private AutoCloseable headlessUpstreamPlatform(ClassLoader loader) throws Exception {
        var platform = Class.forName("com.mt1006.nbt_ac.NBTac", false, loader).getField("loaderInterface");
        Object previous = platform.get(null);
        Object adapter = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{platform.getType()},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "isFabric" -> false;
                    case "toString" -> "headless-forge-platform";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new AssertionError("Unexpected upstream platform call: " + method);
                });
        platform.set(null, adapter);
        return () -> platform.set(null, previous);
    }
}
