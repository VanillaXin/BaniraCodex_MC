package xin.vanilla.banira.common.config;

/**
 * Supplied by trusted local callers, never decoded from a client packet.
 */
public enum ConfigEditOrigin {
    LOCAL_MIGRATION, API, COMMAND, UI, REMOTE
}
