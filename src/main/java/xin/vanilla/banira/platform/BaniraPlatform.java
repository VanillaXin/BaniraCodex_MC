package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * 只暴露跨 MC 版本稳定的基础平台信息。
 */
public interface BaniraPlatform {

    @Nonnull
    String loaderType();

    @Nonnull
    String minecraftVersion();

    boolean isClient();

    boolean isDedicatedServer();

    boolean isDevelopment();

    boolean isModLoaded(@Nonnull String modId);

    @Nonnull
    String modDisplayName(@Nonnull String modId);

    @Nonnull
    Path configDir();
}
