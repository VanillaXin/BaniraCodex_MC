package xin.vanilla.banira.internal.fabric.cost;

import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.*;
import xin.vanilla.banira.api.cost.*;

import java.util.*;

final class FabricScoreCostResource implements CostResourceAccess {
    private final ServerPlayer player;
    private final UUID payer;
    private String holder;
    private Scoreboard board;
    private Map<String, Objective> objectives;
    private Map<String, Score> entries;

    FabricScoreCostResource(ServerPlayer player) {
        this.player = player;
        payer = player.getUUID();
    }
    private void bind() {
        if (board != null) return;
        holder = player.getScoreboardName();
        board = Objects.requireNonNull(player.getScoreboard(), "scoreboard");
        objectives = new TreeMap<>();
        for (Objective objective : board.getObjectives()) objectives.put(objective.getName(), objective);
        entries = new TreeMap<>();
        Map<Objective, Score> current = board.getPlayerScores(holder);
        for (Map.Entry<String, Objective> entry : objectives.entrySet()) entries.put(entry.getKey(), current.get(entry.getValue()));
    }
    private boolean live() {
        if (board != player.getScoreboard() || !payer.equals(player.getUUID()) || !holder.equals(player.getScoreboardName())) return false;
        Map<Objective, Score> current = board.getPlayerScores(holder);
        for (Map.Entry<String, Objective> entry : objectives.entrySet()) if (board.getObjective(entry.getKey()) != entry.getValue()
                || current.get(entry.getValue()) != entries.get(entry.getKey())) return false;
        return true;
    }
    @Override public String snapshot() {
        bind();
        JsonObject json = new JsonObject();
        json.addProperty("schema", 1); json.addProperty("payer", player.getUUID().toString());
        json.addProperty("resource", "banira:score"); json.addProperty("holder", player.getScoreboardName());
        json.addProperty("bindings", live());
        JsonObject scores = new JsonObject();
        Map<Objective, Score> existing = board.getPlayerScores(holder);
        for (Map.Entry<String, Objective> entry : objectives.entrySet()) {
            Score score = existing.get(entry.getValue());
            if (score != null) scores.addProperty(entry.getKey(), score.getScore());
        }
        json.add("scores", scores);
        return json.toString();
    }
    @Override public CostFailure check(ResourceCost cost, int units, String before) {
        bind();
        if (!live()) return CostFailure.INVALID_OPERATION;
        Objective objective = objectives.get(cost.specification());
        if (objective == null) return CostFailure.INSUFFICIENT_RESOURCE;
        if (objective.getCriteria().isReadOnly()) return CostFailure.INVALID_PAYMENT;
        JsonObject scores = read(before).getAsJsonObject("scores");
        if (!scores.has(cost.specification())) return CostFailure.INSUFFICIENT_RESOURCE;
        int amount = Math.multiplyExact(cost.amount(), units);
        return integer(scores, cost.specification()) >= amount ? CostFailure.NONE : CostFailure.INSUFFICIENT_RESOURCE;
    }
    @Override public CostFailure validate(ResourceCost cost) {
        bind();
        if (!"banira:score".equals(cost.resourceId()) || cost.specification().isEmpty() || cost.specification().length() > 128)
            return CostFailure.INVALID_PAYMENT;
        Objective objective = objectives.get(cost.specification());
        return objective != null && objective.getCriteria().isReadOnly() ? CostFailure.INVALID_PAYMENT : CostFailure.NONE;
    }
    @Override public String after(ResourceCost cost, int units, String before) {
        CostFailure failure = check(cost, units, before);
        if (failure.isFailure()) throw new IllegalArgumentException(failure.code());
        JsonObject json = read(before), scores = json.getAsJsonObject("scores");
        scores.addProperty(cost.specification(), integer(scores, cost.specification()) - Math.multiplyExact(cost.amount(), units));
        return json.toString();
    }
    @Override public CostFailure debit(String before, String after) {
        bind();
        if (!live() || !before.equals(snapshot())) return CostFailure.INVALID_OPERATION;
        JsonObject old = read(before).getAsJsonObject("scores"), next = read(after).getAsJsonObject("scores");
        if (!keys(old).equals(keys(next))) return CostFailure.INVALID_PAYMENT;
        List<Change> updates = new ArrayList<>();
        Map<Objective, Score> existing = board.getPlayerScores(holder);
        for (String name : keys(old)) {
            int from = integer(old, name), to = integer(next, name);
            if (from == to) continue;
            Objective objective = objectives.get(name);
            Score score = existing.get(objective);
            if (objective == null || score == null || objective.getCriteria().isReadOnly() || to < 0 || to > from) return CostFailure.INVALID_PAYMENT;
            updates.add(new Change(score, from, to));
        }
        for (Change update : updates) {
            if (!live() || board.getPlayerScores(holder).get(update.score.getObjective()) != update.score
                    || update.score.getScore() != update.before) return CostFailure.UNCERTAIN;
            update.score.setScore(update.after);
        }
        return after.equals(snapshot()) ? CostFailure.NONE : CostFailure.UNCERTAIN;
    }
    @Override public CostFailure restore(String before, String after) {
        bind();
        if (!live() || !after.equals(snapshot())) return CostFailure.UNCERTAIN;
        try {
            JsonObject old = read(before).getAsJsonObject("scores"), paid = read(after).getAsJsonObject("scores");
            if (!keys(old).equals(keys(paid))) return CostFailure.UNCERTAIN;
            Map<Objective, Score> existing = board.getPlayerScores(holder);
            List<Change> updates = new ArrayList<>();
            for (String name : keys(old)) {
                int original = integer(old, name), current = integer(paid, name);
                if (original == current) continue;
                Objective objective = objectives.get(name);
                Score score = existing.get(objective);
                if (objective == null || score == null || objective.getCriteria().isReadOnly() || original < current) return CostFailure.UNCERTAIN;
                updates.add(new Change(score, current, original));
            }
            for (Change update : updates) {
                if (!live() || board.getPlayerScores(holder).get(update.score.getObjective()) != update.score
                        || update.score.getScore() != update.before) return CostFailure.UNCERTAIN;
                update.score.setScore(update.after);
            }
            return before.equals(snapshot()) ? CostFailure.NONE : CostFailure.UNCERTAIN;
        } catch (RuntimeException invalid) { return CostFailure.UNCERTAIN; }
    }
    @Override public boolean reversible() { return true; }
    private JsonObject read(String text) {
        JsonObject json = new JsonParser().parse(text).getAsJsonObject();
        if (integer(json, "schema") != 1 || !payer.toString().equals(json.get("payer").getAsString())
                || !holder.equals(json.get("holder").getAsString()) || !"banira:score".equals(json.get("resource").getAsString())
                || !json.get("bindings").getAsBoolean()) throw new IllegalArgumentException("Foreign score snapshot");
        return json;
    }
    private static int integer(JsonObject json, String name) {
        try { return json.get(name).getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException | NullPointerException invalid) { throw new IllegalArgumentException("Invalid score value", invalid); }
    }
    private static Set<String> keys(JsonObject json) {
        Set<String> names = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) names.add(entry.getKey());
        return names;
    }
    private static final class Change {
        final Score score;
        final int before, after;
        Change(Score score, int before, int after) { this.score=score; this.before=before; this.after=after; }
    }
}
