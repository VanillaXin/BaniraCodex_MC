package xin.vanilla.banira.common.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 按墙钟时间（{@link System#nanoTime()}）调度
 */
@Getter
@ToString
@AllArgsConstructor
@Accessors(fluent = true)
public class WallClockScheduledTask implements Comparable<WallClockScheduledTask> {
    private static final AtomicLong SEQ = new AtomicLong(0);

    private final long seqNo;
    private final long executeDeadlineNanos;
    private final Runnable runnable;
    private final boolean clientSide;

    public static WallClockScheduledTask server(double delayMillis, Runnable runnable) {
        return new WallClockScheduledTask(SEQ.getAndIncrement(), deadlineNanos(delayMillis), runnable, false);
    }

    public static WallClockScheduledTask client(double delayMillis, Runnable runnable) {
        return new WallClockScheduledTask(SEQ.getAndIncrement(), deadlineNanos(delayMillis), runnable, true);
    }

    private static long deadlineNanos(double delayMillis) {
        long now = System.nanoTime();
        if (delayMillis <= 0 || Double.isNaN(delayMillis)) {
            return now;
        }
        if (Double.isInfinite(delayMillis)) {
            return Long.MAX_VALUE;
        }
        long add = Math.round(delayMillis * 1_000_000.0);
        if (add < 0 || now > Long.MAX_VALUE - add) {
            return Long.MAX_VALUE;
        }
        return now + add;
    }

    @Override
    public int compareTo(WallClockScheduledTask o) {
        int cmp = Long.compare(this.executeDeadlineNanos, o.executeDeadlineNanos);
        if (cmp != 0) return cmp;
        return Long.compare(this.seqNo, o.seqNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WallClockScheduledTask)) return false;
        WallClockScheduledTask that = (WallClockScheduledTask) o;
        return executeDeadlineNanos == that.executeDeadlineNanos && seqNo == that.seqNo && runnable.equals(that.runnable);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(executeDeadlineNanos);
        result = 31 * result + Long.hashCode(seqNo);
        result = 31 * result + runnable.hashCode();
        return result;
    }
}
