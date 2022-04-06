package j.parser;

import j.Ast;
import j.parser.Parser.Grammar;
import org.jetbrains.annotations.NotNull;

import static j.parser.Parser.Led;
import static j.parser.TokenType.*;

/**
 * Parser Specification : Syntactic Grammar
 * @author chuxiaofeng
 */
@SuppressWarnings({"WeakerAccess", "Convert2MethodRef"})
public class GrammarRule extends Grammar<ParserJ, TokenType> {

    public GrammarRule() {
        super(values().length);

        prefix(NAME, (p, tok) -> Ast.id(tok.loc, tok.lexeme));

        // 字面量
        prefix(NULL, (p, tok) -> Ast.litNull(tok.loc));
        prefix(TRUE, (p, tok) -> Ast.litTrue(tok.loc));
        prefix(FALSE, (p, tok) -> Ast.litFalse(tok.loc));
        prefix(INT, (p, tok) -> p.litInt(tok));
        prefix(FLOAT, (p, tok) -> p.litFloat(tok));
        prefix(STRING, (p, tok) -> p.interpolation(tok));
        prefix(SYMBOL, (p, tok) -> p.litSymbol(tok));

        // prefix(LEFT_BRACKET, (p, tok) -> p.list(tok));
        // prefix(LEFT_BRACE, (p, tok) -> p.braceMap(tok));
        prefix(LEFT_BRACKET, (p, tok) -> p.leftBracket(tok));
        prefix(LEFT_BRACE, (p, tok) -> /*p.blockOrClosure(tok)*/ p.block(tok));
        prefix(LEFT_PAREN, (p, tok) -> p.leftParen(tok));
        // prefix(BACKTICK, (p, tok) -> );

        // 范围
        // infixLeft(RANGE, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(RANGE, (p, expr, tok) -> p.desugarRange(expr, tok));
        // infixLeft(EXCLUSIVE_RANGE, (p, expr, tok) -> p.binaryR(expr, tok));

        // 声明
        prefix(ENUM, (p, tok) -> p.defEnumClass(tok));
        prefix(SEALED, (p, tok) -> p.defSealed(tok));
        prefix(CLASS, (p, tok) -> p.defClass(tok));
        prefix(OBJECT, (p, tok) -> p.defObject(tok));
        prefix(VAL, (p, tok) -> p.defVar(tok));     // !MUT var+prop
        prefix(VAR, (p, tok) -> p.defVar(tok));     // MUT var+prop
        prefix(FUN, (p, tok) -> p.defFun(tok, null));     // MUT fun+method
        prefix(AT, (p, tok) -> p.decorator(tok));
//        prefix(DEF, (p, tok) -> p.defFun(tok));     // MUT fun+method
//        prefix(LET, (p, tok) -> p.defVar(tok));     // MUT var+prop
//        prefix(CONST, (p, tok) -> p.defVar(tok));   // !MUT var+prop


        // 赋值
        infixRight(ASSIGN, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_PLUS, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_SUB, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_MUL, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_DIV, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_MOD, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_LEFT_SHIFT, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_SIGNED_RIGHT_SHIFT, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_UNSIGNED_RIGHT_SHIFT, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_BIT_OR, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_BIT_XOR, (p, expr, tok) -> p.assign(expr, tok));
        infixRight(ASSIGN_BIT_AND, (p, expr, tok) -> p.assign(expr, tok));

        // (伪)语句
        prefix(NEW, (p, tok) -> p.newStmt(tok));
        prefix(BREAK, (p, tok) -> p.breakStmt(tok));
        prefix(CONTINUE, (p, tok) -> p.continueStmt(tok));
        prefix(FOR, (p, tok) -> p.forStmt(tok));
        prefix(IF, (p, tok) -> p.ifStmt(tok));
        prefix(RETURN, (p, tok) -> p.returnStmt(tok));
        prefix(WHILE, (p, tok) -> p.whileStmt(tok));
        prefix(MATCH, (p, tok) -> p.matchStmt(tok));
        // prefix(LEFT_BRACE, (p, tok) -> p.block(tok));
        prefix(ASSERT, (p, tok) -> p.assert1(tok));
        prefix(DEBUGGER, (p, tok) -> Ast.debugger(tok.loc));
        prefix(THROW, (p, tok) -> p.throw1(tok));
        prefix(TRY, (p, tok) -> p.try1(tok));

        // 一元算子
        prefix(MINUS, (p, tok) -> p.unary(Token.copy(tok, UNARY_MINUS)));
        prefix(PLUS, (p, tok) -> p.unary(Token.copy(tok, UNARY_PLUS)));
        prefix(PREFIX_INCR, (p, tok) -> p.unary(tok));
        prefix(PREFIX_DECR, (p, tok) -> p.unary(tok));
        postfix(PREFIX_INCR, (p, expr, tok) -> p.unaryPostfix(expr, Token.copy(tok, POSTFIX_INCR)));
        postfix(PREFIX_DECR, (p, expr, tok) -> p.unaryPostfix(expr, Token.copy(tok, POSTFIX_DECR)));


        // 二元算子：算术运算
        infixLeft(MUL, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(DIV, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(MOD, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(PLUS, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(MINUS, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(POWER, (p, expr, tok) -> p.binaryR(expr, tok));

        // 二元算子：比特运算
        infixLeft(BIT_OR, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(BIT_XOR, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(BIT_AND, (p, expr, tok) -> p.binaryL(expr, tok));
        // 一元算子：比特运算
        prefix(BIT_NOT, (p, tok) -> p.unary(tok));

        // 二元运算：instanceof
        infixLeft(IS, (p, expr, tok) -> p.is(expr, tok));
        infixLeft(IN, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(MATCH, (p, expr, tok) -> p.matchStmt(expr, tok));

        // 二元算子：比较
        infixLeft(GT, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(LT, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(LE, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(GE, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(EQ, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(NE, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(STRICT_EQ, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(STRICT_NE, (p, expr, tok) -> p.binaryL(expr, tok));

        // 二元算子：位移算子
        infixRight(GT_GT, (p, expr, tok) -> p.binaryR(expr, tok));
        infixRight(LT_LT, (p, expr, tok) -> p.binaryR(expr, tok));
        infixRight(LT_LT_LT, (p, expr, tok) -> p.binaryR(expr, tok));
        infixRight(GT_GT_GT, (p, expr, tok) -> p.binaryR(expr, tok));

        // 三元运算
        infixRight(COND, (p, expr, tok) -> p.cond(expr, tok));

        // 一元算子：逻辑算子
        prefix(LOGIC_NOT, (p, tok) -> p.unary(tok));
        // 二元算子：逻辑算子
        infixLeft(LOGIC_AND, (p, expr, tok) -> p.binaryL(expr, tok));
        infixLeft(LOGIC_OR, (p, expr, tok) -> p.binaryL(expr, tok));

        // call
        infixLeft(LEFT_PAREN, (p, expr, tok) -> p.callLeftParen(expr, tok));
        // infixLeft(LEFT_BRACE, (p, expr, tok) -> p.callLeftBrace(expr, tok));
        infixLeft(DOT, (p, expr, tok) -> p.dot(expr, tok));

        // misc
        infixLeft(LEFT_BRACKET, (p, expr, tok) -> p.subscript(expr, tok));
        infixRight(ARROW, (p, expr, tok) -> p.arrowFn(expr, tok));
    }

    void infixRight(@NotNull TokenType type, @NotNull Led<ParserJ, TokenType> led) {
        infixRight(type, type.bp.prec, led);
    }

    void infixLeft(@NotNull TokenType type, @NotNull Led<ParserJ, TokenType> led) {
        infixLeft(type, type.bp.prec, led);
    }

    void postfix(@NotNull TokenType type, @NotNull Led<ParserJ, TokenType> led) {
        postfix(type, type.bp.prec, led);
    }
}
