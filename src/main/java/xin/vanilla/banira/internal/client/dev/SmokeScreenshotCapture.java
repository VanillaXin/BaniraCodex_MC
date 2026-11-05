package xin.vanilla.banira.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/** Screenshots requested during extraction must wait for the completed GUI frame. */
public final class SmokeScreenshotCapture {
    private static final Queue<Request> REQUESTS = new ArrayDeque<>();
    private static final ArrayList<CompletableFuture<Void>> PENDING = new ArrayList<>();

    private SmokeScreenshotCapture() {
    }

    public static CompletableFuture<Void> capture(Path path) {
        var future = new CompletableFuture<Void>();
        REQUESTS.add(new Request(path, future));
        PENDING.add(future);
        return future;
    }

    public static boolean ready() {
        boolean ready = true;
        for (var iterator = PENDING.iterator(); iterator.hasNext();) {
            var future = iterator.next();
            if (!future.isDone()) ready = false;
            else {
                future.join();
                iterator.remove();
            }
        }
        return ready;
    }

    public static void afterFrame() {
        while (!REQUESTS.isEmpty()) {
            Request request = REQUESTS.remove();
            try {
                Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), image -> {
                    try (image) {
                        Path parent = request.path.toAbsolutePath().getParent();
                        Files.createDirectories(parent);
                        image.writeToFile(request.path);
                        request.future.complete(null);
                    } catch (Throwable failure) {
                        request.future.completeExceptionally(failure);
                    }
                });
            } catch (Throwable failure) {
                request.future.completeExceptionally(failure);
            }
        }
    }

    private record Request(Path path, CompletableFuture<Void> future) {
    }
}
