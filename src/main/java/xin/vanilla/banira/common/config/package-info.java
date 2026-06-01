/**
 * Loader-neutral configuration metadata and value access APIs.
 *
 * <p>Dependent mods should register annotated config classes through
 * {@link xin.vanilla.banira.api.Banira#platform()} and use
 * {@link xin.vanilla.banira.common.config.ConfigScope} for config scope.
 * Forge-specific builders and adapters are implementation details of this
 * branch.</p>
 *
 * <pre>{@code
 * ConfigHolder holder = ConfigSpecBuilder.create("mymod-server", ConfigScope.SERVER)
 *     .category("base", "Base settings")
 *     .defineInRange("helpNumPerPage", 5, 1, 9999, "Items per page")
 *     .endCategory()
 *     .build("mymod");
 *
 * int num = holder.get("base.helpNumPerPage");
 * }</pre>
 */
package xin.vanilla.banira.common.config;
