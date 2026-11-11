package xin.vanilla.banira.internal.compat.nbtac;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class NbtAutocompleteArtifactTest {
    @Test
    public void publishedArtifactExposesTheExpectedPublicApi() throws Exception {
        String path = System.getProperty("banira.nbtacApiJar");
        Assume.assumeNotNull(path);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{new File(path).toURI().toURL()}, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("net.mt1006.nbtac.")) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) loaded = findClass(name);
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        }) {
            Class<?> api = Class.forName("net.mt1006.nbtac.api.NBTacAPI", false, loader);
            var method = NbtAutocomplete.itemDataMethod(api);
            assertEquals(loader, api.getClassLoader());
            assertTrue(Modifier.isPublic(method.getModifiers()));
            assertTrue(Modifier.isStatic(method.getModifiers()));
            assertTrue(CompletableFuture.class.isAssignableFrom(method.getReturnType()));
            assertNotNull(NbtAutocomplete.bind(loader, "item/minecraft:custom_data"));
        }
    }
}
