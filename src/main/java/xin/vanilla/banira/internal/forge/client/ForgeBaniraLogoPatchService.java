package xin.vanilla.banira.internal.forge.client;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.BaniraLogoPatchService;
import xin.vanilla.banira.internal.forge.util.ForgeInternalFieldAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Forge 侧 logoFile 补丁；ModInfo 私有字段只在 loader adapter 内部反射。
 * <p>
 * 若高版本 ModInfo 改为 record，不能依赖原地改字段，应改为替换元数据持有者或拦截 logo 读取路径。
 */
public final class ForgeBaniraLogoPatchService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static String logoFieldName;

    private ForgeBaniraLogoPatchService() {
    }

    public static void install() {
        BaniraLogoPatchService.installApplier(ForgeBaniraLogoPatchService::apply);
    }

    private static void apply(Function<String, Optional<String>> resolver) {
        try {
            String fieldName = resolveLogoFieldName();
            if (StringUtils.isNullOrEmpty(fieldName)) {
                return;
            }
            for (ModInfo info : ModList.get().getMods()) {
                Optional<String> customLogo = resolver.apply(info.getModId());
                if (!customLogo.isPresent()) {
                    continue;
                }
                if (!ForgeInternalFieldAccess.writeObjectField(ModInfo.class, info, fieldName, customLogo)) {
                    LOGGER.debug("Failed to patch Forge mod logo field {}", fieldName);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to modify mod logos", e);
        }
    }

    private static String resolveLogoFieldName() {
        if (StringUtils.isNotNullOrEmpty(logoFieldName)) {
            return logoFieldName;
        }

        List<? extends ModInfo> mods = ModList.get().getMods();
        if (mods.isEmpty()) {
            return "";
        }

        ModInfo sample = mods.get(0);
        for (String name : findOptionalFieldNames(ModInfo.class)) {
            try {
                @SuppressWarnings("unchecked")
                Optional<String> logo = (Optional<String>) ForgeInternalFieldAccess.readField(ModInfo.class, sample, name);
                if (logo != null && logo.isPresent()
                        && StringUtils.isNotNullOrEmpty(logo.get())
                        && logo.get().matches(".*\\.png$")) {
                    logoFieldName = name;
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        if (StringUtils.isNullOrEmpty(logoFieldName)) {
            logoFieldName = "logoFile";
        }
        return logoFieldName;
    }

    private static List<String> findOptionalFieldNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if ((Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers()))
                    && field.getType() == Optional.class) {
                names.add(field.getName());
            }
        }
        return names;
    }
}
