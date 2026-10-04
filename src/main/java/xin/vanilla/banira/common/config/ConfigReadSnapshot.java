package xin.vanilla.banira.common.config;

import java.util.Map;

/** Frozen runtime values without file revisions or edit authority. */
public interface ConfigReadSnapshot {
    Map<String, Object> getValues();

    static ConfigReadSnapshot of(Map<String, Object> values) {
        Map<String, Object> frozen = ConfigEditSnapshot.immutableValues(values);
        return () -> frozen;
    }
}
