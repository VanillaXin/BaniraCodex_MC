package xin.vanilla.banira.client.enums;

import lombok.Getter;

@Getter
public enum EnumStringInputRegex {
    NONE(""),
    INTEGER("-?\\d*"),
    POSITIVE_INTEGER("\\d*"),
    DECIMAL("-?\\d*(?:\\.\\d+)?"),
    DECIMAL_5("-?\\d*(?:\\.\\d{0,5})?"),
    POSITIVE_DECIMAL("\\d*(?:\\.\\d+)?"),
    POSITIVE_DECIMAL_5("\\d*(?:\\.\\d{0,5})?"),
    PERCENTAGE("(1|0(\\.\\d+)?)?"),
    PERCENTAGE_5("(1|0(\\.\\d{0,5})?)?"),
    WORD("\\w*"),

    ;

    private final String regex;

    EnumStringInputRegex(String regex) {
        this.regex = regex;
    }
}
