package xin.vanilla.banira.api.cost;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import xin.vanilla.banira.common.cost.ResourcePaymentAccess;
import xin.vanilla.banira.platform.UnavailableCostService;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class CostResourceAccessTest {
    @Test
    public void customResourcesPreviewAndDebitWithoutSharingMutableBalances() {
        Resource first = new Resource(), second = new Resource();
        CostAccess access = access().withResource("test:first", first).withResource("test:second", second);
        CostRequirement requirement = CostRequirement.all(Arrays.asList(leaf("test:first", 2), leaf("test:second", 3)));
        CostPaymentPlan plan = access.preview(requirement, 2);
        assertTrue(plan.isSuccess());
        assertEquals(10, first.balance);
        assertEquals(10, second.balance);
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertEquals(6, first.balance);
        assertEquals(4, second.balance);
        assertEquals(1, first.debits);
        assertEquals(CostFailure.INVALID_OPERATION, access.debit(plan));
    }

    @Test
    public void failedSecondDebitRestoresOnlyConfirmedFirstDebit() {
        Resource first = new Resource(), second = new Resource();
        second.failure = CostFailure.PAYMENT_FAILED;
        CostAccess access = access().withResource("test:first", first).withResource("test:second", second);
        CostPaymentPlan plan = access.preview(CostRequirement.all(Arrays.asList(leaf("test:first", 2), leaf("test:second", 3))), 1);
        assertEquals(CostFailure.PAYMENT_FAILED, access.debit(plan));
        assertEquals(10, first.balance);
        assertEquals(10, second.balance);
        assertEquals(1, first.restores);
        assertEquals(0, second.restores);
    }

    @Test
    public void externalMutationIsNotOverwrittenDuringRollback() {
        Resource first = new Resource(), second = new Resource();
        second.failure = CostFailure.PAYMENT_FAILED;
        second.onDebit = () -> first.balance++;
        CostAccess access = access().withResource("test:first", first).withResource("test:second", second);
        CostPaymentPlan plan = access.preview(CostRequirement.all(Arrays.asList(leaf("test:first", 2), leaf("test:second", 3))), 1);
        assertEquals(CostFailure.UNCERTAIN, access.debit(plan));
        assertEquals(9, first.balance);
        assertEquals(0, first.restores);
    }

    @Test
    public void irreversibleResourcesAreNotAutomaticallyRestored() {
        Resource first = new Resource(), second = new Resource();
        first.reversible = false;
        second.failure = CostFailure.PAYMENT_FAILED;
        CostAccess access = access().withResource("test:first", first).withResource("test:second", second);
        CostPaymentPlan plan = access.preview(CostRequirement.all(Arrays.asList(leaf("test:first", 2), leaf("test:second", 3))), 1);
        assertEquals(CostFailure.UNCERTAIN, access.debit(plan));
        assertEquals(8, first.balance);
        assertEquals(0, first.restores);
    }

    @Test
    public void restorationRequiresTheActualPostPaymentSnapshot() {
        Resource resource = new Resource();
        CostAccess access = access().withResource("test:card", resource);
        CostPaymentPlan plan = access.preview(leaf("test:card", 2), 1);
        assertEquals(CostFailure.NONE, access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        resource.balance++;
        assertEquals(CostFailure.UNCERTAIN, access.restore(receipt));
        assertEquals(9, resource.balance);
        assertEquals(0, resource.restores);
    }

    @Test
    public void builtinsCannotBeReplacedAndUnavailablePlatformCannotBeFree() {
        for (String name : new String[]{"banira:item", "banira:xp_point", "banira:xp_level", "banira:health", "banira:hunger", "banira:score"}) {
            try { access().withResource(name, new Resource()); fail("Builtin replaced"); }
            catch (IllegalArgumentException expected) { }
        }
        try { UnavailableCostService.INSTANCE.access(new Object(), InventoryScope.MAIN); fail("Unavailable service returned access"); }
        catch (IllegalStateException expected) { }
    }

    @Test
    public void unsupportedResourceCannotBecomeFreeBecauseItsAmountIsZero() {
        assertEquals(CostFailure.INVALID_PAYMENT, access().preview(leaf("test:missing", 0), 1).failure());
    }

    @Test
    public void sharedSnapshotResourceDebitsOnlyOnceForSeveralLeaves() {
        Resource resource = new Resource();
        CostAccess access = access().withResource("test:card", resource);
        CostPaymentPlan plan = access.preview(CostRequirement.all(Arrays.asList(leaf("test:card", 2), leaf("test:card", 3))), 1);
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertEquals(5, resource.balance);
        assertEquals(1, resource.debits);
        assertEquals(CostFailure.NONE, access.restore(access.receipt(plan)));
        assertEquals(10, resource.balance);
    }

    @Test
    public void uncertainReceiptCannotAuthorizeAutomaticRestoration() {
        Resource resource = new Resource();
        CostAccess access = access().withResource("test:card", resource);
        CostPaymentPlan plan = access.preview(leaf("test:card", 2), 1);
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertEquals(CostFailure.UNCERTAIN, access.restore(plan.paid().uncertainReceipt()));
        assertEquals(8, resource.balance);
        assertEquals(0, resource.restores);
    }

    @Test
    public void lostActorDoesNotAllowMutationDuringRollback() {
        Resource first = new Resource(), second = new Resource();
        boolean[] available = {true};
        second.failure = CostFailure.PAYMENT_FAILED;
        second.onDebit = () -> available[0] = false;
        CostAccess access = new ResourcePaymentAccess(Collections.emptyMap(),
                () -> available[0] ? CostFailure.NONE : CostFailure.ACTOR_UNAVAILABLE)
                .withResource("test:first", first).withResource("test:second", second);
        CostPaymentPlan plan = access.preview(CostRequirement.all(Arrays.asList(leaf("test:first", 2), leaf("test:second", 3))), 1);
        assertEquals(CostFailure.UNCERTAIN, access.debit(plan));
        assertEquals(8, first.balance);
        assertEquals(0, first.restores);
    }

    @Test
    public void unpaidPreviewCannotUndoAnIndependentSpend() {
        Resource resource = new Resource();
        CostAccess access = access().withResource("test:card", resource);
        CostPaymentPlan preview = access.preview(leaf("test:card", 2), 1);
        resource.balance = 8;
        assertEquals(CostFailure.INVALID_OPERATION, access.restore(preview));
        assertEquals(CostFailure.INVALID_OPERATION, access.restore(preview.paid()));
        assertEquals(8, resource.balance);
        assertEquals(0, resource.restores);
    }

    @Test
    public void completedDebitSnapshotFailurePreservesPaidOperation() {
        Resource resource = new Resource();
        resource.throwPaidSnapshot = true;
        CostAccess access = access().withResource("test:card", resource);
        CostIdentity identity = new CostIdentity("test:resource", new java.util.UUID(0, 1), 1, 1, "snapshot");
        CostOperation operation = CostOperation.create(identity);
        CostPaymentService<CostAccess> service = new CostPaymentService<>(a -> a.preview(leaf("test:card", 2), 1),
                a -> identity, new CostPaymentService.PaymentAccess<CostAccess>() {
            public CostFailure validate(CostAccess context) { return CostFailure.NONE; }
            public CostFailure pay(CostAccess context, CostPaymentPlan plan) { return context.debit(plan); }
            public java.util.List<CostPaymentPlan.ResourceSnapshot> receipt(CostAccess context, CostPaymentPlan plan) { return context.receipt(plan).snapshots(); }
        });
        CostPaymentPlan result = service.commit(operation, access);
        assertTrue(result.isCommitted());
        assertEquals(CostOperation.State.PAID, operation.state());
        assertEquals(CostFailure.UNCERTAIN, result.recoveryFailure());
        assertEquals(8, resource.balance);
        assertEquals(1, resource.debits);
        assertEquals(CostFailure.INVALID_OPERATION, service.commit(operation, access).failure());
    }

    @Test
    public void partiallyPaidSnapshotFailureDoesNotDebitRemainingResources() {
        Resource first = new Resource(), second = new Resource();
        first.throwPaidSnapshot = true;
        CostAccess access = access().withResource("test:first", first).withResource("test:second", second);
        CostPaymentPlan plan = access.preview(CostRequirement.all(Arrays.asList(leaf("test:first", 2), leaf("test:second", 3))), 1);
        assertEquals(CostFailure.UNCERTAIN, access.debit(plan));
        assertEquals(8, first.balance);
        assertEquals(10, second.balance);
        assertEquals(0, second.debits);
    }

    @Test
    public void mutationThenThrowCannotBecomeSuccessfulOrBlindlyRefunded() {
        Resource resource = new Resource();
        resource.onDebit = () -> { resource.balance--; throw new IllegalStateException("Unknown provider failure"); };
        CostAccess access = access().withResource("test:card", resource);
        CostPaymentPlan plan = access.preview(leaf("test:card", 2), 1);
        assertEquals(CostFailure.UNCERTAIN, access.debit(plan));
        assertEquals(9, resource.balance);
        assertEquals(0, resource.restores);
        assertEquals(CostFailure.INVALID_OPERATION, access.debit(plan));
    }

    @Test
    public void throwingRestoreIsReportedAsUncertain() {
        Resource resource = new Resource();
        resource.onRestore = () -> { throw new IllegalStateException("Restore unavailable"); };
        CostAccess access = access().withResource("test:card", resource);
        CostPaymentPlan plan = access.preview(leaf("test:card", 2), 1);
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertEquals(CostFailure.UNCERTAIN, access.restore(access.receipt(plan)));
        assertEquals(8, resource.balance);
    }

    @Test
    public void reservationCompensationPreservesAwardsOnFailedPaymentOnly() {
        Resource first = new Resource(), second = new Resource();
        first.reservationRefund = true;
        second.failure = CostFailure.PAYMENT_FAILED;
        second.onDebit = () -> first.balance++;
        CostAccess access = access().withResource("test:first", first).withResource("test:second", second);
        CostPaymentPlan plan = access.preview(CostRequirement.all(Arrays.asList(leaf("test:first", 2), leaf("test:second", 3))), 1);
        assertEquals(CostFailure.PAYMENT_FAILED, access.debit(plan));
        assertEquals(11, first.balance);
        assertEquals(1, first.compensations);
        assertEquals(0, first.restores);
    }

    @Test
    public void reservationCompensationDoesNotAuthorizeLaterRestoration() {
        Resource resource = new Resource();
        resource.reservationRefund = true;
        CostAccess access = access().withResource("test:card", resource);
        CostPaymentPlan plan = access.preview(leaf("test:card", 2), 1);
        assertEquals(CostFailure.NONE, access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        resource.balance++;
        assertEquals(CostFailure.UNCERTAIN, access.restore(receipt));
        assertEquals(9, resource.balance);
        assertEquals(0, resource.compensations);
    }

    @Test
    public void receiptCapturesFinalStateAfterFollowingProviderCallbacks() {
        Resource first = new Resource(), second = new Resource();
        second.onDebit = () -> first.balance++;
        CostAccess access = access().withResource("test:first", first).withResource("test:second", second);
        CostPaymentPlan plan = access.preview(CostRequirement.all(Arrays.asList(leaf("test:first", 2), leaf("test:second", 3))), 1);
        assertEquals(CostFailure.NONE, access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        assertEquals(9, Resource.amount(receipt.snapshots().get(0).after()));
        assertEquals(7, Resource.amount(receipt.snapshots().get(1).after()));
    }

    @Test
    public void receiptIsFrozenBeforeReturningFromSuccessfulDebit() {
        Resource resource=new Resource();
        CostAccess access=access().withResource("test:card",resource);
        CostPaymentPlan plan=access.preview(leaf("test:card",2),1);
        assertEquals(CostFailure.NONE,access.debit(plan));
        resource.balance++;
        CostPaymentPlan receipt=access.receipt(plan);
        assertEquals(8,Resource.amount(receipt.snapshots().get(0).after()));
        assertEquals(CostFailure.UNCERTAIN,access.restore(receipt));
        assertEquals(9,resource.balance);
    }

    @Test
    public void manuallyCreatedPlanCannotAuthorizeDebit() {
        Resource resource = new Resource();
        CostAccess access = access().withResource("test:card", resource);
        CostPaymentPlan forged = CostPaymentPlan.ready(1, Collections.emptyList(), Collections.emptyList());
        assertEquals(CostFailure.INVALID_OPERATION, access.debit(forged));
        assertEquals(0, resource.debits);
    }

    private static CostRequirement leaf(String name, int amount) {
        return CostRequirement.leaf(new ResourceCost(name, amount, ""));
    }

    private static CostAccess access() {
        return new ResourcePaymentAccess(Collections.emptyMap(), () -> CostFailure.NONE);
    }

    private static final class Resource implements CostResourceAccess {
        int balance = 10, debits, restores;
        boolean reversible = true;
        boolean throwPaidSnapshot;
        boolean reservationRefund;
        int compensations;
        CostFailure failure = CostFailure.NONE;
        Runnable onDebit;
        Runnable onRestore;
        public String snapshot() {
            if (throwPaidSnapshot && debits > 0) throw new IllegalStateException("Paid snapshot unavailable");
            return json(balance);
        }
        public CostFailure check(ResourceCost cost, int units, String before) {
            return amount(before) >= Math.multiplyExact(cost.amount(), units) ? CostFailure.NONE : CostFailure.INSUFFICIENT_RESOURCE;
        }
        public String after(ResourceCost cost, int units, String before) { return json(amount(before) - Math.multiplyExact(cost.amount(), units)); }
        public CostFailure debit(String before, String after) {
            debits++;
            if (onDebit != null) onDebit.run();
            if (failure.isFailure()) return failure;
            balance = amount(after);
            return CostFailure.NONE;
        }
        public CostFailure restore(String before, String after) {
            restores++;
            if (onRestore != null) onRestore.run();
            balance = amount(before);
            return CostFailure.NONE;
        }
        public boolean reversible() { return reversible; }
        public CostFailure compensateReservation(String before, String after) {
            if (!reservationRefund) return CostFailure.UNCERTAIN;
            compensations++;
            balance += amount(before) - amount(after);
            return CostFailure.NONE;
        }
        private static int amount(String json) { return new JsonParser().parse(json).getAsJsonObject().get("balance").getAsInt(); }
        private static String json(int balance) {
            JsonObject json = new JsonObject();
            json.addProperty("schema", 1);
            json.addProperty("balance", balance);
            return json.toString();
        }
    }
}
