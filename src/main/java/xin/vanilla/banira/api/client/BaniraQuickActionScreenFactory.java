package xin.vanilla.banira.api.client;

/** 加载器无关的窗口工厂；实现返回当前版本的原生 Screen 对象。 */
@FunctionalInterface
public interface BaniraQuickActionScreenFactory {
    Object create(Object previousScreen);
}
