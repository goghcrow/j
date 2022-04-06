package j.parser;

import j.Expect;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

import static j.parser.Lexer.StrRule;
import static j.parser.TokenType.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * Lexical Grammar
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class LexerRules extends Lexer.Lexicon<TokenType> {

    public LexerRules() {
        super(false);

        // ❗️❗️❗️注意顺序, e.g. 1 - 1, 应该 1,-,1  而不是 1, -1

        regex("/\\*[\\s\\S]*?\\*+/", BLOCK_COMMENT, false);
        regex("//.*", LINE_COMMENT, false);
        regex("[ \\r\\t]+", WHITESPACE, false); // 不能 \s+ 换行也在这里头

//        strs(
//                LEFT_PAREN,
//                RIGHT_PAREN,
//                LEFT_BRACKET,
//                RIGHT_BRACKET,
//                LEFT_BRACE,
//                RIGHT_BRACE,
//                COLON,
//                COMMA,
//
//                BIT_NOT,
//                COND,
//
//                ASSIGN_MUL,
//                POWER,
//                MUL,
//
//                ASSIGN_DIV,
//                DIV,
//
//                ASSIGN_MOD,
//                MOD,
//
//                ASSIGN_PLUS,
//                PREFIX_INCR,
//                PLUS,
//
//                ARROW_BLOCK,
//                ASSIGN_SUB,
//                PREFIX_DECR,
//                MINUS,
//
//                ASSIGN_BIT_XOR,
//                BIT_XOR,
//
//                ASSIGN_BIT_OR,
//                LOGIC_OR,
//                BIT_OR,
//
//                ASSIGN_LEFT_SHIFT,
//// LT_LT_LT,
//                LT_LT,
//                LE,
//                LT,
//
//                ASSIGN_UNSIGNED_RIGHT_SHIFT,
//                ASSIGN_SIGNED_RIGHT_SHIFT,
//                GT_GT_GT,
//                GT_GT,
//                GE,
//                GT,
//
//                ARROW,
//                STRICT_EQ,
//                EQ,
//                ASSIGN,
//
//                STRICT_NE,
//                NE,
//                LOGIC_NOT,
//
//// EXCLUSIVE_RANGE,
//                RANGE,
//                DOT,
//
//// BACKTICK,
//
//                NEWLINE,
//                SEMICOLON,
//
//                ASSIGN_BIT_AND,
//                LOGIC_AND,
//                BIT_AND
//                );

        str(LEFT_PAREN);        // {
        str(RIGHT_PAREN);       // }
        str(LEFT_BRACKET);      // [
        str(RIGHT_BRACKET);     // ]
        str(LEFT_BRACE);        // {
        str(RIGHT_BRACE);       // }
        str(COLON);             // :
        str(COMMA);             // ,

        str(BIT_NOT);           // ~
        str(COND);              // ?
        str(AT);                // @

        {
            str(ASSIGN_MUL);    // *=
            str(POWER);         // **
            str(MUL);           // *
        }

        {
            str(ASSIGN_DIV);    // /=
            str(DIV);           // / 必须在 //  /* 注释之后
        }

        {
            str(ASSIGN_MOD);    // %=
            str(MOD);           // %
        }

        {
            str(ASSIGN_PLUS);   // +=
            str(PREFIX_INCR);   // ++
            str(PLUS);          // +
        }

        {
            str(ARROW_BLOCK);   // ->
            str(ASSIGN_SUB);    // -=
            str(PREFIX_DECR);   // --
            str(MINUS);         // -
        }

        {
            str(ASSIGN_BIT_XOR);// ^=
            str(BIT_XOR);       // ^
        }


        {
            str(ASSIGN_BIT_OR); // |=
            str(LOGIC_OR);      // ||
            str(BIT_OR);        // |
        }

        {
            str(ASSIGN_LEFT_SHIFT); // <<=
            // str(LT_LT_LT);          // <<<
            str(LT_LT);             // <<
            str(LE);                // <=
            str(LT);                // <
        }

        {
            str(ASSIGN_UNSIGNED_RIGHT_SHIFT);   // >>>=
            str(ASSIGN_SIGNED_RIGHT_SHIFT);     // >>=
            str(GT_GT_GT);                      // >>>
            str(GT_GT);                         // >>
            str(GE);                            // >=
            str(GT);                            // >
        }

        {
            str(ARROW);         // =>
            str(STRICT_EQ);     // ===
            str(EQ);            // ==
            str(ASSIGN);        // =
        }

        {
            str(STRICT_NE);     // !==
            str(NE);            // !=
            str(LOGIC_NOT);     // !
        }

        {
            // str(EXCLUSIVE_RANGE);   // ...
            str(RANGE);             // ..
            str(DOT);               // .
        }

        // str(BACKTICK);      // `

        str(NEWLINE);       // \n
        str(SEMICOLON);     // ;

        {
            str(ASSIGN_BIT_AND);    // &=
            str(LOGIC_AND);         // &&
            str(BIT_AND);           // &
        }

//        // reserved!!! 是为了更友好的报错与先把关键字保留下来
//        keywords(
//                ASSERT,
//                BREAK,
//                CASE,
//                CATCH,
//                CLASS,
//                CONST, // reserved!!!
//                CONTINUE,
//                CONSTRUCT,
//                DEBUGGER,
//                DEF, // reserved!!!
//                DO, // reserved!!!
//                ELSE,
//                EXTENDS,
//                EXPORT, // reserved!!!
//                FALSE,
//                FOR,
//                FUN,
//                FINALLY,
//                IF,
//                IMPORT, // reserved!!!
//                IN,
//                IS,
//                LET, // reserved!!!
//                MATCH,
//                MODULE, // reserved!!!
//                NEW,
//                NAMESPACE, // reserved!!!
//                NULL,
//                OBJECT,
//                RETURN,
//                STATIC,
////                SUPER, // 直接当做 name 处理了
////                THIS, // 直接当做 name 处理了
//                THEN, // reserved!!!
//                THROW,
//                TRUE,
//                TRY,
//                VAL,
//                VAR,
//                WHILE,
//                WITH // reserved!!!
//        );

        keyword(ASSERT);
        keyword(BREAK);
        keyword(CASE);
        keyword(CATCH);
        keyword(CLASS);
        keyword(CONST); // reserved!!!
        keyword(CONTINUE);
        keyword(CONSTRUCT);
        keyword(DEBUGGER);
        keyword(DEF); // reserved!!!
        keyword(DO); // reserved!!!
        keyword(ENUM);
        keyword(ELSE);
        keyword(EXTENDS);
        keyword(EXPORT); // reserved!!!
        keyword(FALSE);
        keyword(FOR);
        keyword(FUN);
        keyword(FINALLY);
        keyword(IF);
        keyword(IMPORT); // reserved!!!
        keyword(IN);
        keyword(IS);
        keyword(LET); // reserved!!!
        keyword(MATCH);
        keyword(MODULE); // reserved!!!
        keyword(NEW);
        keyword(NAMESPACE); // reserved!!!
        keyword(NULL);
        keyword(OBJECT);
        keyword(RETURN);
        keyword(STATIC);
        keyword(SEALED);
        // keyword(SUPER); // 直接当做 name 处理了
        // keyword(THIS); // 直接当做 name 处理了
        keyword(THEN); // reserved!!!
        keyword(THROW);
        keyword(TRUE);
        keyword(TRY);
        keyword(VAL);
        keyword(VAR);
        keyword(WHILE);
        keyword(WITH); // reserved!!!

        regex("[a-zA-Z_][a-zA-Z0-9_]*", NAME);
        // todo [+-] 这里无效, 因为木有使用最长路径来匹配, +- 被前面优先匹配了
        regex("[+-]?(?:0|[1-9][0-9]*)(?:[.][0-9]+)+(?:[eE][-+]?[0-9]+)?", FLOAT);
        regex("[+-]?(?:0|[1-9][0-9]*)(?:[.][0-9]+)?(?:[eE][-+]?[0-9]+)+", FLOAT);
        regex("[+-]?0b(?:0|1[0-1]*)", INT);
        regex("[+-]?0x(?:0|[1-9a-fA-F][0-9a-fA-F]*)", INT);
        regex("[+-]?0o(?:0|[1-7][0-7]*)", INT);
        regex("[+-]?(?:0|[1-9][0-9]*)", INT);

        regex("`(?:[^`]*)`", SYMBOL);
        // todo 这里处理掉换行
        regex("\"((?:[^\"\\\\]*|\\\\[\"\\\\trnbf\\/]|\\\\u[0-9a-fA-F]{4})*)\"", STRING);
        // todo 这里处理掉换行
        regex("'((?:[^'\\\\]*|\\\\['\\\\trnbf\\/]|\\\\u[0-9a-fA-F]{4})*)'", STRING);

//        Function<String, String> join = chs -> stream(chs.split(""))
//                .map(Pattern::quote).collect(joining("|"));
//        String prefix = "=<>!+-*&|/%^";
//        String suffix = "=<>&|*"; // "=<>&|"
//        regex("(?:" + join.apply(prefix) + ")(?:" + join.apply(suffix) + ")*", OTHER_OP);
    }
    void keywords(@NotNull TokenType ...types) {
        rules.add(new MergedStrRules(true, types));
    }
    void strs(@NotNull TokenType ...types) {
        rules.add(new MergedStrRules(false, types));
    }
    void keyword(@NotNull TokenType type) {
        rules.add(new KeyWordRule(type.name, type, true));
    }
    void str(@NotNull TokenType type) {
        str(type.name, type, true);
    }
    void regex(@NotNull String pattern, @NotNull TokenType type) {
        regex(pattern, type, true);
    }

    static class MergedStrRules implements Lexer.Rule<TokenType> {
        Pattern pat;
        MergedStrRules(boolean keyword, TokenType ...tokenTypes) {
            Expect.notEmpty(tokenTypes, "😒");
            String merged = stream(tokenTypes).map(it -> Pattern.quote(it.name)).collect(joining("|"));
            if (keyword) {
                pat = Pattern.compile("(" + merged + ")(?![a-zA-Z0-9_])");
            } else {
                pat = Pattern.compile(merged);
            }
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
        @Override public TokenType type(String matched) {
            return Expect.notNull(lexerTable.get(matched), "😒");
        }
        @Override public boolean keep() { return true; }
        @Override public String toString() { return pat.toString(); }
    }

    static class KeyWordRule extends Lexer.StrRule<TokenType> {
        KeyWordRule(@NotNull String toMatch, @NotNull TokenType type, boolean keep) {
            super(toMatch, type, keep);
        }
        @Override
        public int tryMatch(String sub) {
            int len = super.tryMatch(sub);
            if (len == -1 || len >= sub.length()) {
                return len;
            }
            char nextChar = sub.charAt(len);
            // keyword 需要匹配完整单词
            if (isLegalIdChar(nextChar)) {
                return -1;
            }
            return len;
        }
        boolean isLegalIdChar(char c) {
            return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
        }
    }
    public static void main(String[] args) {
        // ❗️❗️❗️一个lexeme只能对应于一个 token-type
        LexerRules lexerRules = new LexerRules();
        Map<String, Lexer.Rule<TokenType>> rules = new HashMap<>();
        for (Lexer.Rule<TokenType> rule : lexerRules.rules) {
            if (rule instanceof StrRule) {
                Lexer.Rule<TokenType> put = rules.put(((StrRule<TokenType>) rule).toMatch, rule);
                if (put != null) {
                    System.err.println(rule + "重复添加");
                }
            }
        }
    }
}

// 多行注释匹配 Stack Overflow
// 参考: https://stackoverflow.com/questions/7509905/java-lang-stackoverflowerror-while-using-a-regex-to-parse-big-strings
// 原来实现 "/\\*(?:.|[\\n\\r])*?\\*/" 会 Stack Overflow, 估计是 (?:.|[\\n\\r])* 这个被编译成递归调用...
// regex("/\\*[\\s\\S]*?\\*/", BLOCK_COMMENT, false);

// 之前 operator 的写法...
/*
String prefix = "=<>!+-*&|/%^";
String suffix = "=<>&|*"; // "=<>&|"
Function<String, String> qJoin = chs -> stream(chs.split("")).map(Pattern::quote).collect(joining("|"));
regex("(?:" + qJoin.apply(prefix) + ")(?:" + qJoin.apply(suffix) + ")*", OPERATOR, true);
regex(stream("()[]{}.,?:;~".split("")).map(Pattern::quote).collect(joining("|")), OPERATOR, true);
*/