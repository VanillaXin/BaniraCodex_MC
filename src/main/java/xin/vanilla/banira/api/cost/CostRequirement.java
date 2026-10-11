package xin.vanilla.banira.api.cost;

import java.util.*;

public final class CostRequirement {
    public enum Kind { FREE, LEAF, ALL, ANY }
    private static final CostRequirement FREE = new CostRequirement(Kind.FREE, null, Collections.emptyList());
    private final Kind kind;
    private final ResourceCost resource;
    private final List<CostRequirement> children;

    private CostRequirement(Kind kind, ResourceCost resource, List<CostRequirement> children) {
        this.kind = kind;
        this.resource = resource;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
        for (CostRequirement child : this.children) Objects.requireNonNull(child, "child");
    }
    public static CostRequirement free() { return FREE; }
    public static CostRequirement leaf(ResourceCost resource) {
        return new CostRequirement(Kind.LEAF, Objects.requireNonNull(resource, "resource"), Collections.emptyList());
    }
    public static CostRequirement all(List<CostRequirement> children) {
        return new CostRequirement(Kind.ALL, null, Objects.requireNonNull(children, "children"));
    }
    public static CostRequirement any(List<CostRequirement> children) {
        if (Objects.requireNonNull(children, "children").isEmpty()) throw new IllegalArgumentException("Empty alternatives");
        return new CostRequirement(Kind.ANY, null, children);
    }
    public Kind kind() { return kind; }
    public ResourceCost resource() {
        if (kind != Kind.LEAF) throw new IllegalStateException("Not a resource leaf");
        return resource;
    }
    public List<CostRequirement> children() { return children; }
}
