package xin.vanilla.banira.common.data;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

/**
 * 基于 {@link ArrayList} 实现的 Set</br>
 * 不允许重复元素但是保持插入顺序
 *
 * @param <E> 元素类型
 */
public class ArraySet<E> implements Collection<E> {
    private final List<E> list = new ArrayList<>();

    public ArraySet() {
    }

    /**
     * 从集合创建ArraySet
     */
    public ArraySet(Collection<? extends E> elements) {
        if (elements != null) {
            for (E element : elements) {
                if (!list.contains(element)) {
                    list.add(element);
                }
            }
        }
    }

    /**
     * 添加元素
     *
     * @return 元素不存在且添加成功
     */
    @Override
    public boolean add(E e) {
        if (!list.contains(e)) {
            list.add(e);
            return true;
        }
        return false;
    }

    /**
     * 添加集合中的所有元素
     *
     * @param elements 要添加的元素集合
     * @return 是否至少添加了一个新元素
     */
    @Override
    public boolean addAll(@Nonnull Collection<? extends E> elements) {
        if (elements.isEmpty()) {
            return false;
        }
        boolean modified = false;
        for (E element : elements) {
            if (add(element)) {
                modified = true;
            }
        }
        return modified;
    }

    /**
     * 在指定索引处添加集合中的所有元素
     *
     * @param index 插入位置
     * @param c     要添加的元素集合
     * @return 是否至少添加了一个新元素
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }
        boolean modified = false;
        int insertIndex = index;
        for (E element : c) {
            if (!list.contains(element)) {
                list.add(insertIndex, element);
                insertIndex++;
                modified = true;
            }
        }
        return modified;
    }

    /**
     * 移除元素
     *
     * @param o 要移除的元素
     * @return 元素存在且成功移除
     */
    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    /**
     * 移除指定索引处的元素
     *
     * @param index 要移除的元素索引
     * @return 被移除的元素
     * @throws IndexOutOfBoundsException 索引超出范围
     */
    public E remove(int index) {
        return list.remove(index);
    }

    /**
     * 获取指定索引处的元素
     *
     * @param index 索引
     * @return 元素
     * @throws IndexOutOfBoundsException 索引超出范围
     */
    public E get(int index) {
        return list.get(index);
    }

    /**
     * 获取元素的索引
     *
     * @return 元素的索引，不存在返回-1
     */
    public int indexOf(E e) {
        return list.indexOf(e);
    }

    /**
     * 返回集合的大小
     *
     * @return 集合中元素的数量
     */
    @Override
    public int size() {
        return list.size();
    }

    /**
     * 检查集合是否为空
     */
    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * 检查是否包含某个元素
     */
    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    /**
     * 检查是否包含集合中的所有元素
     */
    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return new HashSet<>(list).containsAll(c);
    }

    /**
     * 清空集合
     */
    @Override
    public void clear() {
        list.clear();
    }

    /**
     * 移除集合中的所有元素
     *
     * @param c 要移除的元素集合
     * @return 是否至少移除了一个元素
     */
    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        if (c.isEmpty()) {
            return false;
        }
        boolean modified = false;
        Iterator<E> it = list.iterator();
        while (it.hasNext()) {
            if (c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * 仅保留集合中的元素
     *
     * @param c 要保留的元素集合
     * @return 是否至少移除了一个元素
     */
    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        if (c.isEmpty()) {
            return false;
        }
        boolean modified = false;
        Iterator<E> it = list.iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * 获取不可变列表视图
     */
    public List<E> asList() {
        return Collections.unmodifiableList(list);
    }

    /**
     * 转换为数组
     */
    @Nonnull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    /**
     * 转换为指定类型的数组
     */
    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        return list.toArray(a);
    }

    /**
     * 返回迭代器
     */
    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    /**
     * 返回流
     */
    @Nonnull
    public Stream<E> stream() {
        return list.stream();
    }

    /**
     * 返回并行流
     */
    @Nonnull
    public Stream<E> parallelStream() {
        return list.parallelStream();
    }

    /**
     * 返回集合的字符串表示
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return list.toString();
    }
}
