package j.parser;

import j.Error;
import j.Helper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Lexical Analyzer (Regex Based Lexer)
 * tokenize :: stream<char> -> stream<tok>
 * @author chuxiaofeng
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Lexer<T extends Enum<T>> {

    public final Lexicon<T> lex;

    public final T EOF;

    public final SourceInput src;

    public Lexer(@NotNull Lexicon<T> lex, @NotNull T EOF, @NotNull SourceInput src) {
        this.lex = lex;
        this.EOF = EOF;
        this.src = src;
    }

    public static SourceInput source(@NotNull Path path) {
        return new SourceInput(path.normalize().toString(), Helper.read(path));
    }

    public static SourceInput source(@NotNull String src, @NotNull String input) {
        return new SourceInput(src, input);
    }

    /*
    public TokenIterator<T> parser() {
        return new TokenIterator<>(this);
    }

    public HistoryLexer<T> historical() {
        return new HistoryLexer<>(new TokenIterator<>(this));
    }
    */

    @NotNull
    public Token<T> firstToken() {
        return nextKeepToken(null);
    }

    @NotNull
    Token<T> nextKeepToken(@Nullable Token<T> pre) {
        Token<T> tok = pre;

        int idxBegin = 0;
        int rowBegin = 1;
        int colBegin = 1;

        while (true) {
            if (tok != null) {
                idxBegin = tok.loc.idxBegin + tok.lexeme.length();
                rowBegin = tok.loc.rowEnd;
                colBegin = tok.loc.colEnd;
            }
            tok = nextToken(pre, idxBegin, rowBegin, colBegin);
            if (tok == null) {
                Location loc = location("", idxBegin, idxBegin, rowBegin, colBegin);
                return new Token<>(this, pre, EOF, "<EOF>", loc, true);
            }
            if (tok.keep) {
                return tok;
            }
        }
    }

    @Nullable
    Token<T> nextToken(@Nullable Token<T> prev, int idxBegin, int rowBegin, int colBegin) {
        if (idxBegin >= src.input.length()) {
            return null; // EOF
        }

        String subInput = src.input.substring(idxBegin);
        Token<T> tok = null;

        for (Rule<T> rule : lex.rules) {
            int idxEnd = rule.tryMatch(subInput);
            if (idxEnd >= 0) {
                String matched = subInput.substring(0, idxEnd);
                Location loc = location(matched, idxBegin, idxBegin + idxEnd, rowBegin, colBegin);
                Token<T> tok1 = new Token<>(this, prev, rule.type(matched), matched, loc, rule.keep());

                if (lex.greedy) {
                    // 取最长的匹配
                    if (tok == null || tok.lexeme.length() < tok1.lexeme.length()) {
                        tok = tok1;
                    }
                } else {
                    // 取第一个, 依赖 rules 的顺序
                    tok = tok1;
                    break;
                }
            }
        }

        if (tok == null) {
            Location loc = new Location(src, idxBegin, src.input.length(), rowBegin, colBegin, rowBegin, colBegin);
            throw Error.lexer(loc, "匹配不到任何Token规则");
        } else {
            return tok;
        }
    }

    Location location(String matched, int idxBegin, int idxEnd, int rowBegin, int colBegin) {
        int rowEnd = rowBegin;
        int colEnd = colBegin;
        for (char c : matched.toCharArray()) {
            switch (c) {
                case '\r': break;
                case '\n':
                    rowEnd++;
                    colEnd = 1;
                    break;
                default: colEnd++;
            }
        }
        return new Location(src, idxBegin, idxEnd, rowBegin, colBegin, rowEnd, colEnd);
    }

    // --------------------------------------------------------------------------------------------------------------
    /*
    public abstract static class Rule<T extends Enum<T>> {
        final boolean keep;
        final T type;

        // 返回匹配 endIdx, 失败返回-1
        abstract int tryMatch(String sub);

        Rule(T type, boolean keep) {
            this.type = type;
            this.keep = keep;
        }
    }
    */
    public interface Rule<T extends Enum<T>> {
        /**
         * 返回匹配 endIdx, 失败返回-1
         */
        int tryMatch(String sub);
        T type(String matched);
        boolean keep();
    }

    public static class StrRule<T extends Enum<T>> implements Rule<T> {
        final String toMatch;
        final T type;
        final boolean keep;
        public StrRule(@NotNull String toMatch, @NotNull T type, boolean keep) {
            this.type = type;
            this.keep = keep;
            this.toMatch = toMatch;
        }

        @Override
        public int tryMatch(String sub) {
            if (sub.regionMatches(0, toMatch, 0, toMatch.length())) {
                return toMatch.length();
            } else {
                return -1;
            }
        }
        @Override public T type(String matched) { return type; }
        @Override public boolean keep() { return keep; }
        @Override public String toString() { return toMatch; }
    }

    public static class RegRule<T extends Enum<T>> implements Rule<T> {
        final Pattern pat;
        final T type;
        final boolean keep;
        public RegRule(@NotNull String pat, @NotNull T type, boolean keep) {
            this.type = type;
            this.keep = keep;
            this.pat = Pattern.compile(pat);
        }

        @Override
        public int tryMatch(String sub) {
            java.util.regex.Matcher matcher = pat.matcher(sub);
            if (matcher.lookingAt()) {
                return matcher.end();
            } else {
                return -1;
            }
        }

        @Override public T type(String matched) { return type; }
        @Override public boolean keep() { return keep; }
        @Override public String toString() { return pat.toString(); }
    }

    public static class Lexicon<T extends Enum<T>> {
        public final List<Rule<T>> rules = new ArrayList<>();
        final boolean greedy;

        public Lexicon(boolean greedy) {
            this.greedy = greedy;
        }

        public Rule<T> str(@NotNull String str, @NotNull T type, boolean keep) {
            StrRule<T> r = new StrRule<>(str, type, keep);
            rules.add(r);
            return r;
        }

        public Rule<T> regex(@NotNull String pattern, @NotNull T type, boolean keep) {
            RegRule<T> r = new RegRule<>(pattern, type, keep);
            rules.add(r);
            return r;
        }
    }

    public static class SourceInput {
        /**
         * file source
         */
        @NotNull
        public final String src;
        @NotNull
        public final String input;

        private SourceInput(@NotNull String src, @NotNull String input) {
            this.src = src.isEmpty() ? "" : src;
            this.input = input;
        }
    }

    public static class TokenIterator<T extends Enum<T>> implements Iterator<Token<T>> {
        public final Lexer<T> lexer;

        // 至少有一个 EOF, 一定会迭代一次
        private boolean hasNext = true;
        private @NotNull Token<T> next;

        public TokenIterator(@NotNull Lexer<T> lexer) {
            this.lexer = lexer;
            this.next = lexer.firstToken();
        }

        public @NotNull Token<T> mark() {
            return next;
        }

        public void reset(@NotNull Token<T> tok) {
            next = tok;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public Token<T> next() {
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            Token<T> t = next;
            if (next.is(lexer.EOF)) {
                hasNext = false;
            } else {
                next = t.getNext();
            }
            return t;
        }
    }

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static class HistoryLexer<T extends Enum<T>> implements Iterable<Token<T>> {

        public final TokenIterator<T> tokIter;

        /**
         * 其实这里可以自己维护一下 current token, 而不使用迭代器，因为 token 本身就是个双向链表
         * 唯一的好处是 HistoryLexer 可以借 iter 直接实现 iterable
         */
        public HistoryLexer(@NotNull Lexer<T> lexer) {
            this.tokIter = new TokenIterator<>(lexer);
        }

        @NotNull
        @Override
        public Iterator<Token<T>> iterator() {
            return tokIter;
        }

        public @NotNull Token<T> mark() {
            return tokIter.next;
        }

        public void reset(@NotNull Token<T> tok) {
            tokIter.next = tok;
        }

        @NotNull
        public Token<T> eat() {
            if (tokIter.hasNext()) {
                return tokIter.next();
            } else {
                throw Error.lexer(peek().loc, "EOF");
            }
        }

        public Token<T> eat(T type) {
            if (peek(type) == null) {
                throw Error.lexer(peek().loc,
                        String.format("期望 [%s]%s, 实际是 [%s]%s", type.name(), type, peek().type.name(), peek()));
            } else {
                return eat();
            }
        }

        @SafeVarargs
        public final Token<T> eatAny(T... types) {
            for (T type : types) {
                Token<T> tok = tryEat(type);
                if (tok != null) {
                    return tok;
                }
            }
            throw Error.lexer(peek().loc, String.format("期望 %s 之一, 实际是 %s", Arrays.toString(types), peek()));
        }

        // todo !!! eat(T type) 尽量都改成这个，给个靠谱的提示
        /*
        public Token<T> eat(T type, String msg) {
            if (peek(type) == null) {
                throw Error.lexer(peek().loc, msg);
            } else {
                return eat();
            }
        }
        */

        /**
         * peek
         */
        @NotNull
        public Token<T> peek() {
            return peek(0);
        }

        /**
         * peek N
         */
        public @NotNull Token<T> peek(int n) throws IndexOutOfBoundsException {
            try {
                Token<T> tok = tokIter.next;
                while (n != 0) {
                    if (n < 0) {
                        tok = tok.getPrev();
                        n++;
                    } else {
                        tok = tok.getNext();
                        n--;
                    }
                }
                return tok;
            } catch (NoSuchElementException e) {
                throw new IndexOutOfBoundsException();
            }
        }

        @Nullable
        public final Token<T> peek(T type) {
            Token<T> tok = peek(0);
            if (tok.is(type)) {
                return tok;
            } else {
                return null;
            }
        }

        @SafeVarargs
        public final boolean peek(T... types) {
            for (int i = 0; i < types.length; i++) {
                Token<T> tok = peek(i);
                if (!tok.is(types[i])) {
                    return false;
                }
                /*if (tok.is(tokIter.lexer.EOF)) {
                    return false;
                }*/
            }
            return true;
        }

        @Nullable
        @SafeVarargs
        public final Token<T> peekAny(T... types) {
            for (T type : types) {
                Token<T> tok = peek(type);
                if (tok != null) {
                    return tok;
                }
            }
            return null;
        }

        /*
        @NotNull
        public Token<T> last(int n) throws IndexOutOfBoundsException {
            if (n <= 0) {
                throw new IndexOutOfBoundsException();
            }
            try {
                Token<T> tok = tokIter.next;
                while (n-- > 0) {
                    tok = tok.getPrev();
                }
                return tok;
            } catch (NoSuchElementException e) {
                throw new IndexOutOfBoundsException();
            }
        }
        */

        @Nullable
        public final Token<T> tryEat(T type) {
            if (peek(type) == null) {
                return null;
            } else {
                return eat();
            }
        }

        @Nullable
        @SafeVarargs
        public final List<Token<T>> tryEat(T... types) {
            if (!peek(types)) {
                return null;
            }
            List<Token<T>> lst = new ArrayList<>(types.length);
            for (int i = 0; i < types.length; i++) {
                lst.add(eat());
            }
            return lst;
        }

        @Nullable
        @SafeVarargs
        public final Token<T> tryEatAny(T... types) {
            for (T type : types) {
                Token<T> tok = tryEat(type);
                if (tok != null) {
                    return tok;
                }
            }
            return null;
        }
    }

    /**
     * 原来这么写的，维护 一个存储未消费的 queue，和一个存储已消费 tok 的 eaten 链表
     * ...eaten + peek(0) + ...queue 来代替 tok 上的 next 与 prev 属性
     * 好处是 token 不需要 lexer/next/prev 属性，比较干净纯粹
     * 不需要在 token 上实现双向链表，也不需要每次构造 tok 都显式传入 prev
     * 但是，我觉得 tok 串起来比较符合直觉!!!
     */
    public static class HistoryLexer1<T extends Enum<T>> extends HistoryLexer<T> {
        public final List<Token<T>> q = new ArrayList<>();
        public final List<Token<T>> eatten = new LinkedList<>();

        public HistoryLexer1(@NotNull Lexer<T> lexer) {
            super(lexer);
        }

        @Override
        @NotNull
        public Token<T> eat() {
            fillQ(0);
            eatten.add(0, q.remove(0));
            return eatten.get(0);
        }

        /**
         * peek N
         */
        @Override
        @NotNull
        public Token<T> peek(int n) {
            if (n < 0) {
                return eatten.get(-n - 1);
            } else {
                fillQ(n);
                return q.get(n);
            }
        }
        void fillQ(int n) {
            while (n >= q.size()) {
                if (tokIter.hasNext()) {
                    q.add(tokIter.next());
                } else {
                    throw Error.lexer(peek().loc, "EOF");
                }
            }
        }

    // @Override @NotNull public Token<T> last(int n) { return eatten.get(n - 1); }
//        Lexer<T> lexer;
//        void fillQ_X(int n) {
//            while (n >= q.size()) {
//                Token<T> tok = lexer.tokenize();
//                q.add(tok);
//            }
//        }
    }

}
