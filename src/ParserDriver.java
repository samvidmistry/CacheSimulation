import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ParserDriver {
    public static void main(String[] args) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        CharStream input = CharStreams.fromReader(new FileReader("/run/media/samvidmistry/Data/IITK/CS698L/CacheSimulation/src/TestCode.java"));
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        parser.setBuildParseTree(true);
        ParseTree tree = parser.compilationUnit();
        System.out.print("Enter cache power: ");
        final int cachePower = Integer.parseInt(reader.readLine().trim());
        System.out.print("Enter block power: ");
        final int blockPower = Integer.parseInt(reader.readLine().trim());
        System.out.print("Enter name of array: ");
        final String arrayName = reader.readLine().trim();
        ArrayAccessReplacer replacer;
        outer: while (true) {
            System.out.print("Enter cache type (1 = Direct Mapped, 2 = Fully Associative): ");
            final int cacheType = Integer.parseInt(reader.readLine().trim());
            switch (cacheType) {
                case 1:
                    replacer = ArrayAccessReplacer.createDirectMappedCache(tokens, cachePower, blockPower, arrayName);
                    break outer;
                case 2:
                    inner: while (true) {
                        System.out.print("Enter cache eviction policy (1 = Least Recently Used): ");
                        final int evictionPolicy = Integer.parseInt(reader.readLine().trim());
                        switch (evictionPolicy) {
                            case 1:
                                replacer = ArrayAccessReplacer.createFullyAssociativeCache(tokens,
                                        cachePower,
                                        blockPower,
                                        arrayName,
                                        ArrayAccessReplacer.CacheEvictionPolicy.LeastRecentlyUsed);
                                break inner;
                        }
                    }
                    break outer;
            }
        }

        ParseTreeWalker.DEFAULT.walk(replacer, tree);
        System.out.println(replacer.streamRewriter.getText());
    }
}
