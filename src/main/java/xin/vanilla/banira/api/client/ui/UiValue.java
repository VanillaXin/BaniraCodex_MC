package xin.vanilla.banira.api.client.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class UiValue {
    public enum Kind { TEXT, DECIMAL, BOOLEAN, STRING_LIST, KEY_CHORDS }
    private final Kind kind;
    private final Object value;

    private UiValue(Kind kind, Object value) {
        this.kind = kind;
        this.value = Objects.requireNonNull(value, "value");
    }

    public static UiValue text(String value) { return new UiValue(Kind.TEXT, value); }
    public static UiValue decimal(BigDecimal value) { return new UiValue(Kind.DECIMAL, value); }
    public static UiValue bool(boolean value) { return new UiValue(Kind.BOOLEAN, value); }
    public static UiValue strings(List<String> value) { return list(Kind.STRING_LIST, value); }
    public static UiValue keyChords(List<String> value) { return list(Kind.KEY_CHORDS, value); }

    private static UiValue list(Kind kind, List<String> value) {
        List<String> copy = new ArrayList<>();
        for (String item : Objects.requireNonNull(value, "value")) copy.add(Objects.requireNonNull(item, "item"));
        return new UiValue(kind, Collections.unmodifiableList(copy));
    }

    public Kind kind() { return kind; }
    private Object require(Kind expected) {
        if (kind != expected) throw new IllegalStateException("Expected " + expected + ", found " + kind);
        return value;
    }
    public String asText() { return (String) require(Kind.TEXT); }
    public BigDecimal asDecimal() { return (BigDecimal) require(Kind.DECIMAL); }
    public boolean asBoolean() { return (Boolean) require(Kind.BOOLEAN); }
    @SuppressWarnings("unchecked")
    public List<String> asStrings() { return (List<String>) require(Kind.STRING_LIST); }
    @SuppressWarnings("unchecked")
    public List<String> asKeyChords() { return (List<String>) require(Kind.KEY_CHORDS); }
}
