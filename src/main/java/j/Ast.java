package j;

import j.parser.Location;
import j.parser.TokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static j.Helper.lists;
import static j.Value.*;

/**
 * AST: 全部都是 Expr
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public interface Ast {
    static boolean isAnonymous(@NotNull Id id) { return Id.isAnonymous(id); }

    static Id wildcards(Location loc) { return id(loc, Matcher.Wildcards); }
    static Id id(Location loc, String name) { return new Id(loc, name); }
    static Id anon(Location loc) { return Id.anonymous(loc); }
    static Id idSuffix(Location loc, String name) { return id(loc, Id.anonymous(loc).name + "_" + name); }
    static Id idIfNecessary(Location loc, String suffix, Expr expr) {
        if (expr instanceof Id) {
            return ((Id) expr);
        } else {
            return idSuffix(loc, suffix);
        }
    }

    static Literal literal(Location loc, LiteralType type, Object content) {
        return new Literal(loc, type, content);
    }
    static Literal litInt(Location loc, String v) { return literal(loc, LiteralType.Int, v); }
    static Literal litFloat(Location loc, String v) { return literal(loc, LiteralType.Float, v); }
    static Literal litString(Location loc, String v) { return literal(loc, LiteralType.String, v); }
    static Literal litSymbol(Location loc,  String v) { return literal(loc, LiteralType.Symbol, v); }
    static Literal litClass(Location loc, String v) { return literal(loc, LiteralType.Class, v); }
    static Literal litNull(Location loc) { return literal(loc, LiteralType.Null, "null"); }
    static Literal litUndefined(Location loc) { return literal(loc, LiteralType.Undefined, "/*undefined*/"); }
    static Literal litTrue(Location loc) { return literal(loc, LiteralType.Bool, "true"); }
    static Literal litFalse(Location loc) { return literal(loc, LiteralType.Bool, "false"); }

    static Literal litList(Location loc, List<Literal> v) { return literal(loc, LiteralType.List, v); }
    static Literal litMap(Location loc, Map<Literal, Literal> v) { return literal(loc, LiteralType.Map, v); }


    static boolean isSymbol(Expr expr) {
        return expr instanceof Literal && ((Literal) expr).type == LiteralType.Symbol;
    }
    static boolean isLiteral(Expr expr) {
        return expr instanceof Literal && !isSymbol(expr);
    }
    static boolean isLiteral(List<Expr> exprs) {
        for (Expr expr : exprs) {
            // 注意: list / map 中 sym 不被作为字面量看待, 会改写成 a[`sym`] = b, 触发 sym 属性写入的特殊逻辑
            if (!isLiteral(expr)) return false;
        }
        return true;
    }
    static boolean isLiteral(Map<Expr, Expr> props) {
        for (Map.Entry<Expr, Expr> it : props.entrySet()) {
            // 注意: list / map 中 sym 不被作为字面量看待, 会改写成 a[`sym`] = b, 触发 sym 属性写入的特殊逻辑
            if (!isLiteral(it.getKey())) return false;
            if (!isLiteral(it.getValue())) return false;
        }
        return true;
    }
    static boolean isNull(Expr expr) {
        return expr instanceof Literal && ((Literal) expr).type == LiteralType.Null;
    }
    static boolean isUndefined(Expr expr) {
        return expr instanceof Literal && ((Literal) expr).type == LiteralType.Undefined;
    }

    static MatchCase matchCase(Location loc, Pattern pattern, @Nullable Expr guard, Block body) {
        return new MatchCase(loc, pattern, guard, body);
    }
    static Match match(Location loc, Expr value, List<MatchCase> cases) {
        return new Match(loc, value, cases);
    }
    static ListPattern listPattern(Location loc, List<Pattern> elems) {
        return new ListPattern(loc, elems);
    }
    static MapPattern mapPattern(Location loc, Map<Expr, Pattern> props) {
        return new MapPattern(loc, props);
    }
    static UnApplyPattern unApplyPattern(Location loc, Location oploc, Id name, List<Pattern> props) {
        return new UnApplyPattern(loc, oploc, name, props);
    }
    static ExprPattern exprPattern(@NotNull Location loc, @NotNull Expr expr) {
        return new ExprPattern(loc, expr);
    }

    static Group group(Location loc, Expr expr) { return new Group(loc, expr); }
    static Block block(Location loc, List<Expr> stmts, boolean scope) { return new Block(loc, stmts, scope); }
    static Block block(Location loc, List<Expr> stmts) { return block(loc, stmts, true); }
    static Block emptyBlock(Location loc) { return block(loc, lists()); }
    static VarDef varDef(Location loc, Id id, Expr init, boolean mut, boolean hasInit) {
        return new VarDef(loc, id, init, mut, hasInit);
    }
    static FunDef funDef(Location loc, @Nullable Id className, Expr name, LinkedHashMap<Id, Literal> params, Block body,
                         boolean arrow, boolean symbol, boolean extend) {
        return new FunDef(loc, className, name, params, body, arrow, symbol, extend);
    }
    static ClassDef classDef(Location loc, Id name, @Nullable Id parent, List<VarDef> props, List<FunDef> methods, FunDef ctor) {
        return new ClassDef(loc, name, parent, props, methods, ctor, false, false);
    }
    static ClassDef classDef(Location loc, Id name, @Nullable Id parent, List<VarDef> props, List<FunDef> methods, FunDef ctor,
                             boolean sealed, boolean tagged) {
        return new ClassDef(loc, name, parent, props, methods, ctor, sealed, tagged);
    }
    // todo remove tokenType
    static Assign assign(Location loc, Location oploc, AssignType type, Expr lhs, Expr rhs) {
        return new Assign(loc, oploc, type, lhs, rhs);
    }
    static Binary binary(Location loc, Location oploc, TokenType op, Expr lhs, Expr rhs) {
        return new Binary(loc, oploc, op, lhs, rhs);
    }
    static Unary unary(Location loc, Location oploc, TokenType op, Expr arg, boolean prefix) {
        return new Unary(loc, oploc, op, arg, prefix);
    }
    static Ternary ternary(Location loc, Location oploc, TokenType op, Expr left, Expr mid, Expr right) {
        return new Ternary(loc, oploc, op, left, mid, right);
    }
    static Subscript subscript(Location loc, Location oploc, Expr lstMap, Expr idxKey) {
        return new Subscript(loc, oploc, lstMap, idxKey);
    }
    static ObjectMember objectMember(Location loc, Location oploc, Expr obj, Id prop) {
        return new ObjectMember(loc, oploc, obj, prop);
    }
    static Break breakStmt(Location loc) { return new Break(loc); }
    static Continue continueStmt(Location loc) { return new Continue(loc); }
    static Return returnStmt(Location loc, Expr arg) { return new Return(loc, arg); }
    static Throw throwStmt(Location loc, Expr expr) { return new Throw(loc, expr); }
    static Try tryStmt(Location loc, Block try1, @Nullable Id error, @Nullable Block catch1, Block final1) {
        return new Try(loc, try1, error, catch1, final1);
    }
    static If ifStmt(Location loc, Expr test, Block then, /*If|Block*/@Nullable Expr orElse) {
        return new If(loc, test, then, orElse);
    }
    static While whileStmt(Location loc, Expr test, Block body) {
        return new While(loc, test, body);
    }
    static New newStmt(Location loc, Location oploc, Id name, List<Expr> args) {
        return new New(loc, oploc, name, args);
    }
    static Call call(Location loc, Location oploc, Expr callee, List<Expr> args) {
        return new Call(loc, oploc, callee, args);
    }
    static Assert assert1(Location loc, Expr expr, @Nullable Expr msg) { return new Assert(loc, expr, msg); }
    static Debugger debugger(Location loc) { return new Debugger(loc); }

    static IterFragment iterator(Location loc, Expr iterVal) {
        return new IterFragment(loc, iterVal);
    }

    class IterFragment {
        public final Subscript iterable;
        public final Id iterVar;
        public final VarDef iterDef;
        public final Call callNext;
        public final Call callHasNext;
        private IterFragment(Location loc, Expr iterVal) {
            iterVar = Ast.idSuffix(loc, "iter");

            iterable = subscript(loc, loc, iterVal, litSymbol(loc, SymbolIterable.toString()));
            Call iterator = call(loc, loc, iterable, lists());
            iterDef = varDef(loc, iterVar, iterator, false, true);

            ObjectMember next = objectMember(loc, loc, iterVar, id(loc, Constant.Fun_Iterator_Next));
            ObjectMember hasNext = objectMember(loc, loc, iterVar, id(loc, Constant.Fun_Iterator_HasNext));

            callNext = call(loc, loc, next, lists());
            callHasNext = call(loc, loc, hasNext, lists());
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    class Node {
        public Location loc;
        /**
         * operator loc
         * 用于 assert 的位置, 比如 a + b 表达式, oploc 位置在 +
         */
        public Location oploc;

        @Override
        public String toString() {
            return loc.codeSpan();
        }
    }
    // todo synthetic
    class Expr extends Node {
//        @Override
//        public boolean equals(Object other) {
//            if (other == this) {
//                return true;
//            } else if(other instanceof Node) {
//                return Objects.equals(loc, ((Node) other).loc);
//            } else {
//                return false;
//            }
//        }
//        @Override
//        public int hashCode() {
//            return loc.hashCode();
//        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
    enum LiteralType {
        Bool, Int, Float, String, Symbol, List, Map, Class, Null, Undefined,
    }
    class Literal extends Expr implements Pattern {
        @NotNull public final LiteralType type;
        @NotNull public final Object content; // String|List<Literal>|Map<Literal, Literal> todo 这里也改一下
        private Literal(@NotNull Location loc, @NotNull LiteralType type, @NotNull Object content) {
            this.type = type;
            this.content = content;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    interface Pattern { }
    class MatchCase extends Expr {
        @NotNull public final Pattern pattern;
        @Nullable public final Expr guard;
        @NotNull public final Block body;
        private MatchCase(@NotNull Location loc, @NotNull Pattern pattern, @Nullable Expr guard, @NotNull Block body) {
            this.pattern = pattern;
            this.guard = guard;
            this.body = body;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class Match extends Expr {
        @NotNull public final Expr value;
        @NotNull public final List<MatchCase> cases;
        private Match(@NotNull Location loc, @NotNull Expr value, @NotNull List<MatchCase> cases) {
            this.value = value;
            this.cases = cases;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    // identifier
    class Id extends Expr implements Pattern {
        @NotNull public final String name;
        private Id(@NotNull Location loc, @NotNull String name) {
            this.name = name;
            this.loc = loc;
            this.oploc = loc;
        }

        static int AnonymousCnt = 1;
        public final static String AnonymousPrefix = "__";// "anonymous$";
        private static Id anonymous(Location loc) {
            return Ast.id(loc, AnonymousPrefix + (AnonymousCnt++));
        }
        private static boolean isAnonymous(@NotNull Id id) {
            return id.name.startsWith(AnonymousPrefix);
        }
    }
    class ListPattern extends Expr implements Pattern {
        @NotNull public final List<Pattern> elems;
        private ListPattern(@NotNull Location loc, @NotNull List<Pattern> elems) {
            this.elems = elems;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class MapPattern extends Expr implements Pattern {
        @NotNull public final Map<Expr, Pattern> props;
        private MapPattern(@NotNull Location loc, @NotNull Map<Expr, Pattern> props) {
            this.props = props;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class UnApplyPattern extends Expr implements Pattern {
        @NotNull public final Id name;
        @NotNull public final List<Pattern> props;
        private UnApplyPattern(@NotNull Location loc,  @NotNull Location oploc,
                              @NotNull Id name, @NotNull List<Pattern> props) {
            this.name = name;
            this.props = props;
            this.loc = loc;
            this.oploc = oploc;
        }
    }
    class ExprPattern extends Expr implements Pattern {
        @NotNull public final Expr expr;
        private ExprPattern(@NotNull Location loc, @NotNull Expr expr) {
            this.expr = expr;
            this.loc = loc;
        }
    }
    class Group extends Expr {
        @NotNull public final Expr expr;
        private Group(@NotNull Location loc, @NotNull Expr expr) {
            this.expr = expr;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class Block extends Expr {
        @NotNull public final List<Expr> stmts;
        public final boolean scope;
        private Block(@NotNull Location loc, @NotNull List<Expr> stmts, boolean scope) {
            this.stmts = stmts;
            this.scope = scope;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    interface Def { }
    // todo
    enum Modifier {
        PRIVATE, PUBLIC;
        static Modifier of(String name) {
            char firstC = name.charAt(0);
            return firstC >= 'A' && firstC <= 'Z' ? PUBLIC : PRIVATE;
        }
    }
    class VarDef extends Expr implements Def {
        // @NotNull public final Modifier mod;
        @NotNull public final Id id;
        // @NotNull public final Pattern pattern;
        @NotNull public final Expr init; // !! Undefined , hasInit = false
        public final boolean mut;
        public final boolean hasInit;
        private VarDef(@NotNull Location loc,
                       @NotNull Id id,
                       // @NotNull Pattern pattern,
                       @NotNull Expr init, boolean mut, boolean hasInit) {
            // this.mod = Modifier.of(name.name);
            this.id = id;
            // this.pattern = pattern;
            this.init = init;
            this.mut = mut;
            this.hasInit = hasInit;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class FunDef extends Expr implements Def {
        // @NotNull public final Modifier mod;
        @Nullable public final Id className;
        @NotNull public final Expr name; // Id|Literal<Symbol> todo: 这里最好改一下
        // @Nullable public final List<Call> decorators;
        // @NotNull public final List<Id> params;
        @NotNull public final LinkedHashMap<Id, /*@Nullable*/Literal> params;
        @NotNull public final Block body;
        public final boolean symbol;
        public final boolean extend;
        public final boolean arrow;
        // @NotNull public final List<Expr> defaults;
        // @Nullable public final Id rest;
        // public final boolean generator;
        private FunDef(@NotNull Location loc, @Nullable Id className, @NotNull Expr name,
                       // @Nullable List<Call> decorators,
                       @NotNull LinkedHashMap<Id, Literal> params, @NotNull Block body,
                       boolean arrow, boolean symbol, boolean extend) {
            if (symbol) {
                Expect.expect(loc, isSymbol(name), "name 必须为 Symbol"); ;
            } else {
                Expect.expect(loc, name instanceof Id, "name 必须为 Id");
            }
            // this.mod = Modifier.of(name.name);
            this.className = className;
            this.name = name;
            // this.decorators = decorators;
            this.params = params;
            this.body = body;
            this.arrow = arrow;
            this.symbol = symbol;
            this.extend = extend;
            this.loc = loc;
            this.oploc = loc;
        }
        public String stringName() {
            if (symbol) {
                return ((String) ((Literal) name).content);
            } else {
                return ((Id) name).name;
            }
        }
        @Override
        public String toString() {
            return "fun → " + name;
        }
    }
    class ClassDef extends Expr implements Def {
        // @NotNull public final Modifier mod;
        @NotNull public final Id name;
        @Nullable public final Id parent;
        @NotNull public final List<VarDef> props;
        @NotNull public final List<FunDef> methods;
        @NotNull public final FunDef ctor;
        public final boolean sealed;
        public final boolean tagged; // discriminated;
        private ClassDef(@NotNull Location loc, @NotNull Id name, @Nullable Id parent,
                         @NotNull List<VarDef> props,
                         @NotNull List<FunDef> methods,
                         @NotNull FunDef ctor,
                         boolean sealed,
                         boolean tagged) {
            // this.mod = Modifier.of(name.name);
            this.name = name;
            this.parent = parent;
            this.props = props;
            this.methods = methods;
            this.ctor = ctor;
            this.sealed = sealed;
            this.tagged = tagged;
            this.loc = loc;
            this.oploc = loc;
        }
        @Override
        public String toString() {
            return "class → " + name;
        }
    }

    class Break extends Expr {
        private Break(@NotNull Location loc) {
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class Continue extends Expr {
        private Continue(@NotNull Location loc) {
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class Return extends Expr {
        @NotNull public final Expr arg;
        private Return(@NotNull Location loc, @NotNull Expr arg) {
            this.arg = arg;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class Throw extends Expr {
        public final Expr expr;
        private Throw(@NotNull Location loc, @NotNull Expr expr) {
            this.expr = expr;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class Try extends Expr {
        @NotNull public final Block try1;

        @Nullable public final Id error;
        @Nullable public final Block catch1;

        // @Nullable public final Match catch1;

        @NotNull public final Block final1;
        private Try(@NotNull Location loc, @NotNull Block try1,
                    @Nullable Id error,
                    @Nullable Block catch1,
                    @NotNull Block final1) {
            this.try1 = try1;
            this.error = error;
            this.catch1 = catch1;
            this.final1 = final1;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class If extends Expr {
        @NotNull public final Expr test;
        @NotNull public final Block then;
        @Nullable public final /*If|Block*/Expr orelse;
        private If(@NotNull Location loc, @NotNull Expr test, @NotNull Block then, @Nullable Expr orelse) {
            this.test = test;
            this.then = then;
            this.orelse = orelse;
            this.loc = loc;
            this.oploc = loc;
        }
    }

    class While extends Expr {
        @NotNull public final Expr test;
        @NotNull public final Block body;
        private While(@NotNull Location loc, @NotNull Expr test, @NotNull Block body) {
            this.test = test;
            this.body = body;
            this.loc = loc;
            this.oploc = loc;
        }
    }

//    class For extends Expr {
//        @NotNull public final Expr init;
//        @NotNull public final Expr test;
//        @NotNull public final Expr update;
//        @NotNull public final Block body;
//        private For(@NotNull Location loc, @NotNull Expr init, @NotNull Expr test, @NotNull Expr update, @NotNull Block body) {
//            this.init = init;
//            this.test = test;
//            this.update = update;
//            this.body = body;
//            this.loc = loc;
//            this.oploc = loc;
//        }
//    }

    class Member extends Expr {}
    /**
     * subscript / array-index / array-member / map-get
     */
    class Subscript extends Member {
        @NotNull public final Expr object;
        @NotNull public final Expr idxKey;
        private Subscript(@NotNull Location loc, @NotNull Location oploc,
                          @NotNull Expr object, @NotNull Expr idxKey) {
            this.object = object;
            this.idxKey = idxKey;
            this.loc = loc;
            this.oploc = oploc;
        }
    }
    class ObjectMember extends Member {
        @NotNull public final Expr object;
        @NotNull public final Id prop;
        private ObjectMember(@NotNull Location loc, @NotNull Location oploc,
                             @NotNull Expr object, @NotNull Id prop) {
            this.object = object;
            this.prop = prop;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class New extends Expr {
        @NotNull public final Id name;
        @NotNull public final List<Expr> args;
        private New(@NotNull Location loc, @NotNull Location oploc, @NotNull Id name, @NotNull List<Expr> args) {
            this.name = name;
            this.args = args;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class Call extends Expr {
        @NotNull public final Expr callee;
        @NotNull public final List<Expr> args;
        private Call(@NotNull Location loc, @NotNull Location oploc, @NotNull Expr callee, @NotNull List<Expr> args) {
            this.callee = callee;
            this.args = args;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    class Unary extends Expr {
        // todo 这里是不是不用 tokenType
        enum Type {}
        @NotNull public final TokenType op;
        @NotNull public final Expr arg;
        public final boolean prefix;
        private Unary(@NotNull Location loc, @NotNull Location oploc,
                        @NotNull TokenType op, @NotNull Expr arg, boolean prefix) {
            this.op = op;
            this.arg = arg;
            this.prefix = prefix;
            this.loc = loc;
            this.oploc = oploc;
        }
    }
    class Binary extends Expr {
        // todo 这里是不是不用 tokenType
        enum Type {}
        @NotNull public final TokenType op;
        @NotNull public final Expr lhs;
        @NotNull public final Expr rhs;
        private Binary(@NotNull Location loc, @NotNull Location oploc,
                         @NotNull TokenType op, @NotNull Expr lhs, @NotNull Expr rhs) {
            this.op = op;
            this.lhs = lhs;
            this.rhs = rhs;
            this.loc = loc;
            this.oploc = oploc;
        }
    }
    class Ternary extends Expr {
        // todo 这里是不是不用 tokenType
        enum Type {}
        @NotNull public final TokenType op;
        @NotNull public final Expr left;
        @NotNull public final Expr mid;
        @NotNull public final Expr right;
        private Ternary(@NotNull Location loc, @NotNull Location oploc,
                        @NotNull TokenType op, @NotNull Expr left, @NotNull Expr mid, @NotNull Expr right) {
            this.op = op;
            this.left = left;
            this.mid = mid;
            this.right = right;
            this.loc = loc;
            this.oploc = oploc;
        }
    }

    enum AssignType {
        PATTERN, SUBSCRIPT, OBJECT_MEMBER
    }
    class Assign extends Binary {
        public final @NotNull AssignType type;
        private Assign(@NotNull Location loc, @NotNull Location oploc,
                       @NotNull AssignType type, @NotNull Expr lhs, @NotNull Expr rhs) {
            super(loc, oploc, TokenType.ASSIGN, lhs, rhs);
            this.type = type;
        }
    }

    class Assert extends Expr {
        public final @NotNull Expr expr;
        public final @Nullable Expr msg;
        private Assert(@NotNull Location loc, @NotNull Expr expr, @Nullable Expr msg) {
            this.expr = expr;
            this.msg = msg;
            this.loc = loc;
            this.oploc = loc;
        }
    }
    class Debugger extends Expr {
        private Debugger(@NotNull Location loc) {
            this.loc = loc;
            this.oploc = loc;
        }
    }
}
