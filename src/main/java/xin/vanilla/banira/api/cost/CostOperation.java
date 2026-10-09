package xin.vanilla.banira.api.cost;

import java.util.Objects;

public final class CostOperation {
    public enum State { PENDING, COMMITTING, PAID, FAILED, CANCELLED }
    private final Thread owner = Thread.currentThread();
    private final CostIdentity identity;
    private State state = State.PENDING;

    private CostOperation(CostIdentity identity) { this.identity = Objects.requireNonNull(identity, "identity"); }
    public static CostOperation create(CostIdentity identity) { return new CostOperation(identity); }
    public CostIdentity identity() { return identity; }
    public State state() { requireOwner(); return state; }
    public boolean cancel() {
        requireOwner();
        if (state != State.PENDING) return false;
        state = State.CANCELLED;
        return true;
    }

    public CostFailure begin(CostIdentity current) {
        requireOwner();
        Objects.requireNonNull(current, "current");
        if (state != State.PENDING) return CostFailure.INVALID_OPERATION;
        CostFailure failure = checkIdentity(current);
        state = failure.isFailure() ? State.FAILED : State.COMMITTING;
        return failure;
    }

    CostFailure checkIdentity(CostIdentity current) {
        requireOwner();
        Objects.requireNonNull(current, "current");
        CostFailure failure = CostFailure.NONE;
        if (identity.generation() != current.generation()
                || !identity.configurationSignature().equals(current.configurationSignature())) failure = CostFailure.CONFIGURATION_UNAVAILABLE;
        else if (identity.operationId() != current.operationId() || !identity.domain().equals(current.domain())
                || !identity.payer().equals(current.payer()) || !identity.businessSignature().equals(current.businessSignature()))
            failure = CostFailure.INVALID_OPERATION;
        return failure;
    }

    public void finish(CostPaymentPlan result) {
        requireOwner();
        if (state != State.COMMITTING) throw new IllegalStateException("Operation is not committing");
        state = Objects.requireNonNull(result, "result").isCommitted() ? State.PAID : State.FAILED;
    }

    void requireOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Payment requires its owning server thread");
    }
}
