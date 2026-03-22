package xin.vanilla.banira.common.data;

import com.google.gson.JsonObject;
import lombok.NonNull;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.enums.EnumI18nType;

/**
 * 每个 Mod 继承此类并实现 {@link #modId()}，即可在不重复传入 modId 的情况下构建 {@link Component}。
 * {@link Component} 的静态工厂仅包内可见，对外须通过本类（或 {@link ScopedComponent}）创建。
 * <pre>{@code
 * public final class MyModComponents extends AbstractModComponentSource {
 *     public static final MyModComponents INSTANCE = new MyModComponents();
 *
 *     private MyModComponents() {
 *     }
 *
 *     @Override
 *     protected String modId() {
 *         return MyMod.MODID;
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractComponent {

    /**
     * 当前 Mod 的命名空间（与语言文件 assets/&lt;modId&gt;/lang 一致）。
     */
    protected abstract @NonNull String modId();

    public Component trans(String key) {
        return Component.trans(modId(), key);
    }

    public Component trans(String key, Object... args) {
        return Component.trans(modId(), key, args);
    }

    public Component trans(EnumI18nType type, String key, Object... args) {
        return Component.trans(modId(), type, key, args);
    }

    public Component trans(ServerPlayerEntity player, EnumI18nType type, String key, Object... args) {
        return Component.trans(player, type, key, args).modId(modId());
    }

    public Component transAuto(String key) {
        return Component.transAuto(modId(), key);
    }

    public Component transAuto(String key, Object... args) {
        return Component.transAuto(modId(), key, args);
    }

    public Component transClient(String key) {
        return Component.transClient(modId(), key);
    }

    public Component transClient(String key, Object... args) {
        return Component.transClient(modId(), key, args);
    }

    public Component transClient(EnumI18nType type, String key, Object... args) {
        return Component.transClient(type, key, args).modId(modId());
    }

    public Component transClientAuto(String key) {
        return Component.transClientAuto(modId(), key);
    }

    public Component transClientAuto(String key, Object... args) {
        return Component.transClientAuto(modId(), key, args);
    }

    public Component transLang(String languageCode, String key, Object... args) {
        return Component.transLang(modId(), languageCode, EnumI18nType.NONE, key, args);
    }

    public Component transLang(String languageCode, EnumI18nType type, String key, Object... args) {
        return Component.transLang(modId(), languageCode, type, key, args);
    }

    public final Component empty() {
        return Component.empty();
    }

    public final Component literal(String text) {
        return Component.literal(text);
    }

    public final Component object(Object original) {
        return Component.object(original);
    }

    public final Component deserialize(JsonObject jsonObject) {
        return Component.deserialize(jsonObject);
    }

    public static JsonObject serialize(Component component) {
        return Component.serialize(component);
    }
}
