package j;

import j.parser.Lexer;
import j.parser.Lexer.HistoryLexer;
import j.parser.Lexer.HistoryLexer1;
import j.parser.Lexer.TokenIterator;
import j.parser.LexerRules;
import j.parser.Token;
import j.parser.TokenType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static j.parser.TokenType.EOF;
import static j.parser.TokenType.NEWLINE;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class LexerTest {

    static Lexer.SourceInput source(@NotNull String input) {
        return Lexer.source("<inline>", input);
    }
    @Test
    public void mark_reset() {
        String input = "1,2,3,4,5,6";
        TokenIterator<TokenType> iter = new TokenIterator<>(new Lexer<>(new LexerRules(), EOF, source(input)));
        Token<TokenType> tok;
        assertEquals("1", iter.next().lexeme);
        assertEquals(",", iter.next().lexeme);
        assertEquals("2", iter.next().lexeme);
        assertEquals(",", iter.next().lexeme);
        Token<TokenType> marked = iter.mark();
        assertEquals("3", iter.next().lexeme);
        assertEquals(",", iter.next().lexeme);
        assertEquals("4", iter.next().lexeme);
        assertEquals(",", iter.next().lexeme);
        iter.reset(marked);
        assertEquals("3", iter.next().lexeme);
        assertEquals(",", iter.next().lexeme);
        assertEquals("4", iter.next().lexeme);
        assertEquals(",", iter.next().lexeme);
    }

    @Test
    public void testLexer() {
        Function<String, List<Token<TokenType>>> tokenizer = input -> {
            TokenIterator<TokenType> iter = new Lexer.TokenIterator<>(
                    new Lexer<>(
                            new LexerRules(), EOF, source(input)
                    )
            );
            List<Token<TokenType>> r = new ArrayList<>();
            while (iter.hasNext()) {
                r.add(iter.next());
            }
            System.out.println(r);
            return r;
        };

        // comment & space
        List<Token<TokenType>> toks = tokenizer.apply(" /*/**/  ");
        assertEquals(1, toks.size());
        assertSame(EOF, toks.get(0).type);
        toks = tokenizer.apply(" //...\na  ");
        assertEquals(3, toks.size());
        assertEquals(NEWLINE, toks.get(0).type);
        assertEquals(EOF, toks.get(2).type);

        // operator
        List<Token<TokenType>> lst = tokenizer.apply("1 + 2 <= 3");
        assertEquals("+", lst.get(1).lexeme);
        assertEquals("<=", lst.get(3).lexeme);

        // name
        assertEquals("abc_123", tokenizer.apply("abc_123 xyz").get(0).lexeme);

        // num
        System.out.println(tokenizer.apply("-123.123E-123abc").get(0).lexeme);
        assertEquals("-", tokenizer.apply("-123.123E-123abc").get(0).lexeme);
        assertEquals("123.123E-123", tokenizer.apply("-123.123E-123abc").get(1).lexeme);
        assertEquals("-", tokenizer.apply("-0x123abc").get(0).lexeme);
        assertEquals("0x123abc", tokenizer.apply("-0x123abc").get(1).lexeme);

        // str
        assertEquals("\"he\\\"llo\"", tokenizer.apply("\"he\\\"llo\"abc").get(0).lexeme);
        assertEquals("'he\\'llo'", tokenizer.apply("'he\\'llo'abc").get(0).lexeme);
    }


    public static TokenIterator<TokenType> parser(Lexer<TokenType> lexer) {
        return new TokenIterator<>(lexer);
    }

    public static HistoryLexer<TokenType> historical(Lexer<TokenType> lexer) {
        return new HistoryLexer<>(lexer);
    }

    @Test
    public void test_peek() {
        Lexer<TokenType> lexer = new Lexer<>(new LexerRules(), TokenType.EOF, source("a b c"));
        HistoryLexer<TokenType> hexer = historical(lexer);
        assertEquals("a", hexer.peek(0).lexeme);
        assertEquals("b", hexer.peek(1).lexeme);
        assertEquals("c", hexer.peek(2).lexeme);
        assertTrue(hexer.peek(3).is(EOF));

        try {
            hexer.peek(4);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertTrue(true);
        }

        assertEquals("a", hexer.eat().lexeme);
        assertEquals("b", hexer.eat().lexeme);
        assertEquals("c", hexer.eat().lexeme);
        assertTrue(hexer.eat().is(EOF));

        try {
            hexer.eat();
            fail();
        } catch (Error.Lexer e) {
            assertTrue(true);
        }

        assertTrue(hexer.peek(0).is(EOF));
        assertEquals("c", hexer.peek(-1).lexeme);
        assertEquals("b", hexer.peek(-2).lexeme);
        assertEquals("a", hexer.peek(-3).lexeme);

        try {
            hexer.peek(-4);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertTrue(true);
        }
    }

    @Test
    public void test_null() {
        Lexer<TokenType> lexer = new Lexer<>(new LexerRules(), TokenType.EOF, source(""));
        assertTrue(lexer.firstToken().is(lexer.EOF));
        assertTrue(historical(lexer).peek().is(lexer.EOF));
        assertTrue(historical(lexer).eat().is(lexer.EOF));

        HistoryLexer<TokenType> hl = historical(lexer);
        assertNotNull(hl.peek(TokenType.EOF));
        assertNotNull(hl.tryEat(TokenType.EOF));
    }

    @Test
    public void test_1() {
        Lexer<TokenType> lexer = new Lexer<>(new LexerRules(), TokenType.EOF, source("val = 1"));
        HistoryLexer<TokenType> hl = historical(lexer);
        assertTrue(hl.peek(TokenType.VAL, TokenType.ASSIGN, TokenType.INT, TokenType.EOF));
        hl.eat(TokenType.VAL);
        hl.eat(TokenType.ASSIGN);
        hl.eat(TokenType.INT);
        hl.eat(TokenType.EOF);
    }

    @Test
    public void test_2() {
        Lexer<TokenType> lexer = new Lexer<>(new LexerRules(), TokenType.EOF, source("a = 1"));
        assertNotNull(lexer.firstToken());

        HistoryLexer<TokenType> historical = historical(lexer);
        assertEquals(requireNonNull(historical.peek()).lexeme, "a");
        assertEquals(requireNonNull(historical.eat()).lexeme, "a");

        assertEquals(requireNonNull(historical.peek()).lexeme, "=");
        assertEquals(requireNonNull(historical.eat()).lexeme, "=");

        assertEquals(requireNonNull(historical.peek()).lexeme, "1");
        assertEquals(requireNonNull(historical.eat()).lexeme, "1");

        assertTrue(historical.peek().is(lexer.EOF));
        assertTrue(historical.eat().is(lexer.EOF));

        assertEquals(historical.peek(-1).lexeme, "1");
        assertEquals(historical.peek(-2).lexeme, "=");
        assertEquals(historical.peek(-3).lexeme, "a");

//        try {
//            historical.last(4);
//            fail();
//        } catch (IndexOutOfBoundsException ignored) {
//
//        }
    }


    @Test
    public void test_lexer() {
        Lexer<TokenType> lexer = new Lexer<>(new LexerRules(), TokenType.EOF, source("val x = 1 \n while(x > 2) { \n" +
                "def f(a,b) { a + 1} \n" +
                "break\n" +
                " }"));

        HistoryLexer<TokenType> h1 = historical(lexer);
        HistoryLexer1<TokenType> h2 = new HistoryLexer1<>(lexer);

        assertEquals(h1.eat(), h2.eat());
        assertEquals(h1.eat(), h2.eat());
        assertEquals(h1.peek(3), h2.peek(3));
        assertEquals(h1.peek(-1), h2.peek(-1));
        assertEquals(h1.peek(-2), h2.peek(-2));
        assertEquals(h1.eat(), h2.eat());
        assertEquals(h1.eat(), h2.eat());
        assertEquals(h1.eat(), h2.eat());
        assertEquals(h1.eat(), h2.eat());

        List<Token<TokenType>> l1 = new ArrayList<>();
        List<Token<TokenType>> l2 = new ArrayList<>();
        for (Token<TokenType> it : h1) {
            l1.add(it);
        }
        for (Token<TokenType> it : h2) {
            l2.add(it);
        }

        assertEquals(l1, l2);
    }
}
