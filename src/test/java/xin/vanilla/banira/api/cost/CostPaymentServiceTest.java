package xin.vanilla.banira.api.cost;

import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class CostPaymentServiceTest {
    @Test
    public void previewIsReadOnlyAndCommitCannotReplay() {
        Fixture f = new Fixture();
        assertTrue(f.service.plan(f).isSuccess());
        assertTrue(f.service.plan(f).isSuccess());
        assertEquals(0, f.debits);
        assertEquals(10, f.balance);
        CostOperation op = CostOperation.create(f.identity());
        assertTrue(f.service.commit(op, f).isCommitted());
        assertEquals(CostOperation.State.PAID, op.state());
        assertEquals(CostFailure.INVALID_OPERATION, f.service.commit(op, f).failure());
        assertEquals(1, f.debits);
        assertEquals(5, f.balance);
        assertEquals(1, f.followups);
    }

    @Test
    public void followupFailureLeavesPaymentTerminal() {
        Fixture f = new Fixture();
        f.followup = () -> { throw new IllegalStateException("Transport gone"); };
        CostOperation op = CostOperation.create(f.identity());
        assertTrue(f.service.commit(op, f).isCommitted());
        assertEquals(CostOperation.State.PAID, op.state());
        assertEquals(CostFailure.INVALID_OPERATION, f.service.commit(op, f).failure());
        assertEquals(1, f.debits);
    }

    @Test
    public void changedIdentitiesFailBeforePlanningOrDebit() {
        for (int change = 0; change < 5; change++) {
            Fixture f = new Fixture();
            CostOperation op = CostOperation.create(f.identity());
            switch (change) {
                case 0: f.payer = new UUID(0, 2); break;
                case 1: f.generation++; break;
                case 2: f.signature = "other-target"; break;
                case 3: f.domain = "other:business"; break;
                case 4: f.id++; break;
                default: throw new AssertionError();
            }
            assertEquals(change == 1 ? CostFailure.CONFIGURATION_UNAVAILABLE : CostFailure.INVALID_OPERATION,
                    f.service.commit(op, f).failure());
            assertEquals(0, f.plans);
            assertEquals(0, f.debits);
            assertEquals(CostOperation.State.FAILED, op.state());
        }
    }

    @Test
    public void invalidActorDoesNotRunPlanner() {
        Fixture f = new Fixture();
        f.validity = CostFailure.of("narcissus:actor_unavailable");
        assertEquals(f.validity, f.service.plan(f).failure());
        assertEquals(f.validity, f.service.commit(CostOperation.create(f.identity()), f).failure());
        assertEquals(0, f.plans);
        assertEquals(0, f.debits);
    }

    @Test
    public void commitReplansAndRejectsLostBalance() {
        Fixture f = new Fixture();
        assertTrue(f.service.plan(f).isSuccess());
        CostOperation op = CostOperation.create(f.identity());
        f.balance = 4;
        assertEquals(CostFailure.INSUFFICIENT_RESOURCE, f.service.commit(op, f).failure());
        assertEquals(0, f.debits);
        assertEquals(CostOperation.State.FAILED, op.state());
    }

    @Test
    public void cancelledAndReentrantOperationsNeverDebitTwice() {
        Fixture f = new Fixture();
        CostOperation op = CostOperation.create(f.identity());
        assertTrue(op.cancel());
        assertFalse(op.cancel());
        assertEquals(CostFailure.INVALID_OPERATION, f.service.commit(op, f).failure());
        assertEquals(0, f.debits);
        f = new Fixture();
        CostOperation paying = CostOperation.create(f.identity());
        Fixture current = f;
        f.onPay = () -> assertEquals(CostFailure.INVALID_OPERATION, current.service.commit(paying, current).failure());
        assertTrue(f.service.commit(paying, f).isCommitted());
        assertEquals(1, f.debits);
    }

    @Test
    public void paymentExceptionIsTerminalAndDoesNotRunFollowup() {
        Fixture f = new Fixture();
        f.onPay = () -> { throw new IllegalStateException("Payment failed"); };
        CostOperation op = CostOperation.create(f.identity());
        assertEquals(CostFailure.PAYMENT_FAILED, f.service.commit(op, f).failure());
        assertEquals(CostOperation.State.FAILED, op.state());
        assertEquals(CostFailure.INVALID_OPERATION, f.service.commit(op, f).failure());
        assertEquals(1, f.debits);
        assertEquals(0, f.followups);
    }

    @Test
    public void crossThreadAccessDoesNotConsumeTheOperation() throws Exception {
        Fixture f = new Fixture();
        CostOperation op = CostOperation.create(f.identity());
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try { f.service.commit(op, f); } catch (Throwable e) { error.set(e); }
        });
        thread.start();
        thread.join();
        assertTrue(error.get() instanceof IllegalStateException);
        assertEquals(CostOperation.State.PENDING, op.state());
        assertTrue(f.service.commit(op, f).isCommitted());
    }

    @Test
    public void plannerCallbacksCannotChangePaymentIdentityOrEligibility() {
        for (int change = 0; change < 4; change++) {
            Fixture f = new Fixture();
            CostOperation operation = CostOperation.create(f.identity());
            int currentChange = change;
            f.onPlan = () -> {
                switch (currentChange) {
                    case 0: f.generation++; break;
                    case 1: f.payer = new UUID(0, 2); break;
                    case 2: f.signature = "replacement"; break;
                    case 3: f.validity = CostFailure.ACTOR_UNAVAILABLE; break;
                    default: throw new AssertionError();
                }
            };
            CostFailure expected = change == 0 ? CostFailure.CONFIGURATION_UNAVAILABLE
                    : change == 3 ? CostFailure.ACTOR_UNAVAILABLE : CostFailure.INVALID_OPERATION;
            assertEquals(expected, f.service.commit(operation, f).failure());
            assertEquals(0, f.debits);
            assertEquals(10, f.balance);
            assertEquals(CostOperation.State.FAILED, operation.state());
        }
    }

    @Test
    public void returnedPaymentFailureIsTerminalWithoutFollowup() {
        Fixture f = new Fixture();
        f.paymentFailure = CostFailure.of("narcissus:command_failed");
        CostOperation operation = CostOperation.create(f.identity());
        assertEquals(f.paymentFailure, f.service.commit(operation, f).failure());
        assertEquals(CostOperation.State.FAILED, operation.state());
        assertEquals(10, f.balance);
        assertEquals(1, f.debits);
        assertEquals(0, f.followups);
        assertEquals(CostFailure.INVALID_OPERATION, f.service.commit(operation, f).failure());
    }

    @Test
    public void nullPlannerResultCannotAuthorizePayment() {
        Fixture f = new Fixture();
        CostPaymentService<Fixture> service = new CostPaymentService<>(context -> null, Fixture::identity, f);
        CostOperation operation = CostOperation.create(f.identity());
        assertEquals(CostFailure.PAYMENT_FAILED, service.commit(operation, f).failure());
        assertEquals(CostOperation.State.FAILED, operation.state());
        assertEquals(0, f.debits);
        assertEquals(0, f.followups);
    }

    @Test
    public void alreadyPaidPlannerResultCannotAuthorizeAnotherDebit() {
        Fixture f = new Fixture();
        CostPaymentPlan paid = CostPaymentPlan.ready(1, Collections.emptyList(), Collections.emptyList()).paid();
        CostPaymentService<Fixture> service = new CostPaymentService<>(context -> paid, Fixture::identity, f);
        CostOperation operation = CostOperation.create(f.identity());
        assertEquals(CostFailure.PAYMENT_FAILED, service.commit(operation, f).failure());
        assertEquals(CostOperation.State.FAILED, operation.state());
        assertEquals(0, f.debits);
        assertEquals(0, f.followups);
    }

    @Test
    public void resourceSnapshotsAreDefensivelyCopied() {
        java.util.List<CostPaymentPlan.ResourceSnapshot> snapshots = new java.util.ArrayList<>();
        snapshots.add(new CostPaymentPlan.ResourceSnapshot("banira:xp_point", "before", "after", true));
        CostPaymentPlan plan = CostPaymentPlan.ready(1, Collections.emptyList(), snapshots);
        snapshots.clear();
        assertEquals(1, plan.snapshots().size());
        assertEquals("before", plan.snapshots().get(0).before());
        assertEquals("after", plan.snapshots().get(0).after());
        assertTrue(plan.snapshots().get(0).reversible());
        try { plan.snapshots().clear(); fail("Mutable snapshots"); } catch (UnsupportedOperationException expected) { }
    }

    @Test
    public void paidReceiptCapturesActualStateBeforeFollowup() {
        Fixture f = new Fixture();
        CostPaymentPlan original = CostPaymentPlan.ready(1, Collections.emptyList(), Collections.singletonList(
                new CostPaymentPlan.ResourceSnapshot("test:resource", "before", "forecast", true)));
        CostOperation operation = CostOperation.create(f.identity());
        f.receipt = plan -> {
            assertEquals(CostOperation.State.PAID, operation.state());
            return Collections.singletonList(new CostPaymentPlan.ResourceSnapshot("test:resource", "before", "actual", true));
        };
        f.onPay = () -> { };
        CostPaymentService<Fixture> service = new CostPaymentService<>(context -> original, Fixture::identity, new CostPaymentService.PaymentAccess<Fixture>() {
            public CostFailure validate(Fixture context) { return CostFailure.NONE; }
            public CostFailure pay(Fixture context, CostPaymentPlan plan) { f.debits++; return CostFailure.NONE; }
            public java.util.List<CostPaymentPlan.ResourceSnapshot> receipt(Fixture context, CostPaymentPlan plan) { return f.receipt.apply(plan); }
            public void afterPayment(Fixture context, CostPaymentPlan plan) { assertEquals("actual", plan.snapshots().get(0).after()); }
        });
        CostPaymentPlan result = service.commit(operation, f);
        assertTrue(result.isCommitted());
        assertEquals("actual", result.snapshots().get(0).after());
        assertEquals(CostFailure.NONE, result.recoveryFailure());
        assertEquals(1, f.debits);
    }

    @Test
    public void receiptFailureCannotReopenPaidOperation() {
        Fixture f = new Fixture();
        CostOperation operation = CostOperation.create(f.identity());
        f.receipt = plan -> {
            assertEquals(CostOperation.State.PAID, operation.state());
            throw new IllegalStateException("Receipt unavailable");
        };
        CostPaymentPlan result = f.service.commit(operation, f);
        assertTrue(result.isCommitted());
        assertEquals(CostFailure.UNCERTAIN, result.recoveryFailure());
        assertEquals(CostOperation.State.PAID, operation.state());
        assertEquals(CostFailure.INVALID_OPERATION, f.service.commit(operation, f).failure());
        assertEquals(1, f.debits);
    }

    @Test
    public void receiptCannotChangeResourceIdentityOrBeforeSnapshot() {
        CostPaymentPlan plan = CostPaymentPlan.ready(1, Collections.emptyList(), Collections.singletonList(
                new CostPaymentPlan.ResourceSnapshot("test:resource", "before", "forecast", true)));
        for (CostPaymentPlan.ResourceSnapshot snapshot : new CostPaymentPlan.ResourceSnapshot[]{
                new CostPaymentPlan.ResourceSnapshot("test:other", "before", "actual", true),
                new CostPaymentPlan.ResourceSnapshot("test:resource", "foreign", "actual", true),
                new CostPaymentPlan.ResourceSnapshot("test:resource", "before", "actual", false)}) {
            try { plan.withSnapshots(Collections.singletonList(snapshot)); fail("Receipt changed identity"); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test
    public void planPayloadIsImmutableAndFailedPlanCannotBecomePaid() {
        ResourceCost cost = new ResourceCost("banira:xp_point", 5, "");
        java.util.List<ResourceCost> costs = new java.util.ArrayList<>();
        costs.add(cost);
        CostPaymentPlan plan = CostPaymentPlan.ready(1, costs, Collections.emptyList());
        costs.clear();
        assertEquals(1, plan.requirements().size());
        try { plan.requirements().clear(); fail("Mutable plan"); } catch (UnsupportedOperationException expected) { }
        try { CostPaymentPlan.failed(CostFailure.PAYMENT_FAILED).paid(); fail("Failed plan paid"); }
        catch (IllegalStateException expected) { }
    }

    private static final class Fixture implements CostPaymentService.PaymentAccess<Fixture> {
        UUID payer = new UUID(0, 1);
        long id = 1, generation = 1;
        String domain = "test:payment", signature = "target";
        int balance = 10, plans, debits, followups;
        CostFailure validity = CostFailure.NONE;
        CostFailure paymentFailure = CostFailure.NONE;
        Runnable onPlan, onPay, followup;
        java.util.function.Function<CostPaymentPlan, java.util.List<CostPaymentPlan.ResourceSnapshot>> receipt = CostPaymentPlan::snapshots;
        final CostPaymentService<Fixture> service = new CostPaymentService<>(f -> {
            f.plans++;
            if (f.onPlan != null) f.onPlan.run();
            return f.balance >= 5 ? CostPaymentPlan.ready(1,
                    Collections.singletonList(new ResourceCost("banira:xp_point", 5, "")), Collections.emptyList())
                    : CostPaymentPlan.failed(CostFailure.INSUFFICIENT_RESOURCE);
        }, Fixture::identity, this);

        CostIdentity identity() { return new CostIdentity(domain, payer, id, generation, signature); }
        public CostFailure validate(Fixture context) { return validity; }
        public CostFailure pay(Fixture context, CostPaymentPlan plan) {
            debits++;
            if (onPay != null) onPay.run();
            if (paymentFailure.isFailure()) return paymentFailure;
            balance -= plan.requirements().get(0).amount();
            return CostFailure.NONE;
        }
        public void afterPayment(Fixture context, CostPaymentPlan plan) {
            followups++;
            if (followup != null) followup.run();
        }
        public java.util.List<CostPaymentPlan.ResourceSnapshot> receipt(Fixture context, CostPaymentPlan plan) { return receipt.apply(plan); }
    }
}
