package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.common.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LogoModifier {
    private LogoModifier() {
    }

    /**
     * modId -> Supplier
     */
    private static final Map<String, Supplier<String>> SUPPLIER_REGISTRY = new ConcurrentHashMap<>();

    /**
     * Function列表, 按注册顺序执行
     */
    private static final List<Function<String, String>> FUNCTION_REGISTRY = new ArrayList<>();


    /**
     * 注册Logo提供者
     *
     * @param logoFileSupplier Logo文件路径提供者
     */
    public static void register(String modId, Supplier<String> logoFileSupplier) {
        if (modId == null || logoFileSupplier == null) {
            throw new IllegalArgumentException("modId and logoFileSupplier cannot be null");
        }
        SUPPLIER_REGISTRY.put(modId, logoFileSupplier);
    }

    /**
     * 注册Logo提供者
     *
     * @param logoFileFunction Logo文件路径函数, 接收 modId, 返回 logoFile
     */
    public static void register(Function<String, String> logoFileFunction) {
        if (logoFileFunction == null) {
            throw new IllegalArgumentException("logoFileFunction cannot be null");
        }
        FUNCTION_REGISTRY.add(logoFileFunction);
    }

    /**
     * 获取指定Mod的Logo文件路径
     *
     * @return Logo文件路径
     */
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
        // Fabric 的 mod 元数据在运行时不可按 Forge ModInfo 的方式安全改写。
        // 保留注册 API，避免外部调用失效；实际图标由 fabric.mod.json 控制。
    }

    /**
     * 清除所有注册
     */
    public static void clear() {
        SUPPLIER_REGISTRY.clear();
        FUNCTION_REGISTRY.clear();
    }
}
