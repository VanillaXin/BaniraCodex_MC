package xin.vanilla.banira.common.cost;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import xin.vanilla.banira.api.cost.*;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class CostRequirementPlannerTest {
    @Test public void firstFullyAffordableAlternativeWinsWithoutPayingFailedBranches() {
        Wallet first=new Wallet(1), second=new Wallet(5);
        CostAccess access=new ResourcePaymentAccess(Collections.emptyMap(),()->CostFailure.NONE)
                .withResource("test:first",first).withResource("test:second",second);
        CostRequirement choices=CostRequirement.any(Arrays.asList(leaf("test:first",2),leaf("test:second",3)));
        CostPaymentPlan plan=access.preview(choices,1);
        assertTrue(plan.isSuccess()); assertEquals("test:second",plan.requirements().get(0).resourceId());
        assertEquals(1,first.balance); assertEquals(5,second.balance);
        assertEquals(CostFailure.NONE,access.debit(plan)); assertEquals(0,first.debits); assertEquals(1,second.debits);
        assertEquals(1,first.balance); assertEquals(2,second.balance);
    }
    @Test public void alternativesInsideAndRetainWrittenOrder() {
        Wallet first=new Wallet(5), second=new Wallet(5);
        CostAccess access=new ResourcePaymentAccess(Collections.emptyMap(),()->CostFailure.NONE)
                .withResource("test:first",first).withResource("test:second",second);
        CostRequirement requirement=CostRequirement.all(Arrays.asList(
                CostRequirement.any(Arrays.asList(leaf("test:first",4),leaf("test:second",1))),leaf("test:first",2)));
        CostPaymentPlan plan=access.preview(requirement,1);
        assertTrue(plan.isSuccess());
        assertEquals(CostFailure.NONE,access.debit(plan));
        assertEquals(3,first.balance); assertEquals(4,second.balance);
    }
    @Test public void repeatedResourcesAreAggregatedBeforeCheckedScaling() {
        Wallet wallet=new Wallet(10);
        CostAccess access=new ResourcePaymentAccess(Collections.emptyMap(),()->CostFailure.NONE).withResource("test:wallet",wallet);
        CostPaymentPlan plan=access.preview(CostRequirement.all(Arrays.asList(leaf("test:wallet",2),leaf("test:wallet",3))),2);
        assertEquals(1,plan.requirements().size()); assertEquals(10,plan.requirements().get(0).amount());
        assertEquals(CostFailure.NONE,access.debit(plan)); assertEquals(0,wallet.balance); assertEquals(1,wallet.debits);
    }
    @Test public void pathsMixingExperienceUnitsAreInvalid() {
        CostAccess access=new ResourcePaymentAccess(Collections.emptyMap(),()->CostFailure.NONE);
        CostRequirement mixed=CostRequirement.all(Arrays.asList(leaf("banira:xp_point",1),leaf("banira:xp_level",1)));
        assertEquals(CostFailure.INVALID_PAYMENT,access.preview(mixed,1).failure());
    }
    @Test public void amountOverflowCannotFallThroughToAnotherFreeAlternative() {
        Wallet wallet=new Wallet(10);
        CostAccess access=new ResourcePaymentAccess(Collections.emptyMap(),()->CostFailure.NONE).withResource("test:wallet",wallet);
        CostRequirement invalid=CostRequirement.any(Arrays.asList(CostRequirement.all(Arrays.asList(
                leaf("test:wallet",Integer.MAX_VALUE),leaf("test:wallet",1))),CostRequirement.free()));
        assertEquals(CostFailure.AMOUNT_OVERFLOW,access.preview(invalid,1).failure());
        assertEquals(0,wallet.debits);
    }
    @Test public void invalidLeafHiddenBehindInsufficiencyCannotBecomeFree() {
        Wallet wallet=new Wallet(0);
        CostAccess access=new ResourcePaymentAccess(Collections.emptyMap(),()->CostFailure.NONE).withResource("test:wallet",wallet);
        CostRequirement invalid=CostRequirement.any(Arrays.asList(CostRequirement.all(Arrays.asList(
                leaf("test:wallet",2),leaf("test:missing",1))),CostRequirement.free()));
        assertEquals(CostFailure.INVALID_PAYMENT,access.preview(invalid,1).failure());
        assertEquals(0,wallet.debits);
    }
    @Test public void invalidLaterAlternativeCannotBeHiddenByAnAffordableFirstPath() {
        Wallet wallet=new Wallet(10);
        CostAccess access=new ResourcePaymentAccess(Collections.emptyMap(),()->CostFailure.NONE).withResource("test:wallet",wallet);
        assertEquals(CostFailure.INVALID_PAYMENT,access.preview(CostRequirement.any(Arrays.asList(
                leaf("test:wallet",1),leaf("test:missing",1))),1).failure());
        assertEquals(0,wallet.debits);
    }
    private static CostRequirement leaf(String id,int amount) { return CostRequirement.leaf(new ResourceCost(id,amount,"")); }
    private static final class Wallet implements CostResourceAccess {
        int balance,debits;
        Wallet(int amount){balance=amount;}
        public String snapshot(){return json(balance);}
        public CostFailure check(ResourceCost cost,int units,String before){return amount(before)>=cost.amount()*units?CostFailure.NONE:CostFailure.INSUFFICIENT_RESOURCE;}
        public String after(ResourceCost cost,int units,String before){return json(amount(before)-cost.amount()*units);}
        public CostFailure debit(String before,String after){debits++;balance=amount(after);return CostFailure.NONE;}
        public CostFailure restore(String before,String after){balance=amount(before);return CostFailure.NONE;}
        public boolean reversible(){return true;}
        static int amount(String json){return new JsonParser().parse(json).getAsJsonObject().get("balance").getAsInt();}
        static String json(int amount){JsonObject json=new JsonObject();json.addProperty("schema",1);json.addProperty("balance",amount);return json.toString();}
    }
}
