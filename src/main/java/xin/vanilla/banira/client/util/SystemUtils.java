package xin.vanilla.banira.client.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 系统工具类
 */
public final class SystemUtils {
    private SystemUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();


    /**
     * 打开指定路径的文件夹
     */
    public static void openFileInFolder(Path path) {
        try {
            if (Files.isDirectory(path)) {
                // 如果是文件夹，直接打开文件夹
                openFolder(path);
            } else if (Files.isRegularFile(path)) {
                // 如果是文件，打开文件所在的文件夹，并选中文件
                openFolderAndSelectFile(path);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to open file/folder: ", e);
        }
    }

    private static void openFolder(Path path) {
        try {
            // Windows
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("explorer.exe", path.toString()).start();
            }
            // macOS
            else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                new ProcessBuilder("open", path.toString()).start();
            }
            // Linux
            else {
                new ProcessBuilder("xdg-open", path.toString()).start();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to open folder: ", e);
        }
    }

    private static void openFolderAndSelectFile(Path file) {
        try {
            // Windows
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("explorer.exe", "/select,", file.toString()).start();
            }
            // macOS
            else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                new ProcessBuilder("open", "-R", file.toString()).start();
            }
            // Linux
            else {
                new ProcessBuilder("xdg-open", "--select", file.toString()).start();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to open folder and select file: ", e);
        }
    }

    /**
     * 获取用户主目录
     */
    public static Path getUserHome() {
        try {
            // 尝试获取用户主目录
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                return Paths.get(userHome);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get user home directory", e);
        }
        // 如果获取失败，返回当前工作目录
        return Paths.get(".");
    }
}
