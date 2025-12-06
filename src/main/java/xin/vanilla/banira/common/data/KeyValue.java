package xin.vanilla.banira.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class KeyValue<K, V> {
    private K key;
    private V value;

    public K key() {
        return this.key;
    }

    public V val() {
        return this.value;
    }

    public K left() {
        return this.key;
    }

    public V right() {
        return this.value;
    }
}
