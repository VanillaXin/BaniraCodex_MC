/**
 * ForgeConfigSpec 便捷构建与解析系统，提供类似 Fabric Cloth Config 的 POJO + fluent 风格。
 * <p>
 * 推荐使用 {@link xin.vanilla.banira.common.config.ForgeConfigAdapter} 从注解配置类构建，
 * 配置类结构与 Fabric 兼容，迁移时仅需修改 get() 与注册逻辑。
 * <p>
 * 备选：使用 {@link xin.vanilla.banira.common.config.ConfigSpecBuilder} 流式 API：
 * <pre>{@code
 * // 1. 使用 ConfigSpecBuilder 定义配置
 * ConfigHolder holder = ConfigSpecBuilder.create("mymod-server", ConfigScope.SERVER)
 *   .category("base", "基础设置")
 *     .define("helpHeader", "-----==== Help ====-----", "帮助头部")
 *     .defineInRange("helpNumPerPage", 5, 1, 9999, "每页数量")
 *     .define("defaultLanguage", "en_us", "默认语言")
 *   .endCategory()
 *   .category("sweep", "定时清理")
 *     .defineInRange("sweepInterval", 600000L, 0L, 604800000L, "清理间隔(ms)")
 *     .defineList("entityList", Arrays.asList("minecraft:arrow"), null, "实体名单")
 *   .endCategory()
 *   .build();
 *
 * // 2. 注册配置
 * ModList.get().getModContainerById("mymod").ifPresent(c ->
 *   ConfigRegistry.register(holder, c));
 *
 * // 3. 读取配置
 * int num = holder.get("base.helpNumPerPage");
 * String lang = holder.get("base.defaultLanguage");
 *
 * // 4. 打开配置编辑界面（客户端）
 * ConfigEditorScreen.open(holder, minecraft.screen);
 * }</pre>
 * <p>
 * 配置编辑界面支持：
 * <ul>
 *   <li>字符串、布尔、整数、长整数、浮点数、枚举、字符串列表的可视化编辑</li>
 *   <li>保存到本地配置文件</li>
 *   <li>服务端配置：若有权限可将修改同步至服务端</li>
 * </ul>
 */
package xin.vanilla.banira.common.config;
