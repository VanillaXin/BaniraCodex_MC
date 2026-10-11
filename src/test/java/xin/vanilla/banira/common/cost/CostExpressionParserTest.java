package xin.vanilla.banira.common.cost;

import org.junit.Test;
import xin.vanilla.banira.api.BaniraCosts;
import xin.vanilla.banira.api.cost.*;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class CostExpressionParserTest {
    @Test public void blankIsFreeButUnknownExpressionIsNot() {
        assertEquals(CostRequirement.Kind.FREE, BaniraCosts.parse(" \n\t").kind());
        reject("free()");
        reject("unknown(1)");
    }
    @Test public void functionsProduceOwnedRequirementsWithoutJavaExecution() {
        assertLeaf("item(\"minecraft:diamond\",2)", "banira:item", 2, "minecraft:diamond", ResourceCost.MatchMode.ID);
        assertLeaf("itemExact(\"minecraft:paper{fee:1}\",3)", "banira:item", 3, "minecraft:paper{fee:1}", ResourceCost.MatchMode.EXACT);
        assertLeaf("xpPoints(20)", "banira:xp_point", 20, "", ResourceCost.MatchMode.EXACT);
        assertLeaf("xpLevels(3)", "banira:xp_level", 3, "", ResourceCost.MatchMode.EXACT);
        assertLeaf("score(\"lottery_token\",4)", "banira:score", 4, "lottery_token", ResourceCost.MatchMode.EXACT);
    }
    @Test public void precedenceAndParenthesesKeepAlternativeOrder() {
        CostRequirement expression = BaniraCosts.parse("xpPoints(1) && score(\"a\",2) || xpLevels(3)");
        assertEquals(CostRequirement.Kind.ANY, expression.kind());
        assertEquals(CostRequirement.Kind.ALL, expression.children().get(0).kind());
        assertEquals("banira:xp_level", expression.children().get(1).resource().resourceId());
        assertEquals(CostRequirement.Kind.ALL, BaniraCosts.parse("(xpPoints(1) || xpLevels(2)) && score(\"a\",3)").kind());
    }
    @Test public void quotedMetadataIsDecodedAsData() {
        assertEquals("minecraft:paper{note:\"hello\"}", BaniraCosts.parse("itemExact(\"minecraft:paper{note:\\\"hello\\\"}\",1)").resource().specification());
        assertEquals("a", BaniraCosts.parse("score(\"\\u0061\",1)").resource().specification());
    }
    @Test public void rejectsExecutableAndUnsupportedAstNodes() {
        for (String value : new String[]{"System.exit(0)", "xpPoints(1); System.exit(0)", "new Object()", "x=1", "x++", "xpPoints(1+2)",
                "(int)1", "true", "1", "xpPoints(-1)", "xpPoints(1) & xpPoints(2)", "xpPoints(1)?xpPoints(2):xpPoints(3)",
                "xpPoints(1).toString()", "xpPoints(1);", "xpPoints(1) trailing", "!xpPoints(1)", "score('a',1)"}) reject(value);
    }
    @Test public void countsMustBeStrictPositiveDecimalIntegers() {
        assertEquals(Integer.MAX_VALUE, BaniraCosts.parse("xpPoints(2147483647)").resource().amount());
        for (String value : new String[]{"xpPoints(0)", "xpPoints(2147483648)", "xpPoints(1L)", "xpPoints(1.0)",
                "xpPoints(0x10)", "xpPoints(01)", "xpPoints()", "xpPoints(1,2)", "score(\"a\")"}) reject(value);
    }
    @Test public void genericItemCannotSilentlyDiscardMetadata() {
        reject("item(\"minecraft:stone{fee:1}\",1)");
        reject("item(\"minecraft:stone[foo=1]\",1)");
        reject("item(\"STONE\",1)");
        reject("item(\"\",1)");
    }
    @Test public void lengthLimitAppliesBeforeWhitespaceTrimming() {
        assertEquals(CostRequirement.Kind.FREE, BaniraCosts.parse(repeat(" ",4096)).kind());
        reject(repeat(" ",4097));
    }
    @Test public void deepInputIsRejectedBeforeRecursiveJaninoParsing() {
        assertEquals(CostRequirement.Kind.LEAF, BaniraCosts.parse(repeat("(",15)+"xpPoints(1)"+repeat(")",15)).kind());
        reject(repeat("(",16)+"xpPoints(1)"+repeat(")",16));
        reject(repeat("(",2000)+"1"+repeat(")",2000));
        reject(repeat("!",4000)+"1");
    }
    @Test public void flatThirtyTwoLeavesAreAllowedButThirtyThreeAreNot() {
        String flat=IntStream.range(0,32).mapToObj(i->"xpPoints(1)").collect(Collectors.joining("&&"));
        assertEquals(32, BaniraCosts.parse(flat).children().size());
        reject(flat+"&&xpPoints(1)");
    }
    @Test public void boundedExpansionRejectsTheOneHundredTwentyNinthPath() {
        String product=IntStream.range(0,7).mapToObj(i->"(score(\"a\",1)||score(\"b\",1))").collect(Collectors.joining("&&"));
        assertEquals(128, CostRequirementPlanner.paths(BaniraCosts.parse(product)).size());
        reject("("+product+")||score(\"a\",1)");
    }
    @Test public void invalidFeedbackIncludesOwnedLocation() {
        try { BaniraCosts.parse("\nxpPoints(0)"); fail("Accepted zero"); }
        catch (CostExpressionException error) { assertEquals(2,error.line()); assertTrue(error.column()>0); }
    }
    @Test public void scannerTokenBudgetIsExactlyFiveHundredTwelve() {
        assertEquals(31,BaniraCosts.parse(tokenBudgetExpression(31,179)).children().size());
        try {BaniraCosts.parse(tokenBudgetExpression(32,177));fail("Accepted 513 tokens");}
        catch(CostExpressionException expected){assertTrue(expected.getMessage().contains("Too many cost tokens"));}
    }
    private static String tokenBudgetExpression(int leaves,int pairs) {
        java.util.List<String> parts=new java.util.ArrayList<>();
        for(int i=0;i<leaves;i++) {
            int count=Math.min(15,pairs);pairs-=count;
            parts.add(repeat("(",count)+"xpPoints(1)"+repeat(")",count));
        }
        assertEquals(0,pairs);
        return String.join("&&",parts);
    }
    private static void assertLeaf(String source,String id,int count,String specification,ResourceCost.MatchMode mode) {
        ResourceCost result=BaniraCosts.parse(source).resource();
        assertEquals(id,result.resourceId()); assertEquals(count,result.amount());
        assertEquals(specification,result.specification()); assertEquals(mode,result.matchMode());
    }
    private static void reject(String source) {
        try { BaniraCosts.parse(source); fail("Accepted: "+source); }
        catch (IllegalArgumentException expected) { }
    }
    private static String repeat(String text,int count) { return IntStream.range(0,count).mapToObj(i->text).collect(Collectors.joining()); }
}
