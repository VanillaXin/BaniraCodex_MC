package xin.vanilla.banira.common.config.annotation;

import net.minecraftforge.fml.config.ModConfig;

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
    ModConfig.Type type() default ModConfig.Type.COMMON;
}
