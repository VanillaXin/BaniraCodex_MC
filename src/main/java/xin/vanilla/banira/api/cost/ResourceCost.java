package xin.vanilla.banira.api.cost;

import java.util.Objects;

public final class ResourceCost {
    public enum MatchMode { EXACT, ID }
    private final String resourceId, specification;
    private final int amount;
    private final MatchMode matchMode;

    public ResourceCost(String resourceId, int amount, String specification) {
        this(resourceId, amount, specification, MatchMode.EXACT);
    }

    public ResourceCost(String resourceId, int amount, String specification, MatchMode matchMode) {
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.specification = Objects.requireNonNull(specification, "specification");
        if (resourceId.isEmpty() || amount < 0) throw new IllegalArgumentException("Invalid resource cost");
        this.amount = amount;
        this.matchMode = Objects.requireNonNull(matchMode, "matchMode");
    }

    public String resourceId() { return resourceId; }
    public int amount() { return amount; }
    public String specification() { return specification; }
    public MatchMode matchMode() { return matchMode; }
    @Override public boolean equals(Object other) {
        if (!(other instanceof ResourceCost)) return false;
        ResourceCost cost = (ResourceCost) other;
        return amount == cost.amount && resourceId.equals(cost.resourceId) && specification.equals(cost.specification)
                && matchMode == cost.matchMode;
    }
    @Override public int hashCode() { return Objects.hash(resourceId, amount, specification, matchMode); }
}
