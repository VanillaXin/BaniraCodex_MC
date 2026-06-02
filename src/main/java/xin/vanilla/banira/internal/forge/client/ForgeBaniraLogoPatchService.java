package xin.vanilla.banira.internal.forge.client;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.BaniraLogoPatchService;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Forge 侧 logoFile 补丁；ModInfo 私有字段只在 loader adapter 内部反射。
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
                writeLogoField(info, fieldName, customLogo);
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
                Optional<String> logo = (Optional<String>) readField(ModInfo.class, sample, name);
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

    private static Object readField(Class<?> clazz, Object instance, String fieldName) throws ReflectiveOperationException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void writeLogoField(ModInfo info, String fieldName, Optional<String> customLogo) {
        try {
            Field field = ModInfo.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (Modifier.isFinal(field.getModifiers())) {
                clearFinalModifier(field);
            }
            field.set(info, customLogo);
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("Failed to patch Forge mod logo field {}", fieldName, e);
        }
    }

    private static void clearFinalModifier(Field field) throws ReflectiveOperationException {
        // Forge 1.16.x 的 ModInfo 元数据字段可能为 final；这里限制在 loader adapter 内处理。
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
}
