package xin.vanilla.banira.internal.forge.util;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.internal.client.LogoModifier;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.common.ReflectionAccess;

import java.util.List;
import java.util.Optional;

/**
 * Forge 的 modInfo logo 字段写入适配层；更高版本若字段形态变化，只替换这一层。
 */
public final class ForgeLogoModifier {
    private ForgeLogoModifier() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static String fieldName;

    public static void modifyLogo() {
        try {
            String field = logoFieldName();
            if (StringUtils.isNullOrEmpty(field)) {
                return;
            }
            for (IModInfo info : ModList.get().getMods()) {
                if (!(info instanceof ModInfo)) {
                    continue;
                }
                Optional<String> customLogo = LogoModifier.getLogoFile(info.getModId());
                if (customLogo.isPresent()) {
                    ForgeInternalFieldAccess.setObjectField(ModInfo.class, info, field, customLogo);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to modify mod logos", e);
        }
    }

    private static String logoFieldName() {
        if (StringUtils.isNotNullOrEmpty(fieldName)) {
            return fieldName;
        }
        List<? extends IModInfo> mods = ModList.get().getMods();
        if (mods.isEmpty()) {
            return null;
        }
        IModInfo sample = mods.get(0);
        for (String name : ReflectionAccess.privateFieldNames(ModInfo.class, Optional.class)) {
            try {
                @SuppressWarnings("unchecked")
                Optional<String> logo = (Optional<String>) ReflectionAccess.fieldValue(ModInfo.class, sample, name);
                if (logo != null && logo.isPresent()
                        && StringUtils.isNotNullOrEmpty(logo.get())
                        && logo.get().matches(".*\\.png$")) {
                    fieldName = name;
                    return fieldName;
                }
            } catch (Exception ignored) {
            }
        }
        fieldName = "logoFile";
        return fieldName;
    }
}
