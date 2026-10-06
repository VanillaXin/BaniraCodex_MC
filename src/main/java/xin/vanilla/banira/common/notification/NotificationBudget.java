package xin.vanilla.banira.common.notification;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Conservative application budgets, independent of the unchanged native wire limits.
 */
public final class NotificationBudget {
    public static final int MAX_COMPONENT_JSON_BYTES = 16384;
    public static final int MAX_TYPE_ID_BYTES = 128;
    public static final int MAX_NATIVE_COMPONENT_BYTES = 16384;

    private NotificationBudget() {
    }

    public static String componentJson(Component component) {
        String json = JsonUtils.toString(Objects.requireNonNull(component, "component").toJson());
        validateUtf8(json, MAX_COMPONENT_JSON_BYTES, "Banira component JSON");
        return json;
    }

    private static String componentJson(Component component, RegistryAccess registries) {
        String json = JsonUtils.toString(component.toJson(registries));
        validateUtf8(json, MAX_COMPONENT_JSON_BYTES, "Banira component JSON");
        return json;
    }

    public static String notificationType(String type) {
        String normalized = NotificationTypeKeys.normalizeOrDefault(type);
        validateUtf8(normalized, MAX_TYPE_ID_BYTES, "notification type");
        return normalized;
    }

    public static void validateUtf8(String value, int limit, String field) {
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > limit) throw new LimitExceededException(field + " is " + bytes + " UTF-8 bytes; max " + limit);
    }

    public static Payload prepare(Component component, String language) {
        return prepare(component, language, RegistryAccess.EMPTY);
    }

    public static Payload prepare(Component component, String language, RegistryAccess registries) {
        Objects.requireNonNull(registries, "registries");
        Component copy = Objects.requireNonNull(component, "component").clone().languageCodeIfEmpty(language);
        detachNativeReferences(copy, language, registries);
        String componentJson = componentJson(copy, registries);
        String vanillaJson = net.minecraft.network.chat.Component.Serializer.toJson(copy.toChat(language), registries);
        // Measure the conservative common budget again after native translation/formatting.
        validateUtf8(vanillaJson, MAX_COMPONENT_JSON_BYTES, "vanilla component JSON");
        Payload payload = new Payload(componentJson, vanillaJson, registries);
        validateNativeEncoding(payload);
        return payload;
    }

    private static void validateNativeEncoding(Payload payload) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(256, MAX_NATIVE_COMPONENT_BYTES), payload.registries);
        try {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, payload.vanillaMessage());
        } catch (IndexOutOfBoundsException tooLarge) {
            throw new LimitExceededException("Native component encoding exceeds " + MAX_NATIVE_COMPONENT_BYTES + " bytes");
        } finally {
            buffer.release();
        }
    }

    private static void detachNativeReferences(Component copy, String inheritedLanguage, RegistryAccess registries) {
        // Component.clone copies Banira children/args, but shares native objects.
        // toChat can rewrite their styles and append siblings in place.
        if (copy.original() instanceof net.minecraft.network.chat.Component) {
            copy.original(net.minecraft.network.chat.Component.Serializer.fromJson(
                    net.minecraft.network.chat.Component.Serializer.toJson((net.minecraft.network.chat.Component) copy.original(), registries), registries));
        }
        if (copy.hoverEvent() != null) {
            var ops = registries.createSerializationContext(JsonOps.INSTANCE);
            copy.hoverEvent(HoverEvent.CODEC.parse(ops,
                    HoverEvent.CODEC.encodeStart(ops, copy.hoverEvent()).getOrThrow()).getOrThrow());
        }
        copy.languageCodeIfEmpty(inheritedLanguage);
        String language = inheritedLanguage == null ? copy.languageCodeOrDefault()
                : copy.languageCodeOrDefault(inheritedLanguage);
        copy.languageCode(language);
        for (Component child : copy.getChildren())
            detachNativeReferences(Objects.requireNonNull(child, "child"), language, registries);
        for (Component arg : copy.getArgs()) {
            if (arg != null) detachNativeReferences(arg, language, registries);
        }
    }

    public static final class LimitExceededException extends IllegalArgumentException {
        private LimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * Immutable encodings keep preflight and transport on the same snapshot.
     */
    public static final class Payload {
        private final String componentJson;
        private final String vanillaJson;
        private final RegistryAccess registries;

        private Payload(String componentJson, String vanillaJson, RegistryAccess registries) {
            this.componentJson = componentJson;
            this.vanillaJson = vanillaJson;
            this.registries = registries;
        }

        public String componentJson() {
            return componentJson;
        }

        public String vanillaJson() {
            return vanillaJson;
        }

        public net.minecraft.network.chat.Component vanillaMessage() {
            return net.minecraft.network.chat.Component.Serializer.fromJson(vanillaJson, registries);
        }
    }
}
