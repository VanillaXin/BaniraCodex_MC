package xin.vanilla.banira.api.script;

import java.util.Map;

/**
 * Immutable candidate; publishing requires the session that created it.
 */
public interface PreparedScripts<T> {
    Map<String, T> scripts();
}
