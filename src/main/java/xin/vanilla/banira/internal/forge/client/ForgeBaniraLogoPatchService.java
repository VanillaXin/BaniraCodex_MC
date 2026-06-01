package xin.vanilla.banira.internal.forge.client;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.FieldUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.BaniraLogoPatchService;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
                FieldUtils.setPrivateFieldValue(ModInfo.class, info, fieldName, customLogo);
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
        for (String name : FieldUtils.getPrivateFieldNames(ModInfo.class, Optional.class)) {
            try {
                @SuppressWarnings("unchecked")
                Optional<String> logo = (Optional<String>) FieldUtils.getPrivateFieldValue(ModInfo.class, sample, name);
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
}
