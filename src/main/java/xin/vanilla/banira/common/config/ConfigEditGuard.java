package xin.vanilla.banira.common.config;

import java.util.Map;

/** Pure validation only: do not write configuration or execute scripts from a guard. */
@FunctionalInterface
public interface ConfigEditGuard {
    void validate(ConfigEditOrigin origin, Map<String, Object> proposedValues);
}
