package xin.vanilla.banira.api.cost;

public interface CostResourceAccess {
    String snapshot();
    CostFailure check(ResourceCost cost, int units, String before);
    String after(ResourceCost cost, int units, String before);
    CostFailure debit(String before, String after);
    CostFailure restore(String before, String after);
    boolean reversible();
    default void afterPayment() { }
}
