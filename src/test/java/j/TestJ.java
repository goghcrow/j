package j;

import j.parser.Lexer;
import j.parser.Lexer.TokenIterator;
import j.parser.LexerRules;
import j.parser.TokenType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static j.parser.Lexer.source;
import static j.parser.TokenType.EOF;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class TestJ {

    @Test
    public void test() {
        evalString("1 + 1");
        // evalString("10.times(it => println(it))");
    }


    @Test public void tmp() { evalResource("/tmp.j"); }
    @Test public void bug() { evalResource("/bug.j"); }

    @Test public void eval() { evalResource("/eval.j"); }

    @Test public void basic() { evalResource("/basic.j"); }
    @Test public void equals() { evalResource("/equals.j"); }
    @Test public void op_overload() { evalResource("/op_overload.j"); }

    @Test public void extend() { evalResource("/extends.j"); }
    @Test public void symbol() { evalResource("/symbol.j"); }
    @Test public void comment() { evalResource("/comment.j"); }
    @Test public void op() { evalResource("/op.j"); }
    @Test public void map() { evalResource("/map.j"); }
    @Test public void list() { evalResource("/list.j"); }
    @Test public void pattern() { evalResource("/pattern.j"); }
    @Test public void fun() { evalResource("/fun.j"); }
    @Test public void for1() { evalResource("/for.j"); }
    @Test public void while1() { evalResource("/while.j"); }
    @Test public void ex() { evalResource("/ex.j"); }
    @Test public void class1() { evalResource("/class.j"); }
    @Test public void proto() { evalResource("/proto.j"); }
    @Test public void class_scope() { evalResource("/class_scope.j"); }
    @Test public void reflect() { evalResource("/reflect.j"); }
    @Test public void enum1() { evalResource("/enum.j"); }
    @Test public void hindley_milner() { evalResource("/hindley-milner.j"); }
    @Test public void peg() { evalResource("/peg.j"); }

    void evalResource(String src) {
        Path path = Helper.resource2path(src);
        Path dumpPath = writeResource(src, J.dumpFile(path));
        J.interp().eval(path);
        J.interp().eval(dumpPath);
    }
    void evalString(String input) {
        System.out.println(J.dumpString(input));
        System.out.println();
        J.interp().eval(input);
    }

    void tok(String input) {
        TokenIterator<TokenType> iter = new TokenIterator<>(new Lexer<>(new LexerRules(), EOF, source("<inline>", input)));
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static Path writeResource(@NotNull String path, @NotNull String str) {
        try {
            path = Helper.class.getResource(path).getFile() + ".desugar";
            path = URLDecoder.decode(path, "UTF-8").replace("target/test-classes", "src/test/resources");
            File file = new File(path);
            file.delete();
            file.createNewFile();
            Files.write(Paths.get(path), str.getBytes(), CREATE, WRITE);
        } catch (Exception e) {
            Helper.sneakyThrows(e);
        }
        return Paths.get(path);
    }

}
