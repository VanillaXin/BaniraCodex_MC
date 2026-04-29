package xin.vanilla.banira.editable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Banira 配置编辑器专有元数据。
 * Cloth 的 {@link me.shedaniel.autoconfig.annotation.ConfigEntry} 在本仓库不包含以下能力（或等价物不同）。
 */
public final class BaniraFieldMeta {

    private BaniraFieldMeta() {
    }

    public enum EditPermissionPolicy {
        /**
         * 使用 CommonConfig 中「修改服务端配置」的全局设置
         */
        INHERIT,
        FIELD_OVERRIDE
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface RequiresEditPermission {
        EditPermissionPolicy policy() default EditPermissionPolicy.INHERIT;

        int permissionLevel() default -1;

        String virtualPermissionKey() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface BoundedLong {
        long min() default Long.MIN_VALUE;

        long max() default Long.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface BoundedDouble {
        double min() default 0.0;

        double max() default Double.MAX_VALUE;

        int decimalPlaces() default 2;
    }
}
