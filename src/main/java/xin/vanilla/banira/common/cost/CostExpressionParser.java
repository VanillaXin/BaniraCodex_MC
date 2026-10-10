package xin.vanilla.banira.common.cost;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.*;
import xin.vanilla.banira.api.cost.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public final class CostExpressionParser {
    private CostExpressionParser() { }
    public static CostRequirement parse(String source) {
        Objects.requireNonNull(source, "source");
        if (source.length() > 4096) throw error("Cost expression is too long", null);
        if (source.trim().isEmpty()) return CostRequirement.free();
        try {
            prescan(source);
            Parser parser = new Parser(new org.codehaus.janino.Scanner("cost", new StringReader(source)));
            Java.Rvalue expression = parser.parseExpression();
            if (!parser.peek(TokenType.END_OF_INPUT)) throw error("Unexpected trailing text", parser.peek().getLocation());
            CostRequirement result = convert(expression, 1);
            for (List<ResourceCost> path : CostRequirementPlanner.paths(result)) CostRequirementPlanner.aggregate(path, 1);
            return result;
        } catch (CompileException invalid) { throw error("Invalid cost syntax", invalid.getLocation()); }
        catch (IOException invalid) { throw error("Cannot read cost expression", null); }
        catch (CostExpressionException invalid) { throw invalid; }
        catch (IllegalArgumentException invalid) { throw error(invalid.getMessage(), null); }
        catch (ArithmeticException invalid) { throw error("Cost amount overflow", null); }
    }
    private static void prescan(String source) throws IOException, CompileException {
        org.codehaus.janino.Scanner scanner = new org.codehaus.janino.Scanner("cost", new StringReader(source));
        int depth = 0, tokens = 0;
        for (;;) {
            Token token = scanner.produce();
            if (token.type == TokenType.END_OF_INPUT) break;
            if (token.type == TokenType.WHITE_SPACE || token.type == TokenType.C_STYLE_COMMENT || token.type == TokenType.C_PLUS_PLUS_STYLE_COMMENT) continue;
            if (++tokens > 512) throw error("Too many cost tokens", token.getLocation());
            if (token.type == TokenType.OPERATOR) {
                if (token.value.equals("(")) { if (++depth > 16) throw error("Cost expression nesting is too deep", token.getLocation()); }
                else if (token.value.equals(")")) { if (--depth < 0) throw error("Unmatched parenthesis", token.getLocation()); }
                else if (!token.value.equals(",") && !token.value.equals("&&") && !token.value.equals("||"))
                    throw error("Unsupported cost operator", token.getLocation());
            } else if (token.type != TokenType.IDENTIFIER && token.type != TokenType.STRING_LITERAL && token.type != TokenType.INTEGER_LITERAL)
                throw error("Unsupported cost token", token.getLocation());
        }
        if (depth != 0) throw error("Unmatched parenthesis", scanner.location());
    }
    private static CostRequirement convert(Java.Rvalue value, int depth) {
        if (depth > 16) throw error("Cost expression is too deep", value.getLocation());
        if (value instanceof Java.ParenthesizedExpression) return convert(((Java.ParenthesizedExpression) value).value, depth);
        if (value instanceof Java.BinaryOperation) {
            Java.BinaryOperation operation = (Java.BinaryOperation) value;
            if (!operation.operator.equals("&&") && !operation.operator.equals("||")) throw error("Unsupported cost operation", value.getLocation());
            List<Java.Rvalue> inputs = new ArrayList<>();
            flatten(value, operation.operator, inputs);
            List<CostRequirement> children = new ArrayList<>();
            for (Java.Rvalue input : inputs) children.add(convert(input, depth + 1));
            return operation.operator.equals("&&") ? CostRequirement.all(children) : CostRequirement.any(children);
        }
        if (!(value instanceof Java.MethodInvocation)) throw error("Resource function required", value.getLocation());
        Java.MethodInvocation invocation = (Java.MethodInvocation) value;
        if (invocation.target != null) throw error("Targeted calls are not allowed", value.getLocation());
        Java.Rvalue[] arguments = invocation.arguments;
        String name = invocation.methodName;
        if (name.equals("xpPoints") || name.equals("xpLevels")) {
            if (arguments.length != 1) throw error("Expected one amount", value.getLocation());
            return CostRequirement.leaf(new ResourceCost(name.equals("xpPoints") ? "banira:xp_point" : "banira:xp_level", amount(arguments[0]), ""));
        }
        if (!name.equals("item") && !name.equals("itemExact") && !name.equals("score")) throw error("Unknown resource function", value.getLocation());
        if (arguments.length != 2) throw error("Expected specification and amount", value.getLocation());
        String specification = text(arguments[0]);
        if (name.equals("item") && !specification.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw error("Item ID required; use itemExact for metadata", arguments[0].getLocation());
        if (specification.isEmpty() || specification.indexOf('\n') >= 0 || specification.indexOf('\r') >= 0
                || name.equals("score") && specification.length() > 128) throw error("Invalid resource specification", arguments[0].getLocation());
        return CostRequirement.leaf(new ResourceCost(name.equals("score") ? "banira:score" : "banira:item", amount(arguments[1]), specification,
                name.equals("item") ? ResourceCost.MatchMode.ID : ResourceCost.MatchMode.EXACT));
    }
    private static void flatten(Java.Rvalue root, String operator, List<Java.Rvalue> inputs) {
        Deque<Java.Rvalue> nodes = new ArrayDeque<>(); nodes.push(root);
        while (!nodes.isEmpty()) {
            Java.Rvalue node = nodes.pop();
            if (node instanceof Java.BinaryOperation && ((Java.BinaryOperation) node).operator.equals(operator)) {
                Java.BinaryOperation binary = (Java.BinaryOperation) node; nodes.push(binary.rhs); nodes.push(binary.lhs);
            } else inputs.add(node);
        }
    }
    private static int amount(Java.Rvalue value) {
        if (!(value instanceof Java.IntegerLiteral) || !((Java.IntegerLiteral) value).value.matches("[1-9][0-9]{0,9}"))
            throw error("Positive decimal amount required", value.getLocation());
        try { return Integer.parseInt(((Java.IntegerLiteral) value).value); }
        catch (NumberFormatException invalid) { throw error("Cost amount overflow", value.getLocation()); }
    }
    private static String text(Java.Rvalue value) {
        if (!(value instanceof Java.StringLiteral)) throw error("String literal required", value.getLocation());
        try {
            java.io.StreamTokenizer tokenizer = new java.io.StreamTokenizer(new UnicodeUnescapeReader(
                    new StringReader(((Java.StringLiteral) value).value)));
            if (tokenizer.nextToken() != '"') throw error("Invalid string literal", value.getLocation());
            String result = tokenizer.sval;
            if (tokenizer.nextToken() != java.io.StreamTokenizer.TT_EOF) throw error("Invalid string literal", value.getLocation());
            return result;
        } catch (IOException invalid) { throw error("Invalid string literal", value.getLocation()); }
    }
    private static CostExpressionException error(String message, Location location) {
        return new CostExpressionException(message, location == null ? 1 : Math.max(1, location.getLineNumber()),
                location == null ? 1 : Math.max(1, location.getColumnNumber()));
    }
}
