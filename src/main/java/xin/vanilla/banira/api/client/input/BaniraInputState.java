package xin.vanilla.banira.api.client.input;

/**
 * 当前 Screen 使用的输入状态快照接口。
 * <p>实现类由各分支内部维护，子 mod 只应依赖这里列出的稳定查询方法。</p>
 */
public interface BaniraInputState {
    double mouseX();

    double mouseY();

    boolean isKeyPressed(int keyCode);

    boolean isKeyPressed(String keyNames);

    boolean isShiftPressing();

    boolean isCtrlPressing();

    boolean isAltPressing();

    boolean isMousePressed(int button);

    boolean isEscapePressed();

    boolean isEnterPressed();

    boolean isBackspacePressed();

    boolean isPressingLeftEx();

    boolean isPressingRightEx();
}
