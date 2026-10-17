package xin.vanilla.banira.common.notification;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class NotificationPreparationFixture {
    static final int ENTRY_COUNT = 256;
    static final String LANGUAGE = "zh_cn";
    static final String[] CASES = {"literal", "rich", "expansion", "native"};

    private NotificationPreparationFixture() {
    }

    static Fixture create(String name) {
        Component prefix = text("notification-prefix: ").bold(true).color(Color.argb(0xff5555ff));
        prefix.getChildren().add(text("prefix-child: ").languageCode("ja_jp"));
        Component separator = text(" | ").italic(true);
        List<Component> entries = new ArrayList<>();
        for (int i = 0; i < ENTRY_COUNT; i++) {
            String marker = "entry-" + i + ":";
            Component entry;
            switch (name) {
                case "literal":
                    entry = text(marker + "\u4e2d\u6587\ud83c\udf38\"\\\n");
                    break;
                case "rich":
                    entry = new ScopedComponent("notification_preparation_absent")
                            .trans(EnumI18nType.FORMAT, "reward", marker)
                            .translationFallback("\u5956\u52b1 %s \ud83c\udf38\"\\\n")
                            .languageCode(LANGUAGE).bold(i % 2 == 0)
                            .color(Color.argb(0xff55ff55))
                            .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/reward " + i))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    net.minecraft.network.chat.Component.literal(repeat("\u60ac\u6d6e\ud83c\udf38", 8) + marker)));
                    entry.getArgs().get(0).languageCode(LANGUAGE);
                    entry.getChildren().add(text("explicit-child").languageCode("fr_fr"));
                    break;
                case "expansion":
                    entry = new ScopedComponent("notification_preparation_absent")
                            .trans(EnumI18nType.FORMAT, "expanded", marker + repeat("x", 350))
                            .translationFallback("%1$s%1$s%1$s%1$s").languageCode(LANGUAGE);
                    entry.getArgs().get(0).languageCode(LANGUAGE);
                    break;
                case "native":
                    net.minecraft.network.chat.Component original = net.minecraft.network.chat.Component.literal(marker)
                            .withStyle(Style.EMPTY.withBold(true).withUnderlined(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/native " + i))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            net.minecraft.network.chat.Component.literal("native-hover-" + i))))
                            .append(net.minecraft.network.chat.Component.literal("native-sibling-" + i));
                    entry = BaniraComponent.get().object(original).languageCode(LANGUAGE);
                    entry.getChildren().add(text("banira-child-" + i));
                    entry.getArgs().add(text("unused-original-arg-" + i));
                    break;
                default:
                    throw new IllegalArgumentException(name);
            }
            entries.add(entry);
        }
        return new Fixture(name, prefix, entries, separator);
    }

    static Component text(String value) {
        return BaniraComponent.get().literal(value).languageCode(LANGUAGE);
    }

    static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    static List<Encoding> production(Fixture fixture) {
        return encodings(NotificationBatch.prepare(fixture.prefix, fixture.entries, fixture.separator, LANGUAGE));
    }

    static List<Encoding> encodings(List<NotificationBudget.Payload> pages) {
        List<Encoding> result = new ArrayList<>();
        for (NotificationBudget.Payload page : pages)
            result.add(new Encoding(page.componentJson(), page.vanillaJson()));
        return result;
    }

    // Frozen greedy reference and preparation order from 1f4aa41aafb3f09f95f007595bb86ced2959a5a3.
    static List<Encoding> reference(Fixture fixture) {
        Objects.requireNonNull(fixture.prefix, "prefix");
        Objects.requireNonNull(fixture.entries, "entries");
        Objects.requireNonNull(fixture.separator, "separator");
        List<Encoding> pages = new ArrayList<>();
        Component page = fixture.prefix.clone();
        Encoding accepted = referencePrepare(page, LANGUAGE);
        int pageEntries = 0;
        for (int i = 0; i < fixture.entries.size(); i++) {
            Component entry = Objects.requireNonNull(fixture.entries.get(i), "entry " + i);
            if (pageEntries > 0) page.getChildren().add(fixture.separator.clone());
            page.getChildren().add(entry.clone());
            try {
                accepted = referencePrepare(page, LANGUAGE);
                pageEntries++;
            } catch (ReferenceLimit tooLarge) {
                if (pageEntries == 0) throw oversizedEntry(i, tooLarge);
                pages.add(accepted);
                page = fixture.prefix.clone();
                page.getChildren().add(entry.clone());
                try {
                    accepted = referencePrepare(page, LANGUAGE);
                } catch (ReferenceLimit singleTooLarge) {
                    throw oversizedEntry(i, singleTooLarge);
                }
                pageEntries = 1;
            }
        }
        pages.add(accepted);
        return Collections.unmodifiableList(pages);
    }

    private static IllegalArgumentException oversizedEntry(int index, RuntimeException cause) {
        return new IllegalArgumentException("Notification entry " + index
                + " with prefix exceeds the page budget; sentPages=0", cause);
    }

    private static Encoding referencePrepare(Component component, String language) {
        Component copy = Objects.requireNonNull(component, "component").clone().languageCodeIfEmpty(language);
        referenceDetach(copy, language);
        String componentJson = JsonUtils.toString(copy.toJson());
        referenceValidate(componentJson, "Banira component JSON");
        String vanillaJson = net.minecraft.network.chat.Component.Serializer.toJson(copy.toChat(language));
        referenceValidate(vanillaJson, "vanilla component JSON");
        return new Encoding(componentJson, vanillaJson);
    }

    private static void referenceValidate(String json, String field) {
        int bytes = json.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > 16384) throw new ReferenceLimit(field + " is " + bytes + " UTF-8 bytes; max 16384");
    }

    private static void referenceDetach(Component copy, String inheritedLanguage) {
        if (copy.original() instanceof net.minecraft.network.chat.Component) {
            copy.original(net.minecraft.network.chat.Component.Serializer.fromJson(
                    net.minecraft.network.chat.Component.Serializer.toJson((net.minecraft.network.chat.Component) copy.original())));
        }
        if (copy.hoverEvent() != null)
            copy.hoverEvent(HoverEvent.deserialize(copy.hoverEvent().serialize()));
        copy.languageCodeIfEmpty(inheritedLanguage);
        String language = inheritedLanguage == null ? copy.languageCodeOrDefault()
                : copy.languageCodeOrDefault(inheritedLanguage);
        copy.languageCode(language);
        for (Component child : copy.getChildren())
            referenceDetach(Objects.requireNonNull(child, "child"), language);
        for (Component arg : copy.getArgs()) {
            if (arg != null) referenceDetach(arg, language);
        }
    }

    static final class Fixture {
        final String name;
        final Component prefix;
        final List<Component> entries;
        final Component separator;

        Fixture(String name, Component prefix, List<Component> entries, Component separator) {
            this.name = name;
            this.prefix = prefix;
            this.entries = entries;
            this.separator = separator;
        }

        List<String> inputSnapshot() {
            List<String> result = new ArrayList<>();
            result.add(prefix.toJson().toString());
            result.add(separator.toJson().toString());
            for (Component entry : entries) result.add(entry.toJson().toString());
            return result;
        }
    }

    static final class Encoding {
        final String componentJson;
        final String vanillaJson;

        Encoding(String componentJson, String vanillaJson) {
            this.componentJson = componentJson;
            this.vanillaJson = vanillaJson;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Encoding)) return false;
            Encoding that = (Encoding) other;
            return componentJson.equals(that.componentJson) && vanillaJson.equals(that.vanillaJson);
        }

        @Override
        public int hashCode() {
            return 31 * componentJson.hashCode() + vanillaJson.hashCode();
        }
    }

    private static final class ReferenceLimit extends IllegalArgumentException {
        ReferenceLimit(String message) {
            super(message);
        }
    }
}
