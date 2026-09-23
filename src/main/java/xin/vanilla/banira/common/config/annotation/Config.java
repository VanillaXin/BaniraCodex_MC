package xin.vanilla.banira.common.config.annotation;

import xin.vanilla.banira.common.config.ConfigScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记配置类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {

    /**
     * 配置文件名
     */
    String name();

    /**
     * 配置类型
     */
    ConfigScope type() default ConfigScope.COMMON;

    boolean generateView() default false;

    UnboundAccess viewUnbound() default UnboundAccess.REQUIRE_REGISTERED;

    enum UnboundAccess { REQUIRE_REGISTERED, DEFAULTS }
}
