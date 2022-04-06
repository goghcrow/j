package j;

import j.parser.Lexer;
import j.parser.ParserJ;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * @author chuxiaofeng
 */
public class J {

    private static Interp interp;

    public static Interp interp() {
        if (interp == null) {
            // 先赋值 interp 再调用 bootstrap 原因: 需要在 bootstrap.j 中使用 require, 否则死循环
            interp = new Interp();
            interp.bootstrap();
        }
        return interp;
    }

    public static Interp interp(@NotNull Scope s) {
        Value v = s.lookupGlobal(Value.Constant.Key_Interp);
        assert v != null;
        return ((Interp) ((Value.BoxedValue) v).getObject());
    }

    public static Ast.FunDef loadFile(@NotNull Path path) {
        return load(Lexer.source(path));
    }

    public static Ast.FunDef loadString(@NotNull String src, @NotNull String input) {
        return load(Lexer.source(src, input));
    }

    public static String dumpString(@NotNull String input) {
        return dump(Lexer.source("<inline>", input));
    }

    public static String dumpFile(@NotNull Path path) {
        return dump(Lexer.source(path));
    }

    static Ast.FunDef load(Lexer.SourceInput srcInput) {
        return new ParserJ(srcInput).module();
    }

    static String dump(Lexer.SourceInput srcInput) {
        Ast.Block program = new ParserJ(srcInput).parse();
        Dumper dumper = new Dumper();
        dumper.visit(program, new Object());
        return dumper.buf.toString();
    }
}
