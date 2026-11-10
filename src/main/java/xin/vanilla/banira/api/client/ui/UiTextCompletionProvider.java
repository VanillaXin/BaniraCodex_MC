package xin.vanilla.banira.api.client.ui;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface UiTextCompletionProvider {
    CompletableFuture<List<UiTextCompletion>> complete(String text, int cursor);
}
