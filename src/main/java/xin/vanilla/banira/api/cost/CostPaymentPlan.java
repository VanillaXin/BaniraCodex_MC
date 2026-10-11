package xin.vanilla.banira.api.cost;

import java.util.*;

public final class CostPaymentPlan {
    private final int units;
    private final List<ResourceCost> requirements;
    private final List<ResourceSnapshot> snapshots;
    private final CostFailure failure;
    private final boolean committed;
    private final CostFailure recoveryFailure;
    private final boolean confirmedSnapshots;

    private CostPaymentPlan(int units, List<ResourceCost> requirements, List<ResourceSnapshot> snapshots,
                            CostFailure failure, boolean committed) {
        this(units, requirements, snapshots, failure, committed, CostFailure.NONE);
    }

    private CostPaymentPlan(int units, List<ResourceCost> requirements, List<ResourceSnapshot> snapshots,
                            CostFailure failure, boolean committed, CostFailure recoveryFailure) {
        this(units, requirements, snapshots, failure, committed, recoveryFailure, false);
    }

    private CostPaymentPlan(int units, List<ResourceCost> requirements, List<ResourceSnapshot> snapshots,
                            CostFailure failure, boolean committed, CostFailure recoveryFailure, boolean confirmedSnapshots) {
        this.units = units;
        this.requirements = immutable(requirements);
        this.snapshots = immutable(snapshots);
        this.failure = failure;
        this.committed = committed;
        this.recoveryFailure = recoveryFailure;
        this.confirmedSnapshots = confirmedSnapshots;
    }

    private static <T> List<T> immutable(List<T> values) {
        List<T> copy = new ArrayList<>(Objects.requireNonNull(values, "values"));
        for (T value : copy) Objects.requireNonNull(value, "value");
        return Collections.unmodifiableList(copy);
    }

    public static CostPaymentPlan ready(int units, List<ResourceCost> requirements, List<ResourceSnapshot> snapshots) {
        if (units <= 0) throw new IllegalArgumentException("Positive units required");
        return new CostPaymentPlan(units, requirements, snapshots, CostFailure.NONE, false);
    }

    public static CostPaymentPlan failed(CostFailure failure) {
        if (!Objects.requireNonNull(failure, "failure").isFailure()) throw new IllegalArgumentException("Failure required");
        return new CostPaymentPlan(0, Collections.emptyList(), Collections.emptyList(), failure, false);
    }

    public int units() { return units; }
    public List<ResourceCost> requirements() { return requirements; }
    public List<ResourceSnapshot> snapshots() { return snapshots; }
    public CostFailure failure() { return failure; }
    public boolean isSuccess() { return !failure.isFailure(); }
    public boolean isCommitted() { return committed; }
    public CostFailure recoveryFailure() { return recoveryFailure; }
    public boolean hasConfirmedSnapshots() { return confirmedSnapshots; }

    public CostPaymentPlan withSnapshots(List<ResourceSnapshot> actual) {
        if (!isSuccess() || actual.size() != snapshots.size()) throw new IllegalArgumentException("Receipt shape changed");
        for (int i = 0; i < actual.size(); i++) {
            ResourceSnapshot before = snapshots.get(i), after = Objects.requireNonNull(actual.get(i), "snapshot");
            if (!before.resourceId().equals(after.resourceId()) || !before.before().equals(after.before())
                    || before.reversible() != after.reversible()) throw new IllegalArgumentException("Receipt identity changed");
        }
        return new CostPaymentPlan(units, requirements, actual, failure, committed, recoveryFailure, committed);
    }

    public CostPaymentPlan uncertainReceipt() {
        if (!committed) throw new IllegalStateException("Payment is not committed");
        return new CostPaymentPlan(units, requirements, snapshots, failure, true, CostFailure.UNCERTAIN, false);
    }
    public CostPaymentPlan paid() {
        if (!isSuccess() || committed) throw new IllegalStateException("Payment is not pending");
        return new CostPaymentPlan(units, requirements, snapshots, failure, true);
    }

    public static final class ResourceSnapshot {
        private final String resourceId, before, after;
        private final boolean reversible;

        public ResourceSnapshot(String resourceId, String before, String after, boolean reversible) {
            this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
            this.before = Objects.requireNonNull(before, "before");
            this.after = Objects.requireNonNull(after, "after");
            this.reversible = reversible;
        }
        public String resourceId() { return resourceId; }
        public String before() { return before; }
        public String after() { return after; }
        public boolean reversible() { return reversible; }
    }
}
