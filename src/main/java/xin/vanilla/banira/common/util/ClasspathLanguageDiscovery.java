package xin.vanilla.banira.common.util;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** 在开发目录与真实 JAR 中统一枚举语言文件。 */
final class ClasspathLanguageDiscovery {
    private ClasspathLanguageDiscovery() {
    }

    static Set<String> discover(Class<?> anchor, String absoluteDirectory) {
        String directory = normalizeDirectory(absoluteDirectory);
        Set<String> result = new LinkedHashSet<>();
        try {
            collectKnownLanguage(anchor.getResource("/" + directory + "en_us.json"), directory, result);
        } catch (Exception ignored) {
        }
        try {
            Enumeration<URL> resources = anchor.getClassLoader().getResources(directory);
            while (resources.hasMoreElements()) {
                collect(resources.nextElement(), directory, result);
            }
        } catch (Exception ignored) {
        }
        try {
            URL location = anchor.getProtectionDomain().getCodeSource().getLocation();
            collect(location, directory, result);
        } catch (Exception ignored) {
        }
        return result;
    }

    private static void collectKnownLanguage(URL location, String directory, Set<String> result) {
        if (location == null) return;
        try {
            if ("jar".equalsIgnoreCase(location.getProtocol())) {
                try (JarFile jar = ((JarURLConnection) location.openConnection()).getJarFile()) {
                    collectJar(jar, directory, result);
                }
            } else if ("file".equalsIgnoreCase(location.getProtocol())) {
                Path file = Paths.get(location.toURI());
                collect(file.getParent().toUri().toURL(), directory, result);
            }
        } catch (Exception ignored) {
        }
    }

    static Set<String> discoverFromLocation(URL location, String absoluteDirectory) {
        Set<String> result = new LinkedHashSet<>();
        collect(location, normalizeDirectory(absoluteDirectory), result);
        return result;
    }

    private static void collect(URL location, String directory, Set<String> result) {
        if (location == null) return;
        try {
            if ("jar".equalsIgnoreCase(location.getProtocol())) {
                try (JarFile jar = ((JarURLConnection) location.openConnection()).getJarFile()) {
                    collectJar(jar, directory, result);
                }
                return;
            }
            if (!"file".equalsIgnoreCase(location.getProtocol())) return;
            Path path = Paths.get(location.toURI());
            if (Files.isDirectory(path)) {
                Path languageDirectory = path.resolve(directory);
                String normalizedPath = path.toString().replace('\\', '/');
                String normalizedDirectory = directory.substring(0, directory.length() - 1);
                if (!Files.isDirectory(languageDirectory)
                        && (normalizedPath.endsWith(directory) || normalizedPath.endsWith(normalizedDirectory))) {
                    languageDirectory = path;
                }
                if (Files.isDirectory(languageDirectory)) {
                    try (java.util.stream.Stream<Path> files = Files.list(languageDirectory)) {
                        files.map(value -> value.getFileName().toString())
                                .forEach(name -> addLanguage(name, result));
                    }
                }
            } else {
                try (JarFile jar = new JarFile(path.toFile())) {
                    collectJar(jar, directory, result);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void collectJar(JarFile jar, String directory, Set<String> result) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (name.startsWith(directory) && name.indexOf('/', directory.length()) < 0) {
                addLanguage(name.substring(directory.length()), result);
            }
        }
    }

    private static void addLanguage(String fileName, Set<String> result) {
        if (fileName.endsWith(".json")) {
            result.add(fileName.substring(0, fileName.length() - 5).toLowerCase(Locale.ROOT));
        }
    }

    private static String normalizeDirectory(String value) {
        String result = value == null ? "" : value.replace('\\', '/');
        while (result.startsWith("/")) result = result.substring(1);
        return result.endsWith("/") ? result : result + "/";
    }
}
