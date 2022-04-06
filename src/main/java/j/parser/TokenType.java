package j.parser;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static j.parser.TokenType.Associativity.*;
import static j.parser.TokenType.BindingPower.*;
import static j.parser.TokenType.TokenCategory.*;

/**
 * 注意：name不能
 * @author chuxiaofeng
 */
public enum TokenType {

    // =+=+=+=+=+=+=+=+=+=+ 标点&分组 =+=+=+=+=+=+=+=+=+=+=+
    /**
     * LED 作为CALL是左结合中缀操作符 CALL("(", SEPARATOR, 220, LEFT)
     * NUD 作为GROUPING是前缀操作符符号 GROUPING("(", SEPARATOR, 0, NA)
     */
    LEFT_PAREN("(", SEPARATOR, BP_CALL, LEFT),
    RIGHT_PAREN(")", SEPARATOR, BP_NONE, NA),
    /**
     * LED 作为属性访问是左结合中缀操作符 COMPUTED_MEMBER_ACCESS("[", SEPARATOR, 230, LEFT),
     * NUD 作为数组字面量是前缀操作符 ARRAY("[", SEPARATOR, 0, NA),
     */
    LEFT_BRACKET("[", SEPARATOR, BP_MEMBER, LEFT),
    RIGHT_BRACKET("]", SEPARATOR, BP_NONE, NA),
    /**
     * 作为BLOCK是前缀操作符 BLOCK("{", SEPARATOR, BP_NONE, NA),
     * 作为MAP是前缀操作符 MAP("{", SEPARATOR, BP_NONE, NA),
     */
    LEFT_BRACE("{", SEPARATOR, BP_LEFT_BRACE, LEFT),
    RIGHT_BRACE("}", SEPARATOR, BP_NONE, NA),
    // BACKTICK("`", SEPARATOR, BP_NONE, NA),
    COLON(":", SEPARATOR, BP_NONE, NA),
    COMMA(",", SEPARATOR, BP_COMMA, LEFT),


    // =+=+=+=+=+=+=+=+=+=+ 算子：算术 =+=+=+=+=+=+=+=+=+=+=+
    MUL("*", OPERATOR, BP_FACTOR, LEFT), // MULTIPLY | STAR | ASTERISK
    DIV("/", OPERATOR, BP_FACTOR, LEFT), //   DIVIDE | SLASH
    MOD("%", OPERATOR,BP_FACTOR, LEFT), // MODULE | PERCENT
    PLUS("+", OPERATOR, BP_TERM, LEFT),
    MINUS("-", OPERATOR, BP_TERM, LEFT), // SUBTRACT | NEGATIVE
    POWER("**", OPERATOR, BP_EXP, RIGHT), // EXPONENTIATION
    UNARY_PLUS("+", OPERATOR, BP_PREFIX_UNARY, RIGHT, false),
    UNARY_MINUS("-", OPERATOR, BP_PREFIX_UNARY, RIGHT, false),


    // =+=+=+=+=+=+=+=+=+=+ 算子：比较 =+=+=+=+=+=+=+=+=+=+=+
    GT(">", OPERATOR, BP_COMP, LEFT),
    LT("<", OPERATOR, BP_COMP, LEFT),
    LE("<=", OPERATOR, BP_COMP, LEFT),
    GE(">=", OPERATOR, BP_COMP, LEFT),
    EQ("==", OPERATOR, BP_EQ, LEFT),
    NE("!=", OPERATOR, BP_EQ, LEFT),
    STRICT_EQ("===", OPERATOR, BP_EQ, LEFT),
    STRICT_NE("!==", OPERATOR, BP_EQ, LEFT),



    // =+=+=+=+=+=+=+=+=+=+ 算子：位移 =+=+=+=+=+=+=+=+=+=+=+
    GT_GT(">>", OPERATOR, BP_BIT_SHIFT, RIGHT), // BITWISE_RIGHT_SHIFT
    LT_LT("<<", OPERATOR, BP_BIT_SHIFT, RIGHT), // BITWISE_LEFT_SHIFT
    LT_LT_LT("<<<", OPERATOR, BP_BIT_UNSIGNED_SHIFT, RIGHT), // UNSIGNED_BITWISE_LEFT_SHIFT
    GT_GT_GT(">>>", OPERATOR, BP_BIT_UNSIGNED_SHIFT, RIGHT), // UNSIGNED_BITWISE_RIGHT_SHIFT

    // =+=+=+=+=+=+=+=+=+=+ 算子：三元 =+=+=+=+=+=+=+=+=+=+=+
    COND("?", OPERATOR, BP_COND, RIGHT), // CONDITIONAL | QUESTION

    // =+=+=+=+=+=+=+=+=+=+ 算子：Updater =+=+=+=+=+=+=+=+=+=+=+
    PREFIX_INCR("++", OPERATOR, BP_PREFIX_UNARY, RIGHT), // PREFIX_PLUS_PLUS
    PREFIX_DECR("--", OPERATOR, BP_PREFIX_UNARY, RIGHT), // PREFIX__SUB_SUB
    POSTFIX_INCR ("++", OPERATOR, BP_POSTFIX_UNARY, NA, false), // POSTFIX_PLUS_PLUS
    POSTFIX_DECR("--", OPERATOR, BP_POSTFIX_UNARY, NA, false), //  POST_SUB_SUB

    // =+=+=+=+=+=+=+=+=+=+ 算子：赋值 =+=+=+=+=+=+=+=+=+=+=+
    ASSIGN("=", OPERATOR, BP_ASSIGN, RIGHT), // assignment
    ASSIGN_PLUS("+=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_SUB("-=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_MUL("*=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_DIV("/=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_MOD("%=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_LEFT_SHIFT("<<=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_SIGNED_RIGHT_SHIFT(">>=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_UNSIGNED_RIGHT_SHIFT(">>>=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_BIT_OR("|=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_BIT_XOR("^=", OPERATOR, BP_ASSIGN, RIGHT),
    ASSIGN_BIT_AND("&=", OPERATOR, BP_ASSIGN, RIGHT),

    // =+=+=+=+=+=+=+=+=+=+ 算子：位运算 =+=+=+=+=+=+=+=+=+=+=+
    BIT_OR("|", OPERATOR, BP_BIT_OR, LEFT), // BITWISE_OR | PIPE
    BIT_XOR("^", OPERATOR, BP_BIT_XOR, LEFT), // BITWISE_XOR | CARET
    BIT_AND("&", OPERATOR, BP_BIT_AND, LEFT), // BITWISE_AND | AMP
    BIT_NOT("~", OPERATOR, BP_PREFIX_UNARY, RIGHT), // TILDE


    // =+=+=+=+=+=+=+=+=+=+ 算子：Range =+=+=+=+=+=+=+=+=+=+=+
    RANGE("..", OPERATOR, BP_RANGE, LEFT), // DOT_DOT
    // EXCLUSIVE_RANGE("...", OPERATOR, BP_RANGE, LEFT), // DOT_DOT_DOT

    // =+=+=+=+=+=+=+=+=+=+ 算子：逻辑运算 =+=+=+=+=+=+=+=+=+=+=+
    LOGIC_NOT("!", OPERATOR, BP_PREFIX_UNARY, RIGHT),
    LOGIC_AND("&&", OPERATOR, BP_LOGIC_AND, LEFT), // AMP_AMP
    LOGIC_OR("||", OPERATOR, BP_LOGIC_OR, LEFT), // PIPE_PIPE

    // =+=+=+=+=+=+=+=+=+=+ 算子：MISC =+=+=+=+=+=+=+=+=+=+=+
    OTHER_OP("(op)", OPERATOR, BP_NONE, NA), // 自定义算子
    ARROW_BLOCK("->", OPERATOR, BP_NONE, NA),
    ARROW("=>", OPERATOR, BP_MEMBER, RIGHT),
    DOT(".", OPERATOR, BP_MEMBER, LEFT), // MEMBER_ACCESS | METHOD_CALL
    OPT_DOT("?.", OPERATOR, BP_MEMBER, LEFT), // OPTIONAL_CHAINING
    IS("is", OPERATOR, BP_IS, LEFT), // INSTANCE
    IN("in", OPERATOR, BP_IS, LEFT),
    AS("as", OPERATOR, BP_IS, LEFT),
    MATCH("match", KEYWORD, BP_IS, LEFT),
    EXTENDS("extends", KEYWORD, BP_NONE, NA),
    // TYPEOF("typeof", KEYWORD, 200, RIGHT),
    // YIELD("yield", KEYWORD, 60, RIGHT),
    AT("@", OPERATOR, BP_MEMBER, RIGHT),


    // =+=+=+=+=+=+=+=+=+=+ 关键词 =+=+=+=+=+=+=+=+=+=+=+
    BREAK("break", KEYWORD, BP_NONE, NA),
    CONTINUE("continue", KEYWORD, BP_NONE, NA),
    RETURN("return", KEYWORD, BP_NONE, NA),

    IF("if", KEYWORD, BP_NONE, NA),
    ELSE("else", KEYWORD, BP_NONE, NA),
    FOR("for", KEYWORD, BP_NONE, NA),
    DEF("def", KEYWORD, BP_NONE, NA),
    VAL("val", KEYWORD, BP_NONE, NA),
    VAR("var", KEYWORD, BP_NONE, NA),
    NEW("new", KEYWORD, BP_MEMBER, NA),
    CLASS("class", KEYWORD, BP_NONE, NA),
    ENUM("enum", KEYWORD, BP_NONE, NA),
    OBJECT("object", KEYWORD, BP_NONE, NA),
    SEALED("sealed", KEYWORD, BP_NONE, NA),

    CATCH("catch", KEYWORD, BP_NONE, NA),
    THROW("throw", KEYWORD, BP_NONE, NA),
    TRY("try", KEYWORD, BP_NONE, NA),
    FINALLY("finally", KEYWORD, BP_NONE, NA),

    WHILE("while", KEYWORD, BP_NONE, NA),

    STATIC("static", KEYWORD, BP_NONE, NA),
    THIS("this", KEYWORD, BP_NONE, NA),
    SUPER("super", KEYWORD, BP_NONE, NA),
    ASSERT("assert", KEYWORD, BP_NONE, NA),
    DEBUGGER("debugger", KEYWORD,  BP_NONE, NA),
    ARGUMENTS("arguments", KEYWORD, BP_NONE, NA),
    CONSTRUCT("construct", KEYWORD, BP_NONE, NA),

    // 没使用的关键字!!! start
    DO("do", KEYWORD, BP_NONE, NA),
    // END("end", KEYWORD, BP_NONE, NA),
    IMPORT("import", KEYWORD, BP_NONE, NA),
    EXPORT("export", KEYWORD, BP_NONE, NA),
    MODULE("module", KEYWORD, BP_NONE, NA),
    NAMESPACE("namespace", KEYWORD, BP_NONE, NA),
    CASE("case", KEYWORD, BP_NONE, NA),
    THEN("then", KEYWORD, BP_NONE, NA),
    CONST("const", KEYWORD,  BP_NONE, NA),
    LET("let", KEYWORD, BP_NONE, NA),
    FUN("fun", KEYWORD, BP_NONE, NA),
    WITH("with", KEYWORD, BP_NONE, NA),
    // CALLCC("callcc", KEYWORD, BP_NONE, NA),
    // 没使用的关键字!!! end


    // =+=+=+=+=+=+=+=+=+=+ 标识符 + 字面量 =+=+=+=+=+=+=+=+=+=+=+
    NAME("(name)", IDENTIFIER, BP_NONE, NA),
    NULL("null", LITERAL, BP_NONE, NA),
    TRUE("true", LITERAL, BP_NONE, NA),
    FALSE("false", LITERAL, BP_NONE, NA),
    FLOAT("(float)", LITERAL, BP_NONE, NA),
    INT("(int)", LITERAL, BP_NONE, NA),
    STRING("(string)", LITERAL, BP_NONE, NA),
    SYMBOL("(symbol)", LITERAL, BP_NONE, NA),



    // =+=+=+=+=+=+=+=+=+=+ 注释 + 分隔符 =+=+=+=+=+=+=+=+=+=+=+=+
    BLOCK_COMMENT("/*", COMMENT,  BP_NONE, NA),
    LINE_COMMENT("//", COMMENT, BP_NONE, NA),

    SEMICOLON(";", SEPARATOR, BP_NONE, NA),
    NEWLINE("\n", SEPARATOR, BP_NONE, NA),

    WHITESPACE("(space)", SPACE, BP_NONE, NA),
    EOF("(eof)", SPACE, BP_NONE, NA);

    public final String name;
    public final BindingPower bp;
    public final Associativity asso;
    public final TokenCategory type;
    public final boolean lexerResult;

    TokenType(String name, TokenCategory type, BindingPower bp, Associativity asso) {
        this(name, type, bp, asso, true);
    }

    TokenType(String name, TokenCategory type, BindingPower bp, Associativity asso, boolean lexerResult) {
        this.name = name;
        this.bp = bp;
        this.asso = asso;
        this.type = type;
        this.lexerResult = lexerResult;
    }

    public static void main(String[] args) {
        Map<String, TokenType> map = new HashMap<>();
        for (TokenType it : values()) {
            TokenType prev = map.put(it.name, it);
            if (prev != null) {
                System.out.println(it.name() + " 的 name 与 " + prev.name() + " 重复");
            }
        }
    }

    public final static Map<String, TokenType> lexerTable = Collections.unmodifiableMap(lexerTable());
    private static Map<String, TokenType> lexerTable() {
        Map<String, TokenType> lexerTable = new HashMap<>();
        for (TokenType it : values()) {
            if (it.lexerResult) {
                lexerTable.put(it.name, it);
            }
        }
        return lexerTable;
    }

    static TokenType[] assignable() {
        return new TokenType[] {
                ASSIGN, ASSIGN_PLUS, ASSIGN_SUB, ASSIGN_MUL, ASSIGN_DIV, ASSIGN_MOD,
                ASSIGN_LEFT_SHIFT, ASSIGN_SIGNED_RIGHT_SHIFT, ASSIGN_UNSIGNED_RIGHT_SHIFT,
                ASSIGN_BIT_OR, ASSIGN_BIT_XOR, ASSIGN_BIT_AND
        };
    }

    static TokenType trimAssign(@NotNull TokenType t) {
        switch (t) {
            case ASSIGN: return ASSIGN;
            case ASSIGN_PLUS: return PLUS;
            case ASSIGN_SUB: return MINUS;
            case ASSIGN_MUL: return MUL;
            case ASSIGN_DIV: return DIV;
            case ASSIGN_MOD: return MOD;
            case ASSIGN_LEFT_SHIFT: return LT_LT;
            case ASSIGN_SIGNED_RIGHT_SHIFT: return GT_GT;
            case ASSIGN_UNSIGNED_RIGHT_SHIFT: return GT_GT_GT;
            case ASSIGN_BIT_OR: return BIT_OR;
            case ASSIGN_BIT_XOR: return BIT_XOR;
            case ASSIGN_BIT_AND: return BIT_AND;
            default: throw new IllegalStateException();
        }
    }

    static TokenType[] varDefinable() {
        return new TokenType[] { /*LET, CONST,*/ VAR, VAL };
    }
    static TokenType[] definable() {
        return new TokenType[] { AT/*at目前只要 fun 在使用*/, FUN,  /*DEF, LET, CONST,*/ VAR, VAL };
    }
    static boolean mutable(@NotNull TokenType def) {
        return /*def != CONST && */def != VAL;
    }

    // ====================================================================================
    public enum Associativity { NA, LEFT, RIGHT }
    public enum TokenCategory {
        IDENTIFIER, OPERATOR, KEYWORD, SEPARATOR, LITERAL, COMMENT, SPACE, OTHER;
    }
    public enum BindingPower {
        BP_NONE(0),
        BP_LOWEST(0),
        BP_COMMA(50),               // ,
        BP_LEFT_BRACE(55),          // {  ❗️ 这个优先级需要再考虑下
        BP_ARROW(65),               // ->
        BP_ASSIGN(70),              // =
        BP_COND(80),                // ? :
        BP_LOGIC_OR(90),            // ||
        BP_LOGIC_AND(100),          // &&
        BP_EQ(140),                 // == != === !==
        BP_IS(145),                 // is
        BP_COMP(150),               // < > <= >=
        BP_BIT_OR(155),             // |
        BP_BIT_XOR(156),            // ^
        BP_BIT_AND(157),            // &
        BP_BIT_SHIFT(160),          // << >>
        BP_BIT_UNSIGNED_SHIFT(161), // <<< >>>
        BP_RANGE(165),              // .. ...
        BP_TERM(170),               // + -
        BP_FACTOR(180),             // * / %
        BP_EXP(190),                // **
        BP_PREFIX_UNARY(200),       // - ! ~ ++ --
        BP_POSTFIX_UNARY(210),      // ++ --
        BP_CALL(220),               // ()
        BP_MEMBER(230),             // . [] new
        ;
        public final int prec; // precedence
        BindingPower(int prec) {
            this.prec = prec;
        }
    }
}
