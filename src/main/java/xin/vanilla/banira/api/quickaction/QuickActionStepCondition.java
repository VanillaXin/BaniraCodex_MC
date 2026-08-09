package xin.vanilla.banira.api.quickaction;

/** 链式执行时，当前步骤对上一步结果的要求。 */
public enum QuickActionStepCondition {
    ALWAYS,
    ON_SUCCESS,
    ON_FAILURE
}
