package xin.vanilla.banira.common.notification;

import net.minecraft.core.RegistryAccess;
import xin.vanilla.banira.common.data.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Whole-entry notification pages, preflighted before any transport submission. */
public final class NotificationBatch {
    private static final Logger LOGGER = LogManager.getLogger();

    private NotificationBatch() {
    }

    public static List<NotificationBudget.Payload> prepare(Component prefix, List<Component> entries,
                                                            Component separator, String language) {
        return prepare(prefix, entries, separator, language, RegistryAccess.EMPTY);
    }

    public static List<NotificationBudget.Payload> prepare(Component prefix, List<Component> entries,
                                                            Component separator, String language, RegistryAccess registries) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(separator, "separator");
        List<NotificationBudget.Payload> pages = new ArrayList<>();
        Component page = prefix.clone();
        NotificationBudget.Payload accepted = NotificationBudget.prepare(page, language, registries);
        int pageEntries = 0;
        for (int i = 0; i < entries.size(); i++) {
            Component entry = Objects.requireNonNull(entries.get(i), "entry " + i);
            if (pageEntries > 0) page.getChildren().add(separator.clone());
            // append() propagates styles by mutating its argument. Keep each
            // complete child unchanged, including explicit colors and events.
            page.getChildren().add(entry.clone());
            try {
                accepted = NotificationBudget.prepare(page, language, registries);
                pageEntries++;
            } catch (NotificationBudget.LimitExceededException tooLarge) {
                if (pageEntries == 0) throw oversizedEntry(i, tooLarge);
                pages.add(accepted);
                page = prefix.clone();
                page.getChildren().add(entry.clone());
                try {
                    accepted = NotificationBudget.prepare(page, language, registries);
                } catch (NotificationBudget.LimitExceededException singleTooLarge) {
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

    public static void send(Component prefix, List<Component> entries, Component separator, String language,
                            Consumer<NotificationBudget.Payload> sender) {
        send(prefix, entries, separator, language, RegistryAccess.EMPTY, sender);
    }

    public static void send(Component prefix, List<Component> entries, Component separator, String language,
                            RegistryAccess registries, Consumer<NotificationBudget.Payload> sender) {
        Objects.requireNonNull(sender, "sender");
        List<NotificationBudget.Payload> pages = prepare(prefix, entries, separator, language, registries);
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
