package j.parser;

import j.Ast.Expr;
import j.Error;
import j.parser.Lexer.HistoryLexer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Semantic Analysis (Top Down Operator Precedence Parser)
 * parser :: stream<tok> -> tree<node>
 * @author chuxiaofeng
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Parser<P extends Parser<P, T>, T extends Enum<T>> extends HistoryLexer<T> {

    Grammar<P, T> grammar;

    public Parser(@NotNull Grammar<P, T> g, @NotNull Lexer<T> lexer) {
        super(lexer);
        grammar = g;
    }

    Expr expr() {
        return expr(0);
    }

    @SuppressWarnings("unchecked")
    Expr expr(int rbp) {
        Token<T> tok = eat();
        Expr left = grammar.prefixNud(tok).parse(((P) this), tok);
        return parseInfix(left, rbp);
    }

    Expr parseInfix(Expr left, int rbp) {
        while (grammar.infixLbp(peek()) > rbp) {
            Token<T> tok = eat();
            //noinspection unchecked
            left = grammar.infixLed(tok).parse(((P) this), left, tok);
        }
        return left;
    }

    /**
     * 处理字面量、变量、前缀操作符
     */
    interface Nud<P extends Parser<P, T>, T extends Enum<T>> {
        Expr parse(@NotNull P p, @NotNull Token<T> tok);
    }

    /**
     * 处理中缀、后缀操作符
     */
    interface Led<P extends Parser<P, T>, T extends Enum<T>> {
        Expr parse(@NotNull P p, @NotNull Expr left, @NotNull Token<T> tok);
    }

    public static class Grammar<P extends Parser<P, T>, T extends Enum<T>> {
        final int[] lbps;
        final Map<T, Nud<P, T>> prefix = new HashMap<>();
        final Map<T, Led<P, T>> infix = new HashMap<>();

        public Grammar(int tokSz) {
            // ((T[]) type.getMethod("values").invoke(null)).length
            lbps = new int[tokSz];
        }

        public Nud<P, T> prefixNud(@NotNull Token<T> tok) {
            Nud<P, T> nud = prefix.get(tok.type);
            if (nud == null) {
                throw Error.syntax(tok.loc, "(保留字)不支持的token → " + tok);
            }
            return nud;
        }

        public Led<P, T> infixLed(@NotNull Token<T> tok) {
            Led<P, T> led = infix.get(tok.type);
            if (led == null) {
                throw Error.syntax(tok.loc, "(保留字)不支持的token → " + tok);
            }
            return led;
        }

        public int infixLbp(@NotNull Token<T> tok) {
            return lbps[tok.type.ordinal()];
        }

        /**
         * 前缀操作符
         */
        void prefix(@NotNull T type, @NotNull Nud<P, T> nud) {
            prefix.put(type, nud);
            lbps[type.ordinal()] = 0;
        }

        /**
         * 右结合中缀操作符
         */
        void infixRight(@NotNull T type, int lbp, @NotNull Led<P, T> led) {
            infix.put(type, led);
            lbps[type.ordinal()] = lbp;
        }

        /**
         * 左结合中缀操作符
         */
        void infixLeft(@NotNull T type, int lbp, @NotNull Led<P, T> led) {
            infix.put(type, led);
            lbps[type.ordinal()] = lbp;
        }

        /**
         * 后缀操作符（可以看成中缀操作符木有右边操作数）
         */
        void postfix(@NotNull T type, int lbp, @NotNull Led<P, T> led) {
            infix.put(type, led);
            lbps[type.ordinal()] = lbp;
        }

    }
}
