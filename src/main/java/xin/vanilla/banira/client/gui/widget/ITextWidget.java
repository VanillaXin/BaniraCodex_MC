package xin.vanilla.banira.client.gui.widget;

import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.common.data.Component;

public interface ITextWidget {

    ITextWidget text(String text);

    ITextWidget text(Component text);

    ITextWidget text(Text text);

    Text text();

}
