/**
 * Banira 配置系统，提供类似 Fabric Cloth Config 的 POJO + fluent 风格。
 * <p>
 * 推荐使用 {@link xin.vanilla.banira.common.config.BaniraConfig} 从注解配置类构建，
 * 配置类结构与 Fabric 兼容，迁移时由各加载器分支的 {@code BaniraConfigService} 负责具体实现。
 * <p>
 * 配置编辑界面支持：
 * <ul>
 *   <li>字符串、布尔、整数、长整数、浮点数、枚举、字符串列表的可视化编辑</li>
 *   <li>保存到本地配置文件</li>
 *   <li>服务端配置：若有权限可将修改同步至服务端</li>
 * </ul>
 */
package xin.vanilla.banira.common.config;
