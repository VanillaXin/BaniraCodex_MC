package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.common.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Logo 覆盖注册入口；具体 mod 元数据写入由加载器内部适配层安装。
 */
public final class LogoModifier {
    private LogoModifier() {
    }

    private static final Map<String, Supplier<String>> SUPPLIER_REGISTRY = new ConcurrentHashMap<>();
    private static final List<Function<String, String>> FUNCTION_REGISTRY = new ArrayList<>();
    private static LogoApplier applier = LogoApplier.NOOP;

    public static void installApplier(LogoApplier logoApplier) {
        applier = logoApplier != null ? logoApplier : LogoApplier.NOOP;
    }

    public static void register(String modId, Supplier<String> logoFileSupplier) {
        if (modId == null || logoFileSupplier == null) {
            throw new IllegalArgumentException("modId and logoFileSupplier cannot be null");
        }
        SUPPLIER_REGISTRY.put(modId, logoFileSupplier);
    }

    public static void register(Function<String, String> logoFileFunction) {
        if (logoFileFunction == null) {
            throw new IllegalArgumentException("logoFileFunction cannot be null");
        }
        FUNCTION_REGISTRY.add(logoFileFunction);
    }

    public static Optional<String> getLogoFile(String modId) {
        if (StringUtils.isNullOrEmptyEx(modId)) {
            return Optional.empty();
        }

        Supplier<String> supplier = SUPPLIER_REGISTRY.get(modId);
        if (supplier != null) {
            String logoFile = supplier.get();
            if (StringUtils.isNotNullOrEmpty(logoFile)) {
                return Optional.of(logoFile);
            }
        }

        for (Function<String, String> function : FUNCTION_REGISTRY) {
            String logoFile = function.apply(modId);
            if (StringUtils.isNotNullOrEmpty(logoFile)) {
                return Optional.of(logoFile);
            }
        }

        return Optional.empty();
    }

    public static void modifyLogo() {
        if (!SUPPLIER_REGISTRY.isEmpty() || !FUNCTION_REGISTRY.isEmpty()) {
            applier.apply();
        }
    }

    public static void clear() {
        SUPPLIER_REGISTRY.clear();
        FUNCTION_REGISTRY.clear();
    }

    public interface LogoApplier {
        LogoApplier NOOP = () -> {
        };

        void apply();
    }
}
