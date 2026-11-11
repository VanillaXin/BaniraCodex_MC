package xin.vanilla.banira.common.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Whole-entry notification pages, preflighted before any transport submission.
 */
public final class NotificationBatch {
    private static final Logger LOGGER = LogManager.getLogger();

    private NotificationBatch() {
    }

    public static List<NotificationBudget.Payload> prepare(Component prefix, List<Component> entries,
                                                           Component separator, String language) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(separator, "separator");
        PreparedPlainPage plain = PreparedPlainPage.forBatch(prefix, entries, separator, language);
        List<NotificationBudget.Payload> pages = new ArrayList<>();
        Component page = plain == null ? prefix.clone() : plain.newPage();
        NotificationBudget.Payload accepted = preparePage(page, language, plain);
        int pageEntries = 0;
        for (int i = 0; i < entries.size(); i++) {
            Component entry = Objects.requireNonNull(entries.get(i), "entry " + i);
            if (pageEntries > 0) page.getChildren().add(plain == null ? separator.clone() : plain.separator);
            // append() propagates styles by mutating its argument. Keep each
            // complete child unchanged, including explicit colors and events.
            Component preparedEntry = plain == null ? entry.clone() : plain.entry(entry);
            page.getChildren().add(preparedEntry);
            try {
                accepted = preparePage(page, language, plain);
                pageEntries++;
            } catch (NotificationBudget.LimitExceededException tooLarge) {
                if (pageEntries == 0) throw oversizedEntry(i, tooLarge);
                pages.add(accepted);
                page = plain == null ? prefix.clone() : plain.newPage();
                page.getChildren().add(plain == null ? entry.clone() : preparedEntry);
                try {
                    accepted = preparePage(page, language, plain);
                } catch (NotificationBudget.LimitExceededException singleTooLarge) {
                    throw oversizedEntry(i, singleTooLarge);
                }
                pageEntries = 1;
            }
        }
        pages.add(accepted);
        return Collections.unmodifiableList(pages);
    }

    private static NotificationBudget.Payload preparePage(Component page, String language, PreparedPlainPage plain) {
        return plain == null ? NotificationBudget.prepare(page, language)
                : NotificationBudget.encodePrepared(page, language);
    }

    private static final class PreparedPlainPage {
        // This exact factory capture returns only its String argument. Unknown suppliers are never sampled.
        private static final Class<?> CONSTANT_LANGUAGE = BaniraComponent.get().literal("")
                .languageCode("").languageCode().getClass();
        private final Component prefix;
        private final Component separator;
        private final String inheritedLanguage;

        private PreparedPlainPage(Component prefix, Component separator, String language) {
            this.prefix = freeze(prefix, language);
            this.inheritedLanguage = this.prefix.languageCodeOrDefault(language);
            this.separator = freeze(separator, inheritedLanguage);
        }

        private static PreparedPlainPage forBatch(Component prefix, List<Component> entries,
                                                  Component separator, String language) {
            // Exact ArrayList access cannot invoke caller code while checking the whole batch.
            if (language == null || entries.getClass() != ArrayList.class || entries.isEmpty()
                    || !supports(prefix, true) || !supports(separator, false)) return null;
            for (Component entry : entries) {
                if (!supports(entry, false)) return null;
            }
            return new PreparedPlainPage(prefix, separator, language);
        }

        private static boolean supports(Component value, boolean prefix) {
            if (value == null || value.getClass() != Component.class || value.i18nType() != EnumI18nType.PLAIN
                    || value.text() == null || value.original() != null
                    || value.hoverEvent() != null || value.clickEvent() != null
                    || value.color() == null || value.color().getClass() != Color.class
                    || value.bgColor() == null || value.bgColor().getClass() != Color.class
                    || value.languageCode() != null && value.languageCode().getClass() != CONSTANT_LANGUAGE)
                return false;
            List<Component> args = value.getArgs();
            List<Component> children = value.getChildren();
            if (args.getClass() != ArrayList.class || !args.isEmpty() || children.getClass() != ArrayList.class)
                return false;
            if (!prefix) return children.isEmpty();
            for (Component child : children) {
                if (!supports(child, false)) return false;
            }
            return true;
        }

        private Component newPage() {
            return prefix.clone();
        }

        private Component entry(Component source) {
            return freeze(source, inheritedLanguage);
        }

        private static Component freeze(Component source, String inheritedLanguage) {
            Component copy = source.clone();
            bind(copy, inheritedLanguage);
            return copy;
        }

        private static void bind(Component owned, String inheritedLanguage) {
            owned.color(Color.argb(owned.color().argb())).bgColor(Color.argb(owned.bgColor().argb()));
            String language = owned.languageCodeOrDefault(inheritedLanguage);
            owned.languageCode(language);
            for (Component child : owned.getChildren()) bind(child, language);
        }
    }

    private static IllegalArgumentException oversizedEntry(int index, RuntimeException cause) {
        return new IllegalArgumentException("Notification entry " + index
                + " with prefix exceeds the page budget; sentPages=0", cause);
    }

    public static void send(Component prefix, List<Component> entries, Component separator, String language,
                            Consumer<NotificationBudget.Payload> sender) {
        Objects.requireNonNull(sender, "sender");
        List<NotificationBudget.Payload> pages = prepare(prefix, entries, separator, language);
        int sent = 0;
        try {
            for (NotificationBudget.Payload page : pages) {
                sender.accept(page);
                sent++;
            }
        } catch (RuntimeException cause) {
            SendException failure = new SendException(sent, pages.size(), cause);
            LOGGER.error(failure.getMessage(), cause);
            throw failure;
        }
    }

    public static final class SendException extends IllegalStateException {
        private final int sentPages;
        private final int totalPages;

        private SendException(int sentPages, int totalPages, RuntimeException cause) {
            super("Notification batch submission failed: sentPages=" + sentPages + ", totalPages=" + totalPages
                    + "; no retry attempted", cause);
            this.sentPages = sentPages;
            this.totalPages = totalPages;
        }

        public int sentPages() {
            return sentPages;
        }

        public int totalPages() {
            return totalPages;
        }
    }
}
