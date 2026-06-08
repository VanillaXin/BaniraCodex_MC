package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;

/**
 * 客户端运行时访问点，集中隔离 Minecraft 单例和窗口句柄。
 */
public final class BaniraClientRuntime {
    private BaniraClientRuntime() {
    }

    public static void execute(@Nonnull Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    public static long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }
}
