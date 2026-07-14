package xin.vanilla.banira.platform;

/** 测试用配置服务，避免单元测试触碰真实加载器配置。 */
public enum NoopConfigService implements BaniraConfigService {
    INSTANCE;

    @Override
    public <T> void register(Class<T> configClass, String modId) {
    }

    @Override
    public <T> T view(Class<?> configClass, Class<T> viewClass) {
        throw new IllegalStateException("No config registered");
    }

    @Override
    public BaniraConfigHandle handle(Class<?> configClass) {
        return null;
    }
}
