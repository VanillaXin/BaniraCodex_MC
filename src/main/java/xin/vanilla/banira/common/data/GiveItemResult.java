package xin.vanilla.banira.common.data;


import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 给予物品结果
 */
@Getter
@Accessors(fluent = true)
public final class GiveItemResult {
    /**
     * 是否成功
     */
    private final boolean success;
    /**
     * 成功添加至背包的数量
     */
    private final int added;
    /**
     * 掉落在地上的数量
     */
    private final int dropped;
    /**
     * 剩余未处理的数量
     */
    private final int remaining;

    private GiveItemResult(boolean success, int added, int dropped, int remaining) {
        this.success = success;
        this.added = added;
        this.dropped = dropped;
        this.remaining = remaining;
    }

    /**
     * 创建成功结果
     */
    public static GiveItemResult success(int addedCount, int droppedCount) {
        return new GiveItemResult(true, addedCount, droppedCount, 0);
    }

    /**
     * 创建成功结果（带剩余数量）
     */
    public static GiveItemResult success(int addedCount, int droppedCount, int remainingCount) {
        return new GiveItemResult(true, addedCount, droppedCount, remainingCount);
    }

    /**
     * 创建失败结果
     */
    public static GiveItemResult failure(int addedCount, int droppedCount, int remainingCount) {
        return new GiveItemResult(false, addedCount, droppedCount, remainingCount);
    }
}
