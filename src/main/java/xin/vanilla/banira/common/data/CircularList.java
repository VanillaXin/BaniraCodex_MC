package xin.vanilla.banira.common.data;

import java.util.ArrayList;
import java.util.Arrays;

public class CircularList<E> extends ArrayList<E> {

    @SafeVarargs
    public CircularList(E... es) {
        super();
        this.addAll(Arrays.asList(es));
    }

    @Override
    public E get(int index) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("List is empty");
        }
        int actualIndex = index % size();
        if (actualIndex < 0) {
            actualIndex += size();
        }
        return super.get(actualIndex);
    }

    @Override
    public E set(int index, E element) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("List is empty");
        }
        int actualIndex = index % size();
        if (actualIndex < 0) {
            actualIndex += size();
        }
        return super.set(actualIndex, element);
    }

    public E getCircular(int index) {
        return get(index);
    }

    @SafeVarargs
    public static <E> CircularList<E> asList(E... es) {
        return new CircularList<>(es);
    }
}
