package xin.vanilla.banira.common.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置项注解，类似 Fabric Cloth Config 的 @ConfigEntry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {

    /**
     * 配置项键名，为空则使用字段名
     */
    String key() default "";

    /**
     * 工具提示（支持多行，每行一个字符串）
     */
    String[] tooltip() default {};

    /**
     * 配置项所属分类（用于 GUI 分组）
     */
    String category() default "";

    /**
     * 通过 GUI/网络修改<strong>服务端</strong>上该配置项时，如何判定权限。
     */
    enum EditPermissionPolicy {
        /**
         * 使用 {@link xin.vanilla.banira.internal.config.CommonConfig} 中「修改服务端配置」的全局设置
         */
        INHERIT,
        /**
         * 使用本注解中的 {@link RequiresEditPermission#permissionLevel()} 与 {@link RequiresEditPermission#virtualPermissionKey()}
         */
        FIELD_OVERRIDE
    }

    /**
     * 声明修改该字段（同步至服务端时）所需的权限等级与虚拟权限键。
     * <p>
     * {@link EditPermissionPolicy#FIELD_OVERRIDE} 时：{@code permissionLevel == -1} 则仍沿用全局配置的权限等级；
     * 虚拟权限键为空则仍沿用全局配置中的虚拟权限键。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface RequiresEditPermission {
        EditPermissionPolicy policy() default EditPermissionPolicy.INHERIT;

        /**
         * 所需权限等级（0–4），与指令来源 {@link net.minecraft.command.CommandSource#hasPermission(int)} 一致；
         * {@code -1} 表示 FIELD_OVERRIDE 时仍沿用全局「修改服务端配置」的权限等级
         */
        int permissionLevel() default -1;

        /**
         * 虚拟权限完整键（{@code modId:id}）；为空则 FIELD_OVERRIDE 时虚拟键仍继承全局配置
         */
        String virtualPermissionKey() default "";
    }

    /**
     * 整数/长整数范围
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BoundedDiscrete {
        int min() default 0;

        int max() default Integer.MAX_VALUE;
    }

    /**
     * 长整数范围
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BoundedLong {
        long min() default 0L;

        long max() default Long.MAX_VALUE;
    }

    /**
     * 双精度浮点数范围
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BoundedDouble {
        double min() default 0.0;

        double max() default Double.MAX_VALUE;

        /**
         * 小数位数，用于显示与精度控制，默认 2
         */
        int decimalPlaces() default 2;
    }

    /**
     * 可折叠对象（嵌套配置块）
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface CollapsibleObject {
    }

    /**
     * GUI 相关（与 Fabric Cloth Config 兼容）
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Gui {
        /**
         * 工具提示
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface Tooltip {
            String[] value() default {};
        }

        /**
         * 可折叠对象（嵌套配置块）
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface CollapsibleObject {
        }

        /**
         * 枚举显示方式
         */
        enum EnumDisplayOption {
            DROPDOWN,
            BUTTON
        }

        EnumDisplayOption enumHandler() default EnumDisplayOption.DROPDOWN;
    }
}
