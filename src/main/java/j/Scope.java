package j;

import j.parser.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.*;

import static j.Ast.*;
import static j.Value.*;
import static java.util.Collections.*;

import java.lang.String;
import java.lang.Object;
import java.util.function.Predicate;


/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public final class Scope {
    public static final Scope Immutable = new Scope(null, unmodifiableMap(new HashMap<>()));

    static class Def {
        // 扩展其他信息 getter\setter\iterable\frozen等等
        final Value val;
        final boolean mut;
        Def(@NotNull Value val) { this(val, true); }
        Def(@NotNull Value val, boolean mut) {
            this.val = val;
            this.mut = mut;
        }
        @Override public int hashCode() { return val.hashCode(); }
        @Override public String toString() { return val.toString(); }
        @Override public boolean equals(Object obj) {
            return obj == this || obj instanceof Def && val.equals(((Def) obj).val);
        }
    }

    public @Nullable Scope parent;
    public final Map<String, Def> table;

    protected Scope(@Nullable Scope parent, @NotNull Map<String, Def> tbl) {
        this.parent = parent;
        this.table = tbl;
    }

    @NotNull Scope upstream(Predicate<Scope> up) {
        Scope s = this;
        while (s.parent != null && up.test(s.parent)) {
            s = s.parent;
        }
        return s;
    }

    @NotNull Scope rootScope() {
        return upstream(it -> true);
    }

    public void setRootParent(Scope parent) {
        rootScope().parent = parent;
    }

    public static Scope create() {
        return new Scope(null, new LinkedHashMap<>());
    }

    public static Scope create(@NotNull Scope parent) {
        // circle Check
        return new Scope(parent, new LinkedHashMap<>());
    }

    public Scope inherit() {
        return new Scope(this, new LinkedHashMap<>());
    }

    // copy 链接关系, 不 copy scope 中保存的值
    public Scope copy() {
        return new Scope(parent == null ? null : parent.copy(), table);
    }

    @Nullable Value lookupGlobal(String name) {
        return rootScope().lookupLocal(name);
    }

    public @Nullable Value lookup(String name, Predicate<Scope> lookupParent) {
        Value val = lookupLocal(name);
        if (val != null) {
            return val;
        } else if (parent != null && lookupParent.test(parent)) {
            return parent.lookup(name);
        } else {
            return null;
        }
    }

    public @Nullable Value lookup(String name) {
        return lookup(name, it -> true);
    }

    public @Nullable Value.ClassValue lookupClass(Location loc, String cls) {
        Value v = lookup(cls);
        if (v != null) {
            return v.asClass(loc);
        }
        return null;
    }

    public @Nullable Value unset(Location loc, @NotNull String name) {
        Def def = table.remove(name);
        return def == null ? null : def.val;
    }

    public void define(Location loc, @NotNull String name, @NotNull Value value, boolean mut) {
        if (lookupLocalDef(name) == null) {
            table.put(name, new Def(value, mut));
        } else {
            throw Error.runtime(loc, name + " 重复定义");
        }
    }

    public void define(Location loc, @NotNull Id id, @NotNull Value value, boolean mut) {
        bind(loc, id, value, false, mut);
    }

    public void assign(Location loc, @NotNull String name, @NotNull Value value) {
        Scope definedScope = findDefinedScope(name);
        if (definedScope == null) {
            throw Error.runtime(loc, name + " 未定义");
        }
        definedScope.assignLocal(loc, name, value);
    }

    public void assign(Location loc, @NotNull Id id, @NotNull Value value) {
        bind(loc, id, value, true, true);
    }

    void bind(Location loc, @NotNull Pattern pattern, @NotNull Value value, boolean assign, boolean mut) {
        if (pattern instanceof Id) {
            String name = ((Id) pattern).name;
            if (assign) {
                assign(loc, name, value);
            } else {
                define(loc, name, value, mut);
            }
        } else {
            throw Error.bug(loc);
        }
    }

    public void put(Location loc, @NotNull String key, @NotNull Value value) {
        Scope s = findDefinedScope(key);
        if (s == null) {
            table.put(key, new Def(value, true));
        } else {
            s.assignLocal(loc, key, value);
        }
    }

    void assignLocal(@NotNull Location loc, @NotNull String name, @NotNull Value value) {
        Def def = lookupLocalDef(name);
        if (def == null) {
            throw Error.runtime(loc, name + " 未定义");
        }
        // 语义: val 只能初始化一次, 可以延迟初始化
        if(def.mut || def.val == Undefined) {
            // def.val = value;
            table.put(name, new Scope.Def(value, def.mut));
        } else {
            throw Error.runtime(loc, name + " 常量不允许修改");
        }
    }

    @Nullable Value lookupLocal(String name) {
        Def def = lookupLocalDef(name);
        return def == null ? null : def.val;
    }

    @Nullable Def lookupLocalDef(String name) {
        return table.get(name);
    }

    @Nullable Scope findDefinedScope(String name) {
        Def def = table.get(name);
        if (def != null) {
            return this;
        } else if (parent != null) {
            return parent.findDefinedScope(name);
        } else {
            return null;
        }
    }

    @Override
    public int hashCode() {
        // return Objects.hash(parent) * 31 + table.hashCode();
        return table.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Scope) {
            return table.equals(((Scope) obj).table);
            // return table.equals(((Scope) obj).table) && Objects.equals(parent, ((Scope) obj).parent);
        }
        return false;
    }

    @Override
    public String toString() {
        return table.toString();
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
    // 以下是解构赋值 与 模式匹配 未解语法糖之前的代码~~~

//    // 给模式匹配用的...
//    public void tryDefine(Location loc, @NotNull Pattern pattern, @NotNull Value value, boolean mut) {
//        new TryPatternBinder(loc, false, mut).match(pattern, value);
//    }
//    public void define(Location loc, @NotNull Pattern pattern, @NotNull Value value, boolean mut) {
//        new PatternBinder(loc, false, mut).match(pattern, value);
//    }
//    public void assign(Location loc, @NotNull Pattern pattern, @NotNull Value value) {
//        new PatternBinder(loc, true, true).match(pattern, value);
//    }

    // 模式匹配已经desugar了, 这里已经不需要了
    /*
    class PatternBinder implements Matcher<Value, Void> {
        final Location loc;
        final boolean assign;
        final boolean mut;
        PatternBinder(Location loc, boolean assign, boolean mut) {
            this.loc = loc;
            this.assign = assign;
            this.mut = mut;
        }
        @Override public Void match(@NotNull Id ptn, @NotNull Value val) {
            if (Matcher.isWildcards(ptn)) {
                return null;
            }
            String name = ptn.name;
            if (assign) {
                assign(loc, name, val);
            } else {
                define(loc, name, val, mut);
            }
            return null;
        }
        @Override public Void match(@NotNull ListPattern ptn, @NotNull Value val) {
            List<Pattern> patterns = ptn.elems;
            IteratorValue iter = val.asObject(loc).iterator(loc);
            if (iter == null) {
                throw Error.runtime(loc, val + " 木有迭代器, 不能迭代");
            }
            for (Pattern ptn1 : patterns) {
                if (iter.hasNext()) {
                    match(ptn1, iter.next());
                } else {
                    match(ptn1, Null);
                }
            }
            return null;
        }
        @Override public Void match(@NotNull MapPatternValue ptn, @NotNull Value val) {
            Map<Value, Pattern> patterns = ptn.props;
            ObjectValue obj = val.asObject(loc);
            for (Map.Entry<Value, Pattern> it : patterns.entrySet()) {
                Value v;
                Value k = it.getKey();
                if (obj instanceof MapValue) {
                    // 解构 Map
                    v = ((MapValue) obj).get(k);
                } else {
                    // 解构 Object
                    v = obj.get(loc, k.asString(loc).val);
                }
                match(it.getValue(), v == null ? Null : v);
            }
            return null;
        }
        @Override public Void match(@NotNull Literal ptn, @NotNull Value val) {
            throw Error.runtime(loc, "字面量不能作为" + (assign ? "赋值" : "声明") + "左值");
        }
        @Override public Void match(@NotNull MapPattern ptn, @NotNull Value val) {
            throw Error.runtime(ptn.loc, "不支持的 pattern: " + ptn);
        }
        @Override public Void match(@NotNull UnApplyPatternValue ptn, @NotNull Value val) {
            // 应该 parser 阶段做的，因为没做符号表，只能运行时了...
            if (!(ptn.classValue instanceof UserDefClass)) {
                throw Error.runtime(ptn.loc, "只支持结构用户自定义 class");
            }
            if (!val.is(ptn.loc, ptn.classValue)) {
                throw Error.runtime(ptn.loc, "左值 " + ptn.classValue + " 与右值 " + ptn.classValue.type(ptn.loc) + " 不匹配");
            }
            List<Pattern> props = ptn.props;
            List<Id> params = ((UserDefClass) ptn.classValue).cls.ctor.params;
            if (props.size() != params.size()) {
                throw Error.runtime(ptn.loc, "pattern 与 类构造函数参数数量不匹配");
            }
            ObjectValue obj = val.asObject(ptn.loc);
            for (int i = 0; i < props.size(); i++) {
                match(props.get(i), obj.get(ptn.loc, params.get(i).name));
            }
            return null;
        }
    }

    // 忽略不支持的 bind
    class TryPatternBinder extends PatternBinder {
        TryPatternBinder(Location loc, boolean assign, boolean mut) { super(loc, assign, mut); }
        @Override public Void match(@NotNull UnApplyPattern ptn, @NotNull Value ctx) { return null; }
        @Override public Void match(@NotNull Literal ptn, @NotNull Value val) { return null; }
        @Override public Void match(@NotNull MapPattern ptn, @NotNull Value val) { return null; }
    }
    */

    // parser 之中把语法糖解开, 全部递归转变成对象与数组访问的声明或赋值，这里不需要了
    /*
    void bind(Location loc, @NotNull Pattern pattern, @NotNull Value value, boolean assign, boolean mut) {
        if (pattern instanceof Id) {
            String name = ((Id) pattern).name;
            if (assign) {
                assign(loc, name, value);
            } else {
                define(loc, name, value, mut);
            }
        }

        else if (pattern instanceof Literal) {
            throw Error.runtime(loc, "字面量不能作为" + (assign ? "赋值" : "声明") + "左值");
        }

        else if (pattern instanceof ListPattern) {
            List<Pattern> patterns = ((ListPattern) pattern).elems;
            ListValue lst = value.asList(loc);
            for (int i = 0; i < patterns.size(); i++) {
                Pattern ptn = patterns.get(i);
                if (ptn == null) continue;

                if (i < lst.size()) {
                    bind(loc, ptn, lst.get(loc, i), assign, mut);
                } else {
                    bind(loc, ptn, Null, assign, mut);
                }
            }
        }

        // MapPattern 已经被 interp 成 MapPatternValue，scope 需要 value ...
        else if (pattern instanceof MapPatternValue) {
            Map<Value, Pattern> patterns = ((MapPatternValue) pattern).props;
            ObjectValue obj = value.asObject(loc);

            for (Map.Entry<Value, Pattern> it : patterns.entrySet()) {
                Value v;
                // 解构 Map & Object
                if (obj instanceof MapValue) {
                    v = ((MapValue) obj).get(it.getKey());
                } else {
                    v = obj.get(it.getKey().asString(loc).val);
                }
                bind(loc, it.getValue(), v == null ? Null : v, assign, mut);
            }
        }

        else {
            throw Error.bug(loc);
        }
    }
    */
}
