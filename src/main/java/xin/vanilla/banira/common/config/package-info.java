/**
 * Loader-neutral configuration metadata and value access APIs.
 *
 * <p>Dependent mods should register annotated config classes through
 * {@link xin.vanilla.banira.api.Banira#platform()} and use
 * {@link xin.vanilla.banira.common.config.ConfigScope} for config scope.
 * Loader-specific config specs and adapters belong to internal packages.</p>
 */
package xin.vanilla.banira.common.config;
