package xin.vanilla.banira.api.cost;

import java.util.Objects;
import java.util.function.Function;

public final class CostPaymentService<C> {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    public interface PaymentAccess<C> {
        CostFailure validate(C context);
        CostFailure pay(C context, CostPaymentPlan plan);
        default void afterPayment(C context, CostPaymentPlan plan) { }
    }

    private final Function<C, CostPaymentPlan> planner;
    private final Function<C, CostIdentity> identity;
    private final PaymentAccess<C> access;

    public CostPaymentService(Function<C, CostPaymentPlan> planner, Function<C, CostIdentity> identity, PaymentAccess<C> access) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.identity = Objects.requireNonNull(identity, "identity");
        this.access = Objects.requireNonNull(access, "access");
    }

    public CostPaymentPlan plan(C context) {
        CostFailure validity = Objects.requireNonNull(access.validate(context), "validity");
        return validity.isFailure() ? CostPaymentPlan.failed(validity) : Objects.requireNonNull(planner.apply(context), "plan");
    }

    public CostPaymentPlan commit(CostOperation operation, C context) {
        Objects.requireNonNull(operation, "operation").requireOwner();
        CostFailure initial = operation.begin(identity.apply(context));
        if (initial.isFailure()) return CostPaymentPlan.failed(initial);
        CostPaymentPlan result;
        try {
            result = plan(context);
            if (result.isSuccess()) {
                if (result.isCommitted()) throw new IllegalStateException("Planner returned a paid plan");
                CostFailure failure = Objects.requireNonNull(access.validate(context), "validity");
                if (!failure.isFailure()) failure = operation.checkIdentity(identity.apply(context));
                if (!failure.isFailure()) failure = Objects.requireNonNull(access.pay(context, result), "payment failure");
                result = failure.isFailure() ? CostPaymentPlan.failed(failure) : result.paid();
            }
        } catch (RuntimeException error) {
            LOGGER.error("Cost payment failed for operation " + operation.identity().operationId(), error);
            result = CostPaymentPlan.failed(CostFailure.PAYMENT_FAILED);
        }
        operation.finish(result);
        if (result.isCommitted()) {
            try { access.afterPayment(context, result); }
            catch (RuntimeException error) {
                LOGGER.error("Cost payment follow-up failed for operation " + operation.identity().operationId(), error);
            }
        }
        return result;
    }
}
