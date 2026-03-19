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
