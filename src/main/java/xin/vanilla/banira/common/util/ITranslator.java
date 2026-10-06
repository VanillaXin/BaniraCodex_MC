package xin.vanilla.banira.common.util;

import lombok.NonNull;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;

import java.util.Collections;
import java.util.List;

/**
 * 语言助手接口。
 * <p>
 * 每个 Mod 可实现一个子类并显式传入 modId 与同 jar 资源锚点类：
 * <pre>{@code
 * public final class MyModLang extends Translator {
 *     public static final MyModLang INSTANCE = new MyModLang();
 *     private MyModLang() { super("my_mod_id", MyModLang.class); }
 * }
 * // 使用: MyModLang.INSTANCE.translate(type, key);
 * }</pre>
 */
public interface ITranslator {

    /**
     * 获取所属 modId
     */
    String getModId();

    /**
     * 翻译（客户端语言）
     */
    String translate(@NonNull EnumI18nType type, @NonNull String key);

    /**
     * 翻译（指定语言）
     */
    String translate(@NonNull EnumI18nType type, @NonNull String key, @NonNull String languageCode);

    /**
     * 获取翻译文本
     */
    String getTranslation(@NonNull EnumI18nType type, @NonNull String key, @NonNull String languageCode);

    /**
     * 获取翻译文本（原始 key）
     */
    String getTranslation(@NonNull String key, @NonNull String languageCode);

    /**
     * 构建翻译键（type.modId.key 格式）
     */
    String getKey(@NonNull EnumI18nType type, @NonNull String key);

    /**
     * 获取 enabled/disabled 的翻译组件（指定语言）
     */
    Component enabled(@NonNull String languageCode, boolean enabled);

    /**
     * 获取 enabled/disabled 的翻译组件（客户端语言）
     */
    Component enabled(boolean enabled);

    /**
     * 加载语言文件
     */
    void loadLanguage(@NonNull String languageCode);

    /**
     * 获取当前模组声明的语言代码。
     */
    default List<String> getI18nFiles() {
        return Collections.emptyList();
    }

    /**
     * 获取翻译文本（客户端语言，兼容旧 API）
     */
    default String getTranslationClient(@NonNull EnumI18nType type, @NonNull String key) {
        return translate(type, key);
    }
}
