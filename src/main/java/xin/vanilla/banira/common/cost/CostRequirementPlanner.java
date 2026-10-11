package xin.vanilla.banira.common.cost;

import xin.vanilla.banira.api.cost.*;
import java.util.*;

public final class CostRequirementPlanner {
    private CostRequirementPlanner() { }
    public static CostPaymentPlan plan(CostRequirement requirement, int units, CostAccess access) {
        return Objects.requireNonNull(access, "access").preview(requirement, units);
    }
    public static List<List<ResourceCost>> paths(CostRequirement requirement) {
        validate(Objects.requireNonNull(requirement, "requirement"), 1, new int[2]);
        List<List<ResourceCost>> result = new ArrayList<>();
        for (List<ResourceCost> path : expand(requirement)) result.add(Collections.unmodifiableList(path));
        return Collections.unmodifiableList(result);
    }
    private static void validate(CostRequirement requirement, int depth, int[] count) {
        if (depth > 16 || ++count[0] > 256) throw new IllegalArgumentException("Cost requirement is too deep or large");
        if (requirement.kind() == CostRequirement.Kind.LEAF && ++count[1] > 32) throw new IllegalArgumentException("Too many cost leaves");
        for (CostRequirement child : requirement.children()) validate(child, depth + 1, count);
    }
    private static List<List<ResourceCost>> expand(CostRequirement requirement) {
        if (requirement.kind() == CostRequirement.Kind.FREE) return Collections.singletonList(new ArrayList<>());
        if (requirement.kind() == CostRequirement.Kind.LEAF) return Collections.singletonList(new ArrayList<>(Collections.singletonList(requirement.resource())));
        List<List<ResourceCost>> result = new ArrayList<>();
        if (requirement.kind() == CostRequirement.Kind.ANY) {
            for (CostRequirement child : requirement.children()) {
                List<List<ResourceCost>> paths = expand(child);
                if (result.size() + paths.size() > 128) throw new IllegalArgumentException("Too many cost alternatives");
                result.addAll(paths);
            }
        } else {
            result.add(new ArrayList<>());
            for (CostRequirement child : requirement.children()) {
                List<List<ResourceCost>> paths = expand(child);
                if ((long) result.size() * paths.size() > 128) throw new IllegalArgumentException("Too many cost alternatives");
                List<List<ResourceCost>> combined = new ArrayList<>();
                for (List<ResourceCost> prefix : result) for (List<ResourceCost> suffix : paths) {
                    List<ResourceCost> path = new ArrayList<>(prefix); path.addAll(suffix); combined.add(path);
                }
                result = combined;
            }
        }
        return result;
    }
    static List<ResourceCost> aggregate(List<ResourceCost> path, int units) {
        if (units <= 0) throw new IllegalArgumentException("Positive units required");
        boolean points = false, levels = false;
        Map<ResourceCost, Integer> totals = new LinkedHashMap<>();
        for (ResourceCost resource : path) {
            points |= resource.resourceId().equals("banira:xp_point");
            levels |= resource.resourceId().equals("banira:xp_level");
            ResourceCost key = new ResourceCost(resource.resourceId(), 0, resource.specification(), resource.matchMode());
            totals.put(key, Math.addExact(totals.getOrDefault(key, 0), resource.amount()));
        }
        if (points && levels) throw new IllegalArgumentException("Experience points and levels cannot share a payment path");
        List<ResourceCost> result = new ArrayList<>();
        for (Map.Entry<ResourceCost, Integer> entry : totals.entrySet()) {
            ResourceCost resource = entry.getKey();
            result.add(new ResourceCost(resource.resourceId(), Math.multiplyExact(entry.getValue(), units), resource.specification(), resource.matchMode()));
        }
        result.sort(Comparator.comparingInt(c -> c.resourceId().equals("banira:item") && c.matchMode() == ResourceCost.MatchMode.ID ? 1 : 0));
        return result;
    }
}
