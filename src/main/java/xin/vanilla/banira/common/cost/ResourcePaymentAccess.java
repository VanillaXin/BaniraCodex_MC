package xin.vanilla.banira.common.cost;

import xin.vanilla.banira.api.cost.*;
import xin.vanilla.banira.api.cost.CostPaymentPlan.ResourceSnapshot;

import java.util.*;
import java.util.function.Supplier;

public final class ResourcePaymentAccess implements CostAccess {
    private static final Set<String> BUILTINS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "banira:item", "banira:xp_point", "banira:xp_level", "banira:health", "banira:hunger", "banira:score")));
    private final Thread owner = Thread.currentThread();
    private final Map<String, CostResourceAccess> resources;
    private final Supplier<CostFailure> eligibility;
    private final Map<CostPaymentPlan, Attempt> attempts = new WeakHashMap<>();

    public ResourcePaymentAccess(Map<String, CostResourceAccess> resources, Supplier<CostFailure> eligibility) {
        this.resources = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(resources, "resources")));
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
        for (Map.Entry<String, CostResourceAccess> entry : this.resources.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "resource id");
            Objects.requireNonNull(entry.getValue(), "resource");
        }
    }

    @Override public CostAccess withResource(String id, CostResourceAccess resource) {
        requireOwner();
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(resource, "resource");
        if (BUILTINS.contains(id) || resources.containsKey(id) || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Reserved or invalid resource id");
        Map<String, CostResourceAccess> copy = new LinkedHashMap<>(resources);
        copy.put(id, resource);
        return new ResourcePaymentAccess(copy, eligibility);
    }

    @Override public CostPaymentPlan preview(CostRequirement requirement, int units) {
        requireOwner();
        Objects.requireNonNull(requirement, "requirement");
        if (units <= 0) throw new IllegalArgumentException("Positive units required");
        CostFailure eligible = eligible();
        if (eligible.isFailure()) return CostPaymentPlan.failed(eligible);
        try {
            List<ResourceCost> leaves = new ArrayList<>();
            if (!flatten(requirement, leaves, 1)) return CostPaymentPlan.failed(CostFailure.INVALID_PAYMENT);
            if (leaves.stream().anyMatch(c -> c.resourceId().equals("banira:xp_point"))
                    && leaves.stream().anyMatch(c -> c.resourceId().equals("banira:xp_level")))
                return CostPaymentPlan.failed(CostFailure.INVALID_PAYMENT);
            leaves.sort(Comparator.comparingInt(c -> c.resourceId().equals("banira:item")
                    && c.matchMode() == ResourceCost.MatchMode.ID ? 1 : 0));
            List<ResourceCost> scaled = new ArrayList<>();
            Map<String, ResourceSnapshot> snapshots = new LinkedHashMap<>();
            for (ResourceCost leaf : leaves) {
                ResourceCost cost = new ResourceCost(leaf.resourceId(), Math.multiplyExact(leaf.amount(), units),
                        leaf.specification(), leaf.matchMode());
                CostResourceAccess resource = resources.get(cost.resourceId());
                if (resource == null) return CostPaymentPlan.failed(CostFailure.INVALID_PAYMENT);
                if (cost.amount() == 0) continue;
                ResourceSnapshot previous = snapshots.get(cost.resourceId());
                String working = previous == null ? Objects.requireNonNull(resource.snapshot(), "snapshot") : previous.after();
                CostFailure failure = Objects.requireNonNull(resource.check(cost, 1, working), "check failure");
                if (failure.isFailure()) return CostPaymentPlan.failed(failure);
                String after = Objects.requireNonNull(resource.after(cost, 1, working), "planned after");
                snapshots.put(cost.resourceId(), new ResourceSnapshot(cost.resourceId(),
                        previous == null ? working : previous.before(), after, resource.reversible()));
                scaled.add(cost);
            }
            CostPaymentPlan plan = CostPaymentPlan.ready(units, scaled, new ArrayList<>(snapshots.values()));
            attempts.put(plan, new Attempt());
            return plan;
        } catch (ArithmeticException overflow) {
            return CostPaymentPlan.failed(CostFailure.AMOUNT_OVERFLOW);
        } catch (IllegalArgumentException invalid) {
            return CostPaymentPlan.failed(CostFailure.INVALID_PAYMENT);
        }
    }

    private boolean flatten(CostRequirement node, List<ResourceCost> leaves, int depth) {
        if (depth > 16) return false;
        switch (node.kind()) {
            case FREE: return true;
            case LEAF: leaves.add(node.resource()); return leaves.size() <= 32;
            case ALL:
                for (CostRequirement child : node.children()) if (!flatten(child, leaves, depth + 1)) return false;
                return true;
            default: return false;
        }
    }

    @Override public CostFailure recheck(CostPaymentPlan plan) {
        requireOwner();
        Objects.requireNonNull(plan, "plan");
        Attempt attempt = attempts.get(plan);
        if (!plan.isSuccess() || plan.isCommitted() || attempt == null || attempt.started) return CostFailure.INVALID_OPERATION;
        CostFailure eligible = eligible();
        if (eligible.isFailure()) return eligible;
        for (ResourceSnapshot snapshot : plan.snapshots()) {
            CostResourceAccess resource = resources.get(snapshot.resourceId());
            if (resource == null || !snapshot.before().equals(resource.snapshot())) return CostFailure.INVALID_OPERATION;
        }
        return CostFailure.NONE;
    }

    @Override public CostFailure debit(CostPaymentPlan plan) {
        requireOwner();
        CostFailure initial = recheck(plan);
        Attempt attempt = attempts.get(plan);
        if (initial.isFailure()) {
            if (attempt != null) attempt.started = true;
            return initial;
        }
        attempt.started = true;
        List<ResourceSnapshot> paid = new ArrayList<>();
        try {
            for (int index = 0; index < plan.snapshots().size(); index++) {
                ResourceSnapshot snapshot = plan.snapshots().get(index);
                CostResourceAccess resource = resources.get(snapshot.resourceId());
                CostFailure failure = eligible();
                if (!failure.isFailure() && !snapshot.before().equals(resource.snapshot())) failure = CostFailure.INVALID_OPERATION;
                if (!failure.isFailure()) failure = Objects.requireNonNull(resource.debit(snapshot.before(), snapshot.after()), "debit failure");
                if (failure.isFailure()) {
                    boolean unchanged = snapshot.before().equals(resource.snapshot());
                    CostFailure rollback = rollback(paid, true);
                    return !unchanged || rollback.isFailure() ? CostFailure.UNCERTAIN : failure;
                }
                try {
                    paid.add(new ResourceSnapshot(snapshot.resourceId(), snapshot.before(),
                            Objects.requireNonNull(resource.snapshot(), "paid snapshot"), snapshot.reversible()));
                } catch (RuntimeException unknownReceipt) {
                    if (index == plan.snapshots().size() - 1) {
                        attempt.receiptFailure = unknownReceipt;
                        return CostFailure.NONE;
                    }
                    rollback(paid, true);
                    return CostFailure.UNCERTAIN;
                }
            }
            attempt.receipt = CostPaymentPlan.ready(plan.units(), plan.requirements(), paid).paid().withSnapshots(paid);
            return CostFailure.NONE;
        } catch (RuntimeException unknown) {
            rollback(paid, true);
            return CostFailure.UNCERTAIN;
        }
    }

    @Override public CostPaymentPlan receipt(CostPaymentPlan plan) {
        requireOwner();
        Attempt attempt = attempts.get(plan);
        if (attempt != null && attempt.receiptFailure != null) throw new IllegalStateException("Paid receipt unavailable", attempt.receiptFailure);
        if (attempt == null || attempt.receipt == null) throw new IllegalStateException("Payment receipt unavailable");
        if (!attempt.finalized) {
            List<ResourceSnapshot> actual = new ArrayList<>();
            for (ResourceSnapshot snapshot : attempt.receipt.snapshots()) actual.add(new ResourceSnapshot(snapshot.resourceId(),
                    snapshot.before(), resources.get(snapshot.resourceId()).snapshot(), snapshot.reversible()));
            attempt.receipt = attempt.receipt.withSnapshots(actual);
            attempt.finalized = true;
        }
        return attempt.receipt;
    }

    @Override public CostFailure restore(CostPaymentPlan plan) {
        requireOwner();
        Objects.requireNonNull(plan, "plan");
        if (plan.recoveryFailure().isFailure() || eligible().isFailure()) return CostFailure.UNCERTAIN;
        if (!plan.isSuccess() || !plan.isCommitted() || !plan.hasConfirmedSnapshots()) return CostFailure.INVALID_OPERATION;
        return rollback(plan.snapshots(), false);
    }

    private CostFailure rollback(List<ResourceSnapshot> paid, boolean reservationFailure) {
        CostFailure result = CostFailure.NONE;
        for (int i = paid.size() - 1; i >= 0; i--) {
            if (eligible().isFailure()) return CostFailure.UNCERTAIN;
            ResourceSnapshot snapshot = paid.get(i);
            CostResourceAccess resource = resources.get(snapshot.resourceId());
            try {
                if (resource == null || !snapshot.reversible() || !resource.reversible()) { result = CostFailure.UNCERTAIN; continue; }
                if (!snapshot.after().equals(resource.snapshot())) {
                    if (!reservationFailure || resource.compensateReservation(snapshot.before(), snapshot.after()).isFailure())
                        result = CostFailure.UNCERTAIN;
                    continue;
                }
                if (resource.restore(snapshot.before(), snapshot.after()).isFailure()
                        || !snapshot.before().equals(resource.snapshot())) result = CostFailure.UNCERTAIN;
            } catch (RuntimeException unknown) { result = CostFailure.UNCERTAIN; }
        }
        return result;
    }

    @Override public void afterPayment(CostPaymentPlan plan) {
        requireOwner();
        for (ResourceSnapshot snapshot : plan.snapshots()) {
            CostResourceAccess resource = resources.get(snapshot.resourceId());
            if (resource != null) resource.afterPayment();
        }
    }

    private CostFailure eligible() { return Objects.requireNonNull(eligibility.get(), "eligibility"); }
    private void requireOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Resource access requires its owning server thread");
    }
    private static final class Attempt {
        boolean started;
        CostPaymentPlan receipt;
        RuntimeException receiptFailure;
        boolean finalized;
    }
}
