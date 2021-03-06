package j;

import j.parser.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.String;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.Map.Entry;

import static j.Ast.*;
import static j.Helper.*;
import static j.Interp.Renderer.ValRec;
import static j.Interp.Renderer.render;
import static j.Value.*;
import static j.Value.Constant.*;
import static j.parser.Location.None;
import static java.util.stream.Collectors.*;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class Interp implements Visitor<Scope, Value> {
    public int stackOverThreshold = 100;
    public FrameStack stack = new FrameStack(stackOverThreshold);

    public Scope globalEnv;
    Value.ClassValue Class_Throwable;
    Value.ClassValue Class_NativeError;
    Value.ClassValue Class_AssertError;
    Value.ClassValue Class_ClassNotFoundError;
    Value.ClassValue Class_NullPointerError;
    Value.ClassValue Class_CallError;
    Value.ClassValue Class_RuntimeError;
    Value.ClassValue Class_MatchError;
    Value.FunValue Fun_Apply;

    public final static Path bootstrap = Helper.resource2path("/bootstrap.j");

    public Interp() {
        // bootstrap();
    }

    public Value eval(@NotNull Path path) {
        return eval(Lexer.source(path), globalEnv);
    }

    public Value eval(@NotNull String input) {
        return eval(Lexer.source("<inline>", input), globalEnv);
    }

    @SuppressWarnings("unused")
    public Value eval(@NotNull Expr expr) {
        return interp(expr, globalEnv);
    }

    Value eval(@NotNull Lexer.SourceInput si, @NotNull Scope env) {
        Block program = new ParserJ(si).parse();
        try {
            return interp(program, env);
        } catch (ControlFlow.Throw e) {
            throw unhandledError(e);
        }
    }

    RuntimeException unhandledError(ControlFlow.Throw e) {
        String msg = "";
        if (e.value.is(e.loc, Class_Throwable)) {
            Value msgV = ((ObjectValue) e.value).getProperty(e.loc, "msg");
            if (msgV != null) {
                msg = msgV.toString();
            }
        }

        err("??????????????????: " + e.value + " at " + e.loc);

        List<Frame> bt = stack.snapshot();
        if (!bt.isEmpty()) {
            err("backtrace:\n" + join(bt, "\n\t"));
        }
        err(e.loc.inspect(msg));

        // power assert
        if (e.value.is(e.loc, Class_AssertError)) {
            Value reason = ((ObjectValue) e.value).getProperty(e.loc, "reason");
            if (reason != null) {
                err("\n" + reason.toString());
            }
        }

        return runtimeError(e.loc, "??????????????????");
    }

    void bootstrap() {
        if (globalEnv == null) {
            // ????????? env ???????????? env ??????: ????????? bootstrap.j ????????? require, ???????????????
            globalEnv = BuildIn.globalScope();

            // ??????????????? ClassObject, ????????????, ????????? StdObject ??? type
            globalEnv.define(None, Var_Global, Value.Scope(ClassObject, globalEnv), false);

            // Value ??????????????????????????????????????????????????? Interp, ????????????, ?????????????????? Interp ??????
            globalEnv.define(None, Key_Interp, (BoxedValue) () -> this, false);
            interp(new ParserJ(Lexer.source(bootstrap)).parse(), globalEnv);

            // ??? global ?????? ??? class ??? object ?????? scope
            Value.ClassValue classScope = lookup(globalEnv, Class_Scope).asClass(None);
            globalEnv.unset(None, Var_Global);
            globalEnv.define(None, Var_Global, Value.Scope(classScope, globalEnv), false);

            Class_Throwable = lookup(globalEnv, Constant.Class_Throwable).asClass(None);
            Class_NativeError = lookup(globalEnv, Constant.Class_NativeError).asClass(None);
            Class_AssertError = lookup(globalEnv, Constant.Class_AssertError).asClass(None);
            Class_ClassNotFoundError = lookup(globalEnv, Constant.Class_ClassNotFoundError).asClass(None);
            Class_CallError = lookup(globalEnv, Constant.Class_CallError).asClass(None);
            Class_NullPointerError = lookup(globalEnv, Constant.Class_NullPointerError).asClass(None);
            Class_RuntimeError = lookup(globalEnv, Constant.Class_RuntimeError).asClass(None);
            Class_MatchError = lookup(globalEnv, Constant.Class_MatchError).asClass(None);
            Fun_Apply = ((FunValue) ClassFun.metaTable().get(None, Constant.Fun_Apply));
        }
    }

    boolean booted() { return Class_Throwable != null; }

    private ObjectValue lookup(Scope env, String key) {
        Value v = env.lookup(key);
        if (v == null) {
            throw Error.bug(None);
        } else {
            return v.asObject(None);
        }
    }

    ObjectValue newThrowable(Location loc, Value.ClassValue throwableClassValue, String msg) {
        return throwableClassValue.newInstance(loc, String(msg), Null);
    }
    ObjectValue newThrowable(Location loc, Value.ClassValue throwableClassValue, Value ...args) {
        return throwableClassValue.newInstance(loc, args);
    }
    ControlFlow.Throw error(Location loc, Value.ClassValue throwableClass, String msg) {
        return ControlFlow.Throw(loc, stack.snapshot(), newThrowable(loc, throwableClass, msg));
    }
    ControlFlow.Throw error(Location loc, Value.ClassValue throwableClass, Value ...args) {
        return ControlFlow.Throw(loc, stack.snapshot(), newThrowable(loc, throwableClass, args));
    }

    RuntimeException nativeError(Location loc, String msg, String ex) {
        if (Class_NativeError == null) {
            throw Error.runtime(loc, msg);
        } else {
            throw error(loc, Class_NativeError, String(ex), String(ex));
        }
    }
    RuntimeException nullPointerError(Location loc, String msg) {
        if (Class_NullPointerError == null) {
            throw Error.runtime(loc, msg);
        } else {
            throw error(loc, Class_NullPointerError, msg);
        }
    }
    RuntimeException callError(Location loc, String msg) {
        if (Class_CallError == null) {
            throw Error.runtime(loc, msg);
        } else {
            throw error(loc, Class_CallError, msg);
        }
    }
    RuntimeException classNotFound(Location loc, String className) {
        if (Class_ClassNotFoundError == null) {
            throw Error.runtime(loc, "???????????? ??? " + className);
        } else {
            throw error(loc, Class_ClassNotFoundError, "???????????? ??? " + className);
        }
    }
    RuntimeException runtimeError(Location loc, @SuppressWarnings("SameParameterValue") String msg) {
        if (Class_RuntimeError == null) {
            throw Error.runtime(loc, msg);
        } else {
            throw error(loc, Class_RuntimeError, msg);
        }
    }

    ControlFlow.Throw throw1(Location loc, Value val) {
        List<Frame> snapshot = stack.snapshot();
        return ControlFlow.Throw(loc, snapshot, val);
    }


    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    Assert a = null;
    ValRec rec = new ValRec();

    Value interp(@NotNull Node node, @NotNull Scope s) {
        if (a == null) {
            return visit(node, s);
        } else {
            return interpAssert(node, s);
        }
    }

    Value interpAssert(@NotNull Node node, @NotNull Scope s) {
        boolean show = !(node instanceof Literal) && !(node instanceof FunDef);
        if (show && a.expr.loc.contains(node.loc)) {
            int col = node.oploc.colBegin - a.expr.loc.colBegin + 1;
            Value v = visit(node, s);
            return rec.rec(v, col);
        } else {
            return visit(node, s);
        }
    }

    @Override
    public Value visit(@NotNull Debugger debugger, @NotNull Scope s) {
        return Null;
    }

    @Override
    public Value visit(@NotNull Assert assert1, @NotNull Scope s) {
        try {
            a = assert1;
            assert1(assert1, s);
        } finally {
            a = null;
            rec.clear();
        }
        return Null;
    }

    Value assert1(Assert a, Scope s) {
        Expr expr = a.expr;
        String src = expr.loc.codeSpan();
        Value r = interpAssert(expr, s);

        if (!(r instanceof BoolValue)) {
            throw Error.runtime(a.loc, "??????????????? BoolValue");
        }

        boolean ok = r.asBool(expr.loc).val;
        if (ok) {
            // log(render(expr.loc, src, rec));
            return Null;
        } else {
            String msg;
            if (a.msg == null) {
                msg = "????????????";
            } else {
                msg = interp(a.msg, s).asString(a.loc).val;
            }
            // backtrace();
            throw assertError(msg, render(expr.loc, src, rec));
        }
    }

    RuntimeException assertError(String msg, String reason) {
        if (Class_AssertError == null) {
            return Error.runtime(a.loc, msg + "\n" + reason);
        } else {
            // todo a == null
            return error(a == null ? None : a.loc, Class_AssertError, String(msg), String(reason), Null);
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    @Override
    public Value visit(@NotNull Group group, @NotNull Scope s) {
        return interp(group.expr, s);
    }

    @Override
    public Value visit(@NotNull Id id, @NotNull Scope s) {
        Value val = s.lookup(id.name);
        if (val == null || val == Undefined) {
            throw Error.runtime(id.loc, id.name + " ???????????????");
        }
        return val;
    }

    @Override
    public Value visit(@NotNull Literal literal, @NotNull Scope s) {
        return Literals.parse(literal);
    }

    Value stmts(List<Expr> stmts, Scope s) {
        Value v = Null;
        for (Expr stmt : stmts) {
            v = interp(stmt, s);
        }
        return v;
    }

    @Override
    public Value visit(@NotNull Block block, @NotNull Scope s) {
        return stmts(block.stmts, block.scope ? s.inherit() : s);
    }

    @Override // var + prop
    public Value visit(@NotNull VarDef var, @NotNull Scope s) {
        // Pattern pattern = patternInterp.match(var.pattern, s);
        Value v = interp(var.init, s);
        s.define(var.loc, var.id, v, var.mut);
        // ??????????????????????????? null
        return Null;
    }

    @Override // var + fun
    public Value visit(@NotNull FunDef fun, @NotNull Scope s) {
        if (fun.extend) {
            return extFun(fun, s);
        } else {
            return defFun(fun, s);
        }
    }

    // ?????? ?????????????????? extends ?????????????????????????????????????????????????????????????????????
    Scope funScope(FunDef fd, Scope s, Value thisObject) {
//        boolean isMethod = thisObject != null;
//        if (isMethod) {
//            Value.ClassValue cv = thisObject.type(fd.loc);
//            if (cv instanceof UserDefClass) {
//                UserDefClass udc = (UserDefClass) cv;
//                if (udc.extendsScope != null) {
//                    return udc.extendsScope.getOrDefault(fd, s);
//                }
//            }
//        }
        return s;
    }
    @Nullable Value.ClassValue getFunDefClass(FunDef fd, Value this_) {
        if (fd.className == null) {
            return null;
        } else {
            if (this_ == null) {
                throw Error.bug(fd.loc);
            }
            Value v = this_.asObject(fd.loc).getProperty(fd.loc, Key_Class);
            if (v instanceof Value.ClassValue) {
                return ((Value.ClassValue) v);
            } else {
                throw Error.bug(fd.loc);
            }
        }
    }
    FunValue defFun(FunDef fd, Scope s) {
        // s ?????? this/super ????????? circle ref... ?????? method ?????? java lambda ???????????????, ??????
        Value this_ = s.lookupLocal(TokenType.THIS.name);
        Value super_ = s.lookupLocal(TokenType.SUPER.name);

        // ?????? Scope ?????????:
        // 1. ??????????????? scope ????????????
        //      ??????      ext-method-scope -> object-scope -> ...parent-object-scope -> ext-method-def-scope
        //      ????????????????????? method-scope -> object-scope -> ...parent-object-scope -> class-def-scope
        //      ?????????????????? ext-method-scope -> ext-method-def-scope, object-scope ???????????? this super ?????????
        // 2. ??????????????? bind, callFun scope ?????? s1, ?????? s == s1, ?????? bind ?????? s1 ?????? s, ???????????? this

        // ???????????? addExtendsMethod ????????? methods ??????????????????????????????????????? scope ?????????????????????????????????????????????????????????????????????????????????
        // ??????????????? addExtendsMethod ????????? methods ???????????????????????????????????????????????????????????????????????????
        // Scope fs = s;
        FunMeta fm = funMeta(fd/*, getFunDefClass(fd, this_)*/);
        Scope fs = funScope(fd, s, this_);
        Applicative apply = (loc, s1, args) -> callFun(loc, fm, fd, args, s1, this_, super_);
        FunValue fv = UserFun(fm, fs, apply);

        if (fd.symbol) {
            // parser ????????? symbol ?????? class ??? ?????? extend
            if (this_ != null) {
                Value.ClassValue classValue = this_.type(fd.loc);
                classValue.setSymbol(fd.loc, ((SymbolValue) interp(fd.name, s)), fv);
            } else if (!fd.extend) {
                throw Error.bug(fd.loc);
            }
        } else {
            if (!fd.extend) {
                // ??????????????????????????? define-name?????????????????????????????????
                // ????????????????????????????????????????????????????????? rename
                // ????????? fun ??? method, ????????? define, fun?????? env ??? def, method ??? objectScope ?????????
                // fun ??? method ??????????????????, mut = true
                s.define(fd.loc, ((Id) fd.name).name, fv,true);
            }
        }
        return fv;
    }

    // todo ?????? ext ?????????????????????????????????
    Value extFun(FunDef fd, Scope s) {
        assert fd.className != null;
        Value.ClassValue classValue = s.lookupClass(fd.loc, fd.className.name);
        if (classValue == null) {
            throw classNotFound(fd.className.loc, fd.className.name);
        }
        // ??????????????? scope ????????????
        //      ??????      ext-method-scope -> object-scope -> ...parent-object-scope -> ext-method-def-scope
        //      ????????????????????? method-scope -> object-scope -> ...parent-object-scope -> class-def-scope
        //      ?????????????????? ext-method-scope -> ext-method-def-scope, object-scope ???????????? this super ?????????
        // 1. ???????????? class ?????? defFun ????????? ?????? This Super ????????? Object-Scope,????????????????????? extendsScope, defFun???????????????
        // 2. ????????????????????? class ?????????????????????????????? defFun
        // ?????????????????? ?????? classValue.set** ,??????????????? userDefClass???????????????????????? ????????????
//        if (classValue instanceof UserDefClass) {
//            // ????????? ext, ???????????? classDef??????????????????
//            FunDef nonExtend = funDef(fd.loc, fd.className, fd.name, fd.params, fd.body, fd.arrow, fd.symbol, false);
//            ((UserDefClass) classValue).addExtendsMethod(nonExtend, s);
//        } else {
            // ?????????????????????????????????????????????????????????????????? Scope ?????????
            FunValue fv = defFun(fd, s);
            if (fd.symbol) {
                SymbolValue sv = (SymbolValue) interp(fd.name, s);
                classValue.setSymbol(fd.loc, sv, fv);
            } else {
                classValue.setProperty(fd.loc, ((Id) fd.name).name, fv);
            }
//        }
        return Null;
    }

    @Override
    public Value visit(@NotNull ClassDef cd, @NotNull Scope s) {
        UserDefClass c = defClass(cd, s);
        s.define(cd.loc, cd.name.name, c,true);
        return c;
    }

    // ClassDef ???????????? class Object, ?????? Object ????????????, ?????? @NotNull
    @NotNull Value.ClassValue getParent(ClassDef cd, Scope env) {
        Id parent = cd.parent;
        if (parent == null) {
            return ClassObject; // ???????????? Object
        } else {
            Value.ClassValue parentCv = env.lookupClass(cd.loc, parent.name);
            if (parentCv == null) {
                throw classNotFound(parent.loc, parent.name);
            }
            return parentCv;
        }
    }

    VarMeta varMeta(VarDef vd) {
        return Value.varMeta(vd.id.name, vd.mut, env -> interp(vd, env));
    }
    FunMeta funMeta(FunDef fd) {
        return Value.funMeta(
                fd.stringName(),
                fd.className == null ? null : fd.className.name,
                fd.params.keySet().stream().map(it -> it.name).collect(toList()),
                fd.params.values().stream().map(it -> it == null ? Null : Literals.parse(it)).collect(toList()),
                isCtor(fd),
                fd.symbol,
                fd.extend,
                fd.arrow,
                generatedSuperCall(fd),
                requiredParametersSize(fd),
                env -> interp(fd, env)
        );
    }
    ClassMeta classMeta(ClassDef cd, @NotNull Value.ClassValue parent) {
        return Value.classMeta(
                cd.name.name,
                parent,
                cd.props.stream().map(this::varMeta).collect(toList()),
                cd.methods.stream().map(this::funMeta).collect(toList()),
                funMeta(cd.ctor),
                cd.sealed,
                cd.tagged,
                isThrowable(cd)
        );
    }

    boolean isCtor(FunDef fd) { return fd.className != null && Fun_Construct.equals(fd.stringName()); }
    boolean isThrowable(ClassDef cd) {
        return cd.loc.si.src.equals(Interp.bootstrap.toString()) && cd.name.name.equals(Constant.Class_Throwable);
    }
    boolean generatedSuperCall(FunDef ctor) {
        if (!isCtor(ctor)) {
            return false;
        }
        if (ctor.body.stmts.isEmpty()) {
            return false;
        }
        Expr expr = ctor.body.stmts.get(0);
        if (expr instanceof Call) {
            Call c = ((Call) expr);
            // parser ?????????????????????
            boolean generated = c.loc == None;
            // ??????????????? parser ???????????? symbolTable ??????, ????????????????????????????????????????????????
            // ??????????????????????????????
            return generated && c.args.isEmpty();
        }
        return false;
    }
    int requiredParametersSize(Ast.FunDef fd) {
        int n = 0;
        for (Literal value : fd.params.values()) {
            if (value == null) {
                n++;
            } else {
                break;
            }
        }
        return n;
    }

    UserDefClass defClass(ClassDef cd, Scope s) {
        Value.ClassValue parentCv = getParent(cd, s);
        ClassMeta cm = classMeta(cd, parentCv);
        checkClass(cd.loc, cd.parent == null ? cd.name.loc : cd.parent.loc, cm, parentCv);
        return UserClass(cm, s, self -> (loc, s1, args) -> newInstance(loc, self, cm.ctor, cd.ctor, args));
    }

    void checkClass(Location loc, Location extLoc, ClassMeta cm, Value.ClassValue parentCv) {
        ClassMeta parentCm = parentCv.meta();
        checkCtorSuperCall(loc, cm, parentCm);
        checkSealedExtends(loc, extLoc, cm, parentCm);

    }
    void checkSealedExtends(Location loc, Location extLoc, ClassMeta cm, ClassMeta parentCm) {
        if (parentCm.sealed) {
            throw Error.runtime(extLoc, "sealed " + parentCm.name + " ???????????????");
        }
    }
    // ??????????????? parser ???????????? symbolTable ??????, ????????????????????????????????????????????????
    void checkCtorSuperCall(Location loc, ClassMeta self, ClassMeta parent) {
        // ?????????????????????????????? < ??????ctor??????????????????
        if (self.ctor.generatedSuperCall && parent.ctor.requiredParamSz > 0) {
            throw Error.syntax(loc, "????????????????????????????????????????????????????????????");
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    /*
    PatternInterpreter patternInterp = new PatternInterpreter();
    PatternMatcher patternMatcher = new PatternMatcher();

    @Override
    public Value visit(@NotNull Match match, @NotNull Scope s) {
        Value val = interp(match.value, s);
        // ????????????????????? ast, call destruct()
        val = val.asObject(match.loc).destruct(match.loc);

        for (MatchCase mc : match.cases) {
            Pattern ptn = mc.pattern;
            ptn = patternInterp.match(ptn, s);
            if (patternMatcher.match(ptn, val)) {
                Scope s1 = s.inherit();
                s1.tryDefine(mc.loc, ptn, val, true); // ???????????? pattern ?????? define ???...
                if (mc.guard == null) {
                    return stmts(mc.body.stmts, s1); // interp Match Then
                } else {
                    if (interp(mc.guard, s1).asBool(mc.guard.loc).val) {
                        return stmts(mc.body.stmts, s1); // interp Match Then
                    }
                }
            }
        }
        throw error(match.loc, MatchErrorClass, "?????????????????? case");
    }

    class PatternMatcher implements Matcher<Value, Boolean> {
        @Override public Boolean match(@NotNull Id ptn, @NotNull Value v) {
            return true;
        }
        @Override public Boolean match(@NotNull Literal ptn, @NotNull Value v) {
            // ???????????????
            return eq1(ptn.loc, ptn.value, v, false, new HashSet<>());
        }
        @Override public Boolean match(@NotNull Ast.ListPattern ptn, @NotNull Value v) {
            IteratorValue iter = v.asObject(ptn.loc).iterator(ptn.loc);
            if (iter == null) {
                return false;
            }
            for (Pattern ptn1 : ptn.elems) {
                if (iter.hasNext()) {
                    if (!match(ptn1, iter.next())) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            // pattern ??? iterable ??????????????????
            return !iter.hasNext();
        }
        @Override public Boolean match(@NotNull Value.MapPatternValue ptn, @NotNull Value v) {
            // ????????????????????? map(?????????) ??? ?????????????????????
            if (!(v instanceof MapValue) && !(v instanceof ScopeObject)) {
                return false;
            }
            for (Map.Entry<Value, Pattern> it : ptn.props.entrySet()) {
                Value key = it.getKey();
                Pattern ptn1 = it.getValue();
                if (v instanceof MapValue) {
                    if (!match(ptn1, mapMemberGet(ptn.loc, ptn.loc, ptn.loc, v, key))) {
                        return false;
                    }
                } else //noinspection ConstantConditions
                    if(v instanceof ScopeObject) {
                    if (!match(ptn1, objectMemberGet(ptn.loc, ptn.loc, ptn.loc, v, key))) {
                        return false;
                    }
                } else {
                    throw Error.bug(ptn.loc);
                }
            }
            return true;
        }
        @Override public Boolean match(@NotNull Ast.MapPattern ptn, @NotNull Value v) {
            throw Error.syntax(ptn.loc, "???????????? pattern: " + ptn);
        }
        FunDef getConstruct(Location loc, UserDefClass udc) {
//            ObjectValue ov = udc.get(loc, SymbolApply);
//            if (ov instanceof UserDefFun) { // ?????? UserDefFun ??????
//                return ((UserDefFun) ov).fun;
//            } else {
                return udc.cls.ctor;
//            }
        }
        @Override public Boolean match(@NotNull UnApplyPatternValue ptn, @NotNull Value v) {
            // ?????? parser ?????????????????????????????????????????????????????????...
            if (!(ptn.classValue instanceof UserDefClass)) {
                throw Error.runtime(ptn.loc, "?????????????????????????????? class");
            }
            UserDefClass udv = (UserDefClass) ptn.classValue;
            if (!v.is(ptn.loc, ptn.classValue)) {
                return false;
            }
            List<Pattern> props = ptn.props;
            FunDef construct = getConstruct(ptn.loc, udv);
            List<Id> params = construct.params;
            if (props.size() != params.size()) {
                return false;
            }
            ObjectValue obj = v.asObject(ptn.loc);
            for (int i = 0; i < props.size(); i++) {
                if (!match(props.get(i), obj.get(ptn.loc, params.get(i).name))) {
                    return false;
                }
            }
            return true;
        }
    }

    class PatternInterpreter implements Matcher<Scope, Pattern> {
        @Override public Pattern match(@NotNull Id ptn, @NotNull Scope s) { return ptn; }
        @Override public Pattern match(@NotNull Literal ptn, @NotNull Scope s) { return ptn; }
        @Override public Pattern match(@NotNull Ast.ListPattern ptn, @NotNull Scope s) {
            List<Pattern> elems = new ArrayList<>();
            for (Pattern ptn1 : ptn.elems) {
                Pattern elem1 = match(ptn1, s);
                elems.add(elem1);
            }
            return Ast.listPattern(ptn.loc, elems);
        }

        @Override public Pattern match(@NotNull Ast.MapPattern ptn, @NotNull Scope s) {
            Map<Value, Ast.Pattern> props = new LinkedHashMap<>();
            for (Entry<Expr, Pattern> it : ptn.props.entrySet()) {
                Value key = interp(it.getKey(), s);
                Pattern value = match(it.getValue(), s);
                props.put(key, value);
            }
            return new MapPatternValue(ptn.loc, props);
        }

        @Override public Pattern match(@NotNull UnApplyPattern ptn, @NotNull Scope s) {
            Value.ClassValue cv = s.lookupClass(ptn.name);
            if (cv == null) {
                throw Error.runtime(ptn.name.loc, "??? " + ptn.name + " ?????????");
            }
            List<Pattern> ptns = new ArrayList<>(ptn.props.size());
            for (Pattern sub : ptn.props) {
                ptns.add(match(sub, s));
            }
            return new UnApplyPatternValue(ptn.loc, cv, ptns);
        }

        // hacker for map-key
        @Override public Pattern match(@NotNull ExprPattern ptn, @NotNull Scope s) {
            Expr expr = ptn.expr;
            Value v = interp(expr, s);
            return Ast.literal(expr.loc, v);
        }
    }
    */
    /*Pattern interpPattern(Pattern ptn, Scope s) {
        if (ptn instanceof Id) {
            return ptn;
        }
        else if (ptn instanceof Literal) {
            return ptn;
        }
        else if (ptn instanceof ListPattern) {
            List<Pattern> elems = new ArrayList<>();
            for (Pattern elem : ((ListPattern) ptn).elems) {
                if (PatternMatcher.isWildcards(elem)) {
                    elems.add(null);
                } else {
                    Pattern elem1 = interpPattern(elem, s);
                    elems.add(elem1);
                }
            }
            return Ast.ListPattern(((ListPattern) ptn).loc, elems);
        } else if (ptn instanceof MapPattern) {
            Map<Value, Ast.Pattern> props = new LinkedHashMap<>();
            for (Entry<Expr, Pattern> it : ((MapPattern) ptn).props.entrySet()) {
                Value key = interp(it.getKey(), s);
                Pattern value = interpPattern(it.getValue(), s);
                props.put(key, value);
            }
            return new MapPatternValue(props);
        } else {
            throw Error.syntax(((Node) ptn).loc, "???????????? pattern");
        }
    }*/

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    @Override
    public Value visit(@NotNull Ast.Continue cont, @NotNull Scope s) {
        throw ControlFlow.Continue();
    }

    @Override
    public Value visit(@NotNull Ast.Break brk, @NotNull Scope s) {
        throw ControlFlow.Break();
    }

    @Override
    public Value visit(@NotNull Ast.Return ret, @NotNull Scope s) {
        throw ControlFlow.Return(interp(ret.arg, s));
    }

    @Override
    public Value visit(@NotNull Ast.Throw throw1, @NotNull Scope s) {
        throw throw1(throw1.loc, interp(throw1.expr, s));
    }

    @Override
    public Value visit(@NotNull Try try1, @NotNull Scope s) {
        try {
            return interp(try1.try1, s);
        } catch (ControlFlow.Throw e) {
            if (try1.catch1 == null) {
                throw e;
            }
            Scope s1 = s.inherit();

            // 1> ???????????? catch
            // s1.define(try1.error.loc, try1.error.name, e.value, true);
            // return stmts(try1.catch1.stmts, s1);

            // 2> match ??????
            // s1.define(try1.catch1.value.loc, ((Id) try1.catch1.value).name, e.value, true);

            // 3> match desugar ??????
            assert try1.error != null;
            s1.define(try1.error.loc, try1.error, e.value, true);

            try {
                return interp(try1.catch1, s1);
            } catch (ControlFlow.Throw t) {
                if (t.value.is(try1.loc, Class_MatchError)) {
                    // !!!! case ???????????? ??????????????? e...
                    // ????????? case _ -> throw e, ?????????????????? rewrite ast ??????????????????
                    throw e;
                }
                throw t;
            }
        } finally {
            try {
                interp(try1.final1, s);
            } catch (ControlFlow.Return e) {
                //noinspection ThrowFromFinallyBlock
                throw Error.syntax(try1.final1.loc, "finally ???????????? return ");
            }
        }
    }

    @Override
    public Value visit(@NotNull If if1, @NotNull Scope s) {
        if (interp(if1.test, s).asBool(if1.test.loc).val) {
            return interp(if1.then, s);
        } else {
            if (if1.orelse == null) {
                return Null;
            } else {
                return interp(if1.orelse, s);
            }
        }
    }

//    @Override
//    public Value visit(@NotNull Ast.For fori, @NotNull Scope s) {
//        boolean hasInit = !(fori.init instanceof Empty);
//        boolean hasUpdate = !(fori.update instanceof Empty);
//
//        Value v = Null;
//        s = s.inherit();
//        try {
//            inLoop++;
//            if (hasInit) {
//                interp(fori.init, s);
//            }
//            while (interp(fori.test, s).asBool(fori.test.loc).val) {
//                try {
//                    v = interp(fori.body, s);
//                } catch (ControlFlow.Break brk) {
//                    v = brk.value;
//                    // !!! break-symbol ???????????????????????????~
//                    if (brk.value instanceof SymbolValue) {
//                        throw brk;
//                    } else {
//                        break;
//                    }
//                } catch (ControlFlow.Continue ignore) { }
//                if (hasUpdate) {
//                    interp(fori.update, s);
//                }
//            }
//        } finally {
//            inLoop--;
//        }
//        return v;
//    }

    @Override
    public Value visit(@NotNull While while1, @NotNull Scope s) {
        Value v = Null;
        while (interp(while1.test, s).asBool(while1.test.loc).val) {
            try {
                v = interp(while1.body, s.inherit());
            } catch (ControlFlow.Break brk) {
                break;
            } catch (ControlFlow.Continue ignore) { }
        }
        return v;
    }


    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    @Override
    public Value visit(@NotNull New new1, @NotNull Scope s) {
        Value.ClassValue cv = s.lookupClass(new1.loc, new1.name.name);
        if (cv == null) {
            throw classNotFound(new1.loc, new1.name.name);
        }
        Value[] args = interpArgs(new1.args, s);
        checkRequiredArgs(new1.loc, args, cv.meta().ctor);
        return cv.newInstance(new1.loc, args);
    }

    @Override
    public Value visit(@NotNull Call call, @NotNull Scope s) {
        boolean pushed = false;
        try {
            Value callee = interp(call.callee, s);
            Value[] args = interpArgs(call.args, s);
            // FunApply bootstrap.j???????????? null, ?????????????????? booted()
            boolean callApply = booted() && callee.equals(Fun_Apply);
            if (callApply) {
                // hack!!!  f.apply([...args]) ?????????????????????  f(...args)
                Value thisFun = ((StdFun) callee).env.lookupLocal(TokenType.THIS.name);
                Expect.notNull(call.loc, thisFun, "this ??????");
                Value[] args1 = Expect.as(call.loc, args[0], ListValue.class).list.toArray(new Value[0]);
                stack.push(new Frame(call, thisFun, args1, s));
            } else {
                stack.push(new Frame(call, callee, args, s));
            }
            pushed = true;
            return callee.asApply(call.loc).apply(call.loc, s, args);
        } finally {
            if (pushed) {
                stack.pop();
            }
        }
    }
    // ?????????????????????????????????????????????????????????????????????????????? defmacro
    Value[] interpArgs(List<Expr> argExprs, Scope s) {
        Value[] args = new Value[argExprs.size()];
        for (int i = 0; i < argExprs.size(); i++) {
            args[i] = interp(argExprs.get(i), s);
        }
        return args;
    }
    Value callFun(Location loc, FunMeta fm, FunDef fd, Value[] args, Scope funDefEnv,
                  @Nullable Value thisObject, @Nullable Value superObject) {
        // ?????? fun ???????????? Scope?????? fun ????????????
        // <1> ?????? fun ???, scope ??? fun ??????????????? scope
        // <2> ctor ???, scope ??? instanceScope (A->B->C : a->b->c->a-def-scope)
        // <3> method ???, scope ??? instanceScope (A->B->C : a->b->c->a-def-scope)
        //      call-ctor interp(method, instanceScope)
        //      method ???????????? closure, ????????? env ??? instanceScope
        // ?????? <2><3>buildFunScope ??? scope ??????????????? this ????????????

        checkRequiredArgs(loc, args, fm);
        Scope funScope = buildFunScope(loc, fd.params, args, funDefEnv, thisObject, superObject);
        try {
            return stmts(fd.body.stmts, funScope);
        } catch (ControlFlow.Return ret) {
            return ret.val;
        }
    }
    void checkRequiredArgs(Location loc, Value[] args, FunMeta fm) {
        if (args.length < fm.requiredParamSz) {
            throw callError(loc, String.format("???????????? %d ?????????, ???????????? %d ?????????", fm.requiredParamSz, args.length));
        }
    }


    Scope buildFunScope(Location loc, LinkedHashMap<Id, Literal> params, Value[] args, Scope env,
                        @Nullable Value thisObject, @Nullable Value superObject) {
        // ??????????????????:
        // 1. ??????????????? arguments ?????? ?????? block ?????????
        // 2. this ?????? callFun <1><2><3>
        // 3. ????????????????????????????????? null
        Scope funScope = env.inherit();

        Value bindedThis = env.lookupLocal(TokenType.THIS.name);
        if (bindedThis == null) {
            if (thisObject != null) {
                funScope.define(loc, TokenType.THIS.name, thisObject, false);
                if (superObject != null) {
                    funScope.define(loc, TokenType.SUPER.name, superObject, false);
                }
            }
        }

        funScope.define(loc, TokenType.ARGUMENTS.name, List(args), false);

        // todo ?????????????????????

        int i = 0;
        for (Entry<Id, Literal> it : params.entrySet()) {
            Id param = it.getKey();
            if (i < args.length) {
                funScope.define(param.loc, param.name, args[i], true);
            } else {
                Value defVal = it.getValue() == null ? Null : interp(it.getValue(), Scope.Immutable);
                funScope.define(param.loc, param.name, defVal, true);
            }
            i++;
        }
        return funScope;
    }

    // new Object
    // new :: Call Construct ?????? interp ClassDef ?????????
    // interp ClassDef ???????????? Class ???????????????????????????????????????
    ScopeObject newInstance(Location loc, UserDefClass udc, FunMeta ctorFm, FunDef ctorFd, Value[] args) {
        // checkClass(udc);
        Scope objectScope = newInstance(loc, udc, udc.env);
        objectScope.setRootParent(udc.env);

        ScopeObject thisObject = ScopeObject(udc, objectScope);
        ObjectValue superObject = ScopeObject.superObject(udc, objectScope);

        // call-ctor
        callFun(ctorFd.loc, ctorFm, ctorFd, args, objectScope, thisObject, superObject);
        checkUninitializedProp(loc, objectScope);
        return thisObject;
    }
    // <6> ???????????? val ??????????????????
    void checkUninitializedProp(Location loc, Scope obj) {
        Set<Entry<String, Scope.Def>> pairs = obj.table.entrySet();
        for (Entry<String, Scope.Def> pair : pairs) {
            if (pair.getValue().val == Undefined) {
                throw Error.runtime(loc,  "?????? " + pair.getKey() + " ????????????");
            }
        }
    }

    /*
    {
        // scope ?????????????????????

        // scope-1 (A??????????????????, ??????????????????)
        val v = 1
        class A {
            // scope-2 (getValue ??????????????????)
            fun getValue() = v // scope-3 (A.getValue ????????? block ??????)
        }

        // scope-1
        val class_b = {
            // scope-4 (B??????????????????, ??????????????????)
            val v = 42
            class B extends A {
                // scope-5
                fun getValue() = v // scope-6 (B.getValue ????????? block ??????)
                fun getSuperValue() = super.getValue()
            }
        }

        val b = class_b() // ???????????? ClassValue ??? `apply` ??? new ??????

        // b ??? scope ?????? 5 -> 2 -> 4 -> 1
        //      [5(b) -> 2(a)] -> [4(B????????????) -> 1]
        // super ??? a ??? scope ?????? 2 -> 1

        assert b.getValue() == 42
        // B.getValue() v ??? lookup ?????? 6 -> 5 -> 2 -> 4
        // B.getSuperValue() super lookupLocal ?????? a
        //      A.getValue() v ??? lookup ?????? 2 -> 1

        assert b.getSuperValue() == 1
    }
     */
    // ???? class-scope ??????
    Scope newInstance(Location loc, UserDefClass udc, Scope clsDefScope) {
        Scope selfScope;
        ClassMeta cm = udc.meta();
        assert cm.parent != null;
        if (cm.parent == ClassObject) {
            selfScope = Scope.create(); // ????????????
        } else {
            // ??????????????? new ??????????????????????????????????????????????????????????????????????????????????????????
            // ?????? defClass ?????????????????????????????????
            // ?????????????????? class ????????????, ???????????????????????????????????????
            // Value.ClassValue parentCv = clsDefScope.lookupClass(loc, cm.parent.name);
            assert cm.parent instanceof UserDefClass;
            // newParentInstance
            Scope parentScope = newParentInstance(loc, cm.parent);
            selfScope = parentScope.inherit();
        }
        // ?????? scope ?????? class, ???????????? ScopeObject.defineClass()
        // selfScope.define(None, Constant.Key_Class, udc, false);
        initClass(loc, udc, clsDefScope, selfScope);
        fillStackTraceIfNecessary(cm, selfScope);
        return selfScope;
    }
    Scope newParentInstance(Location loc, Value.ClassValue parentCls) {
        if (parentCls instanceof UserDefClass) {
            UserDefClass parent = (UserDefClass) parentCls;
            // ?????? parent.env ???????????????????????????????????????????????????
            return newInstance(loc, parent, parent.env);
        } else {
            throw Error.bug(loc);
        }
    }
    void fillStackTraceIfNecessary(ClassMeta cm, Scope selfScope) {
        // !!! hack ????????? new ??????????????????
        if (cm.throwable) {
            selfScope.define(None, Field_Throwable_StackTrace, stack.backtrace(), false);
        }
    }
    // ?????????????????????scope????????????????????????????????????scope(env)??????
    // ???????????????????????? classChainScope -> ClassDefScope ????
    void initClass(Location loc, UserDefClass udc, Scope clsDefScope, Scope selfScope) {
        // !!!??????????????????????????????????????????env?????????scope?????????setRootParent???????????????????????????????????????
        Scope env = selfScope.copy();
        env.setRootParent(clsDefScope);
        ClassMeta cm = udc.meta();

        for (VarMeta prop : cm.props) {
            prop.defVar.accept(env);
        }

        // ???!!! ???????????????????????????, ?????????????????????????????????
        // so??????????????? scope ?????? this ??? super ??? method ???????????????, ?????? unset ???
        // @see FunValue defFun()
        bindThisSuper(loc, udc, env);
        for (FunMeta method : cm.methods) {
            method.defFun.accept(env);
        }
        unbindThisSuper(loc, udc, env);
    }

    void bindThisSuper(Location loc, UserDefClass udc, Scope env) {
        ScopeObject thisObject = ScopeObject(udc, env);
        ObjectValue superObject = ScopeObject.superObject(udc, env);

        env.define(loc, TokenType.THIS.name, thisObject, false);
        env.define(loc, TokenType.SUPER.name, superObject, false);
    }
    void unbindThisSuper(Location loc, UserDefClass udc, Scope env) {
        env.unset(loc, TokenType.THIS.name);
        env.unset(loc, TokenType.SUPER.name);
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    @Override
    public Value visit(@NotNull Assign assign, @NotNull Scope s) {
        return assign(assign, s);
    }
    @Override
    public Value visit(@NotNull Subscript sub, @NotNull Scope s) {
        return subscriptGet(sub.loc, sub.oploc, sub.object, sub.idxKey, s);
    }
    @Override
    public Value visit(@NotNull ObjectMember om, @NotNull Scope s) {
        return objectMemberGet(om.loc, om.oploc, om.object, om.prop, s);
    }

    Value assign(Assign assign, Scope s) {
        Expr lhs = assign.lhs;
        Value v = interp(assign.rhs, s);
        if (lhs instanceof Id) {
            s.assign(lhs.loc, ((Id) lhs), v);
        }
//        else if (lhs instanceof Pattern) {
//            Pattern pattern = patternInterp.match(((Pattern) lhs), s);
//            s.assign(lhs.loc, pattern, v);
//        }
        else if (lhs instanceof Subscript) {
            Subscript subs = (Subscript) lhs;
            subscriptSet(lhs.loc, lhs.oploc/*assign.loc*/, subs.object, subs.idxKey, v, s);
        } else if (lhs instanceof ObjectMember) {
            ObjectMember om = (ObjectMember) lhs;
            objectMemberSet(lhs.loc, lhs.oploc/*assign.loc*/, om.object, om.prop, v, s);
        } else {
            throw Error.runtime(lhs.loc, "?????????????????????");
        }
        // ?????????????????? null
        return Null;
    }
    Value subscriptGet(Location loc, Location opLoc, Expr obj, Expr idxKey, Scope s) {
        Value o = interp(obj, s);
        Value k = interp(idxKey, s);
        return subscriptGet(loc, opLoc, idxKey.loc, o, k);
    }
    Value subscriptGet(Location loc, Location opLoc, Location keyLoc, Value o, Value k) {
        if (k instanceof SymbolValue) {
            return objectSymbolGet(loc, opLoc, keyLoc, o, k);
        }
        if (o instanceof ListValue) {
            return listMemberGet(loc, opLoc, keyLoc, o, k);
        } else if (o instanceof MapValue) {
            return mapMemberGet(loc, opLoc, keyLoc, o, k);
        } else if (o instanceof StringValue && k instanceof IntValue) {
            return ((StringValue) o).get(loc, Math.toIntExact(((IntValue) k).val));
        } else {
            return objectMemberGet(loc, opLoc, keyLoc, o, k);
        }
    }
    Value subscriptSet(Location loc, Location opLoc, Expr lstMap, Expr idxKey, Value v, Scope s) {
        Value l = interp(lstMap, s);
        Value k = interp(idxKey, s);
        if (k instanceof SymbolValue) {
            return objectSymbolSet(loc, opLoc, idxKey.loc, l, k, v);
        }
        if (l instanceof ListValue) {
            return listMemberSet(loc, opLoc, idxKey.loc, l, k, v);
        } else if (l instanceof MapValue) {
            return mapMemberSet(lstMap.loc, opLoc, l, k, v);
        } else {
            return objectMemberSet(loc, opLoc, idxKey.loc, l, k, v);
        }
    }

    Value objectMemberGet(Location loc, Location opLoc, Value v, String prop) {
        v = v.asObject(loc).getProperty(loc, prop);
        return v == null ? Null : v;
    }
    Value objectMemberGet(Location loc, Location opLoc, Expr object, Id prop, Scope s) {
        return objectMemberGet(loc, opLoc, interp(object, s), prop.name);
    }
    Value objectMemberGet(Location loc, Location opLoc, Location keyLoc, Value obj, Value prop) {
        if (prop instanceof SymbolValue) {
            return objectSymbolGet(loc, opLoc, loc, obj, prop);
        } else {
            return objectMemberGet(loc, opLoc, obj, prop.asString(keyLoc).val);
        }
    }
    Value objectMemberSet(Location loc, Location opLoc, Value obj, String prop, Value val) {
        obj.asObject(loc).setProperty(opLoc, prop, val);
        return val;
    }
    Value objectMemberSet(Location loc, Location opLoc, Expr object, Id prop, Value val, Scope s) {
        return objectMemberSet(loc, opLoc, interp(object, s), prop.name, val);
    }
    Value objectMemberSet(Location loc, Location opLoc, Location propLoc, Value obj, Value prop, Value val) {
        return objectMemberSet(loc, opLoc, obj, prop.asString(propLoc).val, val);
    }
    Value objectSymbolGet(Location loc, Location opLoc, Location keyLoc, Value obj, Value prop) {
        return obj.asObject(loc).getSymbol(opLoc, prop.asSymbol(keyLoc));
    }
    Value objectSymbolSet(Location loc, Location opLoc, Location keyLoc, Value obj, Value prop, Value val) {
        obj.asObject(loc).setSymbol(opLoc, prop.asSymbol(keyLoc), val.asObject(loc));
        return val;
    }

    Value mapMemberGet(Location loc, Location opLoc, Location keyLoc, Value v, Value key) {
        return v.asMap(loc).get(key);
    }
    Value mapMemberSet(Location loc, Location opLoc, Value v, Value key, Value val) {
        v.asMap(loc).put(key, val);
        return val;
    }
    Value listMemberSet(Location loc, Location opLoc, Location idxLoc, Value l, Value idx, Value val) {
        l.asList(loc).set(opLoc, Math.toIntExact(idx.asInt(idxLoc).val), val);
        return val;
    }
    Value listMemberGet(Location loc, Location opLoc, Location keyLoc, Value l, Value idx) {
        if (idx instanceof NumValue) {
            return l.asList(loc).get(opLoc, Math.toIntExact(idx.asInt(keyLoc).val));
        } else {
            // pattern ????????? ?????????????????? (a is Map || a.class is Class) && 1 == a["key"]
            // ?????? a ??? [1,2],   a['key'] ???????????????
            return Null; // todo!!!
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    @Override
    public Value visit(@NotNull Binary binary, @NotNull Scope s) {
        switch (binary.op) {
            case EQ: return eq(binary, s, false);
            case STRICT_EQ: return eq(binary, s, true);
            case NE: return eq(binary, s, false).inverse();
            case STRICT_NE: return eq(binary, s, true).inverse();
            
            case GT: return gt(binary, s);
            case LT: return lt(binary, s);
            case LE: return le(binary, s);
            case GE: return ge(binary, s);

            case MUL: return multiply(binary, s);
            case DIV: return divide(binary, s);
            case MOD: return module(binary, s);
            case PLUS: return plus(binary, s);
            case MINUS: return minus(binary, s);
            case POWER: return power(binary, s);

            case LOGIC_AND: return strictLogicalAnd(binary, s);
            case LOGIC_OR: return strictLogicalOr(binary, s);
            case IS: return is(binary, s);

            case LT_LT: return ltlt(binary, s);
            case GT_GT: return gtgt(binary, s);
            case GT_GT_GT: return gtgtgt(binary, s);

            case BIT_OR: return bitwiseOr(binary, s);
            case BIT_XOR: return bitwiseXor(binary, s);
            case BIT_AND: return bitwiseAnd(binary, s);

            // case IN:
            // case LT_LT_LT:
            // case EXCLUSIVE_RANGE:
            default:
                return binaryOperator(binary, interp(binary.lhs, s), s, interp(binary.rhs, s));
        }
    }
    // todo ?????? strict
    // !!! ?????? eq ?????????????????????????????? equals ??????...scope ??????????????? eq parent
    BoolValue eq(Binary expr, Scope s, boolean strict) {
        Value v1 = interp(expr.lhs, s);
        Value v2 = interp(expr.rhs, s);
        return Bool(eq1(expr.loc, v1, v2, s, strict, new HashSet<>()));
    }
    boolean eq1(Location loc, Value v1, Value v2, Scope s, boolean strict, Set<Value> set) {
        if (set.contains(v1) || set.contains(v2)) {
            throw Error.runtime(loc, "???????????????????????????????????????");
        } else {
            set.add(v1);
            set.add(v2);
        }
        if (v1 instanceof NullValue && v2 instanceof NullValue) {
            return true;
        }
        if (v1 instanceof NullValue) {
            return false;
        }
        if (v1 instanceof BoolValue && v2 instanceof BoolValue) {
            return v1 == v2;
        }
        if (v1 instanceof SymbolValue && v2 instanceof SymbolValue) {
            return v1 == v2;
        }
        if (!strict && v1 instanceof FloatValue && v2 instanceof IntValue) {
            return ((FloatValue) v1).val == ((IntValue) v2).val;
        }
        if (!strict && v1 instanceof IntValue && v2 instanceof FloatValue) {
            return ((IntValue) v1).val == ((FloatValue) v2).val;
        }

        if (v1 instanceof IntValue && v2 instanceof IntValue) {
            return ((IntValue) v1).val == ((IntValue) v2).val;
        }
        if (v1 instanceof FloatValue && v2 instanceof FloatValue) {
            return ((FloatValue) v1).val == ((FloatValue) v2).val;
        }
        if (v1 instanceof StringValue && v2 instanceof StringValue) {
            return Objects.equals(((StringValue) v1).val, ((StringValue) v2).val);
        }
//        if (v1 instanceof ListValue && v2 instanceof ListValue) {
//            return eqList(loc, ((ListValue) v1), ((ListValue) v2), s, strict, set);
//        }
//        if (v1 instanceof MapValue && v2 instanceof MapValue) {
//            return eqMap(loc, (MapValue) v1, (MapValue) v2, s, strict, set);
//        }
//        if (v1 instanceof ScopeObject && v2 instanceof ScopeObject) {
//            return eqScopeObject(loc, (ScopeObject) v1, (ScopeObject) v2, s, strict, set);
//        }
        return symbolApply(loc, v1, s, TokenType.EQ.name, v2).asBool(loc).val;
    }
//    boolean eqList(Location loc, ListValue lst1, ListValue lst2, Scope s, boolean strict, Set<Value> set) {
//        if (lst1.size() != lst2.size()) {
//            return false;
//        }
//        for (int i = 0; i < lst1.size(); i++) {
//            if (!eq1(loc, lst1.get(loc, i), lst2.get(loc, i), s, strict, set)) {
//                return false;
//            }
//        }
//        return true;
//    }
//    boolean eqMap(Location loc, MapValue m1, MapValue m2, Scope s, boolean strict, Set<Value> set) {
//        if (m1.size() != m2.size()) {
//            return false;
//        }
//        for (Entry<Value, Value> e : m1.map.entrySet()) {
//            if (!eq1(loc, e.getValue(), m2.map.get(e.getKey()), s, strict, set)) {
//                return false;
//            }
//        }
//        return true;
//    }
//    boolean eqScopeObject(Location loc, ScopeObject so1, ScopeObject so2, Scope s, boolean strict, Set<Value> set) {
//        if (so1.type != so2.type) {
//            return false;
//        }
//        assert ScopeObject.lookupClass(so1.scope) != null;
//        assert ScopeObject.lookupClass(so1.scope) == ScopeObject.lookupClass(so2.scope);
//        return eqScope(loc, so1.scope, so2.scope, s, strict, set);
//    }
//    // type ?????? scope
//    boolean eqScope(Location loc, Scope s1, Scope s2, Scope env, boolean strict, Set<Value> set) {
//        if (s1 == null && s2 == null) {
//            return true;
//        }
//        if (s1 == null || s2 == null) {
//            return false;
//        }
//        Map<String, Scope.Def> m1 = s1.table;
//        Map<String, Scope.Def> m2 = s2.table;
//        if (m1.size() != m2.size()) {
//            return false;
//        }
//        for (Entry<String, Scope.Def> e : m1.entrySet()) {
//            if (!eq1(loc, e.getValue().val, m2.get(e.getKey()).val, env, strict, set)) {
//                return false;
//            }
//        }
//        return eqParentScope(loc, s1, s2, env, strict, set);
//    }
//    boolean eqParentScope(Location loc, Scope s1, Scope s2, Scope env, boolean strict, Set<Value> set) {
//        if (s1.parent == null && s2.parent == null) {
//            return true;
//        }
//        if (s1.parent != null & s2.parent != null) {
//            Value.ClassValue c1 = ScopeObject.lookupClass(s1.parent);
//            Value.ClassValue c2 = ScopeObject.lookupClass(s2.parent);
//            // ????????????
//            if (c1 == null && c2 == null) {
//                return true;
//            }
//            // ????????????
//            if (c1 == c2) {
//                return eqScope(loc, s1.parent, s2.parent, env, strict, set);
//            }
//        }
//        return false;
//    }

    Value le(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        boolean bool;
        if (l instanceof IntValue && r instanceof IntValue) {
            bool = ((IntValue) l).val <= ((IntValue) r).val;
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            bool = ((FloatValue) l).val <= ((FloatValue) r).val;
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            bool = ((FloatValue) l).val <= ((IntValue) r).val;
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            bool = ((IntValue) l).val <= ((FloatValue) r).val;
        } else {
            return binaryOperator(expr, l, s, r);
        }
        return Bool(bool);
    }
    Value lt(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        boolean bool;
        if (l instanceof IntValue && r instanceof IntValue) {
            bool = ((IntValue) l).val < ((IntValue) r).val;
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            bool = ((FloatValue) l).val < ((FloatValue) r).val;
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            bool = ((FloatValue) l).val < ((IntValue) r).val;
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            bool = ((IntValue) l).val < ((FloatValue) r).val;
        } else {
            return binaryOperator(expr, l, s, r);
        }
        return Bool(bool);
    }
    Value gt(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);
        
        if (l instanceof IntValue && r instanceof IntValue) {
            return Bool(((IntValue) l).val > ((IntValue) r).val);
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            return Bool(((FloatValue) l).val > ((FloatValue) r).val);
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            return Bool(((FloatValue) l).val > ((IntValue) r).val);
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            return Bool(((IntValue) l).val > ((FloatValue) r).val);
        } else {
            return binaryOperator(expr, l, s, r);
        }
    }
    Value ge(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        if (l instanceof IntValue && r instanceof IntValue) {
            return Bool(((IntValue) l).val >= ((IntValue) r).val);
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            return Bool(((FloatValue) l).val >= ((FloatValue) r).val);
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            return Bool(((FloatValue) l).val >= ((IntValue) r).val);
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            return Bool(((IntValue) l).val >= ((FloatValue) r).val);
        } else {
            return binaryOperator(expr, l, s, r);
        }
    }
    Value plus(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        if (l instanceof IntValue && r instanceof IntValue) {
            return Int(((IntValue) l).val + ((IntValue) r).val);
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            return Float(((FloatValue) l).val + ((FloatValue) r).val);
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            return Float(((FloatValue) l).val + ((IntValue) r).val);
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            return Float(((IntValue) l).val + ((FloatValue) r).val);
        } else if (l instanceof StringValue && r instanceof StringValue) {
            return String(((StringValue) l).val + ((StringValue) r).val);
        } else {
            return binaryOperator(expr, l, s, r);
        }
    }
    Value minus(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        if (l instanceof IntValue && r instanceof IntValue) {
            return Int(((IntValue) l).val - ((IntValue) r).val);
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            return Float(((FloatValue) l).val - ((FloatValue) r).val);
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            return Float(((FloatValue) l).val - ((IntValue) r).val);
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            return Float(((IntValue) l).val - ((FloatValue) r).val);
        } else {
            return binaryOperator(expr, l, s, r);
        }
    }
    Value multiply(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        if (l instanceof IntValue && r instanceof IntValue) {
            return Int(((IntValue) l).val * ((IntValue) r).val);
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            return Float(((FloatValue) l).val * ((FloatValue) r).val);
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            return Float(((FloatValue) l).val * ((IntValue) r).val);
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            return Float(((IntValue) l).val * ((FloatValue) r).val);
        } else {
            return binaryOperator(expr, l, s, r);
        }
    }
    Value divide(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        if (l instanceof IntValue && r instanceof IntValue) {
            return Int(((IntValue) l).val / ((IntValue) r).val);
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            return Float(((FloatValue) l).val / ((FloatValue) r).val);
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            return Float(((FloatValue) l).val / ((IntValue) r).val);
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            return Float(((IntValue) l).val / ((FloatValue) r).val);
        } else {
            return binaryOperator(expr, l, s, r);
        }
    }
    Value module(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        if (l instanceof IntValue && r instanceof IntValue) {
            return Int(((IntValue) l).val % ((IntValue) r).val);
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            return Float(((FloatValue) l).val % ((FloatValue) r).val);
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            return Float(((FloatValue) l).val % ((IntValue) r).val);
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            return Float(((IntValue) l).val % ((FloatValue) r).val);
        } else {
            return binaryOperator(expr, l, s, r);
        }
    }
    Value power(Binary expr, Scope s) {
        Value l = interp(expr.lhs, s);
        Value r = interp(expr.rhs, s);

        if (l instanceof IntValue && r instanceof IntValue) {
            return Float(Math.pow(((IntValue) l).val, ((IntValue) r).val));
        } else if (l instanceof FloatValue && r instanceof FloatValue) {
            return Float(Math.pow(((FloatValue) l).val, ((FloatValue) r).val));
        } else if (l instanceof FloatValue && r instanceof IntValue) {
            return Float(Math.pow(((FloatValue) l).val, ((IntValue) r).val));
        } else if (l instanceof IntValue && r instanceof FloatValue) {
            return Float(Math.pow(((IntValue) l).val, ((FloatValue) r).val));
        } else {
            return binaryOperator(expr, l, s, r);
        }
    }
    Value strictLogicalAnd(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        if (lhs instanceof BoolValue) {
            if (((BoolValue) lhs).val) {
                Value rhs = interp(expr.rhs, s);
                if (rhs instanceof BoolValue) {
                    return rhs;
                } else {
                    return binaryOperator(expr, lhs, s, rhs);
                }
            } else {
                return Bool(false);
            }
        } else {
            Value rhs = interp(expr.rhs, s);
            return binaryOperator(expr, lhs, s, rhs);
        }
    }
    Value strictLogicalOr(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        if (lhs instanceof BoolValue) {
            if (((BoolValue) lhs).val) {
                return Bool(true);
            } else {
                Value rhs = interp(expr.rhs, s);
                if (rhs instanceof BoolValue) {
                    return rhs;
                } else {
                    return binaryOperator(expr, lhs, s, rhs);
                }
            }
        } else {
            Value rhs = interp(expr.rhs, s);
            return binaryOperator(expr, lhs, s, rhs);
        }
    }
    Value dynamicLogicAnd(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        if (lhs.asBool(expr.lhs.loc).val) {
            return interp(expr.rhs, s);
        } else {
            return lhs;
        }
    }
    Value dynamicLogicOr(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        if (lhs.asBool(expr.lhs.loc).val) {
            return lhs;
        } else {
            return interp(expr.rhs, s);
        }
    }
    Value is(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        Value rhs = interp(expr.rhs, s);
        if (rhs instanceof Value.ClassValue) {
            if (lhs == Null) {
                return False;
            }
            return Bool(lhs.type(expr.lhs.loc).isSubClassOf(rhs.asClass(expr.rhs.loc)));
        } else {
            return binaryOperator(expr, lhs, s, rhs);
        }
    }
    Value ltlt(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        Value rhs = interp(expr.rhs, s);
        if (lhs instanceof IntValue && rhs instanceof IntValue) {
            return Int(((IntValue) lhs).val << ((IntValue) rhs).val);
        } else {
            return binaryOperator(expr, lhs, s, rhs);
        }
    }
    Value gtgt(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        Value rhs = interp(expr.rhs, s);
        if (lhs instanceof IntValue && rhs instanceof IntValue) {
            return Int(((IntValue) lhs).val >> ((IntValue) rhs).val);
        } else {
            return binaryOperator(expr, lhs, s, rhs);
        }
    }
    Value gtgtgt(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        Value rhs = interp(expr.rhs, s);
        if (lhs instanceof IntValue && rhs instanceof IntValue) {
            return Int(((IntValue) lhs).val >>> ((IntValue) rhs).val);
        } else {
            return binaryOperator(expr, lhs, s, rhs);
        }
    }
    Value bitwiseXor(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        Value rhs = interp(expr.rhs, s);
        if (lhs instanceof IntValue && rhs instanceof IntValue) {
            return Int(((IntValue) lhs).val ^ ((IntValue) rhs).val);
        } else {
            return binaryOperator(expr, lhs, s, rhs);
        }
    }
    Value bitwiseOr(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        Value rhs = interp(expr.rhs, s);
        if (lhs instanceof IntValue && rhs instanceof IntValue) {
            return Int(((IntValue) lhs).val | ((IntValue) rhs).val);
        } else {
            return binaryOperator(expr, lhs, s, rhs);
        }
    }
    Value bitwiseAnd(Binary expr, Scope s) {
        Value lhs = interp(expr.lhs, s);
        Value rhs = interp(expr.rhs, s);
        if (lhs instanceof IntValue && rhs instanceof IntValue) {
            return Int(((IntValue) lhs).val & ((IntValue) rhs).val);
        } else {
            return binaryOperator(expr, lhs, s, rhs);
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=
    static Value symbolApply(Location loc, Value lhs, Scope s, String sym, Value ...args) {
        ObjectValue symbol = lhs.asObject(loc).getSymbol(loc, Symbol(sym));
        if (symbol == Null) {
            throw Error.runtime(loc, "??? ["  + lhs + "] ??? symbol [" + sym + "] ??????: ");
        }
        return symbol.asApply(loc).apply(loc, s, args);
    }
    static Value unaryOperator(Unary unary, Value lhs, Scope s) {
        return symbolApply(unary.loc, lhs, s, unary.op.name);
    }
    static Value binaryOperator(Binary binary, @NotNull Value lhs, Scope s, @NotNull Value rhs) {
        return symbolApply(binary.loc, lhs, s, binary.op.name, rhs);
    }
    @Override
    public Value visit(@NotNull Unary unary, @NotNull Scope s) {
        switch (unary.op) {
            case UNARY_PLUS: return positive(unary, s);
            case UNARY_MINUS: return negation(unary, s);
            case BIT_NOT: return bitwiseNot(unary, s);
            case LOGIC_NOT: return logicalNot(unary, s);
            case PREFIX_INCR: return prefixIncr(unary, s);
            case PREFIX_DECR: return prefixDecr(unary, s);
            case POSTFIX_INCR: return postfixIncr(unary, s);
            case POSTFIX_DECR: return postfixDecr(unary, s);
            default:
                return unaryOperator(unary, interp(unary.arg, s), s);
        }
    }
    Value positive(Unary expr, Scope s) {
        Value v = interp(expr.arg, s);
        if (v instanceof NumValue) {
            return v;
        } else {
            return unaryOperator(expr, v, s);
        }
    }
    Value negation(Unary expr, Scope s) {
        Value v = interp(expr.arg, s);
        if (v instanceof IntValue) {
            return Int(-((IntValue) v).val);
        } else if (v instanceof FloatValue) {
            return Float(-((FloatValue) v).val);
        } else {
            return unaryOperator(expr, v, s);
        }
    }
    Value bitwiseNot(Unary expr, Scope s) {
        Value v = interp(expr.arg, s);
        if (v instanceof IntValue) {
            return Int(~(((IntValue) v).val));
        } else {
            return unaryOperator(expr, v, s);
        }
    }

    Value logicalNot(Unary expr, Scope s) {
        Value v = interp(expr.arg, s);
        // return Bool(!v.asBool(expr.loc).val);
        if (v instanceof BoolValue) {
            return ((BoolValue) v).inverse();
        } else {
            return unaryOperator(expr, v, s);
        }
    }
    @SuppressWarnings("Duplicates")
    Value prefixIncr(Unary expr, Scope s) {
        Value v = interp(expr.arg, s);
        if (v instanceof IntValue) {
            IntValue iv = (IntValue) v;
            IntValue nv = Int(iv.val + 1, iv.base);
            s.assign(expr.loc, ((Id) expr.arg).name, nv);
            return nv;
        } else if (v instanceof FloatValue) {
            FloatValue fv = (FloatValue) v;
            FloatValue nv = Float(fv.val + 1);
            s.assign(expr.loc, ((Id) expr.arg).name, nv);
            return nv;
        } else {
            return unaryOperator(expr, v, s);
        }
    }
    @SuppressWarnings("Duplicates")
    Value prefixDecr(Unary expr, Scope s) {
        Value v = interp(expr.arg, s);
        if (v instanceof IntValue) {
            IntValue iv = (IntValue) v;
            IntValue nv = Int(iv.val - 1, iv.base);
            s.assign(expr.loc, ((Id) expr.arg).name, nv);
            return nv;
        } else if (v instanceof FloatValue) {
            FloatValue fv = (FloatValue) v;
            FloatValue bv = Float(fv.val - 1);
            s.assign(expr.loc, ((Id) expr.arg).name, bv);
            return bv;
        } else {
            return unaryOperator(expr, v, s);
        }
    }
    @SuppressWarnings("Duplicates")
    Value postfixIncr(Unary expr, Scope s) {
        Value v = interp(expr.arg, s);
        if (v instanceof IntValue) {
            IntValue iv = (IntValue) v;
            IntValue nv = Int(iv.val + 1, iv.base);
            s.assign(expr.loc, ((Id) expr.arg).name, nv);
            return iv;
        } else if (v instanceof FloatValue) {
            FloatValue fv = (FloatValue) v;
            FloatValue nv = Float(fv.val + 1);
            s.assign(expr.loc, ((Id) expr.arg).name, nv);
            return fv;
        } else {
            return unaryOperator(expr, v, s);
        }
    }
    @SuppressWarnings("Duplicates")
    Value postfixDecr(Unary expr, Scope s) {
        Value v = interp(expr.arg, s);
        if (v instanceof IntValue) {
            IntValue iv = (IntValue) v;
            IntValue nv = Int(iv.val - 1, iv.base);
            s.assign(expr.loc, ((Id) expr.arg).name, nv);
            return iv;
        } else if (v instanceof FloatValue) {
            FloatValue fv = (FloatValue) v;
            FloatValue bv = Float(fv.val - 1);
            s.assign(expr.loc, ((Id) expr.arg).name, bv);
            return fv;
        } else {
            return unaryOperator(expr, v, s);
        }
    }
    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    @Override
    public Value visit(@NotNull Ternary ternary, @NotNull Scope s) {
        TokenType op = ternary.op;
        if (op != TokenType.COND) {
            throw Error.todo(ternary.loc, "????????? ?: ???????????????");
        }
        Value lhs = interp(ternary.left, s);
        if (lhs.asBool(ternary.left.loc).val) {
            return interp(ternary.mid, s);
        } else {
            return interp(ternary.right, s);
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    static class ControlFlow extends RuntimeException {
        protected ControlFlow() {
            super(null, null, true, false);
        }

        static Continue Continue() { return new Continue(); }
        static Break Break() { return new Break(); }
        static Return Return(Value val) { return new Return(val); }
        static Throw Throw(Location loc, List<Frame> stack, Value value) { return new Throw(loc, stack, value); }

        static class Continue extends ControlFlow { }
        static class Break extends ControlFlow { }
        static class Return extends ControlFlow {
            @NotNull public final Value val;
            private Return(@NotNull Value val) {
                this.val = val;
            }
        }
        static class Throw extends ControlFlow {
            @NotNull public final Location loc;
            @NotNull public final List<Frame> stack;
            @NotNull public final Value value;
            private Throw(@NotNull Location loc, @NotNull List<Frame> stack, @NotNull Value value) {
                this.loc = loc;
                this.stack = stack;
                this.value = value;
            }
        }
    }


    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    public static class Frame {
        public final Call call;
        public final Value callee;
        public final Value[] args;
        public final Scope s;
        public Frame(
                @NotNull Call call,
                @NotNull Value callee,
                @NotNull Value[] args,
                @NotNull Scope s) {
            this.call = call;
            this.callee = callee;
            this.args = args;
            this.s = s;
        }
        ObjectValue objectValue() {
            Location loc = call.loc;
            ObjectValue obj = Value.Object();
            // obj.set(loc, "callee_expr");
            // obj.set(loc, "parameter", );
            // obj.set(loc, "location", );
            obj.setProperty(loc, "callee", callee);
            obj.setProperty(loc, "args", Value.List(args));
            // obj.set(loc, "scope", Value.ScopeObject(s)); // stack over flow ...
            return obj;
        }
        @Override public String toString() {
            return call.loc.toString();
            // return call.callee + "(" + join(call.args, ", ") + ") at " + call.loc;
        }
    }

    public static class FrameStack {
        public final Deque<Frame> frames = new LinkedList<>();
        public int threshold;
        public int cnt = 0;
        FrameStack(int threshold) {
            this.threshold = threshold;
        }
        public void push(@NotNull Frame f) {
            if (++cnt > threshold) {
                log(toString() + "\n");
                throw Error.runtime(None, "StackOverflow");
            }
            frames.push(f);
        }
        public void pop() {
            cnt--;
            frames.pop();
        }
        public List<Frame> snapshot() {
            List<Frame> copy = new ArrayList<>(frames.size());
            copy.addAll(frames);
            return copy;
        }
        ListValue backtrace() {
            ObjectValue[] bt = snapshot().stream().map(Frame::objectValue).toArray(ObjectValue[]::new);
            return List(bt);
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=

    /**
     * assert ????????????????????? Groovy ??? PowerAssert
     * ?????? Groovy ???????????????????????????????????????????????????????????????????????????????????? if ?????????
     * ???????????????????????????????????? hook ????????????, ?????? node ????????????????????????????????????
     * ??????node ????????? ?????? oploc, ???????????????????????? node assert ???????????????
     *
     * ???????????????????????? Groovy???
     * @author groovy ??????
     *
     */
    static class Renderer {
        static class Val {
            final Value v;
            final int col; // index start with 1

            Val(@NotNull Value v, int col) {
                this.v = v;
                this.col = col;
            }
        }
        static class ValRec {
            final List<Val> vals = lists();
            void clear() { vals.clear(); }
            Value rec(Value v, int col) {
                for (Val it : vals) {
                    if (it.col == col) return rec(v, col + 1);
                }
                vals.add(new Val(v, col));
                return v;
            }
        }

        final String src;
        final ValRec rec;
        final List<StringBuilder> lines = new ArrayList<>();
        // startColumns.get(i) is the first non-empty column of lines.get(i)
        final List<Integer> startColumns = new ArrayList<>();

        Renderer(Location loc, String src, ValRec rec) {
            if (src.contains("\n")) {
                throw Error.runtime(loc, "assert ??????????????????");
            }
            this.src = src;
            this.rec = rec;
        }

        static String render(Location loc, String src, ValRec recorder) {
            return new Renderer(loc, src, recorder).render();
        }

        String render() {
            renderAssertExpr();
            sortValues();
            renderValues();
            return linesToString();
        }

        void renderAssertExpr() {
            lines.add(new StringBuilder(src));
            startColumns.add(0);

            lines.add(new StringBuilder()); // empty line
            startColumns.add(0);
        }

        void sortValues() {
            // it's important to use a stable sort here, otherwise
            // renderValues() will skip the wrong values
            rec.vals.sort((v1, v2) -> v2.col - v1.col);
        }

        void renderValues() {
            List<Val> vals = rec.vals;
            int valSz = vals.size();

            nextValue:
            for (int i = 0; i < valSz; i++) {
                Val value = vals.get(i);
                int startColumn = value.col;
                if (startColumn < 1) continue; // skip values with unknown source position

                // if multiple values are associated with the same column, only
                // render the value which was recorded last (i.e. the value
                // corresponding to the outermost expression)
                Val next = i + 1 < valSz ? vals.get(i + 1) : null;
                if (next != null && next.col == startColumn) continue;

                String str = value.v.toString();
                if (str == null) continue; // null signals the value shouldn't be rendered

                String[] strs = str.split("\r\n|\r|\n");
                int endColumn = strs.length == 1 ?
                        startColumn + str.length() : // exclusive
                        Integer.MAX_VALUE; // multi-line strings are always placed on new lines

                for (int j = 1; j < lines.size(); j++)
                    if (endColumn < startColumns.get(j)) {
                        placeString(lines.get(j), str, startColumn);
                        startColumns.set(j, startColumn);
                        continue nextValue;
                    } else {
                        placeString(lines.get(j), "|", startColumn);
                        if (j > 1) // make sure that no values are ever placed on empty line
                            startColumns.set(j, startColumn + 1); // + 1: no whitespace required between end of value and "|"
                    }

                // value could not be placed on existing lines, so place it on new line(s)
                for (String s : strs) {
                    StringBuilder newLine = new StringBuilder();
                    lines.add(newLine);
                    placeString(newLine, s, startColumn);
                    startColumns.add(startColumn);
                }
            }
        }

        String linesToString() {
            return Helper.join(lines, "\n");
        }

        static void placeString(StringBuilder line, String str, int column) {
            while (line.length() < column) {
                line.append(' ');
            }
            line.replace(column - 1, column - 1 + str.length(), str);
        }
    }
}
