import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ArrayAccessReplacer extends JavaParserBaseListener {
    final TokenStreamRewriter streamRewriter;
    final String arrayName;
    final Map<ParseTree, List<Integer>> dimensions;
    final Map<ParseTree, List<String>> accesses;
    final Stack<String> arrayIndexVariables;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<ParseTree> currentArray = Optional.empty();
    final int cachePower;
    final int blockPower;
    String cacheClassName, parameters;

    final String cacheName = UUID.randomUUID().toString()
            .chars()
            .filter(Character::isAlphabetic)
            .mapToObj(i -> (char) i)
            .map(String::valueOf)
            .collect(Collectors.joining());
    final String counterName = UUID.randomUUID().toString()
            .chars()
            .filter(Character::isAlphabetic)
            .mapToObj(i -> (char) i)
            .map(String::valueOf)
            .collect(Collectors.joining());

    private ArrayAccessReplacer(TokenStream tokens,
                               int cachePower,
                               int blockPower,
                               String arrayName) {
        streamRewriter = new TokenStreamRewriter(tokens);
        dimensions = new IdentityHashMap<>();
        accesses = new IdentityHashMap<>();
        arrayIndexVariables = new Stack<>();
        this.cachePower = cachePower;
        this.blockPower = blockPower;
        this.arrayName = arrayName;
    }

    public static ArrayAccessReplacer createDirectMappedCache(TokenStream tokens,
                                                              int cachePower,
                                                              int blockPower,
                                                              String arrayName) {
        final ArrayAccessReplacer replacer = new ArrayAccessReplacer(tokens, cachePower, blockPower, arrayName);
        replacer.cacheClassName = "DirectMappedCache";
        replacer.parameters = cachePower + "," + blockPower;
        return replacer;
    }

    public static ArrayAccessReplacer createFullyAssociativeCache(TokenStream tokens,
                                                                  int cachePower,
                                                                  int blockPower,
                                                                  String arrayName,
                                                                  CacheEvictionPolicy policy) {
        final ArrayAccessReplacer replacer = new ArrayAccessReplacer(tokens, cachePower, blockPower, arrayName);
        replacer.cacheClassName = "FullyAssociativeCache";
        String parameters = cachePower + "," + blockPower + ",";
        if (policy == CacheEvictionPolicy.LeastRecentlyUsed) {
            parameters += "new LeastRecentlyUsedEvictionPolicy()";
        } else {
            throw new IllegalStateException("Unexpected value: " + policy);
        }
        replacer.parameters = parameters;
        return replacer;
    }

    @Override
    public void exitCachePolicy(JavaParser.CachePolicyContext ctx) {
        streamRewriter.insertAfter(ctx.stop,
                String.format("\nstatic Cache %s = new %s(%s);\n" +
                        "static long %s = 0;", cacheName, cacheClassName, parameters, counterName));
    }

    @Override
    public void enterArrayCreatorRest(JavaParser.ArrayCreatorRestContext ctx) {
        final String dims = ctx.getText();
        final List<Integer> dimensionList = Arrays.stream(dims.replaceAll("\\[", "").split("]"))
                .map(String::trim)
                .map(Integer::valueOf)
                .collect(Collectors.toList());

        ParserRuleContext context = ctx;
        while (!(context instanceof JavaParser.VariableDeclaratorContext)) context = context.getParent();
        JavaParser.VariableDeclaratorContext declaratorContext = (JavaParser.VariableDeclaratorContext) context;
        dimensions.put(declaratorContext.getChild(0), dimensionList);
    }

    @Override
    public void exitStatement(JavaParser.StatementContext ctx) {
        currentArray.ifPresent(context -> {
            currentArray = Optional.empty();
        });
        accesses.entrySet().stream()
                .map(e -> {
                    final Optional<List<Integer>> dims = dimensions.entrySet().stream()
                            .filter(d -> d.getKey().getText().equals(e.getKey().getText()))
                            .map(Map.Entry::getValue)
                            .findAny();
                    return dims.map(dimensions -> "Integer.toBinaryString(" + IntStream.range(0, dimensions.size() - 1)
                            .mapToObj(i -> e.getValue().get(i) + "*" + dimensions.get(i))
                            .collect(Collectors.joining("+")) + "+" + e.getValue().get(e.getValue().size() - 1)
                            + ")");
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(binaryString -> String.format("String.format(\"%%%ds\", %s).replaceAll(\" \", \"0\")", cachePower, binaryString))
                .map(extendedBinary -> String.format("!%s.load(%s)", cacheName, extendedBinary))
                .map(load -> String.format("if(%s) %s++", load, counterName))
                .forEachOrdered(s -> streamRewriter.insertAfter(ctx.stop, "\n" + s + ";"));
        accesses.clear();
    }

    @Override
    public void enterExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.getChildCount() == 4
                && ctx.getChild(0) instanceof JavaParser.ExpressionContext
                && ctx.getChild(0).getChild(0) instanceof JavaParser.PrimaryContext
                && ctx.getChild(1).getText().equals("[")
                && ctx.getChild(3).getText().equals("]")
                && ctx.getChild(0).getChild(0).getText().equals(arrayName)
                && accesses.keySet().stream()
                    .map(ParseTree::getText)
                    .noneMatch(k -> k.equals(ctx.getChild(0).getChild(0).getText()))) {
            currentArray = Optional.of(ctx.getChild(0).getChild(0));
            List<String> dims = new ArrayList<>();
            accesses.put(ctx.getChild(0).getChild(0), dims);
        }
    }

    @Override
    public void exitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.getChildCount() == 4
                && ctx.getChild(1).getText().equals("[")
                && ctx.getChild(3).getText().equals("]")) {
            currentArray.ifPresent(currentArray -> {
                final List<String> variablesUsed = accesses.get(currentArray);
                variablesUsed.add(ctx.getChild(2).getText());
                accesses.put(currentArray, variablesUsed);
                dimensions.entrySet().stream()
                        .filter(e -> e.getKey().getText().equals(currentArray.getText()))
                        .mapToInt(e -> e.getValue().size())
                        .findAny()
                        .ifPresent(i -> {
                            if (i == variablesUsed.size()) {
                                this.currentArray = Optional.empty();
                            }
                        });
            });
        }
    }

    @Override
    public void exitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (ctx.getChildCount() > 2 && ctx.getChild(1).getText().equals("main")) {
            streamRewriter.insertBefore(ctx.stop, String.format("\nSystem.out.println(\"\\n\" + %s);\n", counterName));
        }
    }

    enum CacheEvictionPolicy {
        LeastRecentlyUsed
    }
}
