package xin.vanilla.banira.client.enums;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

@Getter
@Accessors(fluent = true)
public enum EnumStringInputRegex implements IEnumDescribable {
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

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
