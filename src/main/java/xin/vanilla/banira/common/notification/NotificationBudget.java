package xin.vanilla.banira.common.notification;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Actual wire JSON budgets, not Java character counts.
 */
public final class NotificationBudget {
    public static final int MAX_COMPONENT_JSON_BYTES = 16384;
    public static final int MAX_TYPE_ID_BYTES = 128;

    private NotificationBudget() {
    }

    public static String componentJson(Component component) {
        String json = JsonUtils.toString(Objects.requireNonNull(component, "component").toJson());
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
        Component copy = Objects.requireNonNull(component, "component").clone().languageCodeIfEmpty(language);
        detachNativeReferences(copy, language);
        String componentJson = componentJson(copy);
        String vanillaJson = ITextComponent.Serializer.toJson(copy.toChat(language));
        // Forge 1.16.5 writeComponent allows 262144 bytes; use the conservative
        // common budget, measured independently after native translation/formatting.
        validateUtf8(vanillaJson, MAX_COMPONENT_JSON_BYTES, "vanilla component JSON");
        return new Payload(componentJson, vanillaJson);
    }

    private static void detachNativeReferences(Component copy, String inheritedLanguage) {
        // Component.clone copies Banira children/args, but shares native objects.
        // toChat can rewrite their styles and append siblings in place.
        if (copy.original() instanceof ITextComponent) {
            copy.original(ITextComponent.Serializer.fromJson(
                    ITextComponent.Serializer.toJson((ITextComponent) copy.original())));
        }
        if (copy.hoverEvent() != null) {
            copy.hoverEvent(HoverEvent.deserialize(copy.hoverEvent().serialize()));
        }
        copy.languageCodeIfEmpty(inheritedLanguage);
        String language = inheritedLanguage == null ? copy.languageCodeOrDefault()
                : copy.languageCodeOrDefault(inheritedLanguage);
        copy.languageCode(language);
        for (Component child : copy.getChildren())
            detachNativeReferences(Objects.requireNonNull(child, "child"), language);
        for (Component arg : copy.getArgs()) {
            if (arg != null) detachNativeReferences(arg, language);
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

        private Payload(String componentJson, String vanillaJson) {
            this.componentJson = componentJson;
            this.vanillaJson = vanillaJson;
        }

        public String componentJson() {
            return componentJson;
        }

        public String vanillaJson() {
            return vanillaJson;
        }

        public ITextComponent vanillaMessage() {
            return ITextComponent.Serializer.fromJson(vanillaJson);
        }
    }
}
