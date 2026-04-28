package xin.vanilla.banira.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
public class KeyValue<K, V> {
    private K key;
    private V value;

    public K key() {
        return key;
    }

    public KeyValue<K, V> key(K key) {
        this.key = key;
        return this;
    }

    public V value() {
        return value;
    }

    public KeyValue<K, V> value(V value) {
        this.value = value;
        return this;
    }

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
