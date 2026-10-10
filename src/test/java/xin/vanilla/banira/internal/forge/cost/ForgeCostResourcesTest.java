package xin.vanilla.banira.internal.forge.cost;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.food.FoodData;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.cost.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ForgeCostResourcesTest {
    private Player player;

    @Before
    public void setup() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        player = allocate(Player.class);
        player.id = new UUID(0, 1);
        player.food = new FoodData();
        player.food.setFoodLevel(10);
        player.health = 10;
        Field inventory = net.minecraft.world.entity.player.Player.class.getDeclaredField("inventory");
        inventory.setAccessible(true);
        inventory.set(player, new Inventory(player));
        player.totalExperience = 10;
        player.experienceLevel = 10;
        player.experienceProgress = .625f;
        player.board = new net.minecraft.world.scores.Scoreboard();
    }

    private CostAccess access(InventoryScope scope) {
        return ForgeCostResources.access(player, scope, p -> p == player && p.isAlive() && !p.isRemoved());
    }

    private CostPaymentPlan preview(CostAccess access, String resource, int amount, String spec) {
        return access.preview(CostRequirement.leaf(new ResourceCost(resource, amount, spec)), 1);
    }

    @Test
    public void mainExcludesEquipmentAndOffhandWhileAllKeepsLegacyRange() {
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
        player.getInventory().setItem(36, new ItemStack(Items.STONE, 3));
        player.getInventory().setItem(40, new ItemStack(Items.STONE, 4));
        CostAccess main = access(InventoryScope.MAIN);
        assertEquals(CostFailure.INSUFFICIENT_RESOURCE, preview(main, "banira:item", 3, "minecraft:stone").failure());
        CostAccess all = access(InventoryScope.ALL);
        CostPaymentPlan plan = preview(all, "banira:item", 9, "minecraft:stone");
        assertTrue(plan.isSuccess());
        assertEquals(2, player.getInventory().getItem(0).getCount());
        assertEquals(CostFailure.NONE, all.debit(plan));
        assertTrue(player.getInventory().getItem(0).isEmpty());
        assertTrue(player.getInventory().getItem(36).isEmpty());
        assertTrue(player.getInventory().getItem(40).isEmpty());
        assertEquals(CostFailure.INVALID_OPERATION, all.debit(plan));
    }

    @Test
    public void exactNbtMatchesIndependentlyOfStackCount() {
        ItemStack tagged = new ItemStack(Items.STONE, 2);
        CompoundTag tag = new CompoundTag();
        tag.putInt("fee", 1);
        tagged.setTag(tag);
        player.getInventory().setItem(0, tagged);
        ItemStack other = tagged.copy();
        other.setCount(3);
        player.getInventory().setItem(1, other);
        ItemStack extra = tagged.copy();
        extra.setCount(10);
        extra.getTag().putInt("extra", 2);
        player.getInventory().setItem(2, extra);
        player.getInventory().setItem(3, new ItemStack(Items.STONE, 10));
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:item", 5, "minecraft:stone{fee:1}");
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertTrue(player.getInventory().getItem(0).isEmpty());
        assertTrue(player.getInventory().getItem(1).isEmpty());
        assertEquals(10, player.getInventory().getItem(2).getCount());
        assertEquals(10, player.getInventory().getItem(3).getCount());
    }

    @Test
    public void invalidAndTrailingItemSpecificationsNeverBecomeFree() {
        CostAccess access = access(InventoryScope.MAIN);
        for (String spec : new String[]{"minecraft:missing", "minecraft:stone trailing", "minecraft:stone{broken"}) {
            assertEquals(spec, CostFailure.INVALID_PAYMENT, preview(access, "banira:item", 1, spec).failure());
        }
        assertEquals(CostFailure.AMOUNT_OVERFLOW, access.preview(CostRequirement.leaf(
                new ResourceCost("banira:item", Integer.MAX_VALUE, "minecraft:stone")), 2).failure());
    }

    @Test
    public void staleInventoryIsRejectedWithoutPartialRemoval() {
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:item", 3, "minecraft:stone");
        player.getInventory().getItem(0).shrink(1);
        assertEquals(CostFailure.INVALID_OPERATION, access.debit(plan));
        assertEquals(4, player.getInventory().getItem(0).getCount());
    }

    @Test
    public void mainRestorationDoesNotOverwriteOutsideSlots() {
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:item", 3, "minecraft:stone");
        assertEquals(CostFailure.NONE, access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        player.getInventory().setItem(40, new ItemStack(Items.DIAMOND, 7));
        assertEquals(CostFailure.NONE, access.restore(receipt));
        assertEquals(5, player.getInventory().getItem(0).getCount());
        assertEquals(7, player.getInventory().getItem(40).getCount());
    }

    @Test
    public void inventoryChangedAfterPaymentCannotBeBlindlyRestored() {
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:item", 3, "minecraft:stone");
        assertEquals(CostFailure.NONE, access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        player.getInventory().getItem(0).grow(1);
        assertEquals(CostFailure.UNCERTAIN, access.restore(receipt));
        assertEquals(3, player.getInventory().getItem(0).getCount());
    }

    @Test
    public void exactExperienceFailureRestoresPointsLevelsAndProgress() {
        for (String resource : new String[]{"banira:xp_point", "banira:xp_level"}) {
            for (boolean cancelled : new boolean[]{true, false}) {
                player.totalExperience = player.experienceLevel = 10;
                player.experienceProgress = .625f;
                player.cancel = cancelled;
                player.factor = 2;
                player.mutateSecondary = true;
                CostAccess access = access(InventoryScope.MAIN);
                CostPaymentPlan plan = preview(access, resource, 5, "");
                assertEquals(CostFailure.PAYMENT_FAILED, access.debit(plan));
                assertEquals(10, player.totalExperience);
                assertEquals(10, player.experienceLevel);
                assertEquals(.625f, player.experienceProgress, 0);
                assertEquals(CostFailure.INVALID_OPERATION, access.debit(plan));
            }
        }
    }

    @Test
    public void successfulExperienceReceiptCapturesActualSecondaryFields() {
        player.mutateSecondary = true;
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:xp_point", 5, "");
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertEquals(5, player.totalExperience);
        assertEquals(11, player.experienceLevel);
        assertEquals(.125f, player.experienceProgress, 0);
        CostPaymentPlan receipt = access.receipt(plan);
        assertNotEquals(plan.snapshots().get(0).after(), receipt.snapshots().get(0).after());
        assertEquals(CostFailure.NONE, access.restore(receipt));
        assertEquals(10, player.totalExperience);
        assertEquals(10, player.experienceLevel);
        assertEquals(.625f, player.experienceProgress, 0);
    }

    @Test
    public void changedExperienceReceiptCannotEraseLaterAwards() {
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:xp_level", 5, "");
        assertEquals(CostFailure.NONE, access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        player.totalExperience++;
        assertEquals(CostFailure.UNCERTAIN, access.restore(receipt));
        assertEquals(11, player.totalExperience);
        assertEquals(5, player.experienceLevel);
    }

    @Test
    public void healthMustStayPositiveAndHungerMayReachZero() {
        CostAccess access = access(InventoryScope.MAIN);
        assertEquals(CostFailure.INSUFFICIENT_RESOURCE, preview(access, "banira:health", 10, "").failure());
        assertEquals(CostFailure.NONE, access.debit(preview(access, "banira:health", 9, "")));
        assertEquals(1, player.health, 0);
        assertEquals(CostFailure.NONE, access.debit(preview(access, "banira:hunger", 10, "")));
        assertEquals(0, player.food.getFoodLevel());
    }

    @Test
    public void anotherPayerCannotRestoreTheReceipt() throws Exception {
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:xp_point", 5, "");
        assertEquals(CostFailure.NONE, access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        player.id = new UUID(0, 2);
        assertEquals(CostFailure.UNCERTAIN, access.restore(receipt));
        assertEquals(5, player.totalExperience);
    }

    @Test
    public void idOnlyItemCostMayConsumeTaggedItemsWhileExactDefaultMayNot() {
        ItemStack stack = new ItemStack(Items.STONE, 5);
        CompoundTag tag = new CompoundTag();
        tag.putInt("custom", 1);
        stack.setTag(tag);
        player.getInventory().setItem(0, stack);
        CostAccess access = access(InventoryScope.MAIN);
        assertEquals(CostFailure.INSUFFICIENT_RESOURCE, preview(access, "banira:item", 3, "minecraft:stone").failure());
        CostPaymentPlan plan = access.preview(CostRequirement.leaf(new ResourceCost("banira:item", 3,
                "minecraft:stone", ResourceCost.MatchMode.ID)), 1);
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertEquals(2, player.getInventory().getItem(0).getCount());
        assertEquals(1, player.getInventory().getItem(0).getTag().getInt("custom"));
    }

    @Test
    public void repeatedItemLeavesShareOneAllocationAndOneReceipt() {
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
        player.getInventory().setItem(40, new ItemStack(Items.STONE, 2));
        CostAccess access = access(InventoryScope.ALL);
        CostPaymentPlan plan = access.preview(CostRequirement.all(java.util.Arrays.asList(
                CostRequirement.leaf(new ResourceCost("banira:item", 3, "minecraft:stone")),
                CostRequirement.leaf(new ResourceCost("banira:item", 4, "minecraft:stone")))), 1);
        assertEquals(1, plan.snapshots().size());
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertTrue(player.getInventory().getItem(0).isEmpty());
        assertTrue(player.getInventory().getItem(40).isEmpty());
        assertEquals(CostFailure.NONE, access.restore(access.receipt(plan)));
        assertEquals(5, player.getInventory().getItem(0).getCount());
        assertEquals(2, player.getInventory().getItem(40).getCount());
    }

    @Test
    public void receiptScopeAndResourceCannotBeSwappedForRestoration() {
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
        CostAccess all = access(InventoryScope.ALL);
        CostPaymentPlan plan = preview(all, "banira:item", 2, "minecraft:stone");
        assertEquals(CostFailure.NONE, all.debit(plan));
        CostPaymentPlan receipt = all.receipt(plan);
        assertEquals(CostFailure.UNCERTAIN, access(InventoryScope.MAIN).restore(receipt));
        CostPaymentPlan.ResourceSnapshot old = receipt.snapshots().get(0);
        CostPaymentPlan.ResourceSnapshot rebound = new CostPaymentPlan.ResourceSnapshot("banira:xp_point", old.before(), old.after(), true);
        CostPaymentPlan foreign = CostPaymentPlan.ready(1, receipt.requirements(), Collections.singletonList(rebound)).paid()
                .withSnapshots(Collections.singletonList(rebound));
        assertEquals(CostFailure.UNCERTAIN, all.restore(foreign));
        assertEquals(3, player.getInventory().getItem(0).getCount());
        assertEquals(10, player.totalExperience);
    }

    @Test
    public void levelDebitReceiptIncludesActualPointAndProgressChanges() {
        player.mutateSecondary = true;
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:xp_level", 5, "");
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertEquals(5, player.experienceLevel);
        assertEquals(11, player.totalExperience);
        assertEquals(.125f, player.experienceProgress, 0);
        assertEquals(CostFailure.NONE, access.restore(access.receipt(plan)));
        assertEquals(10, player.experienceLevel);
        assertEquals(10, player.totalExperience);
        assertEquals(.625f, player.experienceProgress, 0);
    }

    @Test
    public void publicFacadeUsesForgeCostServiceWithoutClientInitialization() throws Exception {
        Field platform = xin.vanilla.banira.platform.BaniraPlatforms.class.getDeclaredField("platform");
        platform.setAccessible(true);
        Object previous = platform.get(null);
        try {
            xin.vanilla.banira.platform.BaniraCostService service = new xin.vanilla.banira.internal.forge.platform.ForgeBaniraCostService();
            xin.vanilla.banira.platform.BaniraPlatforms.install((xin.vanilla.banira.platform.BaniraPlatform) java.lang.reflect.Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{xin.vanilla.banira.platform.BaniraPlatform.class}, (proxy, method, args) -> {
                        if (method.getName().equals("costService")) return service;
                        throw new AssertionError("Unexpected platform service " + method.getName());
                    }));
            CostAccess access = xin.vanilla.banira.api.BaniraCosts.access(player, InventoryScope.MAIN);
            assertEquals(CostFailure.ACTOR_UNAVAILABLE, access.preview(CostRequirement.free(), 1).failure());
            try { service.access(new Object(), InventoryScope.MAIN); fail("Non-player accepted"); }
            catch (IllegalArgumentException expected) { }
        } finally { platform.set(null, previous); }
    }

    @Test
    public void unavailablePlayersCannotEvenPlanFreeCosts() {
        player.health = 0;
        assertEquals(CostFailure.ACTOR_UNAVAILABLE, access(InventoryScope.MAIN).preview(CostRequirement.free(), 1).failure());
    }

    @Test
    public void missingScoreObjectiveAndPlayerEntryAreReadOnlyFailures() {
        CostAccess access = access(InventoryScope.MAIN);
        assertEquals(CostFailure.INSUFFICIENT_RESOURCE, preview(access, "banira:score", 1, "missing").failure());
        assertNull(player.board.getObjective("missing"));
        net.minecraft.world.scores.Objective objective = objective("token");
        assertEquals(CostFailure.INSUFFICIENT_RESOURCE, preview(access(InventoryScope.MAIN), "banira:score", 1, "token").failure());
        assertFalse(player.board.hasPlayerScore(player.getScoreboardName(), objective));
        assertFalse(player.board.getTrackedPlayers().contains(player.getScoreboardName()));
    }

    @Test
    public void scoreDebitAndConditionalRestoreTouchOnlyExistingBalances() {
        net.minecraft.world.scores.Objective objective = objective("token");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(10);
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:score", 3, "token");
        assertTrue(plan.isSuccess());
        assertEquals(10, player.board.getPlayerScores(player.getScoreboardName()).get(objective).getScore());
        assertEquals(CostFailure.NONE, access.debit(plan));
        assertEquals(7, player.board.getPlayerScores(player.getScoreboardName()).get(objective).getScore());
        CostPaymentPlan receipt = access.receipt(plan);
        assertEquals(CostFailure.NONE, access.restore(receipt));
        assertEquals(10, player.board.getPlayerScores(player.getScoreboardName()).get(objective).getScore());
    }

    @Test
    public void laterScoreAwardsAreNotOverwrittenByRestore() {
        net.minecraft.world.scores.Objective objective = objective("token");
        net.minecraft.world.scores.Score score = player.board.getOrCreatePlayerScore(player.getScoreboardName(), objective);
        score.setScore(10);
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:score", 3, "token");
        assertEquals(CostFailure.NONE, access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        score.setScore(8);
        assertEquals(CostFailure.UNCERTAIN, access.restore(receipt));
        assertEquals(8, score.getScore());
    }

    @Test
    public void removedOrRecreatedObjectivesCannotBeChargedFromOldPlan() {
        net.minecraft.world.scores.Objective original = objective("token");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(), original).setScore(10);
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:score", 3, "token");
        assertTrue(plan.isSuccess());
        player.board.removeObjective(original);
        net.minecraft.world.scores.Objective replacement = objective("token");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(), replacement).setScore(10);
        assertEquals(CostFailure.INVALID_OPERATION, access.debit(plan));
        assertEquals(10, player.board.getPlayerScores(player.getScoreboardName()).get(replacement).getScore());
    }

    @Test
    public void zeroAndNegativeScoresCannotBeSpent() {
        net.minecraft.world.scores.Objective objective = objective("token");
        net.minecraft.world.scores.Score score = player.board.getOrCreatePlayerScore(player.getScoreboardName(), objective);
        for (int value : new int[]{0,-1,Integer.MIN_VALUE}) {
            score.setScore(value);
            assertEquals(CostFailure.INSUFFICIENT_RESOURCE, preview(access(InventoryScope.MAIN), "banira:score", 1, "token").failure());
            assertEquals(value, score.getScore());
        }
    }

    @Test
    public void scoreRemovalAfterPaymentCannotRecreateAnEntryDuringRestore() {
        net.minecraft.world.scores.Objective objective = objective("token");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),objective).setScore(10);
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access,"banira:score",3,"token");
        assertEquals(CostFailure.NONE,access.debit(plan));
        CostPaymentPlan receipt = access.receipt(plan);
        player.board.resetPlayerScore(player.getScoreboardName(),objective);
        assertEquals(CostFailure.UNCERTAIN,access.restore(receipt));
        assertFalse(player.board.hasPlayerScore(player.getScoreboardName(),objective));
    }

    @Test
    public void scoreboardCallbacksReplacingTheGoalAreUncertainNotSuccess() {
        player.board = new net.minecraft.world.scores.Scoreboard() {
            @Override public void onScoreChanged(net.minecraft.world.scores.Score score) {
                if (score.getScore() == 7) {
                    removeObjective(score.getObjective());
                    addObjective("token",net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,new net.minecraft.network.chat.TextComponent("token"),
                            net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
                }
            }
        };
        net.minecraft.world.scores.Objective objective = objective("token");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),objective).setScore(10);
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access,"banira:score",3,"token");
        assertEquals(CostFailure.UNCERTAIN,access.debit(plan));
        assertNotSame(objective,player.board.getObjective("token"));
        assertFalse(player.board.hasPlayerScore(player.getScoreboardName(),player.board.getObjective("token")));
        assertEquals(CostFailure.INVALID_OPERATION,access.debit(plan));
    }

    @Test
    public void scoreDebitDoesNotOverwriteCallbackAwardsToFollowingObjective() {
        boolean[] active={false};
        player.board = new net.minecraft.world.scores.Scoreboard() {
            @Override public void onScoreChanged(net.minecraft.world.scores.Score score) {
                if(active[0] && score.getObjective().getName().equals("first") && score.getScore()==7) {
                    net.minecraft.world.scores.Score other=getPlayerScores(player.getScoreboardName()).get(getObjective("second"));
                    other.setScore(other.getScore()+5);
                }
            }
        };
        net.minecraft.world.scores.Objective first=objective("first"),second=objective("second");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),first).setScore(10);
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),second).setScore(10);
        CostAccess access=access(InventoryScope.MAIN);
        CostPaymentPlan plan=access.preview(CostRequirement.all(java.util.Arrays.asList(
                CostRequirement.leaf(new ResourceCost("banira:score",3,"first")),
                CostRequirement.leaf(new ResourceCost("banira:score",3,"second")))),1);
        active[0]=true;
        assertEquals(CostFailure.UNCERTAIN,access.debit(plan));
        assertEquals(15,player.board.getPlayerScores(player.getScoreboardName()).get(second).getScore());
    }

    @Test
    public void scoreRestoreDoesNotOverwriteCallbackAwardsToFollowingObjective() {
        boolean[] active={false};
        player.board = new net.minecraft.world.scores.Scoreboard() {
            @Override public void onScoreChanged(net.minecraft.world.scores.Score score) {
                if(active[0] && score.getObjective().getName().equals("first") && score.getScore()==10) {
                    net.minecraft.world.scores.Score other=getPlayerScores(player.getScoreboardName()).get(getObjective("second"));
                    other.setScore(other.getScore()+5);
                }
            }
        };
        net.minecraft.world.scores.Objective first=objective("first"),second=objective("second");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),first).setScore(10);
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),second).setScore(10);
        CostAccess access=access(InventoryScope.MAIN);
        CostPaymentPlan plan=access.preview(CostRequirement.all(java.util.Arrays.asList(
                CostRequirement.leaf(new ResourceCost("banira:score",3,"first")),
                CostRequirement.leaf(new ResourceCost("banira:score",3,"second")))),1);
        assertEquals(CostFailure.NONE,access.debit(plan));
        CostPaymentPlan receipt=access.receipt(plan);
        active[0]=true;
        assertEquals(CostFailure.UNCERTAIN,access.restore(receipt));
        assertEquals(12,player.board.getPlayerScores(player.getScoreboardName()).get(second).getScore());
    }

    @Test
    public void exactAndIdItemRequirementsDoNotSpendTheSameStackTwice() {
        ItemStack tagged = new ItemStack(Items.PAPER);
        CompoundTag tag = new CompoundTag(); tag.putInt("fee",1); tagged.setTag(tag);
        player.getInventory().setItem(0,tagged);
        CostAccess access = access(InventoryScope.MAIN);
        CostRequirement requirement = CostRequirement.all(java.util.Arrays.asList(
                CostRequirement.leaf(new ResourceCost("banira:item",1,"minecraft:paper",ResourceCost.MatchMode.ID)),
                CostRequirement.leaf(new ResourceCost("banira:item",1,"minecraft:paper{fee:1}"))));
        assertEquals(CostFailure.INSUFFICIENT_RESOURCE, access.preview(requirement,1).failure());
        player.getInventory().setItem(1,new ItemStack(Items.PAPER));
        CostPaymentPlan plan = access.preview(requirement,1);
        assertEquals(CostFailure.NONE,access.debit(plan));
        assertTrue(player.getInventory().getItem(0).isEmpty()); assertTrue(player.getInventory().getItem(1).isEmpty());
    }

    @Test
    public void repeatedItemAndLeavesCannotDoubleCountOneItem() {
        player.getInventory().setItem(0,new ItemStack(Items.DIAMOND));
        CostRequirement requirement = CostRequirement.all(java.util.Arrays.asList(
                CostRequirement.leaf(new ResourceCost("banira:item",1,"minecraft:diamond")),
                CostRequirement.leaf(new ResourceCost("banira:item",1,"minecraft:diamond"))));
        assertEquals(CostFailure.INSUFFICIENT_RESOURCE,access(InventoryScope.MAIN).preview(requirement,1).failure());
        assertEquals(1,player.getInventory().getItem(0).getCount());
    }

    private net.minecraft.world.scores.Objective objective(String name) {
        return player.board.addObjective(name,net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                new net.minecraft.network.chat.TextComponent(name),net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
    }

    @Test
    public void malformedMetadataInAnUnaffordablePathCannotAuthorizeFallback() {
        CostRequirement requirement = xin.vanilla.banira.api.BaniraCosts.parse(
                "xpPoints(999)&&itemExact(\"minecraft:stone{broken\",1)||xpPoints(1)");
        assertEquals(CostFailure.INVALID_PAYMENT,access(InventoryScope.MAIN).preview(requirement,1).failure());
        assertEquals(10,player.totalExperience);
    }

    @Test
    public void sameValuedScoreEntryReplacementCannotReusePreview() {
        net.minecraft.world.scores.Objective objective=objective("token");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),objective).setScore(10);
        CostAccess access=access(InventoryScope.MAIN);
        CostPaymentPlan plan=preview(access,"banira:score",3,"token");
        player.board.resetPlayerScore(player.getScoreboardName(),objective);
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),objective).setScore(10);
        assertEquals(CostFailure.INVALID_OPERATION,access.debit(plan));
        assertEquals(10,player.board.getPlayerScores(player.getScoreboardName()).get(objective).getScore());
    }

    @Test
    public void sameValuedScoreEntryReplacementCannotAuthorizeRestore() {
        net.minecraft.world.scores.Objective objective=objective("token");
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),objective).setScore(10);
        CostAccess access=access(InventoryScope.MAIN);
        CostPaymentPlan plan=preview(access,"banira:score",3,"token");
        assertEquals(CostFailure.NONE,access.debit(plan));
        CostPaymentPlan receipt=access.receipt(plan);
        player.board.resetPlayerScore(player.getScoreboardName(),objective);
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),objective).setScore(7);
        assertEquals(CostFailure.UNCERTAIN,access.restore(receipt));
        assertEquals(7,player.board.getPlayerScores(player.getScoreboardName()).get(objective).getScore());
    }

    @Test
    public void readOnlyScoreCriteriaCannotAuthorizeDebit() {
        net.minecraft.world.scores.Objective objective=player.board.addObjective("health",net.minecraft.world.scores.criteria.ObjectiveCriteria.HEALTH,
                new net.minecraft.network.chat.TextComponent("health"),net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
        player.board.getOrCreatePlayerScore(player.getScoreboardName(),objective).setScore(10);
        assertEquals(CostFailure.INVALID_PAYMENT,preview(access(InventoryScope.MAIN),"banira:score",1,"health").failure());
        assertEquals(10,player.board.getPlayerScores(player.getScoreboardName()).get(objective).getScore());
    }

    @Test
    public void crossThreadDoesNotConsumeNativeResources() throws Exception {
        CostAccess access = access(InventoryScope.MAIN);
        CostPaymentPlan plan = preview(access, "banira:xp_point", 5, "");
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try { access.debit(plan); } catch (Throwable e) { error.set(e); }
        });
        thread.start();
        thread.join();
        assertTrue(error.get() instanceof IllegalStateException);
        assertEquals(10, player.totalExperience);
        assertEquals(CostFailure.NONE, access.debit(plan));
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static final class Player extends ServerPlayer {
        UUID id;
        FoodData food;
        float health;
        boolean cancel, mutateSecondary;
        int factor;
        net.minecraft.world.scores.Scoreboard board;
        private Player() { super(null, null, null); }
        @Override public UUID getUUID() { return id; }
        @Override public net.minecraft.world.scores.Scoreboard getScoreboard() { return board; }
        @Override public String getScoreboardName() { return "CostWallet"; }
        @Override public FoodData getFoodData() { return food; }
        @Override public float getHealth() { return health; }
        @Override public void setHealth(float value) { health = value; }
        @Override public boolean isAlive() { return health > 0 && !isRemoved(); }
        @Override public void giveExperiencePoints(int amount) {
            if (!cancel) totalExperience += amount * (factor == 0 ? 1 : factor);
            if (mutateSecondary) { experienceLevel++; experienceProgress = .125f; }
        }
        @Override public void giveExperienceLevels(int amount) {
            if (!cancel) experienceLevel += amount * (factor == 0 ? 1 : factor);
            if (mutateSecondary) { totalExperience++; experienceProgress = .125f; }
        }
    }
}
