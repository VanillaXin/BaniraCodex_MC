package xin.vanilla.banira.internal.compat.nbtac;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.Assert.*;

public class NbtAutocompleteArtifactTest {
    @Test
    public void publishedArtifactExposesARecognizedPublicEntryPoint() throws Exception {
        String path = System.getProperty("banira.nbtacApiJar");
        Assume.assumeNotNull(path);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{new File(path).toURI().toURL()}, getClass().getClassLoader()) {
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
        }) {
            Class<?> api = Class.forName("com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager", false, loader);
            var method = api.getMethod("loadFromName", String.class, String.class, SuggestionsBuilder.class, boolean.class);
            assertEquals(loader, api.getClassLoader());
            assertTrue(Modifier.isPublic(method.getModifiers()));
            assertTrue(Modifier.isStatic(method.getModifiers()));
            assertTrue(CompletableFuture.class.isAssignableFrom(method.getReturnType()));
            assertNotNull(NbtAutocomplete.bind(loader, "item/minecraft:diamond_sword"));
        }
    }
}
