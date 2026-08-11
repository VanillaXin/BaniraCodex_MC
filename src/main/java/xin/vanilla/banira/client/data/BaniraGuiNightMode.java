package xin.vanilla.banira.client.data;

import xin.vanilla.banira.common.enums.EnumGuiNightMode;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.config.ClientConfig;

import java.time.LocalTime;

/**
 * 根据 {@link ClientConfig} 判定是否使用 GUI 夜间配色（仅客户端）
 */
public final class BaniraGuiNightMode {

    private BaniraGuiNightMode() {
    }

    public static boolean isActive() {
        EnumGuiNightMode mode = ClientConfig.get().guiNightMode();
        if (mode == null) {
            mode = EnumGuiNightMode.OFF;
        }
        switch (mode) {
            case OFF:
                return false;
            case ALWAYS:
                return true;
            case SCHEDULED:
                return scheduledNight(
                        ClientConfig.get().guiNightModeStartMinute(),
                        ClientConfig.get().guiNightModeEndMinute());
            case AUTO:
            default:
                return autoNight();
        }
    }

    private static boolean scheduledNight(int startMin, int endMin) {
        int start = clampMinute(startMin);
        int end = clampMinute(endMin);
        LocalTime now = LocalTime.now();
        int nowMin = now.getHour() * 60 + now.getMinute();
        if (start == end) {
            return false;
        }
        if (start < end) {
            return nowMin >= start && nowMin < end;
        }
        return nowMin >= start || nowMin < end;
    }

    private static int clampMinute(int m) {
        if (m < 0) {
            return 0;
        }
        if (m > 1439) {
            return 1439;
        }
        return m;
    }

    private static boolean autoNight() {
        net.minecraft.world.level.Level level = BaniraClientRuntime.level();
        if (level != null) {
            long t = level.getDayTime() % 24000L;
            return t >= 13000L && t < 23000L;
        }
        LocalTime now = LocalTime.now();
        int h = now.getHour();
        return h < 6 || h >= 18;
    }
}
