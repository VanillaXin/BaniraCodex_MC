package xin.vanilla.banira.common.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.concurrent.atomic.AtomicLong;

@Getter
@ToString
@AllArgsConstructor
@Accessors(fluent = true)
public class ScheduledTask implements Comparable<ScheduledTask> {
    private static final AtomicLong SEQ = new AtomicLong(0);

    private final long seqNo;
    private final long executeTick;
    private final Runnable runnable;
    private final boolean clientSide;

    public static ScheduledTask server(long executeTick, Runnable runnable) {
        return new ScheduledTask(executeTick, SEQ.getAndIncrement(), runnable, false);
    }

    public static ScheduledTask client(long executeTick, Runnable runnable) {
        return new ScheduledTask(executeTick, SEQ.getAndIncrement(), runnable, true);
    }

    @Override
    public int compareTo(ScheduledTask o) {
        int cmp = Long.compare(this.executeTick, o.executeTick);
        if (cmp != 0) return cmp;
        return Long.compare(this.seqNo, o.seqNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledTask)) return false;
        ScheduledTask that = (ScheduledTask) o;
        return executeTick == that.executeTick && seqNo == that.seqNo && runnable.equals(that.runnable);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(executeTick);
        result = 31 * result + Long.hashCode(seqNo);
        result = 31 * result + runnable.hashCode();
        return result;
    }
}
