package xin.vanilla.banira.common.network.packet;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.quickaction.CustomQuickActionStep;
import xin.vanilla.banira.api.quickaction.QuickActionExecutionMode;
import xin.vanilla.banira.api.quickaction.QuickActionStepCondition;
import xin.vanilla.banira.api.quickaction.QuickActionStepType;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.CommandUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 以玩家自身权限在服务端执行快捷入口指令，并在链式模式下使用真实结果判定后续步骤。 */
public final class QuickActionCommandsToServer implements NetworkPacket {
    public static final int MAX_STEPS = 32;
    private final QuickActionExecutionMode mode;
    private final List<CustomQuickActionStep> steps;

    public QuickActionCommandsToServer(QuickActionExecutionMode mode, List<CustomQuickActionStep> steps) {
        this.mode = mode == null ? QuickActionExecutionMode.PARALLEL : mode;
        List<CustomQuickActionStep> copy = steps == null ? Collections.emptyList() : steps;
        this.steps = Collections.unmodifiableList(new ArrayList<>(copy.subList(0, Math.min(copy.size(), MAX_STEPS))));
    }

    public QuickActionCommandsToServer(BaniraPacketBuffer buffer) {
        this.mode = readEnum(buffer.readUtf(32), QuickActionExecutionMode.class, QuickActionExecutionMode.PARALLEL);
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_STEPS) {
            throw new IllegalArgumentException("Invalid quick-action command count: " + count);
        }
        List<CustomQuickActionStep> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            decoded.add(new CustomQuickActionStep()
                    .setType(QuickActionStepType.COMMAND)
                    .setCondition(readEnum(buffer.readUtf(32), QuickActionStepCondition.class,
                            QuickActionStepCondition.ALWAYS))
                    .setValue(buffer.readUtf(1024)));
        }
        this.steps = Collections.unmodifiableList(decoded);
    }

    public void toBytes(BaniraPacketBuffer buffer) {
        buffer.writeUtf(mode.name(), 32);
        List<CustomQuickActionStep> commands = commandSteps(steps);
        buffer.writeVarInt(commands.size());
        for (CustomQuickActionStep step : commands) {
            QuickActionStepCondition condition = step.getCondition() == null
                    ? QuickActionStepCondition.ALWAYS : step.getCondition();
            buffer.writeUtf(condition.name(), 32);
            buffer.writeUtf(step.getValue(), 1024);
        }
    }

    public static void handle(QuickActionCommandsToServer packet, BaniraNetworkContext context) {
        context.enqueueWork(() -> {
            Object sender = context.sender();
            if (!context.isServerSide() || !(sender instanceof ServerPlayer)) {
                return;
            }
            boolean previousSuccess = true;
            for (CustomQuickActionStep step : commandSteps(packet.steps)) {
                if (packet.mode == QuickActionExecutionMode.CHAINED
                        && !conditionMatches(step.getCondition(), previousSuccess)) {
                    continue;
                }
                String command = normalizeCommand(step.getValue());
                previousSuccess = !command.isEmpty() && CommandUtils.executeCommand(
                        (ServerPlayer) sender, command, 0, false);
            }
        });
        context.markHandled();
    }

    private static List<CustomQuickActionStep> commandSteps(List<CustomQuickActionStep> source) {
        List<CustomQuickActionStep> result = new ArrayList<>();
        if (source == null) return result;
        for (CustomQuickActionStep step : source) {
            if (step != null && step.getType() == QuickActionStepType.COMMAND
                    && step.getValue() != null && !step.getValue().trim().isEmpty()) {
                result.add(step);
                if (result.size() >= MAX_STEPS) break;
            }
        }
        return result;
    }

    static boolean conditionMatches(QuickActionStepCondition condition, boolean previousSuccess) {
        if (condition == QuickActionStepCondition.ON_SUCCESS) return previousSuccess;
        if (condition == QuickActionStepCondition.ON_FAILURE) return !previousSuccess;
        return true;
    }

    private static String normalizeCommand(String command) {
        String value = command == null ? "" : command.trim();
        while (value.startsWith("/")) value = value.substring(1);
        return value;
    }

    private static <E extends Enum<E>> E readEnum(String value, Class<E> type, E fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
