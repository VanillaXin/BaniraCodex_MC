package xin.vanilla.banira.api.cost;

import java.util.Objects;

public final class ResourceCost {
    private final String resourceId, specification;
    private final int amount;

    public ResourceCost(String resourceId, int amount, String specification) {
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.specification = Objects.requireNonNull(specification, "specification");
        if (resourceId.isEmpty() || amount < 0) throw new IllegalArgumentException("Invalid resource cost");
        this.amount = amount;
    }

    public String resourceId() { return resourceId; }
    public int amount() { return amount; }
    public String specification() { return specification; }
    @Override public boolean equals(Object other) {
        if (!(other instanceof ResourceCost)) return false;
        ResourceCost cost = (ResourceCost) other;
        return amount == cost.amount && resourceId.equals(cost.resourceId) && specification.equals(cost.specification);
    }
    @Override public int hashCode() { return Objects.hash(resourceId, amount, specification); }
}
