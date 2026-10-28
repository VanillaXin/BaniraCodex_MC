package xin.vanilla.banira.api.client.ui;

import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class UiControlSpec {
    public enum Kind {TEXT, PASSWORD, KEY_CHORD, DECIMAL, FILE, COLOR, PICKER, SELECT}

    private final Kind kind;
    private final int maxLength;
    private final String regex;
    private final boolean multiple;
    private final List<DropdownOption> options;
    private final DropdownInputMode inputMode;

    public UiControlSpec(Kind kind, int maxLength, String regex, boolean multiple) {
        this(kind, maxLength, regex, multiple, Collections.emptyList(), DropdownInputMode.SELECTION_ONLY);
    }

    public UiControlSpec(Kind kind, int maxLength, String regex, boolean multiple,
                         List<DropdownOption> options, DropdownInputMode inputMode) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (maxLength < 0) throw new IllegalArgumentException("Negative input length");
        if (multiple && kind != Kind.SELECT) throw new IllegalArgumentException("Only selections can be multiple");
        this.maxLength = maxLength;
        this.regex = Objects.requireNonNull(regex, "regex");
        this.multiple = multiple;
        this.options = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(options, "options")));
        this.inputMode = Objects.requireNonNull(inputMode, "inputMode");
    }

    public Kind kind() {
        return kind;
    }

    public int maxLength() {
        return maxLength;
    }

    public String regex() {
        return regex;
    }

    public boolean multiple() {
        return multiple;
    }

    public List<DropdownOption> options() {
        return options;
    }

    public DropdownInputMode inputMode() {
        return inputMode;
    }
}
