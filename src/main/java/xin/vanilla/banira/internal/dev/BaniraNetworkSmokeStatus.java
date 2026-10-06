package xin.vanilla.banira.internal.dev;

import xin.vanilla.banira.api.BaniraEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 开发期跨进程联机烟测状态文件。
 */
public final class BaniraNetworkSmokeStatus {
    private BaniraNetworkSmokeStatus() {
    }

    public static boolean enabled() {
        return !BaniraEnvironment.isProduction() && Boolean.getBoolean("banira.networkSmoke");
    }

    public static String phase() {
        return System.getProperty("banira.networkSmoke.phase", "").trim();
    }

    public static synchronized void append(String line) {
        String configured = System.getProperty("banira.networkSmoke.status", "").trim();
        if (configured.isEmpty()) throw new IllegalStateException("Missing banira.networkSmoke.status");
        try {
            Path path = Paths.get(configured);
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(path, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to write network smoke status", error);
        }
    }
}
