package xin.vanilla.banira.internal.fabric.cost;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import xin.vanilla.banira.api.cost.*;
import xin.vanilla.banira.common.cost.ResourcePaymentAccess;

import java.util.*;
import java.util.function.Predicate;

public final class FabricCostResources {
    private FabricCostResources() { }

    public static CostAccess access(ServerPlayer player, InventoryScope scope) {
        return access(player, scope, p -> p.server != null && p.server.isSameThread() && !p.hasDisconnected()
                && p.server.getPlayerList().getPlayer(p.getUUID()) == p && p.isAlive() && !p.isRemoved());
    }

    static CostAccess access(ServerPlayer player, InventoryScope scope, Predicate<ServerPlayer> available) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(scope, "scope");
        UUID payer = player.getUUID();
        Map<String, CostResourceAccess> resources = new LinkedHashMap<>();
        for (String id : new String[]{"banira:item", "banira:xp_point", "banira:xp_level", "banira:health", "banira:hunger"})
            resources.put(id, new Resource(player, payer, scope, id));
        resources.put("banira:score", new FabricScoreCostResource(player));
        return new ResourcePaymentAccess(resources, () -> payer.equals(player.getUUID()) && available.test(player)
                ? CostFailure.NONE : CostFailure.ACTOR_UNAVAILABLE);
    }

    private static final class Resource implements CostResourceAccess {
        private final ServerPlayer player;
        private final UUID payer;
        private final InventoryScope scope;
        private final String id;

        Resource(ServerPlayer player, UUID payer, InventoryScope scope, String id) {
            this.player = player;
            this.payer = payer;
            this.scope = scope;
            this.id = id;
        }

        @Override public String snapshot() {
            JsonObject json = new JsonObject();
            json.addProperty("schema", 1);
            json.addProperty("payer", player.getUUID().toString());
            json.addProperty("resource", id);
            if (id.equals("banira:item")) {
                json.addProperty("scope", scope.name());
                JsonArray slots = new JsonArray();
                for (int i = 0; i < slotCount(); i++) slots.add(encode(player.getInventory().getItem(i)));
                json.add("slots", slots);
            } else if (experience()) {
                json.addProperty("points", player.totalExperience);
                json.addProperty("levels", player.experienceLevel);
                json.addProperty("progress", Float.floatToIntBits(player.experienceProgress));
            } else if (id.equals("banira:health")) {
                json.addProperty("health", Float.floatToIntBits(player.getHealth()));
            } else {
                json.addProperty("hunger", player.getFoodData().getFoodLevel());
            }
            return json.toString();
        }

        @Override public CostFailure check(ResourceCost cost, int units, String before) {
            try {
                if (!id.equals(cost.resourceId())) return CostFailure.INVALID_PAYMENT;
                int amount = Math.multiplyExact(cost.amount(), units);
                JsonObject json = read(before);
                boolean enough;
                if (id.equals("banira:item")) {
                    ItemStack required = item(cost.specification());
                    if (required == null || cost.matchMode() == ResourceCost.MatchMode.ID && required.hasTag())
                        return CostFailure.INVALID_PAYMENT;
                    long count = 0;
                    for (JsonElement entry : json.getAsJsonArray("slots")) {
                        ItemStack stack = decode(entry.getAsJsonObject());
                        if (matches(stack, required, cost.matchMode())) count += stack.getCount();
                    }
                    enough = count >= amount;
                } else if (experience()) {
                    int points = integer(json, "points"), levels = integer(json, "levels");
                    float progress = Float.intBitsToFloat(integer(json, "progress"));
                    if (points < 0 || levels < 0 || !Float.isFinite(progress)) return CostFailure.ACTOR_UNAVAILABLE;
                    enough = integer(json, id.equals("banira:xp_point") ? "points" : "levels") >= amount;
                } else if (id.equals("banira:health")) {
                    float health = Float.intBitsToFloat(integer(json, "health"));
                    if (!Float.isFinite(health)) return CostFailure.ACTOR_UNAVAILABLE;
                    enough = health > amount;
                } else enough = integer(json, "hunger") >= amount;
                return enough ? CostFailure.NONE : CostFailure.INSUFFICIENT_RESOURCE;
            } catch (ArithmeticException overflow) { return CostFailure.AMOUNT_OVERFLOW; }
            catch (IllegalArgumentException | IllegalStateException invalid) { return CostFailure.INVALID_PAYMENT; }
        }

        @Override public CostFailure validate(ResourceCost cost) {
            if (!id.equals(cost.resourceId())) return CostFailure.INVALID_PAYMENT;
            if (!id.equals("banira:item")) return CostFailure.NONE;
            ItemStack required = item(cost.specification());
            return required == null || cost.matchMode() == ResourceCost.MatchMode.ID && required.hasTag()
                    ? CostFailure.INVALID_PAYMENT : CostFailure.NONE;
        }

        @Override public String after(ResourceCost cost, int units, String before) {
            CostFailure failure = check(cost, units, before);
            if (failure.isFailure()) throw new IllegalArgumentException(failure.code());
            int amount = Math.multiplyExact(cost.amount(), units);
            JsonObject json = read(before);
            if (id.equals("banira:item")) {
                ItemStack required = Objects.requireNonNull(item(cost.specification()), "item");
                JsonArray slots = json.getAsJsonArray("slots");
                int remaining = amount;
                for (int i = 0; i < slots.size() && remaining > 0; i++) {
                    ItemStack stack = decode(slots.get(i).getAsJsonObject());
                    if (matches(stack, required, cost.matchMode())) {
                        int take = Math.min(remaining, stack.getCount());
                        stack.shrink(take);
                        slots.set(i, encode(stack));
                        remaining -= take;
                    }
                }
                if (remaining != 0) throw new IllegalStateException("Inventory plan is insufficient");
            } else if (experience()) {
                String field = id.equals("banira:xp_point") ? "points" : "levels";
                json.addProperty(field, integer(json, field) - amount);
            } else if (id.equals("banira:health")) {
                json.addProperty("health", Float.floatToIntBits(Float.intBitsToFloat(integer(json, "health")) - amount));
            } else json.addProperty("hunger", integer(json, "hunger") - amount);
            return json.toString();
        }

        @Override public CostFailure debit(String before, String after) {
            JsonObject original = read(before), expected = read(after);
            if (!before.equals(snapshot())) return CostFailure.INVALID_OPERATION;
            if (experience()) return debitExperience(original, expected);
            if (id.equals("banira:item")) {
                List<ItemStack> replacements = stacks(expected);
                List<ItemStack> originals = stacks(original);
                for (int i = 0; i < replacements.size(); i++) {
                    ItemStack next = replacements.get(i), old = originals.get(i);
                    if (!next.isEmpty() && (old.isEmpty() || old.getItem() != next.getItem()
                            || !ItemStack.tagMatches(old, next) || next.getCount() > old.getCount()))
                        return CostFailure.INVALID_PAYMENT;
                }
                for (int i = 0; i < replacements.size(); i++) {
                    int count = replacements.get(i).isEmpty() ? 0 : replacements.get(i).getCount();
                    ItemStack stack = player.getInventory().getItem(i);
                    int take = stack.getCount() - count;
                    if (take > 0) stack.shrink(take);
                }
            } else if (id.equals("banira:health")) {
                float health = Float.intBitsToFloat(integer(expected, "health"));
                if (!Float.isFinite(health) || health <= 0 || health > player.getHealth()) return CostFailure.INVALID_PAYMENT;
                player.setHealth(health);
            } else {
                int hunger = integer(expected, "hunger");
                if (hunger < 0 || hunger > player.getFoodData().getFoodLevel()) return CostFailure.INVALID_PAYMENT;
                player.getFoodData().setFoodLevel(hunger);
            }
            return after.equals(snapshot()) ? CostFailure.NONE : CostFailure.UNCERTAIN;
        }

        private CostFailure debitExperience(JsonObject original, JsonObject expected) {
            int points = integer(original, "points"), levels = integer(original, "levels");
            float progress = Float.intBitsToFloat(integer(original, "progress"));
            String primary = id.equals("banira:xp_point") ? "points" : "levels";
            int target = integer(expected, primary);
            int before = id.equals("banira:xp_point") ? points : levels;
            if (target < 0 || target > before) return CostFailure.INVALID_PAYMENT;
            boolean success = false;
            try {
                if (id.equals("banira:xp_point")) {
                    player.giveExperiencePoints(target - before);
                    success = player.totalExperience == target;
                } else {
                    player.giveExperienceLevels(target - before);
                    success = player.experienceLevel == target;
                }
                return success ? CostFailure.NONE : CostFailure.PAYMENT_FAILED;
            } finally {
                if (!success) {
                    player.totalExperience = points;
                    player.experienceLevel = levels;
                    player.experienceProgress = progress;
                }
            }
        }

        @Override public CostFailure restore(String before, String after) {
            try {
                JsonObject original = read(before);
                read(after);
                if (!after.equals(snapshot())) return CostFailure.UNCERTAIN;
                if (id.equals("banira:item")) {
                    List<ItemStack> replacements = stacks(original);
                    for (int i = 0; i < replacements.size(); i++) player.getInventory().setItem(i, replacements.get(i));
                    player.getInventory().setChanged();
                } else if (experience()) {
                    int points = integer(original, "points"), levels = integer(original, "levels");
                    float progress = Float.intBitsToFloat(integer(original, "progress"));
                    if (points < 0 || levels < 0 || !Float.isFinite(progress)) return CostFailure.UNCERTAIN;
                    player.totalExperience = points;
                    player.experienceLevel = levels;
                    player.experienceProgress = progress;
                } else if (id.equals("banira:health")) {
                    float health = Float.intBitsToFloat(integer(original, "health"));
                    if (!Float.isFinite(health) || health <= 0) return CostFailure.UNCERTAIN;
                    player.setHealth(health);
                } else {
                    int hunger = integer(original, "hunger");
                    if (hunger < 0) return CostFailure.UNCERTAIN;
                    player.getFoodData().setFoodLevel(hunger);
                }
                return before.equals(snapshot()) ? CostFailure.NONE : CostFailure.UNCERTAIN;
            } catch (RuntimeException invalid) { return CostFailure.UNCERTAIN; }
        }

        @Override public boolean reversible() { return true; }
        @Override public void afterPayment() {
            if (id.equals("banira:item")) {
                player.getInventory().setChanged();
                if (player.inventoryMenu != null) player.inventoryMenu.broadcastChanges();
            }
        }

        private boolean experience() { return id.equals("banira:xp_point") || id.equals("banira:xp_level"); }
        private int slotCount() { return scope == InventoryScope.MAIN ? player.getInventory().items.size() : player.getInventory().getContainerSize(); }

        private JsonObject read(String text) {
            JsonObject json = new JsonParser().parse(text).getAsJsonObject();
            if (integer(json, "schema") != 1 || !payer.toString().equals(json.get("payer").getAsString())
                    || !payer.equals(player.getUUID()) || !id.equals(json.get("resource").getAsString()))
                throw new IllegalArgumentException("Foreign resource snapshot");
            if (id.equals("banira:item") && (!scope.name().equals(json.get("scope").getAsString())
                    || json.getAsJsonArray("slots").size() != slotCount())) throw new IllegalArgumentException("Foreign inventory scope");
            return json;
        }

        private List<ItemStack> stacks(JsonObject json) {
            List<ItemStack> stacks = new ArrayList<>();
            for (JsonElement value : json.getAsJsonArray("slots")) stacks.add(decode(value.getAsJsonObject()));
            return stacks;
        }

        private static int integer(JsonObject json, String field) {
            try { return json.get(field).getAsBigDecimal().intValueExact(); }
            catch (ArithmeticException | NullPointerException invalid) { throw new IllegalArgumentException("Invalid snapshot integer", invalid); }
        }

        private static JsonObject encode(ItemStack stack) {
            JsonObject json = new JsonObject();
            json.addProperty("count", stack.isEmpty() ? 0 : stack.getCount());
            ItemStack identity = stack.copy();
            if (!identity.isEmpty()) identity.setCount(1);
            json.addProperty("item", identity.isEmpty() ? "" : identity.save(new CompoundTag()).toString());
            return json;
        }

        private static ItemStack decode(JsonObject json) {
            int count = integer(json, "count");
            if (count == 0) return ItemStack.EMPTY;
            if (count < 0) throw new IllegalArgumentException("Negative stack count");
            try {
                ItemStack stack = ItemStack.of(TagParser.parseTag(json.get("item").getAsString()));
                if (stack.isEmpty()) throw new IllegalArgumentException("Unavailable snapshot item");
                stack.setCount(count);
                return stack;
            } catch (CommandSyntaxException invalid) { throw new IllegalArgumentException("Invalid snapshot item", invalid); }
        }

        private static boolean matches(ItemStack stack, ItemStack required, ResourceCost.MatchMode mode) {
            return !stack.isEmpty() && stack.getItem() == required.getItem()
                    && (mode == ResourceCost.MatchMode.ID || ItemStack.tagMatches(stack, required));
        }

        private static ItemStack item(String specification) {
            try {
                StringReader reader = new StringReader(specification.trim());
                ItemParser.ItemResult parsed = ItemParser.parseForItem(
                        net.minecraft.core.HolderLookup.forRegistry(net.minecraft.core.Registry.ITEM), reader);
                if (reader.canRead()) return null;
                ItemStack item = new ItemInput(parsed.item(), parsed.nbt()).createItemStack(1, false);
                return item.isEmpty() ? null : item;
            } catch (CommandSyntaxException | IllegalArgumentException invalid) { return null; }
        }
    }
}
