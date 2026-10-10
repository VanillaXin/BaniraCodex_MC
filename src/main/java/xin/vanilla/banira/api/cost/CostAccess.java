package xin.vanilla.banira.api.cost;

public interface CostAccess {
    CostPaymentPlan preview(CostRequirement requirement, int units);
    CostFailure recheck(CostPaymentPlan plan);
    CostFailure debit(CostPaymentPlan plan);
    CostPaymentPlan receipt(CostPaymentPlan plan);
    CostFailure restore(CostPaymentPlan plan);
    void afterPayment(CostPaymentPlan plan);
    CostAccess withResource(String id, CostResourceAccess resource);
}
