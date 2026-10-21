package xin.vanilla.banira.internal.client.ui;

import java.util.List;
import java.util.function.Predicate;

public final class UiFocusTraversal {
    private UiFocusTraversal() { }

    public static <T> T next(List<T> order, T current, Predicate<T> eligible, boolean reverse) {
        int size = order.size();
        if (size == 0) return null;
        int index = current == null ? -1 : order.indexOf(current);
        if (index < 0) index = reverse ? 0 : -1;
        int direction = reverse ? -1 : 1;
        for (int step = 0; step < size; step++) {
            index = Math.floorMod(index + direction, size);
            T candidate = order.get(index);
            if (eligible.test(candidate)) return candidate;
        }
        return null;
    }
}
