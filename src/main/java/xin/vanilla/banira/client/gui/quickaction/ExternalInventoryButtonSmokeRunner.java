package xin.vanilla.banira.client.gui.quickaction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.mojang.blaze3d.platform.NativeImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/** 仅由显式开发参数启用的外部背包按钮截图烟测。 */
public final class ExternalInventoryButtonSmokeRunner {
    private static final String ENABLED_PROPERTY = "banira.externalButtonsSmoke";
    private static final String REQUESTED_HOST =
            System.getProperty(ENABLED_PROPERTY, "").trim();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SETTLE_TICKS = 30;
    private static final int CAPTURE_TICKS = 5;
    private static boolean started;
    private static boolean hostApplied;
    private static boolean finished;
    private static int ticks;

    private ExternalInventoryButtonSmokeRunner() {
    }

    public static void onClientTick() {
        if (REQUESTED_HOST.isEmpty() || finished) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        if (!started) {
            started = true;
            minecraft.setScreen(new InventoryScreen(minecraft.player));
            LOGGER.info("External inventory button smoke opened the inventory screen");
            return;
        }
        boolean configured = "CONFIGURED".equalsIgnoreCase(REQUESTED_HOST);
        EnumExternalInventoryButtonHost requested = configured
                ? null : requestedHost(REQUESTED_HOST);
        ExternalInventoryButtonManager manager = ExternalInventoryButtonManager.get();
        if (!hostApplied) {
            if (++ticks < SETTLE_TICKS) return;
            if (configured) {
                manager.refreshCurrentScreen();
            } else {
                manager.refresh(requested, minecraft.screen);
            }
            hostApplied = true;
            ticks = 0;
            return;
        }
        if (++ticks < CAPTURE_TICKS) return;

        EnumExternalInventoryButtonHost effective = manager.effectiveHost();
        String screenshot = writeScreenshot(minecraft, effective);
        String status = screenshot.startsWith("ERROR:") ? "FAILED" : "FINISHED";
        writeResult("requestedHost=" + (configured ? "CONFIGURED" : requested) + '\n'
                + manager.diagnosticSnapshot(minecraft.screen)
                + "screenshot=" + screenshot + '\n'
                + status + '\n');
        finished = true;
    }

    private static EnumExternalInventoryButtonHost requestedHost(String value) {
        try {
            return EnumExternalInventoryButtonHost.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EnumExternalInventoryButtonHost.BANIRA;
        }
    }

    private static void writeResult(String content) {
        try {
            Path directory = Paths.get("screenshots", "external-buttons-smoke");
            Files.createDirectories(directory);
            Path result = directory.resolve("state.txt");
            Files.write(result, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOGGER.info("External inventory button smoke FINISHED: {}", result.toAbsolutePath());
        } catch (Exception exception) {
            LOGGER.error("Unable to write external inventory button smoke result", exception);
        }
    }

    private static String writeScreenshot(Minecraft minecraft,
                                          EnumExternalInventoryButtonHost host) {
        Path path = Paths.get("screenshots", "external-buttons-smoke",
                host.name().toLowerCase(java.util.Locale.ROOT) + ".png");
        try {
            Files.createDirectories(path.getParent());
            NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget());
            try {
                image.writeToFile(path);
            } finally {
                image.close();
            }
            return path.toString().replace('\\', '/');
        } catch (Exception exception) {
            LOGGER.error("Unable to capture external inventory button smoke screenshot", exception);
            return "ERROR:" + exception.getClass().getName() + ':' + exception.getMessage();
        }
    }
}
