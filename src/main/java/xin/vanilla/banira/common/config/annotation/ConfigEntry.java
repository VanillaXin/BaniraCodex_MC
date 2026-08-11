package xin.vanilla.banira.common.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置项注解，类似 Fabric Cloth Config 的 @ConfigEntry
 * <p>
 * {@link java.util.List} 字段须带元素泛型，以便生成配置后端列表校验与 GUI：
 * {@code List<String>}、{@code List<Integer>}、{@code List<Long>}、{@code List<Double>}、
 * {@code List<Boolean>}、{@code List<枚举类型>}；原始 {@code List} 视为字符串列表。
 * </p>
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
         * 所需权限等级（0–4），与指令来源 {@link net.minecraft.commands.CommandSourceStack#hasPermission(int)} 一致；
         * {@code -1} 表示 FIELD_OVERRIDE 时仍沿用全局「修改服务端配置」的权限等级
         */
        int permissionLevel() default -1;

        /**
         * 虚拟权限完整键（{@code modId:id}）；为空则 FIELD_OVERRIDE 时虚拟键仍继承全局配置
         */
        String virtualPermissionKey() default "";
    }

    /**
     * 整数范围。若字段类型为 {@link java.util.List}{@code <Integer>}，则约束列表<strong>每个元素</strong>的取值。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BoundedDiscrete {
        int min() default 0;

        int max() default Integer.MAX_VALUE;
    }

    /**
     * 长整数范围。若字段类型为 {@link java.util.List}{@code <Long>}，则约束列表每个元素。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BoundedLong {
        long min() default 0L;

        long max() default Long.MAX_VALUE;
    }

    /**
     * 双精度浮点数范围。若字段类型为 {@link java.util.List}{@code <Double>}，则约束列表每个元素；{@link #decimalPlaces()} 同时用于列表元素精度。
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
         * 工具提示（三种用法互斥，优先级从高到低）：
         * <ol>
         *   <li>{@code translationKey}：不写 TOML 注释，GUI 用模组翻译键</li>
         *   <li>各 {@code xx_xx} 语言字段：TOML 多行注释（按固定语言顺序输出，字段内换行拆成多行），GUI 由 {@link xin.vanilla.banira.common.util.Translator#pickLocalizedMapValue(String, java.util.Map)} 按当前语言匹配（含族内回退，例如 {@code zh_tw} 可回落到 {@code zh_cn}）</li>
         *   <li>{@code value}：{@code @Tooltip({"中文", "English"})} 与 {@code @Tooltip(value = {"中文", "English"})} 等价；TOML 与 GUI 均为这些行（GUI 合并为多行）</li>
         * </ol>
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface Tooltip {
            /**
             * 模组内翻译键；指定后配置文件不写该项注释，编辑界面悬浮提示用该键走 i18n。
             * <p>
             * 优先级高于各语言字段与 {@link #value()}。
             */
            String translationKey() default "";

            /**
             * 多行硬编码说明（每元素一行）；写入配置文件为多行注释，悬浮提示合并为多行显示。
             * <p>
             * 可与 {@code @ConfigEntry#tooltip()} 二选一；未写 {@link #translationKey()} 且未写语言字段时使用。
             */
            String[] value() default {};

            // region 常见语言硬编码

            String en_us() default "";

            String en_gb() default "";

            String zh_cn() default "";

            String zh_tw() default "";

            String zh_hk() default "";

            String ja_jp() default "";

            String ko_kr() default "";

            String ru_ru() default "";

            String de_de() default "";

            String fr_fr() default "";

            String fr_ca() default "";

            String es_es() default "";

            String es_mx() default "";

            String pt_br() default "";

            String pt_pt() default "";

            String it_it() default "";

            String pl_pl() default "";

            // endregion 常见语言硬编码
        }

        /**
         * 可折叠对象（嵌套配置块）
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface CollapsibleObject {
        }

        /** 将字符串列表作为组合键列表编辑，界面通过实际按键捕获内容。 */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface KeyChords {
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
