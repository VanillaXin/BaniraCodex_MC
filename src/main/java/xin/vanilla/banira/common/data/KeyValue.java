package xin.vanilla.banira.common.data;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
public class KeyValue<K, V> {
    private K key;
    private V value;

    public V val() {
        return this.value;
    }

    public KeyValue<K, V> val(V value) {
        return this.value(value);
    }

    public K left() {
        return this.key;
    }

    public KeyValue<K, V> left(K key) {
        return this.key(key);
    }

    public V right() {
        return this.value;
    }

    public KeyValue<K, V> right(V value) {
        return this.value(value);
    }
}
