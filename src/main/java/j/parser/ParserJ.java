package j.parser;

import j.*;
import j.Error;
import j.parser.Lexer.SourceInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

import static j.Ast.*;
import static j.Ast.AssignType.*;
import static j.Helper.*;
import static j.Value.*;
import static j.Value.Constant.*;
import static j.parser.Location.None;
import static j.parser.Location.range;
import static j.parser.TokenType.*;
import static java.util.stream.Collectors.*;

/**
 * Parser for J
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class ParserJ extends Parser<ParserJ, TokenType> {
    int inLoop  = 0;
    int inClass = 0;
    Id classId = null;
    int inFun = 0;

    public ParserJ(@NotNull SourceInput si) {
        super(
                new GrammarRule(),
                new Lexer<>(new LexerRules(), EOF, si)
        );
    }

    public Block parse() {
        try {
            return program();
        } catch (Error error) {
            if (error.loc != null) {
                err(error.loc.inspect(error.msg));
            }
            throw error;
        }
    }

    Block program() {
        List<Expr> stmts = new ArrayList<>();

        String src = tokIter.lexer.src.src;
        String file = null;
        String dir = null;
        if (fileExists(src)) {
            Path path = Paths.get(src).normalize();
            file = path.toString();
            dir = path.getParent().toString();
        }
        stmts.add(Ast.varDef(None, Ast.id(None, "__FILE__"), file == null ? Ast.litNull(None) : Ast.litString(None, "\"" + file + "\""), false, true));
        stmts.add(Ast.varDef(None, Ast.id(None, "__DIR__"), dir == null ? Ast.litNull(None) : Ast.litString(None, "\"" + dir + "\""), false, true));

        // 所有文件都在单独的 scope 中 !!!
        Block program = stmts(peek().loc, stmts, EOF, true);
        eat(EOF);
        return program;
    }

    public Ast.FunDef module() {
        Ast.Block body = parse();
        return Ast.funDef(Location.None, null, Ast.anon(Location.None), new LinkedHashMap<>(0), body,
                false, false, false);
    }



    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    // 其实都是表达式...
    Expr stmt() {
        return expr();
    }

    // 多加个参数，详细的报错...
    boolean unexpectEOF(int i) {
        if (peek(i).is(EOF)) {
            throw Error.syntax(peek(i).loc, "到底了...");
        }
        return true;
    }

    Token<TokenType> eatTriviaName() {
        Token<TokenType> name = eat(NAME);
        // 这里 keyword 都用其他 tokenType 表示, 不用判断
        // if (TokenType.reserved(name.lexeme)) { throw Error.syntax(name.loc, name + " 是保留字"); }
        return name;
    }

    boolean tryEatLines() {
        if (tryEat(NEWLINE) == null) {
            return false;
        }
        //noinspection StatementWithEmptyBody
        while (tryEat(NEWLINE) != null);
        return true;
    }

    Block stmts(Location from, List<Expr> stmts, TokenType until, boolean scope) {
        tryEatLines();
        while (peek(until) == null) {
            stmts.add(stmt());
            tryEatLines();
        }
        Location loc = stmts.isEmpty() ? from : range(stmts.get(0).loc, stmts.get(stmts.size() - 1).loc);
        return Ast.block(loc, stmts, scope);
    }

    Block exprBlock() {
        tryEatLines();
        return exprBlock(expr());
    }

    Block exprBlock(@NotNull Expr expr) {
        return Ast.block(expr.loc, lists(expr));
    }

    LinkedHashMap<Id, Literal> params(Token<TokenType> lp) {
        tryEatLines();
        Token<TokenType> rp = tryEat(RIGHT_PAREN);
        if (rp == null) {
            LinkedHashMap<Id, Literal> params = params();
            tryEatLines();
            rp = eat(RIGHT_PAREN);
            checkParametersDefaults(range(lp, rp), params);
            return params;
        } else {
            return new LinkedHashMap<>(0);
        }
    }
    void checkParametersDefaults(Location loc, LinkedHashMap<Id, Literal> params) {
        boolean hasDefault = false;
        for (Literal value : params.values()) {
            if (hasDefault && value == null) {
                throw Error.syntax(loc, "默认参数位置错误");
            }
            hasDefault = value != null;
        }
    }
    LinkedHashMap<Id, Literal> params() {
        LinkedHashMap<Id, Literal> params = new LinkedHashMap<>();
        do {
            tryEatLines();
            Token<TokenType> tok = eatTriviaName();
            Literal defVal = optionalDefaultValue(tok.loc);
            params.put(id(tok.loc, tok.lexeme), defVal);
        } while (tryEat(COMMA) != null);
        return params;
    }
    @Nullable Literal optionalDefaultValue(Location loc) {
        tryEatLines();
        Token<TokenType> assign = tryEat(ASSIGN);
        if (assign == null) {
            return null;
        } else {
            tryEatLines();
            Expr expr = expr();
            if (!(expr instanceof Literal)) {
                throw Error.syntax(range(loc, expr.loc), "参数默认值只能是字面量");
            }
            return ((Literal) expr);
        }
    }
    Token<TokenType> type() {
        Token<TokenType> colon = tryEat(COLON);
        if (colon == null) {
            return null;
        } else {
            return eatTriviaName();
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    @SafeVarargs
    final <T> T any(Supplier<T>... choices) {
        Token<TokenType> marked = mark();
        List<Error> errors = new ArrayList<>();
        for (Supplier<T> choice : choices) {
            try {
                return choice.get();
            } catch (Error e) {
                errors.add(e);
                reset(marked);
            }
        }
        throw Error.mixed(errors);
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    Expr litInt(Token<TokenType> tok) {
        return Ast.litInt(tok.loc, tok.lexeme);
    }

    Expr litFloat(Token<TokenType> tok) {
        return Ast.litFloat(tok.loc, tok.lexeme);
    }

    Expr interpolation(Token<TokenType> tok) {
        // TODO 支持字符串 插值 BACKTICK, ast 转换成 +
        return Ast.litString(tok.loc, tok.lexeme);
    }

    Expr litSymbol(Token<TokenType> tok) {
        return Ast.litSymbol(tok.loc, tok.lexeme);
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    void enterClass(Id classId) {
        inClass++;
        this.classId = classId;
    }
    void exitClass() {
        inClass--;
        this.classId = null;
    }
    void enterFun() {
        inFun++;
    }
    void exitFun() {
        inFun--;
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    /*
    @Deprecated
    Expr blockOrClosure(@NotNull Token<TokenType> lb) {
        boolean[] r = blockHasArrow();
        boolean hasParam = r[0];
        boolean hasArrow = r[1];

        if (hasArrow) {
            return closureBlock(lb, hasParam, hasArrow);
        } else {
            return block(lb);
        }
    }
    */

    /**
     * 允许 Statement 的地方都可以使用 block
     * if / for / while / fun-body / method-body 都是 block
     */
    Block block(@NotNull Token<TokenType> lb) {
        tryEatLines();
        Token<TokenType> rb = tryEat(RIGHT_BRACE);
        if (rb != null) {
            return emptyBlock(range(lb, rb)); // { }
        }

        List<Expr> stmts = new ArrayList<>();
        do {
            stmts.add(stmt());
            tryEatLines();
        } while (peek(RIGHT_BRACE) == null);
        return Ast.block(range(lb, eat(RIGHT_BRACE)), stmts);
    }

    Block loopBlock(@NotNull Token<TokenType> lb) {
        try {
            inLoop++;
            return block(lb);
        } finally {
            inLoop--;
        }
    }

    @Nullable
    Block tryLoopBlock() {
        Token<TokenType> lb = tryEat(LEFT_BRACE);
        return lb == null ? null : loopBlock(lb);
    }

    /*
    @Deprecated
    boolean[] blockHasArrow() {
        int i = 0;
        boolean hasParam = false;
        while (unexpectEOF(i)) {
            while (peek(i).is(NEWLINE)) i++;
            if (peek(i).is(NAME)) {
                i++;
                hasParam = true;
            } else {
                break;
            }
            if (peek(i).is(COMMA)) {
                i++;
            } else {
                break;
            }
        }
        while (peek(i).is(NEWLINE)) i++;

        boolean hasArrow = peek(i).is(ARROW_BLOCK);
        if (!hasArrow) hasParam = false;

        return new boolean[] { hasParam, hasArrow };
    }
    // { [params -> ] stmts }
    @Deprecated
    FunDef closureBlock(@NotNull Token<TokenType> lb) {
        tryEatLines();

        boolean[] r = blockHasArrow();
        boolean hasParam = r[0];
        boolean hasArrow = r[1];

        return closureBlock(lb, hasParam, hasArrow);
    }
    @Deprecated
    FunDef closureBlock(@NotNull Token<TokenType> lb, boolean hasParam, boolean hasArrow) {
        try {
            inFun++;
            return closureBlock1(lb, hasParam, hasArrow);
        } finally {
            inFun--;
        }
    }
    @Deprecated
    FunDef closureBlock1(@NotNull Token<TokenType> lb, boolean hasParam, boolean hasArrow) {
        tryEatLines();

        List<Id> params;
        Location loc;
        if (hasParam && hasArrow) {
            // { id1[, idn] -> }
            params = params();
            loc = eat(ARROW_BLOCK).loc;
        } else {
            // { } / { -> }
            params = closureBlockDefaultParam();
            if (hasArrow) eat(ARROW_BLOCK);
            loc = lb.loc;
        }

        Block body = stmts(loc, RIGHT_BRACE);
        Token<TokenType> rb = eat(RIGHT_BRACE);
        return Ast.funDef(range(lb, rb), Ast.id(), params, body, inClass > 0, false);
    }
    @Deprecated
    List<Id> closureBlockDefaultParam() {
        return lists(Ast.id(Location.None, "it"));
    }
    */

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    Expr def(Token<TokenType> def) {
        if (def.is(AT)) {
            return decorator(def);
        } else if (def.is(FUN)) {
            return defFun(def, null);
        } else {
            return defVar(def);
        }
    }

    Expr defVar(Token<TokenType> def) {
        tryEatLines();
        Token<TokenType> lb = tryEat(LEFT_BRACKET);
        List<VarDef> defs;
        if (lb == null) {
            defs = defVarIds(def);
        } else {
            defs = defVarPattern(def, lb);
        }
        if (defs.size() == 1) {
            return defs.get(0);
        } else {
            List<Expr> exprs = defs.stream().map(it -> ((Expr) it)).collect(toList());
            return Ast.block(range(defs.get(0).loc, defs.get(defs.size() - 1).loc), exprs, false);
        }
    }

    List<VarDef> defVarIds(Token<TokenType> defTok) {
        List<VarDef> varDefs = new ArrayList<>();
        do {
            VarDef def = defVarId(defTok, true);
            varDefs.add(def);
            // 逗号分隔声明必须写在一行,否则吃掉换行, var a = 1\n[b, a] = ... 会解析成数组访问
            // tryEatLines();
        } while (tryEat(COMMA) != null);
        return varDefs;
    }

    List<VarDef> defVarPattern(Token<TokenType> def, Token<TokenType> leftBracket) {
        boolean mut = TokenType.mutable(def.type);
        Pattern pattern = pattern(leftBracket);
        tryEatLines();
        Token<TokenType> assign = eat(ASSIGN);
        Expr init = expr();
        return DefineAssignDesugar.desugarDefine(def.loc, assign.loc, mut, pattern, init);
        // 如果不解糖，就得靠 scope 的 define pattern 处理了
        // return varDef(range(def.loc, init.loc), pattern, init, mut, true);
    }

    VarDef defVarId(Token<TokenType> def, boolean tryEatAssign) {
        boolean mut = TokenType.mutable(def.type);
        Token<TokenType> name = eatTriviaName();
        Id id = id(name.loc, name.lexeme);
        // 赋值必须写在一行, 否则语法冲突 e.g.
        // var a
        // var b
        // [a, b] = [1, 2]
        // tryEatLines();
        if (tryEatAssign && tryEat(ASSIGN) != null) {
            Expr init = expr();
            return varDef(range(def.loc, init.loc), id, init, mut, true);
        } else {
            if (mut) {
                return varDef(range(def, name), id, litNull(id.loc), mut, false);
            } else {
                return varDef(range(def, name), id, litUndefined(id.loc), mut, false);
            }
        }
    }

    Expr decorator(@NotNull Token<TokenType> at) {
        List<Call> decorators = new ArrayList<>();
        decorators.add(decorator2(at));
        tryEatLines();
        while ((at = tryEat(AT)) != null) {
            decorators.add(decorator2(at));
            tryEatLines();
        }
        return defFun(eat(FUN), decorators);
    }
    Call decorator2(Token<TokenType> at) {
        Expr expr = expr();
        checkDecorator(expr.loc, expr, true);
        if (expr instanceof Call) {
            Call call = (Call) expr;
            return Ast.call(range(at.loc, call.loc), call.oploc, call.callee, call.args);
        } else {
            return Ast.call(range(at.loc, expr.loc), expr.loc, expr, lists());
        }
    }
    void checkDecorator(Location loc, Expr expr, boolean callAllowed) {
        if (expr instanceof Ast.Id) {
            // Fun
        } else if (callAllowed && expr instanceof Call) {
            Call call = (Call) expr;
            if (!isLiteral(call.args)) {
                throw Error.syntax(loc, "无效的装饰器");
            }
            checkDecorator(loc, call.callee, false);
        } else if (expr instanceof ObjectMember) {
            ObjectMember om = (ObjectMember) expr;
            checkDecorator(loc, om.object, false);
        } else if (expr instanceof Subscript) {
            Subscript subs = (Subscript) expr;
            checkDecorator(loc, subs.object, false);
            if (!isLiteral(subs.idxKey)) {
                throw Error.syntax(loc, "无效的装饰器");
            }
        } else {
            throw Error.syntax(loc, "无效的装饰器");
        }
    }

    Call decorator1(Token<TokenType> at) {
        Token<TokenType> name = eat(NAME);
        Id dName = id(range(at.loc, name.loc), name.lexeme);
        Token<TokenType> lp = tryEat(LEFT_PAREN);
        if (lp == null) {
            return Ast.call(range(at.loc, dName.loc), dName.loc, dName, lists());
        } else {
            Call call = callLeftParen(dName, lp);
            for (Expr arg : call.args) {
                if (!(arg instanceof Literal)) {
                    throw Error.syntax(call.loc, "装饰器参数必须为常量");
                }
            }
            return Ast.call(range(at.loc, call.loc), call.oploc, call.callee, call.args);
        }
    }
    // def x() { }
    Expr defFun(@NotNull Token<TokenType> def, @Nullable List<Call> decorators) {
        try {
            enterFun();
            FunDef funDef = defFun1(def);
            if (decorators == null) {
                return funDef;
            } else {
                return desugarDecorators(funDef, decorators);
            }
        } finally {
            exitFun();
        }
    }
    Expr desugarDecorators(FunDef f, List<Call> decorators) {
        if (f.symbol) {
            throw Error.syntax(f.loc, "symbol 函数不支持装饰器");
        }
        Id fName = ((Id) f.name);

        // 实参形参名字不变
        List<Expr> idArgs = new ArrayList<>(f.params.size());
        // todo 如果做命名参数，这里需要注意
        idArgs.addAll(f.params.keySet());

        Expr funArgs = desugarListLit(f.loc, idArgs);

        List<Expr> exprs = new ArrayList<>();
        Id oriName = Ast.idSuffix(f.name.loc, fName + "_ORIGIN");
        FunDef oriF = funDef(f.loc, f.className, oriName, f.params, f.body, f.arrow, f.symbol, f.extend);
        exprs.add(oriF);

        FunDef lastFunDef = oriF;
        String prefix = anon(f.loc).name;
        for (int i = decorators.size() - 1; i >= 0; i--) {
            Call decorator = decorators.get(i);
            Location loc = decorator.loc;
            String dName = decorator.callee instanceof Id ? ((Id) decorator.callee).name : "x";
            //noinspection StringConcatenationInLoop
            prefix = prefix + "_" + dName;


            Id decoratorId;
            Expr decoratorDef = null ;
            if (decorator.callee instanceof Id) {
                decoratorId = ((Id) decorator.callee);
            } else {
                decoratorId = anon(loc);
                decoratorDef = varDef(loc, decoratorId, decorator.callee, false, true);
            }

            // defaultArgs
            // assert decorator is Fun
            // val defaults = decorator.defaults
            // val args_merge_defaults = require("helper.j").args_merge_defaults
            // decorator(f, [], args_merge_defaults([], defaults.size() == 3 ? defaults[2] : []))

            // assert decorator is Fun
            Assert assertIsFun = Ast.assert1(loc, binary(loc, loc, IS, decoratorId, litClass(loc, Class_Fun)), null);
            Expr decoratorArgs = desugarListLit(loc, decorator.args);
            Id defaultsId = idSuffix(loc, "defaults");
            // val defaults = decorator.defaults
            VarDef defaultsDef = Ast.varDef(loc, defaultsId, objectMember(loc, loc, decoratorId, id(loc, "defaults")), false, true);
            // defaults[2]
            Expr decoratorDefaults = Ast.subscript(loc, loc, defaultsId, Ast.litInt(loc, "2"));
            // defaults.size() == 3 ? defaults[2] : []
            decoratorDefaults = Ast.ternary(loc, loc, COND,
                    Ast.binary(loc, loc, EQ,
                            Ast.call(loc, loc, Ast.objectMember(loc, loc, defaultsId, Ast.id(loc, "size")), lists()),
                            Ast.litInt(loc, "3")),
                    decoratorDefaults,
                    Ast.litList(loc, lists()));

            Id argsMergeDefaultId = idSuffix(loc, "args_merge_defaults");
            // val args_merge_defaults = require("helper.j").args_merge_defaults
            VarDef argsMergeDefault = varDef(loc, argsMergeDefaultId, objectMember(loc, loc,
                    call(loc, loc, id(loc, "require"), lists(litString(loc, "\"helper.j\""))),
                    id(loc, "args_merge_defaults")
            ), false, true);
            // args_merge_defaults([], defaults.size() == 3 ? defaults[2] : [])
            decoratorArgs = Ast.call(loc, loc, argsMergeDefaultId, lists(decoratorArgs, decoratorDefaults));
            // decorator(f, [], args_merge_defaults([], defaults.size() == 3 ? defaults[2] : []))
            Call unDecorator = Ast.call(loc, decorator.oploc, decoratorId, lists(lastFunDef.name, funArgs, decoratorArgs));
            List<Expr> stmts;
            if (decoratorDef == null) {
                stmts = lists(              assertIsFun, defaultsDef, argsMergeDefault, unDecorator);
            } else {
                stmts = lists(decoratorDef, assertIsFun, defaultsDef, argsMergeDefault, unDecorator);
            }
            Block body = Ast.block(loc, stmts);
            Id name = Ast.id(decorator.loc, prefix + "_" + fName.name);
            lastFunDef = funDef(f.loc, f.className, name, f.params, body, f.arrow, f.symbol, f.extend);
            exprs.add(lastFunDef);
        }

        Call proxy2LastFun = call(f.loc, f.loc, lastFunDef.name, idArgs);
        Block body = Ast.block(f.loc, lists(proxy2LastFun));
        FunDef funDef = funDef(f.loc, f.className, fName, f.params, body, f.arrow, f.symbol, f.extend);
        exprs.add(funDef);
        return Ast.block(f.loc, exprs, false);
    }
    FunDef defFun1(@NotNull Token<TokenType> def) {
        /*
    fun()
    fun f()
    fun construct()
    fun `sym`()
    fun f extends C()
    fun construct() extends C()
    fun `sym` extends C()
        */
        tryEatLines();
        Token<TokenType> sym = tryEat(SYMBOL);
        Expr name = funName(sym);
        tryEatLines();

        Token<TokenType> ext = tryEat(EXTENDS);
        Id extClassId = classId;
        if (ext != null) {
            if (inClass > 0) {
                throw Error.syntax(ext.loc, "class 中不能声明扩展方法");
            }
            tryEatLines();

            Token<TokenType> clsName = eatTriviaName();
            extClassId = id(clsName.loc, clsName.lexeme);
            tryEatLines();
        }

        LinkedHashMap<Id, Literal> params = params(eat(LEFT_PAREN));
        tryEatLines();
        Block body = funBody(def);
        Location loc = range(def.loc, body.loc);

        /*
        if (inClass > 0 && name instanceof Id && isAnonymous(((Id) name))) {
            throw Error.syntax(loc, "方法不能匿名");
        }
        */

        if (inClass <= 0 && sym != null && ext == null) {
            throw Error.syntax(loc, "symbol 只能声明方法名, 或者扩展方法，不能声明函数名");
        }

        return Ast.funDef(loc, extClassId, name, params, body, false, sym != null, ext != null);
    }
    Expr funName(Token<TokenType> sym) {
        // 4种情况, ( | sym | construct | name
        if (sym != null) {
            return litSymbol(sym);
        }
        Token<TokenType> tok;
        if (peek(0).is(LEFT_PAREN)) {
            return anon(peek().loc);// 匿名函数
        } else if ((tok = tryEat(CONSTRUCT)) != null) {
            return id(tok.loc, tok.lexeme);
        } else {
            tok = eatTriviaName();
            return id(tok.loc, tok.lexeme);
        }
    }
    Block funBody(Token<TokenType> def) {
        Token<TokenType> tok = tryEatAny(ASSIGN, LEFT_BRACE);
        if (tok == null) {
            throw Error.syntax(def.loc, "方法/函数体必须为 def f() {} 或 def f() = expr 形式");
        }
        if (tok.is(LEFT_BRACE)) {
            return block(tok);
        } else {
            return exprBlock();
        }
    }

    // object App { }
    Expr defObject(Token<TokenType> obj) {
        tryEatLines();

        // <1> class-name
        boolean anonymous = peek(EXTENDS) != null || peek(LEFT_BRACE) != null || peek(NEWLINE, LEFT_BRACE);
        if (anonymous) {
            Id classId = anon(obj.loc);
            ClassDef clsDef = defClass(classId, null, obj);

            // return call(obj.loc, obj.loc, clsDef, lists());
            New newClass = Ast.newStmt(obj.loc, obj.loc, classId, lists());
            return Ast.block(obj.loc, lists(clsDef, newClass));
        } else {
            Token<TokenType> id = eatTriviaName();
            tryEatLines();
            Id objectId = id(id.loc, id.lexeme);
            Id classId = idSuffix(objectId.loc, "_class_" + id.lexeme);
            ClassDef clsDef = defClass(classId, null, obj);

            // Call call = call(obj.loc, obj.loc, clsDef, lists());
            // return varDef(obj.loc, objectId, call, true, true);
            New newClass = Ast.newStmt(obj.loc, obj.loc, classId, lists());
            Block block = Ast.block(obj.loc, lists(clsDef, newClass));
            return varDef(obj.loc, objectId, block, true, true);
        }
    }
    ClassDef defSealed(Token<TokenType> sealed) {
        tryEatLines();
        Token<TokenType> cls = eat(CLASS);
        ClassDef def = defClass(cls);
        return Ast.classDef(range(sealed.loc, def.loc), def.name, def.parent, def.props, def.methods, def.ctor, true, def.tagged);
    }
    ClassDef defClass(Token<TokenType> cls) {
        return defClass(cls, null);
    }
    ClassDef defClass(Token<TokenType> cls, @Nullable Id parentId) {
        tryEatLines();
        // <1> class-name
        boolean anonymous = peekAny(LEFT_BRACE, EXTENDS) != null;
        if (anonymous) {
            return defClass(anon(cls.loc), parentId, cls);
        } else {
            Token<TokenType> id = eatTriviaName();
            tryEatLines();
            return defClass(id(id.loc, id.lexeme), parentId, cls);
        }
    }
    ClassDef defClass(Id classId, @Nullable Id parentId, Token<TokenType> tok) {
        enterClass(classId);
        ClassDef clsDef = defClass1(tok, classId, parentId, true);
        exitClass();
        return clsDef;
    }

    Expr defEnumClass(Token<TokenType> tok) {
        tryEatLines();
        Token<TokenType> name = eatTriviaName();
        tryEatLines();
        Id classId = id(name.loc, name.lexeme);
        enterClass(classId);
        ClassDef enumClass = defClass1(tok, classId, null, false);
        if (enumClass.methods.size() != 1 && !enumClass.methods.get(0).name.equals(enumClass.ctor.name)) {
            throw Error.syntax(tok.loc, "enumClass 不能声明方法");
        }
        if (enumClass.parent != null) {
            throw Error.syntax(tok.loc, "enumClass 不能显式继承");
        }
        tryEat(NEWLINE); // 这里多个换行则是 block
        Token<TokenType> lb = tryEat(LEFT_BRACE);
        Block derivations = null;
        if (lb != null) {
            List<Expr> defs = new ArrayList<>();
            tryEatLines();
            while (peek(NAME) != null) {
                Token<TokenType> next = peek();
                Token<TokenType> fakeCls = new Token<>(next.lexer, next.prev, CLASS, CLASS.name, next.loc, true);
                ClassDef classDef = defClass(fakeCls, classId);
                defs.add(classDef);
                tryEatLines();
            }
            Token<TokenType> rb = eat(RIGHT_BRACE);
            derivations = Ast.block(range(lb, rb), defs, false);
        }
        exitClass();
        return desugarEnumClass(enumClass, derivations);
    }
    Expr desugarEnumClass(ClassDef ec, @Nullable Block derivations) {
        List<Expr> exprs = new ArrayList<>();
        Location loc = ec.loc;
        // 子类需要继承，所以不能 sealed

        ClassDef enumClass = classDef(ec.loc, ec.name, id(None, Class_Enum), ec.props, ec.methods, ec.ctor, false, false);
        exprs.add(enumClass);
        List<Expr> enumValues = new ArrayList<>();
        if (derivations != null && !derivations.stmts.isEmpty()) {
            for (Expr stmt : derivations.stmts) {
                ClassDef cls = (ClassDef) stmt;
                assert cls.parent != null;
                if (!cls.parent.name.equals(ec.name.name)) {
                    throw Error.syntax(cls.loc, "enum class 中的类继承错误");
                }
                ClassDef derivation = classDef(cls.loc, cls.name, ec.name, cls.props, cls.methods, cls.ctor, true, true);
                exprs.add(derivation);
                enumValues.add(cls.name);
            }
            loc = range(ec.loc, derivations.loc);
        }
        // class 枚举添加 values 方法
        Block valuesBlock = Ast.block(None, lists(Ast.newStmt(None, None, id(None, Class_List), enumValues)));
        // xxEnumClass.value = fun values() = [xx,yy,zz]
        Id funValues = idSuffix(None, Fun_Enum_Values);
        exprs.add(funDef(None, null, funValues, new LinkedHashMap<>(0), valuesBlock, false, false, false));
        exprs.add(Ast.assign(None, None, OBJECT_MEMBER, objectMember(None, None, ec.name, id(None, Fun_Enum_Values)), funValues));
        return Ast.block(loc, exprs, false);
    }


    // class User
    // class Point(x, y)
    // class Point(val x, var y)
    // 当使用类签名构造函数时, 函数 body 为构造函数 body，不能重复声明 ctor
    // class Point(val x, var y) { ctor-body }
    // 当使用常规类声明时，body 为属性与 method 列表
    // class User { def User() {} prop methods }
    // tryReadBody callSuperIfImplicitExtends 是为了 enumClass 复用该方法加的，一坨浆糊
    ClassDef defClass1(Token<TokenType> cls, Id clsName, @Nullable Id parentClsName, boolean tryReadBody) {
        // todo parentClsName 只有 enum 会使用这个参数，显式传入 parent， 重构一下

        Location none = Location.None;
        List<VarDef> props = new ArrayList<>();
        List<FunDef> methods = new ArrayList<>();

        // <2> 可选 类签名构造函数 class-ctor
        FunDef sigCtor = null;
        Token<TokenType> lp = tryEat(LEFT_PAREN);
        if (lp != null) {
            if (cls.is(OBJECT)) {
                throw Error.syntax(lp.loc, "object 声明不支持类签名构造函数");
            }
            sigCtor = clsSigCtor(cls, lp, clsName, props);
        }


        // <3> 可选 class-extends
        // Id parentClsName = null;
        Call callSigSuperCtor = null;
        int j = 0;
        while (peek(j).is(NEWLINE)) j++;
        if (peek(j).is(EXTENDS)) {
            if (parentClsName != null) {
                throw Error.syntax(peek(j).loc, "已经隐式继承 " + parentClsName + ", 不能显式继承");
            }
            tryEatLines();
            eat(EXTENDS);
            tryEatLines();
            Token<TokenType> pid = eatTriviaName();
            parentClsName = id(pid.loc, pid.lexeme);
            boolean hasSuperCtorCall = peek(LEFT_PAREN) != null || peek(NEWLINE, LEFT_PAREN);
            if (hasSuperCtorCall) {
                tryEat(NEWLINE);
                // if (sigCtor == null) throw Error.syntax(lp.loc, "子类需要类签名构造函数");
                List<Expr> callSuperCtorArgs = args(eat(LEFT_PAREN));
                callSigSuperCtor = call(none, none, id(none, SUPER.name), callSuperCtorArgs); // super(...)
            }
        }

        // <4> 可选 class-body
        // ✔️ class User {}
        // ✔️ class User \n {}
        // ❌ class User \n\n { } 类 + Block
        boolean hasClassBodyBlock = tryReadBody && (peek(LEFT_BRACE) != null || peek(NEWLINE, LEFT_BRACE));
        if (hasClassBodyBlock) {
            tryEat(NEWLINE); // class xxx 这里之多一个换行 {}, 否则 {} 是 block
            Token<TokenType> lb = eat(LEFT_BRACE);
            if (sigCtor == null) {
                clsBodyBlock(lb, props, methods);
            } else {
                clsSigCtorBlock(lb, sigCtor, props, methods);
            }
        }

        // <5> fix-ctor
        // 具体方法重复定义延时到执行时候检查，这里简单检查构造函数
        propDupCheck(props, methods);
        FunDef ctor = findCtor(methods, clsName);
        if (sigCtor != null) {
            if (ctor == null) {
                ctor = sigCtor;
                methods.add(ctor);
            } else {
                throw Error.syntax(ctor.loc, "构造函数重复");
            }
        }

        // 补全 ctor 与 callSuperIfImplicitExtends: super()
        if (ctor == null) {
            // 填充默认构造函数
            Block body;
            if (callSigSuperCtor == null) {
                if (parentClsName != null) {
                    // todo lists() 是错的,参数个数不对...
                    body = Ast.block(none, lists(call(none, none, id(none, SUPER.name), lists())));
                } else {
                    body = emptyBlock(none);
                }
            } else {
                body = Ast.block(none, lists(callSigSuperCtor));
            }
            // Id ctorName = clsName;
            Id ctorName = id(None, CONSTRUCT.name);
            ctor = funDef(none, classId, ctorName, new LinkedHashMap<>(0), body, false, false, false);
            methods.add(ctor);
        } else {
            Call superCall = getSuperCall(ctor);
            if (callSigSuperCtor == null) {
                if (parentClsName != null) {
                    if (superCall == null) {
                        superCall = call(none, none, id(none, SUPER.name), lists());
                        if (ctor.body.stmts.isEmpty()) {
                            ctor.body.stmts.add(superCall);
                        } else {
                            // todo 可能 indexOutOfBound
                            ctor.body.stmts.add(0, superCall);
                        }
                    }
                }
            } else {
                if (superCall == null) {
                    // todo 可能 indexOutOfBoud
                    ctor.body.stmts.add(0, callSigSuperCtor);
                } else {
                    throw Error.syntax(superCall.loc, "super() 与 extends Class(...) 签名构造隐式super() 冲突");
                }
            }
        }

        // 如果缺失在自身构造函数内插入对父类构造函数的调用
        if (parentClsName != null) {
            Call superCall = getSuperCall(ctor); // super()
            assert superCall != null;
            Location loc = superCall.loc;
            // Id ctorName = parentClsName;
            Id ctorName = id(loc, CONSTRUCT.name);
            ObjectMember callee = objectMember(loc, loc, id(loc, SUPER.name), ctorName);
//            if (superCall == null) {
//                // 填充默认 superCall
//                superCall = Ast.call(loc, loc, callee, lists());
//                if (ctor.body.stmts.isEmpty()) {
//                    ctor.body.stmts.add(superCall);
//                } else {
//                    ctor.body.stmts.add(0, superCall);
//                }
//            } else {
                // 改写 superCall: super(...) super.X(...)
                Call superCall1 = call(superCall.loc, superCall.oploc, callee, superCall.args);
                ctor.body.stmts.set(0, superCall1);
//            }
        }


        // 属性定义处理, 写的好蛋疼, 得有空改下... 现在不想改了...
        List<VarDef> props1 = new ArrayList<>(props.size());
        Iterator<VarDef> iter = props.iterator();
        while (iter.hasNext()) {
            VarDef prop = iter.next();
            if (Ast.isNull(prop.init) || Ast.isUndefined(prop.init)) {
                props1.add(prop);
                iter.remove();
            }
        }
        List<Assign> propInits = new ArrayList<>(props.size());
        for (VarDef prop : props) {
            Assign propInit = desugarPropDef(prop, props1);
            propInits.add(propInit);
        }
        // 必须添加构造函数 superCall 和原来代码中间, 因为代码可能用到属性
        if (ctor.body.stmts instanceof ArrayList) {
            ((ArrayList<Expr>) ctor.body.stmts).ensureCapacity(ctor.body.stmts.size() + props.size());
        }
        for (int i = propInits.size() - 1; i >= 0; i--) {
            ctor.body.stmts.add(getSuperCall(ctor) == null ? 0 : 1, propInits.get(i));
        }
        // if (parentClsName == null) parentClsName = Ast.id(None, Class_Object);
        return classDef(range(cls, peek(-1)), clsName, parentClsName, props1, methods, ctor);
    }


    void propDupCheck(List<VarDef> props, List<FunDef> methods) {
        HashMap<String, Expr> map = new HashMap<>();
        for (VarDef prop : props) {
            Expr prev = map.put(prop.id.name, prop);
            if (prev != null) {
                throw Error.syntax(prop.loc, String.format("%s的%s 与 %s的%s 重复定义", prev.loc, prev, prop.loc, prop));
            }
        }
        for (FunDef m : methods) {
            Expr prev = map.put(m.stringName(), m);
            if (prev != null) {
                throw Error.syntax(m.loc, String.format("%s的%s 与 %s的%s 重复定义", prev.loc, prev, m.loc, m));
            }
        }
    }
    @Nullable FunDef findCtor(List<FunDef> methods, Id clsName) {
        // String ctorName = clsName.name;
        String ctorName = CONSTRUCT.name;
        for (FunDef m : methods) {
            if (m.stringName().equals(ctorName)) {
                return m;
            }
        }
        return null;
    }
    FunDef clsSigCtor(Token<TokenType> cls, Token<TokenType> lp, Id clsName, List<VarDef> props) {
        tryEatLines();
        Token<TokenType> rp = tryEat(RIGHT_PAREN);
        if (rp == null) {
            List<VarDef> params = sigCtorParam1();
            tryEatLines();
            rp = eat(RIGHT_PAREN);
            return clsSigCtorVarDef(range(lp, rp), cls, clsName, props, params);
        } else {
            return clsSigCtorVarDef(range(lp, rp), cls, clsName, props, lists());
        }
    }
    List<VarDef> sigCtorParam1() {
        List<VarDef> vars = new ArrayList<>();
        do {
            tryEatLines();
            Token<TokenType> defTok = tryEatAny(VAR, VAL);
            if (defTok != null) {
                tryEatLines();
            }
            boolean mut = defTok != null && TokenType.mutable(defTok.type);
            Token<TokenType> name = eatTriviaName();
            Id id = id(name.loc, name.lexeme);
            Location loc = range(defTok == null ? name : defTok, name);
            Literal defVal = optionalDefaultValue(loc);
            boolean hasInit = defVal != null;
            if (!hasInit) {
                defVal = mut ? litNull(id.loc) : litUndefined(id.loc);
            }
            VarDef def = varDef(loc, id, defVal, mut, hasInit);
            vars.add(def);
        } while (tryEat(COMMA) != null);
        return vars;
    }
    FunDef clsSigCtorVarDef(Location loc, Token<TokenType> cls, Id clsName, List<VarDef> props, List<VarDef> vars) {
        Location none = Location.None;
        List<Expr> stmts = new ArrayList<>(1);

        LinkedHashMap<Id, Literal> params = new LinkedHashMap<>();
        for (VarDef param : vars) {
            Id name = param.id;
            // 属性默认为 val
            Literal init = param.mut ? litNull(none) : litUndefined(none);
            VarDef def = varDef(param.loc, name, init, param.mut, param.hasInit);
            props.add(def);
            stmts.add(
                    // this.{param} = param
                    Ast.assign(none, none,
                            OBJECT_MEMBER, objectMember(none, none, id(none, THIS.name), name), name
                    )
            );
            Literal defVal = (Literal) param.init;
            if (param.hasInit) {
                params.put(name, isUndefined(defVal) ? litNull(none) : defVal);
            } else {
                params.put(name, null);
            }
        }
        checkParametersDefaults(loc, params);

        Block body = Ast.block(none, stmts);
        // Id ctorName = clsName;
        Id ctorName = id(None, CONSTRUCT.name);
        return funDef(range(cls, peek(-1)), classId, ctorName, params, body, false, false, false);
    }


    void clsBodyBlock(Token<TokenType> lbrace, List<VarDef> props, List<FunDef> methods) {
        tryEatLines();
        while (tryEat(RIGHT_BRACE) == null) {
            Token<TokenType> defTok = eatAny(definable());
            Expr def = def(defTok);
            clsBodyBlock1(def, props, methods);
            tryEatLines();
        }
    }
    void clsBodyBlock1(Expr def, List<VarDef> props, List<FunDef> methods) {
        if (def instanceof VarDef) {
            props.add(((VarDef) def));
            // todo !!! ctor 加东西
        } else if (isNotScopeBlock(def)) {
            for (Expr stmt : ((Block) def).stmts) {
                clsBodyBlock1(stmt, props, methods);
            }
        } else if (def instanceof FunDef) {
            FunDef method = (FunDef) def;
            methods.add(method);
        } else {
            throw Error.bug(peek().loc);
        }
    }
    boolean isNotScopeBlock(Expr expr) {
        return expr instanceof Block && !((Block) expr).scope;
    }

    void clsSigCtorBlock(Token<TokenType> lbrace, FunDef ctor, List<VarDef> props, List<FunDef> methods) {
        Block clsBlock = block(lbrace);
        for (Expr stmt : clsBlock.stmts) {
            if (stmt instanceof Def || isNotScopeBlock(stmt)) {
                clsSigCtorBlock1(stmt, ctor, props, methods);
            } else {
                ctor.body.stmts.add(stmt);
            }
        }
    }
    // var x = 1  rewrite  this.x = 1
    void clsSigCtorBlock1(Expr stmt, FunDef ctor, List<VarDef> props, List<FunDef> methods) {
        // var x = 1  rewrite  this.x = 1
        if (stmt instanceof VarDef) {
            Assign propInit = desugarPropDef((VarDef) stmt, props);
            ctor.body.stmts.add(propInit);
        } else if (isNotScopeBlock(stmt)) {
            for (Expr it : ((Block) stmt).stmts) {
                clsSigCtorBlock1(it, ctor, props, methods);
            }
        } else if (stmt instanceof FunDef) {
            FunDef funDef = (FunDef) stmt;
            methods.add(funDef);
        } else if (stmt instanceof ClassDef) {
            desugarCtorBlockClsDef((ClassDef) stmt, ctor, props);
        } else {
            throw Error.bug(peek().loc);
        }
    }
    // var x = 1  rewrite  this.x = 1
    Assign desugarPropDef(VarDef varDef, List<VarDef> props) {
        Location none = Location.None;
        Id id = varDef.id;
        Expr val = varDef.init;

        Literal init = varDef.mut ? litNull(none) : litUndefined(none);
        VarDef varDefWithoutInit = varDef(varDef.loc, id, init, varDef.mut, true); // 算有 init 还是木有呢?
        ObjectMember thisName = objectMember(id.loc, id.loc, id(none, THIS.name), id);
        Assign varDefAssignInit = Ast.assign(varDef.loc, varDef.oploc, OBJECT_MEMBER, thisName, val);
        props.add(varDefWithoutInit);
        return varDefAssignInit;
    }

    /**
     * 解构赋值解语法糖
     */
    static class DefineAssignDesugar implements Matcher<Expr, Void> {
        static List<Expr> desugarAssign(Location loc, Location oploc, Pattern ptn, Expr val) {
            DefineAssignDesugar d = new DefineAssignDesugar(true, loc, oploc, false);
            d.match(ptn, val);
            return d.exprs;
        }
        static List<VarDef> desugarDefine(Location loc, Location oploc,
                                  boolean mut, Pattern ptn, Expr val) {
            DefineAssignDesugar d = new DefineAssignDesugar(false, loc, oploc, mut);
            d.match(ptn, val);
            return d.exprs.stream().map(it -> ((VarDef) it)).collect(toList());
        }

        final boolean isAssign;
        final Location loc;
        final Location oploc;
        final boolean mut;
        final List<Expr> exprs = new ArrayList<>();
        DefineAssignDesugar(boolean isAssign, Location loc, Location oploc, boolean mut) {
            this.isAssign = isAssign;
            this.loc = loc;
            this.oploc = oploc;
            this.mut = mut;
        }
        @Override public Void match(@NotNull Id ptn, @NotNull Expr val) {
            if (Matcher.isWildcards(ptn)) {
                return null;
            }
            matchId(ptn, tmpVar(val));
            return null;
        }
        void matchId(Id ptn, Expr val) {
            if (isAssign) {
                Assign assign = Ast.assign(loc, oploc, PATTERN, ptn, val);
                exprs.add(assign);
            } else {
                VarDef varDef = varDef(loc, ptn, val, mut, true);
                exprs.add(varDef);
            }
        }
        @Override public Void match(@NotNull ListPattern ptn, @NotNull Expr iterVal) {
            IterFragment iter = Ast.iterator(loc, tmpVar(iterVal));
            Expr nextOrNull = Ast.ternary(loc, loc, COND, iter.callHasNext, iter.callNext, Ast.litNull(loc));
            exprs.add(iter.iterDef);

            for (Pattern ptn1 : ptn.elems) {
                // 注意：这里必须先把 next 调用赋值到临时变量，否则 next 调用会被当做表达式传递，不是 call-by-value 了
                Id nextVar = tmpVar(anon(loc).name + "_" + "next", nextOrNull);
                match(ptn1, nextVar);
            }
            return null;
        }
        @Override public Void match(@NotNull MapPattern ptn, @NotNull Expr val) {
            Id valId = tmpVar(val);
            for (Map.Entry<Expr, Pattern> it : ptn.props.entrySet()) {
                Expr key = it.getKey();
                Pattern ptn1 = it.getValue();
                // 这里生成一个临时变量, [] 如果被重载了会有副作用
                match(ptn1, tmpVar(Ast.subscript(loc, loc, valId, key)));
            }
            return null;
        }
        @Override public Void match(@NotNull Literal ptn, @NotNull Expr val) {
            throw Error.syntax(loc, "不支持的 pattern " + ptn);
        }

        Id tmpVar(Expr val) {
            return tmpVar(null, val);
        }
        Id tmpVar(@Nullable String varName, Expr val) {
            if (val instanceof Id) {
                return ((Id) val);
            } else {
                Id id = varName == null ? Ast.anon(loc) : Ast.id(loc, varName);
                exprs.add(varDef(loc, id, val, false, true));
                return id;
            }
        }
    }
    /*List<Expr> desugarPatternDefOrAssign(boolean isAssign, Location loc, Location oploc,
                                         boolean mut, Pattern ptn, Expr val) {
        List<Expr> exprs = new ArrayList<>();
        if (ptn instanceof Id) {
            Id name = (Id) ptn;
            if (isAssign) {
                Assign assign = Ast.assign(loc, oploc, PATTERN, ASSIGN, name, val);
                exprs.add(assign);
            } else {
                VarDef varDef = Ast.varDef(loc, name, val, mut);
                exprs.add(varDef);
            }
        } else if (ptn instanceof ListPattern) {
            // if (Ast.isNull(varDef.init)) throw Error.syntax(loc, "解构赋值 value 不能为 null");
            List<Pattern> elems = ((ListPattern) ptn).elems;
            for (int i = 0; i < elems.size(); i++) {
                Pattern ptn1 = elems.get(i);
                if (ptn1 == null) continue;
                Expr arrSubs = Ast.subscript(loc, loc, val, Ast.literal(loc, Value.Int(i)));
                List<Expr> exprs1 = desugarPatternDefOrAssign(isAssign, loc, oploc, mut, ptn1, arrSubs);
                exprs.addAll(exprs1);
            }
        } else if (ptn instanceof MapPattern) {
            // if (Ast.isNull(varDef.init)) throw Error.syntax(loc, "解构赋值 value 不能为 null");
            for (Map.Entry<Expr, Pattern> it : ((MapPattern) ptn).props.entrySet()) {
                Expr key = it.getKey();
                Pattern ptn1 = it.getValue();
                Subscript objMember = Ast.subscript(loc, loc, val, key);
                List<Expr> exprs1 = desugarPatternDefOrAssign(isAssign, loc, oploc, mut, ptn1, objMember);
                exprs.addAll(exprs1);
            }
        } else {
            throw Error.syntax(loc, "不支持的 pattern " + ptn);
        }
        return exprs;
    }*/

    // class A {}  rewrite var A = class {}
    void desugarCtorBlockClsDef(ClassDef clsDef, FunDef ctor, List<VarDef> props) {
        Id this_ = id(Location.None, THIS.name);
        Id name = clsDef.name;
        Location loc = clsDef.loc;
        VarDef varDefWithoutInit = varDef(loc, name, litNull(Location.None), true, true);
        Assign varDefAssignInit = Ast.assign(loc, clsDef.oploc,
                OBJECT_MEMBER, objectMember(name.loc, name.loc, this_, name),
                classDef(loc, anon(loc), clsDef.parent, clsDef.props, clsDef.methods, clsDef.ctor)
        );
        props.add(varDefWithoutInit);
        ctor.body.stmts.add(varDefAssignInit);
    }

    @Nullable Call getSuperCall(FunDef ctor) {
        List<Expr> stmts = ctor.body.stmts;
        if (stmts.size() < 1) return null;
        Expr expr = stmts.get(0); // 第一行必须 call parent ctor
        if (!(expr instanceof Call)) return null;
        Call call = (Call) expr;

        if (call.callee instanceof Id) {
            if (TokenType.SUPER.name.equals(((Id) call.callee).name)) {
                return call;
            }
        } else if (call.callee instanceof ObjectMember) {
            ObjectMember om = (ObjectMember) call.callee;
            if (om.object instanceof Id && ((Id) om.object).name.equals(TokenType.SUPER.name)
                    && om.prop.name.equals(TokenType.CONSTRUCT.name)) {
                return call;
            }
        }
        return null;
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    Pattern pattern(@NotNull Token<TokenType> leftBracket) {
        return any(
                () -> listPattern(leftBracket),
                () -> mapPattern(leftBracket)
        );
    }
    Pattern pattern() {
        Token<TokenType> lb = tryEat(LEFT_BRACKET);
        if (lb == null) {
            Expr expr;
            // case Class(a,b) 与 方法调用expr 预发冲突
            // case fun_call(...)
            // case Class~(...params)
            if (isUnApplyPattern()) {
                expr = unApplyPattern();
            } else {
                expr = expr();
            }

            if (expr instanceof Pattern) {
                return ((Pattern) expr);
            } else {
                return exprPattern(expr.loc, expr);
            }
        } else {
            return pattern(lb);
        }
    }
    ListPattern listPattern(@NotNull Token<TokenType> leftBracket) {
        List<Pattern> elems = new ArrayList<>();
        // E?(,(E?))*
        while(!peek().is(RIGHT_BRACKET)) {
            tryEatLines();
            Token<TokenType> comma = tryEat(COMMA);
            if (comma == null) {
                elems.add(pattern());
                tryEatLines();
                if (peek().is(RIGHT_BRACKET)) {
                    break;
                } else {
                    eat(COMMA);
                }
            } else {
                elems.add(wildcards(peek().loc));
            }
        }

        tryEatLines();
        return Ast.listPattern(range(leftBracket, eat(RIGHT_BRACKET)), elems);
    }
    @SuppressWarnings("Duplicates")
    MapPattern mapPattern(@NotNull Token<TokenType> leftBrace) {
        Map<Expr, Pattern> props = new LinkedHashMap<>();
        tryEatLines();
        Token<TokenType> colon = tryEat(COLON);
        if (colon != null) {
            tryEatLines();
            return Ast.mapPattern(range(leftBrace, eat(RIGHT_BRACKET)), props);
        }

        do {
            tryEatLines();
            if (peek(RIGHT_BRACKET) != null) {
                break;
            }
            Expr key = mapKey();
            tryEatLines();
            eat(COLON);
            tryEatLines();
            Pattern val = pattern();
            props.put(key, val);
            tryEatLines();
        } while (tryEat(COMMA) != null);
        tryEatLines();
        return Ast.mapPattern(range(leftBrace, eat(RIGHT_BRACKET)), props);
    }
    // case Class~(...params)
    boolean isUnApplyPattern() {
        int i = 0;
        if (peek(i++).is(NAME)) {
            while (peek(i).is(NEWLINE)) i++;
            return peek(i).is(BIT_NOT);
        } else {
            return false;
        }
    }
    UnApplyPattern unApplyPattern() {
        Token<TokenType> id = eatTriviaName();
        Id clsName = id(id.loc, id.lexeme);
        tryEatLines();
        eat(BIT_NOT);
        tryEatLines();
        Token<TokenType> lp = eat(LEFT_PAREN);

        tryEatLines();
        if (peek().is(RIGHT_PAREN)) {
            return Ast.unApplyPattern(range(id.loc, eat(RIGHT_PAREN).loc), lp.loc, clsName, lists());
        }

        List<Pattern> props = new ArrayList<>();
        do {
            tryEatLines();
            props.add(pattern());
        } while (tryEat(COMMA) != null);
        tryEatLines();
        return Ast.unApplyPattern(range(id.loc, eat(RIGHT_PAREN).loc), lp.loc, clsName, props);
    }

    // x match {}
    Block matchStmt(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        tryEatLines();
        return desugarMatchBlock(tok, lhs);
    }
    // match(x) {}
    Block matchStmt(@NotNull Token<TokenType> match) {
        eat(LEFT_PAREN);
        tryEatLines();
        Expr val = expr();
        tryEatLines();
        eat(RIGHT_PAREN);
        return desugarMatchBlock(match, val);
    }
    Block desugarMatchBlock(@NotNull Token<TokenType> match, Expr val) {
        return desugarMatch(matchBlock(match, val));
    }
    Match matchBlock(@NotNull Token<TokenType> match, Expr val) {
        // 合法 match(){}  合法match()\n{}
        // 不合法 match()\n\n...{} 大于一行会变成 match()函数调用与 block
        tryEat(NEWLINE);
        eat(LEFT_BRACE);

        List<MatchCase> cases = new ArrayList<>();
        Token<TokenType> rb;
        tryEatLines();
        // 允许空 match, 运行时再报错
        while ((rb = tryEat(RIGHT_BRACE)) == null) /*do*/ {
            Token<TokenType> matchCase = eat(CASE);
            tryEatLines();
            Pattern pattern = pattern();
            tryEatLines();
            Token<TokenType> if1 = tryEat(IF);
            Expr guard = null;
            if (if1 != null) {
                guard = parentheseExpr();
            }
            // 这里暂时用 -> 吧
            // 否则现在的实现读 pattern 部分 e.g. case a => a 会与箭头函数冲突
            eat(ARROW_BLOCK);
            Block body = arrowBody();
            cases.add(matchCase(range(matchCase.loc, body.loc), pattern, guard, body));
            tryEatLines();
        } /*while ((rb = tryEat(RIGHT_BRACE)) == null)*/;
        return match(range(match.loc, rb.loc), val, cases);
    }
    Block desugarMatch(Match match) {
        Location loc = match.loc;
        Expr v = match.value;

        List<Expr> stmts = lists();

        Id vMatched = idIfNecessary(loc, "matched", v);
        if (vMatched != v) {
            stmts.add(varDef(loc, vMatched, v, false, true));
        }

        Id vId = idSuffix(loc, "destruct");

        // v.`destruct`
        // VarDef defDestruct = Ast.varDef(loc, vId, ternary, false);
        VarDef defDestruct = Ast.varDef(loc, vId, Ast.call(loc, loc, objectMember(loc, loc,
                call(loc, loc, id(loc, "require"), lists(litString(loc, "\"helper.j\""))),
                id(loc, "destruct")
        ), lists(vMatched)), false, true);
        stmts.add(defDestruct);

        // { throw new MatchError("未匹配到任何 case") }
        New newMatchError = Ast.newStmt(loc, loc, id(loc, "MatchError"), lists(litString(loc, "\"未匹配到任何 case\"")));
        Expr lastIf = Ast.block(loc, lists(Ast.throwStmt(loc, newMatchError)));
        for (int i = match.cases.size() - 1; i >= 0; i--) {
            MatchCase matchCase = match.cases.get(i);
            MatchDesugar matchDesugar = new MatchDesugar();
            matchDesugar.match(matchCase.pattern, vId);

            List<Expr> matchers = matchDesugar.matchers;
            List<Expr> binders = matchDesugar.binders;

            Expr test;
            if (matchers.isEmpty()) {
                test = Ast.litTrue(loc);
            } else {
                test = matchers.get(0);
                for (int j = 1; j < matchers.size(); j++) {
                    test = Ast.binary(loc, loc, LOGIC_AND, test, matchers.get(j));
                }
            }

            if (matchCase.guard != null) {
                List<Expr> guardBlock = new ArrayList<>(binders.size() + 1);
                guardBlock.addAll(binders);
                guardBlock.add(matchCase.guard);
                test = Ast.binary(loc, loc, LOGIC_AND, test, Ast.block(loc, guardBlock));
            }

            List<Expr> bodyStmts = new ArrayList<>(binders.size() + matchCase.body.stmts.size());
            bodyStmts.addAll(binders);
            bodyStmts.addAll(matchCase.body.stmts);
            Block body = Ast.block(matchCase.body.loc, bodyStmts);
            lastIf = Ast.ifStmt(matchCase.loc, test, body, lastIf);
        }
        stmts.add(lastIf);

        return Ast.block(loc, stmts);
    }

    /**
     * 模式匹配解语法糖
     */
    static class MatchDesugar implements Matcher<Expr, Void> {
        List<Expr> matchers = new ArrayList<>();
        List<Expr> binders = new ArrayList<>();

        Id define(Location loc, String suffix, Expr expr) {
            Id id = idIfNecessary(loc, suffix, expr);
            if (id != expr) {
                // 赋值 == null
                matchers.add(Ast.binary(loc, loc, EQ, Ast.group(loc, varDef(loc, id, expr, false, true)), litNull(loc)));
            }
            return id;
        }
        @Override
        public Void match(@NotNull Id ptn, @NotNull Expr expr) {
            if (!Matcher.isWildcards(ptn)) {
                binders.add(Ast.varDef(ptn.loc, ptn, expr, true, true));
            }
            return null;
        }
        @Override
        public Void match(@NotNull Literal ptn, @NotNull Expr expr) {
            matchers.add(Ast.binary(ptn.loc, ptn.loc, EQ, ptn, expr));
            return null;
        }
        @Override
        public Void match(@NotNull ExprPattern ptn, @NotNull Expr expr) {
            matchers.add(Ast.binary(ptn.loc, ptn.loc, EQ, Ast.group(ptn.expr.loc, ptn.expr), expr));
            return null;
        }
        @Override
        public Void match(@NotNull ListPattern ptn, @NotNull Expr expr) {
            Location loc = ptn.loc;
            Id id = define(loc, "list", expr);
            IterFragment iter = Ast.iterator(loc, id);

            // expr != null
            matchers.add(Ast.binary(loc, loc, NE, id, litNull(loc)));
            // expr.`iterable` != null
            matchers.add(Ast.binary(loc, loc, NE, iter.iterable, litNull(loc)));
            // (val iter = expr.`iterable`()) == null
            matchers.add(Ast.binary(loc, loc, EQ, Ast.group(loc, iter.iterDef), litNull(loc)));
            // iter != null
            matchers.add(Ast.binary(loc, loc, NE, iter.iterVar, litNull(loc)));

            for (Pattern ptn1 : ptn.elems) {
                // iter.hasNext() == true
                matchers.add(Ast.binary(loc, loc, EQ, iter.callHasNext, litTrue(loc)));
                match(ptn1, define(loc, "list_ptn", iter.callNext));
            }

            // pattern 与 iterable 数量精确相等 !iter.hasNext()
            matchers.add(Ast.unary(loc, loc, LOGIC_NOT, iter.callHasNext, true));
            return null;
        }
        @Override
        public Void match(@NotNull MapPattern ptn, @NotNull Expr expr) {
            Location loc = ptn.loc;
            Id id = define(loc, "map", expr);
            matchers.add(Ast.binary(loc, loc, NE, id, litNull(loc)));
            // todo: 这里匹配 [key: null] 这种结构有歧义, 存在 key obj.key == null || 不存在 key
            // (key is Map || !(key is Fun || key.class.isPrimitive))
            matchers.add(Ast.group(loc, binary(loc, loc, LOGIC_OR,
                    binary(loc, loc, IS, id, litClass(loc, Class_Map)),
                    Ast.unary(loc, loc, LOGIC_NOT, Ast.group(loc, Ast.binary(loc, loc, LOGIC_OR,
                            binary(loc, loc, IS, id, litClass(loc, Class_Fun)),
                            objectMember(loc, loc, objectMember(loc, loc, id, id(loc, Key_Class)), id(loc, "isPrimitive"))
                    )), true))));

            for (Map.Entry<Expr, Pattern> it : ptn.props.entrySet()) {
                Expr key = it.getKey();
                match(it.getValue(), Ast.subscript(key.loc, key.loc, id, key));
            }
            return null;
        }
        @Override
        public Void match(@NotNull UnApplyPattern ptn, @NotNull Expr expr) {
            Location loc = ptn.loc;
            Id id = define(loc, "unapply", expr);
            // ptn is Class
            Id ptnClassName = ptn.name;
            // 这个应该不应该检查, SomeClass(a,b), 如果 SomeClass 不是 class 直接报错好了
            // matchers.add(binary(loc, loc, IS, ptnClassName, litClass(loc, ClassClass)));
            // todo userDefClass
            matchers.add(binary(loc, loc, IS, id, ptnClassName));

            // ptnClassName.class.construct.parameters
            ObjectMember ctorParams =
                    objectMember(loc, loc,
                        objectMember(loc, loc,
                                ptnClassName, Ast.id(loc, "construct")),
                            Ast.id(loc, "parameters"));
            // val params = ptnClassName.class.reflect.construct.parameters
            Id params = define(loc, "unapply_params", ctorParams);
            // params.size()
            Call paramsSize = call(loc, loc, objectMember(loc, loc, params, id(loc, "size")), lists());
            List<Pattern> props = ptn.props;
            // params.size() == ${ptn.prop.size()}
            matchers.add(Ast.binary(loc, loc, EQ, paramsSize, Ast.litInt(loc, String.valueOf(props.size()))));
            // 这里不检查参数个数了!!!
            // prop.size() == args.size()
            // prop.size() > args.size()
            // prop.size() < args.size()
            // todo prop 多了或者少了...
            for (int i = 0; i < props.size(); i++) {
                // id[params[$i]]
                Subscript nthParams = Ast.subscript(loc, loc, params, Ast.litInt(loc, String.valueOf(i)));
                match(props.get(i), Ast.subscript(loc, loc, id, nthParams));
            }
            return null;
        }
    }
    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    // list Map ListPattern
    Expr leftBracket(@NotNull Token<TokenType> leftBracket) {
        return any(
                () -> list(leftBracket),
                () -> map(leftBracket),
                () -> listPattern(leftBracket)
        );
    }

    Expr list(@NotNull Token<TokenType> leftBracket) {
        List<Expr> elems = new ArrayList<>();
        do {
            tryEatLines();
            if (peek(RIGHT_BRACKET) != null) {
                break;
            }
            elems.add(expr());
        } while (tryEat(COMMA) != null);
        tryEatLines();

        Token<TokenType> rb = eat(RIGHT_BRACKET);

        // 看一下是否是assign，因为 assign 代表后面的是 pattern
        Token<TokenType> marked = mark();
        try {
            tryEatLines();
            Location loc = range(leftBracket, rb);
            if (tryEatAny(assignable()) == null) {
                return desugarListLit(loc, elems);
            } else {
                return Ast.listPattern(loc, listElemToPattern(elems));
            }
        } finally {
            reset(marked);
        }
    }
    List<Pattern> listElemToPattern(List<Expr> elems) {
        List<Pattern> elemPtns = new ArrayList<>(elems.size());
        for (Expr elem : elems) {
            elemPtns.add(expr2Pattern(elem));
        }
        return elemPtns;
    }

    Expr desugarListLit(Location listLoc, List<Expr> elems) {
        if (isLiteral(elems)) {
            return Ast.litList(listLoc, elems.stream().map(it -> ((Literal) it)).collect(toList()));
        } else {
            return Ast.newStmt(listLoc, listLoc, id(listLoc, Class_List), elems);
        }
//        Location loc = None;
//        List<Expr> exprs = new ArrayList<>(elems.size() + 2);
//        Id list = id(loc, Class_List);
//        Id add = id(loc, List_Method_Add);
//        Id var = id(loc);
//        New newList = Ast.newStmt(loc, loc, list, lists());
//        VarDef def = varDef(loc, var, newList, false, true);
//        exprs.add(def);
//        for (Expr elem : elems) {
//            ObjectMember listAdd = objectMember(loc, loc, var, add);
//            Call callListAdd = call(loc, loc, listAdd, lists(elem));
//            exprs.add(callListAdd);
//        }
//        exprs.add(var);
//        return Ast.block(listLoc, exprs);
    }

    /*
    @SuppressWarnings("Duplicates")
    // braceMap
    MapLit map(@NotNull Token<TokenType> leftBrace) {
        Map<Expr, Expr> props = new LinkedHashMap<>();
        do {
            tryEatLines();
            if (peek(RIGHT_BRACE) != null) {
                break;
            }
            Expr key = expr(); // BP_PREFIX_UNARY
            eat(COLON);
            tryEatLines();
            Expr val = expr();
            props.put(key, val);
            tryEatLines();
        } while (tryEat(COMMA) != null);
        tryEatLines();
        return Ast.map(range(leftBrace, eat(RIGHT_BRACE)), props);
    }
    */

    @SuppressWarnings("Duplicates")
    // bracketMap
    Expr map(@NotNull Token<TokenType> lb) {
        Map<Expr, Expr> props = new LinkedHashMap<>();
        tryEatLines();
        Token<TokenType> colon = tryEat(COLON);

        if (colon == null) {
            do {
                tryEatLines();
                if (peek(RIGHT_BRACKET) != null) {
                    break;
                }
                Expr key = mapKey();
                tryEatLines();
                eat(COLON);
                tryEatLines();
                Expr val = expr();
                props.put(key, val);
                tryEatLines();
            } while (tryEat(COMMA) != null);
        }
        Token<TokenType> rb = eat(RIGHT_BRACKET);

        // 看一下是否是assign，因为 assign 代表后面的是 pattern
        Token<TokenType> marked = mark();
        try {
            tryEatLines();
            Location loc = range(lb, rb);
            if (tryEatAny(assignable()) == null) {
                return desugarMapLit(loc, props);
            } else {
                return Ast.mapPattern(loc, mapPropsToPattern(props));
            }
        } finally {
            reset(marked);
        }
    }
    Expr desugarLiteralMap2Lit(Location loc, Map<Expr, Expr> props) {
        Map<Literal, Literal> map = new LinkedHashMap<>();
        for (Map.Entry<Expr, Expr> it : props.entrySet()) {
            map.put(((Literal) it.getKey()), ((Literal) it.getValue()));
        }
        return Ast.litMap(loc, map);
    }
    Expr desugarMapLit(Location loc, Map<Expr, Expr> props) {
        if (isLiteral(props)) {
            return desugarLiteralMap2Lit(loc, props);
        } else {
            Location none = None;
            List<Expr> exprs = new ArrayList<>(props.size() + 2);
            Id map = id(none, Class_Map);
            Id var = anon(none);
            New newMap = Ast.newStmt(none, none, map, lists());
            VarDef def = varDef(none, var, newMap, false, true);
            exprs.add(def);
            for (Map.Entry<Expr, Expr> it : props.entrySet()) {
                Subscript mapSubs = Ast.subscript(none, none, var, it.getKey());
                Assign assign = Ast.assign(none, none, SUBSCRIPT, mapSubs, it.getValue());
                exprs.add(assign);
            }
            exprs.add(var);
            return Ast.block(loc, exprs);
        }
    }
    Map<Expr, Pattern> mapPropsToPattern(Map<Expr, Expr> props) {
        Map<Expr, Pattern> ptnPros = new LinkedHashMap<>();
        for (Map.Entry<Expr, Expr> it : props.entrySet()) {
            ptnPros.put(it.getKey(), expr2Pattern(it.getValue()));
        }
        return ptnPros;
    }

    // map key 运行 一些特殊 tok
    boolean isKeywordMapKey() {
        int i = 0;
        if (peek(i++).type.type == TokenCategory.KEYWORD) {
            while (peek(i).is(NEWLINE)) i++;
            return peek(i).is(COLON);
        }
        return false;
    }
    Expr mapKey() {
        // 新的除了 name 其他都是表达式的逻辑
        Expr expr;
        if (isKeywordMapKey()) {
            Token<TokenType> tok = eat();
            expr = id(tok.loc, tok.lexeme);
        } else {
            expr = expr();
        }
        // ❗️这里是个照顾到使用方便的处理, map key 大部分场景都是字符串, 所以默认不用引号，
        // 因为这里的name 实际包括 关键字(mapkey 可以关键字) 所以 isKeywordMapKey 特殊处理
        // 如果需要使用 id，可以转成表达式, 比如加 group (id)
        if (expr instanceof Id) {
            expr = Ast.litString(expr.loc, "\"" + ((Id) expr).name + "\"");
        }
        return expr;
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    Expr newStmt(@NotNull Token<TokenType> new1) {
        tryEatLines();
        Token<TokenType> name = eatTriviaName();
        // new() 必须一行，否则会冲突 // new A() |  new A \n () => {}
        Token<TokenType> lp = tryEat(LEFT_PAREN);
        List<Expr> args = new ArrayList<>();
        if (lp != null) {
            args = args(lp);
        }
        return Ast.newStmt(range(new1.loc, peek(-1).loc), new1.loc, id(name.loc, name.lexeme), args);
    }
    Break breakStmt(@NotNull Token<TokenType> brk) {
        if (inLoop > 0) {
            return Ast.breakStmt(brk.loc);
        } else {
            throw Error.syntax(brk.loc, "只能 break 循环");
        }
    }
    Continue continueStmt(@NotNull Token<TokenType> cont) {
        if (inLoop > 0) {
            return Ast.continueStmt(cont.loc);
        } else {
            throw Error.syntax(cont.loc, "只能 continue 循环");
        }
    }
    Return returnStmt(@NotNull Token<TokenType> ret) {
        if (inFun > 0) {
            if (peek().is(NEWLINE)) {
                return Ast.returnStmt(ret.loc, litNull(ret.loc));
            } else {
                Expr expr = expr();
                return Ast.returnStmt(range(ret.loc, expr.loc), expr);
            }
        } else {
            throw Error.syntax(ret.loc, "只能 return 函数或方法");
        }
    }

    Expr parentheseExpr() {
        tryEatLines();
        eat(LEFT_PAREN);
        Expr expr = expr();
        tryEatLines();
        eat(RIGHT_PAREN);
        return expr;
    }
    If ifStmt(@NotNull Token<TokenType> if1) {
        Expr test = parentheseExpr();

        Token<TokenType> lb = tryEat(LEFT_BRACE);
        Block then;
        if (lb == null) {
            // throw Error.syntax(if1.loc, "if 的 {} 不能省略");
            then = exprBlock();
        } else {
            then = block(lb);
        }

        tryEatLines();
        if (tryEat(ELSE)  == null) {
            return Ast.ifStmt(range(if1.loc, then.loc), test, then, null);
        } else {
            tryEatLines();
            Token<TokenType> ifn = tryEat(IF);
            Expr orElse;
            if (ifn == null) {
                if ((lb = tryEat(LEFT_BRACE)) == null) {
                    orElse = exprBlock();
                } else {
                    orElse = block(lb);
                }
            } else {
                orElse = ifStmt(ifn);
            }
            return Ast.ifStmt(range(if1.loc, orElse.loc), test, then, orElse);
        }
    }

    // for(i=1;i<10;i++) {}
    // for (v in obj) / for (k,v in obj)
    Expr forStmt(@NotNull Token<TokenType> for1) {
        // 这一坨peek 可能还不如直接换成先 mark 再抛异常回溯来得快...
        int i = 0;
        while (peek(i).is(NEWLINE)) i++;
        if (!peek(i++).is(LEFT_PAREN)) {
            throw Error.syntax(peek(i-1).loc, "for 的 {} 不能 省略");
        }
        int stack = 1;
        while (unexpectEOF(i)) {
            Token<TokenType> t = peek(i++);
            if (t.is(SEMICOLON)) {
                return foriStmt(for1);
            }
            if (t.is(IN)) {
                return forInStmt(for1);
            }
            if (t.is(LEFT_PAREN)) {
                stack++;
                continue;
            }
            // for(i=(1+1);i<10;i++) {}
            if (t.is(RIGHT_PAREN)) {
                if (--stack <= 0) {
                    return forInStmt(for1);
                }
            }
        }
        throw new IllegalStateException();
    }

    While whileStmt(@NotNull Token<TokenType> while1) {
        tryEatLines();
        eat(LEFT_PAREN);
        tryEatLines();
        Expr test = expr();
        tryEatLines();
        Token<TokenType> rp = eat(RIGHT_PAREN);
        tryEatLines();
        Block body = tryLoopBlock();
        if (body == null) {
            // body = Ast.emptyBlock(rp.loc);
            throw Error.syntax(rp.loc, "for、while body{}不能省略");
        }
        return Ast.whileStmt(range(while1.loc, body.loc), test, body);
    }

    Expr foriStmt(@NotNull Token<TokenType> for1) {
        List<Expr> stmts = lists();

        tryEatLines();
        eat(LEFT_PAREN);
        tryEatLines();
        if (!peek().is(SEMICOLON)) {
            Token<TokenType> def = tryEatAny(varDefinable());
            Expr init;
            if (def == null) {
                init = expr();
            } else {
                init = defVar(def);
            }
            stmts.add(init);
            tryEatLines();
        }
        eat(SEMICOLON);
        tryEatLines();
        Expr test;
        if (peek().is(SEMICOLON)) {
            test = litTrue(peek().loc); // 省略 test
        } else {
            test = expr();
        }
        eat(SEMICOLON);
        tryEatLines();

        Expr update = null;
        if (!peek().is(RIGHT_PAREN)) {
            update = expr();
            tryEatLines();
        }
        eat(RIGHT_PAREN);

        Block body = tryLoopBlock();
        if (body == null) {
            throw Error.syntax(peek().loc, "for、while 的 {} 不能省略");
        }

        Location loc = range(for1.loc, body.loc);
        if (update != null) {
            body.stmts.add(update);
        }
        stmts.add(Ast.whileStmt(loc, test, body));
        return Ast.block(loc, stmts);
    }

    Expr forInStmt(@NotNull Token<TokenType> forIn) {
        tryEatLines();
        eat(LEFT_PAREN);
        tryEatLines();
        Token<TokenType> defTok = eatAny(varDefinable());
        boolean mut = TokenType.mutable(defTok.type);

        Pattern pattern;
        tryEatLines();
        Token<TokenType> lb = tryEat(LEFT_BRACKET);
        if (lb == null) {
            VarDef def = defVarId(defTok, false);
            pattern = def.id;
        } else {
            pattern = pattern(lb);
            tryEatLines();
            assert peek().is(IN);
        }

        tryEatLines();
        eat(IN);
        tryEatLines();
        Expr iterVal = expr(); // iter
        Token<TokenType> rp = eat(RIGHT_PAREN);
        Block body = tryLoopBlock();
        if (body == null) {
            throw Error.syntax(rp.loc, "for、while 的 {} 不能省略");
        }

        Location loc = range(forIn.loc, rp.loc);
        IterFragment iter = Ast.iterator(loc, iterVal);

        Id nextVar = anon(loc);
        VarDef nextDef = varDef(loc, nextVar, iter.callNext, false, true);
        List<VarDef> varDefs = DefineAssignDesugar.desugarDefine(loc, loc, mut, pattern, nextVar);

        List<Expr> stmts = new ArrayList<>(1 + varDefs.size() + body.stmts.size());
        stmts.add(nextDef);
        stmts.addAll(varDefs);
        stmts.addAll(body.stmts);

        Block whileBody = Ast.block(body.loc, stmts, body.scope);
        While while1 = Ast.whileStmt(range(loc, body.loc), iter.callHasNext, whileBody);
        return Ast.block(range(iter.iterDef.loc, while1.loc), lists(iter.iterDef, while1), false);
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    // Group ArrowFn
    Expr leftParen(@NotNull Token<TokenType> lparen) {
        return any(
                () -> groupOrArrowFn1(lparen),
                () -> arrowFn(lparen)
        );
    }
    // ( expr ) | ( a ) => a | ( a ) => { }
    Expr groupOrArrowFn1(Token<TokenType> lparen) {
        Group group = group(lparen);
        tryEatLines();
        Token<TokenType> arrow = tryEat(ARROW);
        if (arrow == null) {
            return group;
        } else {
            return arrowFn(group, arrow);
        }
    }
    Group group(Token<TokenType> lparen) {
        tryEatLines();
        Expr expr = expr();
        tryEatLines();
        return Ast.group(range(lparen, eat(RIGHT_PAREN)), expr);
    }
    // 单参数, 有括号或者无括号
    // a =>  | ( a ) =>
    FunDef arrowFn(@NotNull Expr id, @NotNull Token<TokenType> arrow) {
        enterFun();

        if (id instanceof Group) {
            id = ((Group) id).expr;
        }
        if (!(id instanceof Id)) {
            throw Error.syntax(id.loc, "箭头函数语法为 a => expr");
        }
        LinkedHashMap<Id, Literal> params = new LinkedHashMap<>();
        params.put(((Id) id), null);
        Block body = arrowBody();
        Location loc = range(id.loc, body.loc);
        FunDef f = funDef(loc, classId, anon(loc), params, body, true, false, false);

        exitFun();
        return f;
    }
    // 参数部分非合法 group: 1个或者 >= 2个参数
    // ( ) => | ( a[,...] ) =>
    FunDef arrowFn(Token<TokenType> lparen) {
        enterFun();

        tryEatLines();
        LinkedHashMap<Id, Literal> params = params(lparen);
        tryEatLines();
        eat(ARROW);
        Block body = arrowBody();
        Location loc = range(lparen.loc, body.loc);
        FunDef f = funDef(loc, classId, anon(loc), params, body, true, false, false);

        exitFun();
        return f;
    }
    Block arrowBody() {
        tryEatLines();
        Token<TokenType> lb = tryEat(LEFT_BRACE);

        if (lb == null) {
           return exprBlock();
        } else {
            return block(lb);
            /*
            // 支持 这种鬼玩意 val f1 = () => { a -> a }
            // 等同于         val f1 = () => a => a
            return any(
                    () -> block(lb),
                    () -> exprBlock(closureBlock(lb))
            );
            */
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    New desugarRange(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        tryEatLines();
        Expr rhs = expr(tok.type.bp.prec);
        return Ast.newStmt(range(lhs.loc, rhs.loc), tok.loc, id(tok.loc, Constant.Class_Range), lists(lhs, rhs));
    }

    // prefix
    Unary unary(@NotNull Token<TokenType> tok) {
        tryEatLines();
        Expr arg = expr(tok.type.bp.prec);
        return Ast.unary(range(tok.loc, arg.loc), tok.loc, tok.type, arg, true);
    }
    Unary unaryPostfix(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        tryEatLines();
        return Ast.unary(range(lhs.loc, tok.loc), tok.loc, tok.type, lhs, false);
    }
    Expr is(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        tryEatLines();
        if (isUnApplyPattern()) {
            UnApplyPattern pattern = unApplyPattern();
            return desugarIsPattern(lhs, pattern);
        } else {
            Expr rhs = expr(tok.type.bp.prec);
            return binary(range(lhs.loc, rhs.loc), tok.loc, tok.type, lhs, rhs);
        }
    }
    Expr desugarIsPattern(Expr lhs, UnApplyPattern pattern) {
        MatchDesugar matchDesugar = new MatchDesugar();
        Id vMatched = matchDesugar.define(lhs.loc, "is", lhs);
        matchDesugar.match(pattern, vMatched);
        if (!matchDesugar.binders.isEmpty()) {
            throw Error.syntax(pattern.loc, "is pattern 不支持变量绑定");
        }
        List<Expr> matchers = matchDesugar.matchers;
        if (matchers.isEmpty()) {
            throw Error.bug(pattern.loc);
        }
        Expr test = matchers.get(0);
        for (int j = 1; j < matchers.size(); j++) {
            test = Ast.binary(pattern.loc, pattern.loc, LOGIC_AND, test, matchers.get(j));
        }
        return test;
    }
    Expr binaryL(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        tryEatLines();
        Expr rhs = expr(tok.type.bp.prec);
//        if (tok.type == IN) {
//            return desugarIn(lhs, tok, rhs);
//        } else {
            return binary(range(lhs.loc, rhs.loc), tok.loc, tok.type, lhs, rhs);
//        }
    }
    // 不需要, rhs.`isCase`(lhs)
//    Expr desugarIn(Expr lhs, Token<TokenType> tok, Expr rhs) {
//        Location loc = range(lhs.loc, rhs.loc);
//        return Ast.call(loc, tok.loc, Ast.subscript(loc, tok.loc, rhs, Ast.litSymbol(loc, SymbolIn.val)), lists(lhs));
//    }
    Expr binaryR(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        tryEatLines();
        Expr rhs = expr(tok.type.bp.prec - 1);
        return binary(range(lhs.loc, rhs.loc), tok.loc, tok.type, lhs, rhs);
    }
    Expr assign(AssignType type, @NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        if (lhs instanceof Pattern && !(lhs instanceof Id)) {
            return assignPattern(type, ((Pattern) lhs), tok);
        }
        tryEatLines();
        Expr rhs = expr(tok.type.bp.prec - 1);
        if (tok.type == ASSIGN) {
            return Ast.assign(range(lhs.loc, rhs.loc), tok.loc, type, lhs, rhs);
        } else {
            if (lhs instanceof Id || lhs instanceof ObjectMember || lhs instanceof Subscript) {
                Binary rhs1 = binary(rhs.loc, tok.loc, TokenType.trimAssign(tok.type), lhs, rhs);
            } else {
                throw Error.syntax(lhs.loc, "左值(pattern?)不支持 " + tok.type + " 赋值");
            }
            return desugarStarAssign(tok.loc, tok.type, lhs, rhs);
        }
    }
    Expr assignPattern(AssignType type, @NotNull Pattern ptn, @NotNull Token<TokenType> tok) {
        Location ptnLoc = ((Expr) ptn).loc;
        tryEatLines();
        Expr rhs = expr(tok.type.bp.prec - 1);
        if (tok.type == ASSIGN) {
            List<Expr> exprs = DefineAssignDesugar.desugarAssign(ptnLoc, tok.loc, ptn, rhs);
            assert !exprs.isEmpty() : "🍎";
            if (exprs.size() > 1) {
                // assign 可以直接开个"真"scope
                return Ast.block(range(ptnLoc, rhs.loc), exprs, true);
            } else {
                return exprs.get(0);
            }
        } else {
            throw Error.syntax(ptnLoc, "左值(pattern?)不支持 " + tok.type + " 赋值");
        }
    }


    Assign desugarStarAssign(Location loc, TokenType type, Expr lhs, Expr rhs) {
        if (lhs instanceof Id || lhs instanceof ObjectMember || lhs instanceof Subscript) {
            Binary rhs1 = binary(rhs.loc, loc, TokenType.trimAssign(type), lhs, rhs);
            return Ast.assign(range(lhs.loc, rhs.loc), loc, PATTERN, lhs, rhs1);
        } else {
            throw Error.syntax(lhs.loc, "左值(pattern?)不支持 " + type + " 赋值");
        }
    }
    Expr assign(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        return assign(PATTERN, ((Expr) expr2Pattern(lhs)), tok);
    }
    Expr assignByObjectMember(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        return assign(OBJECT_MEMBER, lhs, tok);
    }
    Expr assignBySubscript(@NotNull Expr lhs, @NotNull Token<TokenType> tok) {
        return assign(SUBSCRIPT, lhs, tok);
    }
    Pattern expr2Pattern(Expr expr) {
        if (expr instanceof Pattern) {
            return ((Pattern) expr);
        } else {
            throw Error.syntax(expr.loc, "不支持的 pattern: " + expr);
        }
    }
    // bool ? then : else
    Ternary cond(@NotNull Expr left, @NotNull Token<TokenType> tok) {
        tryEatLines();
        Expr mid;
        // 脚本可以考虑支持,val 自动类型转换  val ?: else
        // Token<TokenType> colon = tryEat(COLON);
        // if (colon == null) {
            mid = expr(COND.bp.prec);
            tryEatLines();
            eat(COLON);
        // }
        tryEatLines();
        Expr right = expr(ASSIGN.bp.prec);
        return ternary(range(left.loc, right.loc), tok.loc, COND, left, mid, right);
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    List<Expr> args() {
        List<Expr> args = new ArrayList<>();
        do {
            tryEatLines();
            args.add(expr());
        } while (tryEat(COMMA) != null);
        return args;
    }

    List<Expr> args(Token<TokenType> lparen) {
        Token<TokenType> rp = tryEat(RIGHT_PAREN);
        if (rp == null) {
            List<Expr> args = args();
            tryEatLines();
            eat(RIGHT_PAREN);
            return args;
        } else {
            return lists();
        }
    }

    // b() / b(x,y)     \\  / b(x,y) {}
    Call callLeftParen(@NotNull Expr callee, @NotNull Token<TokenType> lparen) {
        tryEatLines();
        List<Expr> args = args(lparen);
//        Token<TokenType> lb = tryEat(LEFT_BRACE);
//        if (lb == null) {
            return call(range(callee.loc, peek(-1).loc), lparen.loc, callee, args);
//        } else {
//            Expr closure = closureBlock(lb);
//            args.add(closure);
//            return Ast.call(range(callee.loc, closure.loc), lparen.loc, callee, args);
//        }
    }

    /*
    // b{}
    Call callLeftBrace(@NotNull Expr callee, @NotNull Token<TokenType> lbrace) {
        Expr closure = closureBlock(lbrace);
        List<Expr> args = lists(closure);
        return Ast.call(range(callee.loc, closure.loc), lbrace.loc, callee, args);
    }
    */

    Token<TokenType> dotName() {
        Token<TokenType> name = eat();
        if (name.is(NAME) || name.is(NULL) || name.is(TRUE) || name.is(FALSE) || name.is(SYMBOL)
                || name.type.type == TokenCategory.KEYWORD) {
            return name;
        } else {
            throw Error.syntax(peek().loc, "期望 name 或者 symbol, 实际是 " + name);
        }
    }
    // a.m() / a.m() {} / a.m {} / a.f = 1
    Expr dot(@NotNull Expr obj, @NotNull Token<TokenType> dot) {
        Token<TokenType> name = dotName();
        Location loc = range(obj.loc, name.loc);
        Expr expr;
        if (name.is(SYMBOL)) {
            // .`symbol` 修改成 subscript
            expr = Ast.subscript(loc, dot.loc, obj, litSymbol(name));
        } else {
            expr = objectMember(loc, dot.loc, obj, id(name.loc, name.lexeme));
        }
        Token<TokenType> lp, lb, assign;
        if ((lp = tryEat(LEFT_PAREN)) != null) {
            return callLeftParen(expr, lp);
        } /*else if ((lb = tryEat(LEFT_BRACE)) != null) {
            return callLeftBrace(expr, lb);
        }*/ else if ((assign = tryEatAny(assignable())) != null) {
            return assignByObjectMember(expr, assign);
        } else {
            return expr;
        }
    }

    Expr subscript(@NotNull Expr lstMap, @NotNull Token<TokenType> lbrace) {
        tryEatLines();
        Expr idxKey = expr();
        tryEatLines();
        Token<TokenType> rb = eat(RIGHT_BRACKET);
        Subscript subscript = Ast.subscript(range(lstMap.loc, rb.loc), lbrace.loc, lstMap, idxKey);
        tryEatLines();
        Token<TokenType> assign = tryEatAny(assignable());
        if (assign == null) {
            return subscript;
        } else {
            return assignBySubscript(subscript, assign);
        }
    }

    Throw throw1(@NotNull Token<TokenType> throw1) {
        tryEatLines();
        Expr expr = expr();
        return throwStmt(range(throw1.loc, expr.loc), expr);
    }

    Try try1(@NotNull Token<TokenType> try1) {
        tryEatLines();
        Block tryBlock = block(eat(LEFT_BRACE));
        Id error = null;
        Block catchBlock = null;
        Block finallyBlock = null;

        tryEatLines();
        Token<TokenType> catch1 = tryEat(CATCH);
        if (catch1 != null) {
            tryEatLines();
            eat(LEFT_PAREN);
            Token<TokenType> id = eatTriviaName();
            error = id(id.loc, id.lexeme);
            tryEatLines();
            eat(RIGHT_PAREN);


            int i = 0;
            while (peek(i).is(NEWLINE)) i++; // todo 这里不对.. 应该用回溯的方式判断...
            peek(i++).is(LEFT_BRACE);
            while (peek(i).is(NEWLINE)) i++;
            boolean hasCase = peek(i).is(CASE);

            if (hasCase) {
                catchBlock = desugarMatchBlock(catch1, error);
            } else {
                tryEatLines();
                catchBlock = block(eat(LEFT_BRACE));
            }
        }
        if (tryEat(FINALLY) != null) {
            tryEatLines();
            finallyBlock = block(eat(LEFT_BRACE));
        }
        if (catchBlock == null && finallyBlock == null) {
            throw Error.syntax(try1.loc, "catch 与 finally 不能同时为空");
        }
        if (finallyBlock == null) {
            Location loc = range(try1.loc, catchBlock.loc);
            return tryStmt(loc, tryBlock, error, catchBlock, emptyBlock(loc));
        } else {
            return tryStmt(range(try1.loc, finallyBlock.loc), tryBlock, error, catchBlock, finallyBlock);
        }
    }

    Assert assert1(@NotNull Token<TokenType> assert1) {
        Expr expr = expr();
        if (expr.loc.rowEnd != -1 && assert1.loc.rowBegin != expr.loc.rowEnd) {
            throw Error.syntax(range(assert1.loc, expr.loc), "assert 语句不能换行");
        }
        Expr msg = null;
        Token<TokenType> colon = tryEat(COLON);
        if (colon != null) {
            msg = expr();
        }
        return Ast.assert1(range(assert1.loc, expr.loc), expr, msg);
    }
}