package j;

import j.Value.SymbolValue;
import j.parser.Location;
import j.parser.TokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Cleaner;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static j.BuildIn.*;
import static j.Helper.*;
import static j.Value.Constant.*;
import static j.Value.Meta.*;
import static j.Value.String;
import static j.parser.Location.None;
import static java.util.Objects.requireNonNull;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public interface Value {
    @interface Used {}

    interface Constant {
        String Key_Interp           = "$interp";
        String Key_Class            = "class";
        String Key_Object_Id        = "$object_id";

        String Sym_Iterable         = "`iterable`";
        String Sym_ToString         = "`toString`";
        String Sym_Hash             = "`hash`";
        String Sym_Plus             = "`+`";
        String Sym_Equals           = "`==`";

        String Var_Global           = "global";

        String Fun_Apply            = "apply";
        String Fun_Enum_Values      = "values";
        String Fun_Exec             = "exec";
        String Fun_ObjectId         = "objectId";

        String Class_Class          = "Class";
        String Class_Null           = "Null";
        String Class_Object         = "Object";
        String Class_Bool           = "Bool";
        String Class_Int            = "Int";
        String Class_Float          = "Float";
        String Class_Symbol         = "Symbol";
        String Class_String         = "String";
        String Class_Iterator       = "Iterator";
        String Class_List           = "List";
        String Class_Map            = "Map";
        String Class_Fun            = "Fun";

        String Class_Enum           = "Enum";
        String Class_Scope          = "Scope";
        String Class_JavaValue      = "JavaValue";
        String Class_Range          = "Range";
        String Class_Throwable      = "Throwable";
        String Class_NativeError    = "NativeError";
        String Class_AssertError    = "AssertError";
        String Class_CallError      = "CallError";
        String Class_RuntimeError   = "RuntimeError";
        String Class_MatchError     = "MatchError";
        String Class_ClassNotFoundError     = "ClassNotFoundError";
        String Class_NullPointerError       ="NullPointerError";

        String Field_Throwable_StackTrace = "stackTrace";

        String Fun_Iterator_HasNext = "hasNext";
        String Fun_Iterator_Next = "next";
        String Fun_Construct = "construct";
    }

    SymbolValue SymbolApply     = Symbol("apply"); // () 操作符重载
    SymbolValue SymbolIterable  = Symbol("iterable"); // 可以认为是 for 的操作符重载!!!
    SymbolValue SymbolToString  = Symbol("toString"); // inspect  /  show
    SymbolValue SymbolEquals    = Symbol("==");
    SymbolValue SymbolHash      = Symbol("hash");
    SymbolValue SymbolPlus      = Symbol("+");

    Value Undefined = new Undefined();

    NullValue Null  = new NullValue();
    BoolValue True  = new BoolValue(true);
    BoolValue False = new BoolValue(false);

    // 这里做 NullClass 是为了统一 做 metaTable 操作符重载那些东西，实际获取不到 null.class
    ClassValue ClassNull        = new NullClass();
    ClassValue ClassObject      = new ObjectClass();
    ClassValue ClassClass       = new ClassClass();
    ClassValue ClassBool        = new BoolClass();
    ClassValue ClassInt         = new IntClass();
    ClassValue ClassFloat       = new FloatClass();
    ClassValue ClassSymbol      = new SymbolClass();
    ClassValue ClassString      = new StringClass();
    ClassValue ClassIterator    = new IteratorClass();
    ClassValue ClassList        = new ListClass();
    ClassValue ClassMap         = new MapClass();
    ClassValue ClassFun         = new FunClass();
    ClassValue ClassJavaValue   = new JavaValueClass();

    @Used static ObjectValue Copy(ObjectValue p){ return new PrototypeValue(p);       }
    static ObjectValue  Object()                { return new StdObject();             }
    static BoolValue    Bool(boolean val)       { return val ? True : False;          }
    static IntValue     Int(char val)           { return new IntValue(val, 10); }
    static IntValue     Int(long val)           { return new IntValue(val, 10); }
    static IntValue     Int(long val, int base) { return new IntValue(val, base);     }
    static FloatValue   Float(double val)       { return new FloatValue(val);         }
    static StringValue  String(String val)      { return new StringValue(val);        }
    static SymbolValue  Symbol(String val)      { return SymbolValue.of(val);         }
    static ListValue    Pair(Value a, Value b)  { return new ListValue(a, b);         }
    static ListValue    List(Value ...vals)     { return new ListValue(vals);         }
    static ListValue    List(List<Value> list)  { return new ListValue(list);         }
    static MapValue     Map()                   { return new MapValue();              }

    static FunValue Fun(String className, String name,
                        List<String> params, List</*@Nullable*/Value> defaults,
                        int requiredParamSz, Applicative a) {
        return new StdFun(className, name, Scope.Immutable, a, params, defaults, requiredParamSz);
    }
    static ScopeObject ScopeObject(ClassValue c, Scope s) {
        return new ScopeObject(c, s, true);
    }
    static ScopeObject Scope(ClassValue c, Scope s) {
        return new ScopeObject(c, s, false);
    }
    static IteratorValue Iterator(Iterator<? extends Value> iter) {
        return new IteratorValue(iter);
    }
    static UserDefClass UserClass(ClassMeta meta, Scope env, Function<UserDefClass, Applicative> maker) {
        return new UserDefClass(meta, env, maker);
    }
    static UserDefFun UserFun(FunMeta fm, Scope env, Applicative c) {
        return new UserDefFun(fm, env, c);
    }
    static JavaValue JavaValue(Object v) {
        return v instanceof JavaValue ? (JavaValue) v : new JavaValue(v);
    }

    static VarMeta varMeta(@NotNull String id, boolean mut, @NotNull Consumer<Scope> defVar) {
        return new VarMeta(id, mut, defVar);
    }
    static FunMeta funMeta(@NotNull String name,
                           @Nullable String className,
                           @NotNull List<String> params,
                           @NotNull List</*@Nullable*/Value> defaults,
                           boolean ctor,
                           boolean symbol,
                           boolean extend,
                           boolean arrow,
                           boolean generatedSuperCall,
                           int requiredParamSz,
                           @NotNull Consumer<Scope> defFun) {
        return new FunMeta(name, className, params, defaults,
                ctor, symbol, extend, arrow, generatedSuperCall, requiredParamSz, defFun);
    }
    static ClassMeta classMeta(@NotNull String name, @Nullable ClassValue parent, @NotNull List<VarMeta> props,
                               @NotNull List<FunMeta> methods, @NotNull FunMeta ctor,
                               boolean sealed, boolean tagged, boolean throwable) {
        return new ClassMeta(name, parent, props, methods, ctor, sealed, tagged, throwable);
    }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    ClassValue    type(Location loc);
    default boolean is(Location loc, ClassValue cv) { return type(loc).isSubClassOf(cv);    }
    default boolean       isClass()                 { return this instanceof ClassValue;    }
    default boolean       isObject()                { return this instanceof ObjectValue;   }
    default FloatValue    asFloat(Location loc)     { throw Error.type(loc, this + " 不能转换成 Float");     }
    default IntValue      asInt(Location loc)       { throw Error.type(loc, this + " 不能转换成 Int");       }
    default BoolValue     asBool(Location loc)      { throw Error.type(loc, this + " 不能转换成 Bool");      }
    default StringValue   asString(Location loc)    { throw Error.type(loc, this + " 不能转换成 String");    }
    default SymbolValue   asSymbol(Location loc)    { throw Error.type(loc, this + " 不能转换成 Symbol");    }
    default IteratorValue asIterator(Location loc)  { throw Error.type(loc, this + " 不能转换成 Iterator");  }
    default ListValue     asList(Location loc)      { throw Error.type(loc, this + " 不能转换成 List");      }
    default MapValue      asMap(Location loc)       { throw Error.type(loc, this + " 不能转换成 Map");       }
    default FunValue      asFun(Location loc)       { throw Error.type(loc, this + " 不能转换成 Fun");       }
    default ObjectValue   asObject(Location loc)    { throw Error.type(loc, this + " 不能转换成 Object");    }
    default ClassValue    asClass(Location loc)     { throw Error.type(loc, this + " 不能转换成 Class");     }
    default JavaValue     asJavaValue(Location loc) { throw Error.type(loc, this + " 不能转换成 JavaValue"); }
    default Applicative   asApply(Location loc)     { throw Error.type(loc, this + " 不能转换成 Apply");     }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    interface BoxedValue extends Value {
        default ClassValue type(Location loc) { throw Error.bug(loc, " BoxedValue 内部使用"); }
        Object getObject();
    }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    // 这个单独的接口貌似没什么卵用，可以直接合并到 ObjectValue 接口
    interface SymbolAccessor {
        default ObjectValue lookupSymbol(Location loc, @NotNull SymbolValue key, Predicate<ClassValue> until) {
            // SymbolAccessor 就应该合并到 ObjectValue
            ObjectValue self = ((ObjectValue) this);
            ClassValue type = self.type(loc);
            while (type != null && until.test(type)) {
                ObjectValue v = type.metaTable().get(loc, key);
                if (v != null) {
                    return FunValue.bind(loc, v, self, true);
                }
                type = type.parent();
            }
            return Null;
        }
        default @NotNull ObjectValue getSymbol(Location loc, @NotNull SymbolValue key) {
            return lookupSymbol(loc, key, it -> true);
        }
        default void setSymbol(Location loc, @NotNull SymbolValue key, @NotNull ObjectValue value) {
            // SymbolAccessor 就应该合并到 ObjectValue
            ObjectValue self = ((ObjectValue) this);
            self.type(loc).setSymbol(loc, key, value);
        }
    }

    // 这个单独的接口貌似没什么卵用，可以直接合并到 ObjectValue 接口
    interface PropertyAccessor {
        // public final 除了 Null 子类不要实现
        /*public final*/default @Nullable Value getProperty(Location loc, @NotNull String key) {
            if (Key_Class.equals(key)) {
                return ((ObjectValue) this).type(loc);
            } else {
                return doGetProperty(loc, key, true);
            }
        }

        // todo: 考虑简化成 lookupSymbol 形式, 配合 getSelfProperty
        // 子类不要实现 doGet, 去实现 doGetSelf
        // 对象属性转到 class(meteTable), 配合 extFun 中 set 使用，这里充当给 primitive/buildInClass 获取静态属性或方法使用
        /*protected final*/@Nullable default Value doGetProperty(Location loc, @NotNull String key, boolean bind) {
            // PropertyAccessor 就应该合并到 ObjectValue
            ObjectValue self = ((ObjectValue) this);
            Value v = getSelfProperty(loc, key, bind);
            if (v == null) {
                // class 就不找自己的 class 了
                if (this instanceof ClassValue) {
                    return null;
                } else {
                    // 自己找不着去 type 里头找
                    Value vFromType = self.type(loc).doGetProperty(loc, key, false);
                    return FunValue.bind(loc, vFromType, self, bind);
                }
            } else {
                return v;
            }
        }
        /*protected*/@Nullable default Value getSelfProperty(Location loc, @NotNull String key, boolean bind) { return null; }

        /*public final*/default void setProperty(Location loc, @NotNull String key, @NotNull Value value) {
            if (Key_Class.equals(key)) {
                throw Error.runtime(loc, Key_Class + " 不能修改");
            }
            doSetProperty(loc, key, value);
        }
        /*protected*/default void doSetProperty(Location loc, @NotNull String key, @NotNull Value value) {
            throw Error.runtime(loc, "不支持设置属性" + key);
        }
    }

    interface Metable extends ObjectValue {
        default @NotNull MetaTable metaTable() { return MetaTable.get(this); }
        void init(boolean isPrimitive);
    }
    interface Applicative {
        default Value apply(Location loc, Value ...args) { return apply(loc, Scope.Immutable, args); }
        Value apply(Location loc, @NotNull Scope s, Value ...args);
    }
    interface ValueIterable {
        // todo IteratorValue --> ObjectValue
        default @Nullable IteratorValue iterator(Location loc) { return null; }
    }
    interface ObjectValue extends Value, PropertyAccessor, SymbolAccessor, ValueIterable {
        Value id();
        @Override default StringValue asString(Location loc) { return String(toString()); }
        @Override default Applicative asApply(Location loc) { return getSymbol(loc, SymbolApply).asFun(loc); }
        @Override default @NotNull ObjectValue asObject(Location loc) { return this; }
    }
    interface Boxed { JavaValue unbox();}

    interface FunValue extends Metable, Applicative {
        FunMeta meta();
        @Override default @NotNull FunValue asFun(Location loc) { return this; }
        @Override default @NotNull ClassValue type(Location loc) { return ClassFun; }
        @Override default Applicative asApply(Location loc) { return this; }
        @Override @Nullable default Value getSelfProperty(Location loc, @NotNull String key, boolean bind) {
            ObjectValue v = metaTable().get(loc, key);
            if (v == null) {
                return null;
            } else {
                return bind(loc, v, this, bind);
            }
        }
        /*protected*/@NotNull ObjectValue doBind(Location loc, @NotNull ObjectValue self);
        /*public*/static @Nullable ObjectValue bind(Location loc, Value v, @NotNull ObjectValue self, boolean bind) {
            if (v == null) {
                return null;
            } else {
                ObjectValue ov = v.asObject(loc);
                if (bind && ov instanceof FunValue) {
                    return ((FunValue) ov).doBind(loc, self);
                } else {
                    return ov;
                }
            }
        }
        default void init(boolean isPrimitive) {
            Meta.initMetaTable(this, isPrimitive);
        }
    }
    interface ClassValue extends Metable {
        String name();
        FunMeta construct(); // 移除这个方法... todo
        @Override
        default Value id() { return String("class#" + System.identityHashCode(this)); }
        // 这里不用做缓存，因为内置的类只会有一个 classValue 实例，UserDefClass 也不需要缓存, 因为defClass时候构造，构造时候传入
        default ClassMeta meta() { return primitiveClass(this, parent(), construct()); }
        // 只有 UserDefClass(没显式 extends 则默认继承 Object) 和 ObjectClass(只有 Object 没有 parent) 重写 parent
        default @Nullable ClassValue parent() { return ClassObject; }
        default boolean isSubClassOf(@NotNull ClassValue type) {
            if (this == type) return true;
            ClassValue parent = parent();
            return parent != null && parent.isSubClassOf(type);
        }
        @Override default @NotNull ClassValue type(Location loc) { return ClassClass; }
        @Override default ClassValue asClass(Location loc) { return this; }
        // 对象统一通过 ClassValue.newInstance 创建
        default ObjectValue newInstance(Location loc, Value ...args) { throw Error.syntax(loc, "不能直接构造 " + name()); }
        @Override @Nullable default Value getSelfProperty(Location loc, @NotNull String key, boolean bind) {
            ObjectValue v = metaTable().get(loc, key);
            if (v == null) {
                ClassValue parent = parent();
                if (parent == null) {
                    assert this == ClassObject;
                    return null;
                } else {
                    Value pv = parent.doGetProperty(loc, key, bind);
                    return FunValue.bind(loc, pv, this, bind);
                }
            } else {
                return FunValue.bind(loc, v, this, bind);
            }
        }
        @Override default void doSetProperty(Location loc, @NotNull String key, @NotNull Value value) {
            metaTable().set(loc, key, value.asObject(loc), true);
        }
        @Override default void setSymbol(Location loc, @NotNull SymbolValue key, @NotNull ObjectValue value) {
            // 因为 userDefClass 唯一，但非扩展对象方法的 env 是每次实例化的对象，缓存起来逻辑不对
            // 不同对象实例的，方法的环境不能共享!!! 所以干掉 env, 每次取出来临时把对象实例绑定方法的env
            if (value instanceof UserDefFun) {
                value = ((UserDefFun) value).cacheable();
            }
            metaTable().set(loc, key, value, true);
        }
        default void init(boolean isPrimitive) {
            BuildIn.initMetaTable(this);
            Meta.initMetaTable(this, isPrimitive);
        }
    }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    /**
     * class: 充当 static 属性或方法存储
     * fun: reflect
     */
    final class MetaTable {
        final static Map<ObjectValue/*ClassValue | FunValue*/, MetaTable> cache = new HashMap<>();
        /*private*/ static MetaTable get(@NotNull ObjectValue/*ClassValue | FunValue*/ v) {
            return cache.computeIfAbsent(v, MetaTable::new);
        }
        final Map<Object/*String|SymbolValue*/, Scope.Def> tbl = new LinkedHashMap<>();
        private MetaTable(@NotNull ObjectValue/*ClassValue | FunValue*/ v) {
            Cleaner.create(v, () -> MetaTable.cache.remove(v));
        }
        public @Nullable ObjectValue get(@SuppressWarnings("unused") Location loc, @NotNull SymbolValue key) { return doGet(key); }
        public @Nullable ObjectValue get(@SuppressWarnings("unused") Location loc, @NotNull String key) { return doGet(key); }
        public void set(Location loc, @NotNull String key, @NotNull ObjectValue value, boolean mut) { doSet(loc, key, value, mut); }
        public void set(Location loc, @NotNull SymbolValue key, @NotNull ObjectValue value, boolean mut) { doSet(loc, key, value, mut); }
        @Nullable ObjectValue doGet(Object key) {
            // 这里需要 null 否则有 contains 的歧义，无法判断
            Scope.Def v = tbl.get(key);
            if (v == null) {
                return null;
            } else {
                return ((ObjectValue) v.val);
            }
        }
        void doSet(Location loc, @NotNull Object key, @NotNull ObjectValue value, boolean mut) {
            Scope.Def def = tbl.get(key);
            if (def == null) {
                tbl.put(key, new Scope.Def(value, mut));
            } else {
                if(def.mut) {
                    tbl.put(key, new Scope.Def(value, mut));
                } else {
                    throw Error.runtime(loc, key + " 常量不允许修改");
                }
            }
        }
    }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    // 非实际 class 用来转发 metaTable 请求的
    final class NullClass implements ClassValue {
        private NullClass() { }
        @Override public String name() { return "Null"; }
        @Override public FunMeta construct() { throw Error.bug(None); }
    }
    final class ObjectClass implements ClassValue {
        private ObjectClass() { init(true); }
        @Override public String name() { return "Object"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists(), lists(), 0); }
        @Override public String toString() { return  "Class Object"; }
        @Override public @Nullable ClassValue parent() { return null; } // 只有 Object 没有 parent
        @Override public ObjectValue newInstance(Location loc, Value... args) { return Object(); }
        // 对任意对象的 for 操作, 转换为取对象的迭代器
        @Intrinsic(Sym_Iterable)
        public static Value intrinsic_iterable(Location loc, @NotNull Scope s, Value ...args) {
            ObjectValue self = ScopeObject.lookupThis(s);
            IteratorValue iter = self.iterator(loc);
            return iter == null ? Null : iter;
        }
        @Intrinsic(Sym_ToString)
        public static Value intrinsic_toString(Location loc, @NotNull Scope s, Value ...args) {
            return String(ScopeObject.lookupThis(s).toString());
        }
        @Intrinsic(value = Sym_Plus, requiredParamSz = 1, params = {"rhs"})
        public static Value intrinsic_plus(Location loc, @NotNull Scope s, Value ...args) {
            ObjectValue lhs = ScopeObject.lookupThis(s);
            ObjectValue rhs = ((ObjectValue) args[0]);
            StringValue lhsStr = lhs.getSymbol(loc, SymbolToString).asApply(loc).apply(loc, s).asString(loc);
            StringValue rhsStr = rhs.getSymbol(loc, SymbolToString).asApply(loc).apply(loc, s).asString(loc);
            return String(lhsStr.val + rhsStr.val);
        }
        @Intrinsic(Sym_Hash)
        public static Value intrinsic_hash(Location loc, @NotNull Scope s, Value ...args) {
            return Value.Int(ScopeObject.lookupThis(s).hashCode());
        }
        @Intrinsic(value = Sym_Equals, requiredParamSz = 1, params = {"that"})
        public static Value intrinsic_eq(Location loc, @NotNull Scope s, Value ...args) {
            ObjectValue lhs = ScopeObject.lookupThis(s);
            ObjectValue rhs = ((ObjectValue) args[0]);
            return Bool(Objects.equals(lhs, rhs));
        }
    }
    final class BoolClass implements ClassValue {
        private BoolClass() { init(true); }
        @Override public String name() { return "Bool"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists("bool"), lists(False), 0); }
        @Override public String toString() { return  "Class Bool"; }
        @Override public BoolValue newInstance(Location loc, Value... args) {
            if (args.length == 0) {
                return Value.Bool(false);
            } else {
                return args[0].asBool(loc);
            }
        }
    }
    final class IntClass implements ClassValue {
        private IntClass() { init(true); }
        @Override public String name() { return "Int"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists("int"), lists(Value.Int(0)), 0); }
        @Override public String toString() { return  "Class Int"; }
        @Override public IntValue newInstance(Location loc, Value... args) {
            if (args.length == 0) {
                return Value.Int(0);
            } else {
                return args[0].asInt(loc);
            }
        }
    }
    final class FloatClass implements ClassValue {
        private FloatClass() { init(true); }
        @Override public String name() { return "Float"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists("float"), lists(Float(0.0)), 0); }
        @Override public String toString() { return  "Class Float"; }
        @Override public FloatValue newInstance(Location loc, Value... args) {
            if (args.length == 0) {
                return Value.Float(0.0);
            } else {
                return args[0].asFloat(loc);
            }
        }
    }
    final class SymbolClass implements ClassValue {
        private SymbolClass() { init(true); }
        @Override public String name() { return "Symbol"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists("symbol"), lists(Null), 0); }
        @Override public String toString() { return  "Class Symbol"; }
        @Override public SymbolValue newInstance(Location loc, Value... args) {
            if (args.length == 0) {
                return Symbol(null);
            } else {
                if (args[0] == Null) {
                    return Symbol(null);
                } else {
                    return Symbol(args[0].asString(loc).val);
                }
            }
        }
    }
    final class StringClass implements ClassValue {
        private StringClass() { init(true); }
        @Override public String name() { return "String"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists("string"), lists(String("")), 0); }
        @Override public String toString() { return  "Class String"; }
        @Override public StringValue newInstance(Location loc, Value... args) {
            if (args.length == 0) {
                return Value.String("");
            } else {
                return args[0].asString(loc);
            }
        }
    }
    final class ListClass implements ClassValue {
        private ListClass() { init(true); }
        @Override public String name() { return "List"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists("list"), lists(Value.List()), 0); }
        @Override public String toString() { return  "Class List"; }
        @Override public ListValue newInstance(Location loc, Value... args) {
            return Value.List(args);
        }
//        @Intrinsic("`+`")
//        public static Value intrinsic_plus(Location loc, @NotNull Scope s, Value ...args) {
//            ListValue lhs = ScopeObject.lookupThis(s).asList(loc);
//            Value rhs = args[0];
//            if (rhs instanceof ListValue) {
//                ListValue v = Value.List();
//                v.list.addAll(lhs.list);
//                v.list.addAll(((ListValue) rhs).list);
//                return v;
//            }
//            return ObjectClass.intrinsic_plus(loc, s, args);
//        }
    }
    final class MapClass implements ClassValue {
        private MapClass() { init(true); }
        @Override public String name() { return "Map"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists(), lists(), 0); }
        @Override public String toString() { return  "Class Map"; }
        @Override public MapValue newInstance(Location loc, Value... args) { return Map(); }
    }
    final class IteratorClass implements ClassValue {
        private IteratorClass() { init(true); }
        @Override public String name() { return "Iterator"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists(/*String("objectValue")*/), lists(/*Null*/), 0); }
        @Override public String toString() { return  "Class Iterator"; }
        // new Iterator(object =  {fun hasNext() = ???   fun next() = ???})
        @Override public /*IteratorValue*/ObjectValue newInstance(Location loc, Value... args) {
            Expect.expect(loc, args.length == 1, "参数错误,期望 Iterator(val: Object)");
            IteratorValue iterV = IteratorValue.fromObjectValue(loc, args[0].asObject(loc));
            if (iterV == null) {
                return Null;
            }
            return iterV;
        }
    }
    final class FunClass implements ClassValue {
        private FunClass() { init(true); }
        @Override public String name() { return "Fun"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists(), lists(), 0); }
        @Override public String toString() { return  "Class Fun"; }
        @Intrinsic(value = Fun_Apply, requiredParamSz = 1, params = {"args"})
        public static Value intrinsic_apply(Location loc, @NotNull Scope s, Value ...args) {
            FunValue f = ((FunValue) ScopeObject.lookupThis(s));
            Expect.exactArgs(loc, 1, args.length);
            ListValue realArgs = Expect.as(loc, args[0], ListValue.class);
            Value[] values = realArgs.list.toArray(new Value[realArgs.size()]);
            return f.apply(loc, values);
        }
    }
    final class ClassClass implements ClassValue {
        private ClassClass() { init(true); }
        @Override public String name() { return "Class"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists(), lists(), 0); }
        @Override public String toString() { return  "Class Class"; }
    }
    final class JavaValueClass implements ClassValue {
        private JavaValueClass() { init(true); }
        @Override public String name() { return "JavaValue"; }
        @Override public FunMeta construct() { return primitiveCtor(this, lists(), lists(), 0); }
        @Override public String toString() { return  "Class JavaValue"; }
    }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    // 内部变量, 标记常量未初始化 unbounded
    final class Undefined implements Value {
        @Override public ClassValue type(Location loc) { return ClassClass; }
        @Override public String toString() { return "Undefined"; }
    }
    final class NullValue implements ObjectValue, Boxed {
        private NullValue() { }
        static RuntimeException npe(Location loc) { return Error.nullPointerError(loc, "空指针啦"); }
        static RuntimeException npe(Location loc, Scope s) { return Error.nullPointerError(loc, s, "空指针啦"); }
        @Override public JavaValue unbox() { return JavaValue(null); }
        @Override public Value id() { return String("null"); }
        @Override public Applicative asApply(Location loc) { throw npe(loc); }
        @Override public void setProperty(Location loc, @NotNull String key, @NotNull Value value) { throw npe(loc); }
        @Override public @Nullable Value getProperty(Location loc, @NotNull String key) {  throw npe(loc);}
        @Override public ClassValue type(Location loc) { return ClassNull; }
        @Override public boolean is(Location loc, @NotNull ClassValue type) { return false; }
        @Override public String toString() { return "null"; }
        // @Override public @NotNull Value unaryOperator(Location loc, Scope s, TokenType tok) { throw npe(loc, s); }
        // @Override public @NotNull Value binaryOperator(Location loc, Scope s, TokenType tok, @NotNull Value rhs) { throw npe(loc, s); }
        // @Override public @NotNull ObjectValue getSymbol(Location loc, @NotNull SymbolValue key) { throw npe(loc); }
        // @Override public void setSymbol(Location loc, @NotNull SymbolValue key, @NotNull ObjectValue value) { throw npe(loc); }
        // @Override public ClassValue type(Location loc) { throw npe(loc); }
        // @Override public @NotNull ObjectValue bind(Location loc, @NotNull ObjectValue self) { throw npe(loc); }
        // @Override public BoolValue asBool(Location loc) { return Value.Bool(false); }
        // @Override public IntValue asInt(Location loc) { return Value.Int(0); }
        // @Override public FloatValue asFloat(Location loc) { return Value.Float(0.0); }
    }
    final class BoolValue implements ObjectValue, Boxed {
        public final boolean val;
        private BoolValue(boolean val) { this.val = val; }
        @Override public Value id() { return String("bool#" + val); }
        @Override public JavaValue unbox() { return JavaValue(val); }
        BoolValue inverse() { return Value.Bool(!val); }
        @Override public ClassValue type(Location loc) { return ClassBool; }
        // @Override public IntValue asInt(Location loc) { return Value.Int(this.val ? 1 : 0); }
        // @Override public FloatValue asFloat(Location loc) { return Value.Float(val ? 1 : 0); }
        @Override public BoolValue asBool(Location loc) { return this; }
        @Override public String toString() { return val ? "true" : "false"; }
        @Override public int hashCode() { return this.val ? 79 : 97; }
        @Override public boolean equals(Object o) {
            return o == this || o instanceof BoolValue && val == ((BoolValue) o).val;
        }
    }
    interface NumValue extends ObjectValue { }
    final class IntValue implements NumValue, Boxed {
        public final long val;
        // remove
        public final int base;
        private IntValue(long val, int base) {
            this.val = val;
            this.base = base;
        }
        @Override public Value id() { return String("int#" + val); }
        @Used public char charValue() { return ((char) val); }
        @Override public JavaValue unbox() { return JavaValue(val); }
        @Override public ClassValue type(Location loc) { return ClassInt; }
        // @Override public BoolValue asBool(Location loc) { return Value.Bool(val != 0); }
        @Override public IntValue asInt(Location loc) { return this; }
        @Override public FloatValue asFloat(Location loc) { return Value.Float(val); }
        @Override public String toString() { return String.valueOf(val); }
        @Override public int hashCode() { return 59 + (int) (val >>> 32 ^ val); }
        @Override public boolean equals(Object o) {
            return o == this || o instanceof IntValue && val == ((IntValue) o).val;
        }
    }
    final class FloatValue implements NumValue, Boxed {
        public final double val;
        private FloatValue(double val) { this.val = val; }
        @Override public Value id() { return String("float#" + val); }
        @Override public JavaValue unbox() { return JavaValue(val); }
        @Override public ClassValue type(Location loc) { return ClassFloat; }
        // @Override public BoolValue asBool(Location loc) { return Value.Bool(val != 0); }
        @Override public IntValue asInt(Location loc) { return Value.Int((long) val); }
        @Override public FloatValue asFloat(Location loc) { return this; }
        @Override public String toString() { return String.valueOf(val); }
        @Override public int hashCode() {
            long v = Double.doubleToLongBits(val);
            return  59 + (int) (v >>> 32 ^ v);
        }
        @Override public boolean equals(Object o) {
            return this == o || o instanceof FloatValue && Double.compare(((FloatValue) o).val, val) == 0;
        }
    }
    final class SymbolValue implements ObjectValue {
        static long i = 0;
        static final Map<String, SymbolValue> cache = new HashMap<>();
        private static SymbolValue unique() {
            String k = "π" + i++;
            if (cache.containsKey(k)) return unique();
            return of(k);
        }
        private static SymbolValue of(String val) {
            if (val == null) return unique();
            return cache.computeIfAbsent(val, SymbolValue::new);
        }

        public final String val;
        private SymbolValue(@NotNull String val) { this.val = val; }
        @Override public Value id() { return String("sym#" + val); }
        @Override public ClassValue type(Location loc) { return ClassSymbol; }
        @Override public SymbolValue asSymbol(Location loc) { return this; }
        // @Override public StringValue asStr(Location loc) { return Value.String(val); }
        @Override public String toString() { return /*"Symbol " + */ "`" + val + "`"; }
    }
    final class StringValue implements ObjectValue, Boxed {
        public final String val;
        private StringValue(@NotNull String val) { this.val = val; }
        public Value get(@SuppressWarnings("unused") Location loc, int idx) {
            if (idx < 0) {
                idx = val.length() + idx;
            }
            if (idx < 0 || idx >= val.length()) {
                return Null;
            }
            return String(String.valueOf(val.charAt(idx)));
        }
        @Override public Value id() { return String("string#" + val); }
        @Override public JavaValue unbox() { return JavaValue(val); }
        @Override public ClassValue type(Location loc) { return ClassString; }
        @Override public SymbolValue asSymbol(Location loc) { return Symbol(val); }
        @Override public StringValue asString(Location loc) { return this; }
        // @Override public BoolValue asBool(Location loc) { return Value.Bool(!val.isEmpty()); }
        // @Override public IntValue asInt(Location loc) { return Literals.parseInt(loc, val); }
        // @Override public FloatValue asFloat(Location loc) { return Literals.parseFloat(loc, val); }
        @Override public String toString() { return val; }
        @Override public int hashCode() { return val.hashCode(); }
        @Override public boolean equals(Object o) {
            return o == this || o instanceof StringValue && val.equals(((StringValue) o).val);
        }
    }
    // 等同于 Java 的 Object
    final class StdObject implements ObjectValue {
        public final static StdObject Immutable = immutableObject();
        private static StdObject immutableObject() {
            Map<String, Value> m = new LinkedHashMap<>();
            m.put(Key_Class, ClassObject);
            m.put(Key_Object_Id, Value.Int(0));
            return new StdObject(Collections.unmodifiableMap(m));
        }
        public final Map<String, Value> table;
        private StdObject() { this(new LinkedHashMap<>()); }
        private StdObject(Map<String, Value> tbl) { table = tbl; }
        @Override public Value id() { return String("stdObject#" + System.identityHashCode(this)); }
        @Override public ClassValue type(Location loc) { return ClassObject; }
        @Override public @Nullable Value getSelfProperty(Location loc, @NotNull String key, boolean bind) { return table.get(key); }
        @Override public void doSetProperty(Location loc, @NotNull String key, @NotNull Value value) { table.put(key, value); }
        @Override public @NotNull IteratorValue iterator(Location loc) {
            return Iterator(table.entrySet().stream().map(v -> Pair(String(v.getKey()), v.getValue())).iterator());
        }
        @Override public String toString() { return type(Location.None) + " " + table.toString(); }
        // todo
        @Override public int hashCode() { return table.hashCode(); }
        // todo
        @Override public boolean equals(Object o) {
            return o == this || o instanceof StdObject && table.equals(((StdObject) o).table);
        }
    }
    // ObjectValue -> Iterator
    final class IteratorValue implements ObjectValue, Iterator<Value> {
        static {
            Applicative hasNext = (loc, s, args) -> Value.Bool(ScopeObject.lookupThis(s).asIterator(loc).hasNext());
            Applicative next = (loc, s, args) -> ScopeObject.lookupThis(s).asIterator(loc).next();
            FunValue hasNextFun = Value.Fun(Class_Iterator, Fun_Iterator_HasNext, lists(), lists(), 0, hasNext);
            FunValue nextFun = Value.Fun(Class_Iterator, Fun_Iterator_Next, lists(), lists(), 0, next);
            ClassIterator.setProperty(Location.None, Fun_Iterator_HasNext, hasNextFun);
            ClassIterator.setProperty(Location.None, Fun_Iterator_Next, nextFun);
        }
        public final @NotNull Iterator<? extends Value> iter;
        private IteratorValue(@NotNull Iterator<? extends Value> iter) { this.iter = iter; }
        @Override public Value id() { return String("iterator#" + System.identityHashCode(this)); }
        @Override public IteratorValue iterator(Location loc) { return this; }
        @Override public ClassValue type(Location loc) { return ClassIterator; }
        @Override public IteratorValue asIterator(Location loc) { return this; }
        @Override public boolean hasNext() { return iter.hasNext(); }
        @Override public Value next() { return iter.next(); }
        @Override public String toString() { return "Iterator"; }
        static @Nullable IteratorValue fromObjectValue(Location loc, @NotNull ObjectValue objV) {
            Value hasNext = objV.getProperty(loc, Fun_Iterator_HasNext);
            Value next = objV.getProperty(loc, Fun_Iterator_Next);
            // boolean hasIter = hasNext instanceof Applicative && next instanceof Applicative;
            boolean hasIter = hasNext != null && next != null &&
                    hasNext.type(loc).isSubClassOf(ClassFun) && next.type(loc).isSubClassOf(ClassFun);
            if (hasIter) {
                return Iterator(new Iterator<Value>() {
                    @Override public boolean hasNext() { return ((Applicative) hasNext).apply(loc).asBool(loc).val; }
                    @Override public Value next() { return ((Applicative) next).apply(loc); }
                });
            }
            return null;
        }
    }
    final class ListValue implements ObjectValue, Boxed {
        public final List<Value> list;
        private ListValue(List<Value> list) {
            this.list = list;
        }
        private ListValue(Value ...vals) {
            list = new ArrayList<>();
            list.addAll(Arrays.asList(vals));
        }
        public int size() { return list.size(); }
        public void add(Value v) { list.add(v); }
        public Value get(Location loc, int idx) {
            if (idx < 0) {
                idx = list.size() + idx;
            }
            rangeCheck(loc, idx);
            if (idx < 0 || idx >= list.size()) {
                return Null;
            }
            Value value = list.get(idx);
            return value == null ? Null : value;
        }
        public void set(@SuppressWarnings("unused") Location loc, int idx, @NotNull Value v) {
            // rangeCheck(loc, idx);
            if (list instanceof ArrayList) {
                ((ArrayList<Value>) list).ensureCapacity(idx + 1);
            }
            for (int i = list.size() - 1; i < idx; i++) {
                list.add(Null);
            }
            list.set(idx, v);
        }
        void rangeCheck(Location loc, int idx) {
            if (idx < 0 || idx >= list.size()) {
                throw Error.runtime(loc, "数组size(" + size() + ")下标" + idx + "越界了😶");
            }
        }
        @Override public Value id() { return String("list#" + System.identityHashCode(this)); }
        @Override public JavaValue unbox() { return JavaValue(list); }
        @Override public ClassValue type(Location loc) { return ClassList; }
        @Override public IteratorValue iterator(Location loc) { return Iterator(list.iterator()); }
        // @Override public BoolValue asBool(Location loc) { return Value.Bool(!list.isEmpty()); }
        @Override public ListValue asList(Location loc) { return this; }
        @Override public String toString() { return "List " + list.toString(); }
        @Override public int hashCode() { return list.hashCode(); }
        @Override public boolean equals(Object o) {
            return o == this || o instanceof ListValue && list.equals(((ListValue) o).list);
        }
    }
    final class MapValue implements ObjectValue, Boxed {
        public final Map<Value, Value> map = new LinkedHashMap<>();
        private MapValue() { }
        int size() { return map.size(); }
        public Value get(@NotNull Value key) { return map.getOrDefault(key, Null); }
        public void put(@NotNull Value key, @NotNull Value value) { map.put(key, value); }
        @Override public Value id() { return String("map#" + System.identityHashCode(this)); }
        @Override public JavaValue unbox() { return JavaValue(map); }
        @Override public ClassValue type(Location loc) { return ClassMap; }
        @Override public @NotNull IteratorValue iterator(Location loc) {
            return Iterator(map.entrySet().stream().map(v -> Pair(v.getKey(), v.getValue())).iterator());
        }
        // @Override public BoolValue asBool(Location loc) { return Value.Bool(!map.isEmpty()); }
        @Override public MapValue asMap(Location loc) { return this; }
        @Override public String toString() { return "Map " + map.toString(); }
        @Override public int hashCode() { return map.hashCode(); }
        @Override public boolean equals(Object o) {
            return o == this || o instanceof MapValue && map.equals(((MapValue) o).map);
        }
    }
    final class ScopeObject implements ObjectValue {
        static long objectId = 1; // 0 被 object 使用
        // 父子对象 id 相同 (创建对象的链条中对象 id 相同)
        // bind 之后对象 id 相同
        static long objectId(Scope scope) {
            Scope objScopeRoot = scope.upstream(ScopeObject::is);
            Value v = objScopeRoot.lookupLocal(Key_Object_Id);
            if (v == null) {
                long id = objectId++;
                objScopeRoot.define(None, Key_Object_Id, Value.Int(id), false);
                return id;
            } else {
                return v.asInt(None).val;
            }
        }

        public final long id;
        @NotNull public final Scope scope;
        @NotNull public final ClassValue type;
        private ScopeObject(@NotNull ClassValue type, @NotNull Scope scope, boolean defClass) {
            this.type = type;
            this.scope = scope;
            if (defClass) {
                defineClass();
            }
            this.id = objectId(scope);
        }
        // scope object 内部塞一个 class 值有两个作用
        // 1. 用来区分环境 scope 与 class scope
        // 2. 用来标记该 scope 所属 class, 反向索引 class
        @Used void defineClass() {
            Value value = scope.lookupLocal(Key_Class);
            if (value == null) {
                scope.define(None, Key_Class, type, false);
            } else {
                if (value != type) throw Error.bug(None);
            }
        }
        // 这么判断之所以可以
        // 1. 没办法在某个作用域声明一个名字是 class 的变量
        // 2. 没办法手动修改(unset)掉对象的 class 属性
        // 所以有 class 的 scope 就一定是 objectScope
        static boolean is(@Nullable Scope scope) {
            return scope != null && scope.lookupLocal(Key_Class) instanceof ClassValue;
        }
        // 与 ScopeObject.type.parent() 区别 两个
        // 使用场景：
        //      只有 Scope 没有 ScopeObject 时候只能用 lookupClass来反向索引，
        //      当有 ScopeObject, 则使用 ScopeObject.type.parent()
        // 返回值：
        //      ScopeObject.type.parent() 只有 type 为 ClassObject 时 返回 null
        //      lookupClass 因为参数是 scope，所以语义是 scope 没有 class 就返回 null
        //      对与class A
        //          lookupClass(A.scope) 返回 A, lookupClass(A.scope.parent) 返回 null
        //          ScopeObject.type 返回A  type.parent() 返回 ClassObject，注意区别
        static @Nullable ClassValue lookupClass(@Nullable Scope scope) {
            if (is(scope)) {
                return ((ClassValue) scope.lookupLocal(Key_Class));
            } else {
                return null;
            }
        }
        static @NotNull ObjectValue lookupThis(@NotNull Scope scope) {
            // 这里都依赖 objectValue 的 bind
            Value v = scope.lookup(TokenType.THIS.name);
            assert v instanceof ObjectValue;
            return ((ObjectValue) v);
        }
        static @NotNull ObjectValue superObject(@NotNull ScopeObject scopeObject) {
            ClassValue parentClass = scopeObject.type.parent();
            assert parentClass != null;
            return superObject(parentClass, scopeObject.scope);
        }
        static @NotNull ObjectValue superObject(@NotNull UserDefClass udc, @NotNull Scope objectScope) {
            return superObject(udc.parent(), objectScope);
        }
        static @NotNull ObjectValue superObject(@NotNull ClassValue parentClass, @NotNull Scope objectScope) {
            if (parentClass == ClassObject) {
                // assert objectScope.parent == null || objectScope.parent == udc.env;
                assert !is(objectScope.parent);
                return StdObject.Immutable;
            } else {
                // assert objectScope.parent != null && objectScope.parent != udc.env;
                assert is(objectScope.parent);
                return ScopeObject(parentClass, objectScope.parent);
            }
        }
        @Override public Value id() { return Value.Int(id); }
        @Override public @Nullable Value getSelfProperty(Location loc, @NotNull String key, boolean bind) {
            // 这里 lookupProperty 注意：查找变量无限制上溯作用域 和 查找属性只查找继承树
            Value v = scope.lookup(key, ScopeObject::is);
            if (v == Undefined) {
                throw Error.runtime(loc, key + " 未初始化");
            }
            return v;
        }
        @Override public void doSetProperty(Location loc, @NotNull String key, @NotNull Value value) { scope.put(loc, key, value); }
        @Override public ClassValue type(Location loc) { return type; }
        @Override public IteratorValue iterator(Location loc) {
            // 自身就有 hasNext next
            IteratorValue iterVal = IteratorValue.fromObjectValue(loc, this);
            if (iterVal != null) {
                return iterVal;
            }
            // ClassObject 默认加了 SymbolIterable, 行为是 iterator() 会死循环
            ObjectValue iterable = lookupSymbol(loc, SymbolIterable, type -> type != ClassObject);
            // iterable.is(loc, ClassFun)
            if (iterable instanceof FunValue) {
                Value iterator = iterable.asFun(loc).apply(loc);
                // iterator.is(ClassIterator)
                if (iterator instanceof IteratorValue) {
                    return (IteratorValue) iterator;
                }
                if (iterator instanceof ObjectValue) {
                    return IteratorValue.fromObjectValue(loc, ((ObjectValue) iterator));
                }
            }
            return null;
        }
        @Override public int hashCode() {
            // ObjectClass.hash 会转发到各个对象自己的 hash, 需要检查一下，否则 stackOverflow
            ObjectValue hash = lookupSymbol(None, SymbolHash, it -> it != ClassObject);
            if (hash == Null) {
                return Objects.hash(id);
            } else {
                return (int) hash.asApply(None).apply(None).asInt(None).val;
            }
        }
        @Override public boolean equals(Object o) {
            // ObjectClass.equals 会转发到各个对象自己的 equals, 需要检查一下，否则 stackOverflow
            ObjectValue equals = lookupSymbol(None, SymbolEquals, it -> it != ClassObject);
            if (equals == Null) {
                return this == o || o instanceof ScopeObject && id == ((ScopeObject) o).id;
            } else {
                return equals.asApply(None).apply(None, ((Value) o)).asBool(None).val;
            }
        }
        @Override public String toString() {
            // ObjectClass.equals 会转发到各个对象自己的 equals, 需要检查一下，否则 stackOverflow
            ObjectValue toString = lookupSymbol(None, SymbolToString, it -> it != ClassObject);
            if (toString == Null) {
                return type.name();
            } else {
                return toString.asApply(None).apply(None).asString(None).val;
            }
        }
    }
    final class PrototypeValue implements ObjectValue {
        public final ObjectValue proto;
        public final Map<String, Scope.Def> object = new LinkedHashMap<>();
        private PrototypeValue(ObjectValue proto) { this.proto = proto; }

        @Override public Value id() { return proto.id(); }
        @Override public @Nullable Value getSelfProperty(Location loc, @NotNull String key, boolean bind) {
            Value val = getLocal(key);
            if (val != null) {
                return val;
            } else if (proto != null) {
                return proto.getProperty(loc, key);
            } else {
                return null;
            }
        }
        @Override public void doSetProperty(Location loc, @NotNull String key, @NotNull Value value) {
            // 原型继承的关键 & 不同于 Scope 的 define/assign，而是 define or assign-local
            // 不修改parent，只是在本地遮盖了 parent，语义上达成修改，并且实现继承复用
            // ❗️❗️❗️只修改本地，不存在在定义，不修改 parent
            putLocal(key, value);
        }
        @Nullable Value getLocal(String key) {
            Scope.Def def = object.get(key);
            return def == null ? null : def.val;
        }
        void putLocal(String key, Value value) {
            Scope.Def def = object.get(key);
            if (def == null) {
                object.put(key, new Scope.Def(value));
            } else {
                // def.val = value;
                object.put(key, new Scope.Def(value, def.mut));
            }
        }
        @Override public @NotNull ObjectValue getSymbol(Location loc, @NotNull SymbolValue key) { return proto.getSymbol(loc, key); }
        @Override public void setSymbol(Location loc, @NotNull SymbolValue key, @NotNull ObjectValue value) {
            proto.setSymbol(loc, key, value);
        }
        @Override public ClassValue type(Location loc) { return proto.type(loc); }
        @Override public String toString() { return "Proto " + object; }
        // todo
        @Override public int hashCode() { return object.hashCode(); }
        // todo
        @Override public boolean equals(Object o) {
            return this == o || o instanceof PrototypeValue
                    && Objects.equals(object, ((PrototypeValue) o).object)
                    && Objects.equals(proto, ((PrototypeValue) o).proto);
        }
    }
    class StdFun implements FunValue, Applicative {
        public final @Nullable String className;
        public final String name;
        public final Scope env;
        public final Applicative apply;
        public final List<String> params;
        public final List</*@Nullable*/Value> defaults;
        public final int requiredParamSz;
        private StdFun(@Nullable String className, @NotNull String name,
                       @NotNull Scope env, @NotNull Applicative apply,
                       List<String> params, List</*@Nullable*/Value> defaults,
                       int requiredParamSz) {
            this.className = className;
            this.name = name;
            this.env = env;
            this.apply = apply;
            this.params = params;
            this.defaults = defaults;
            this.requiredParamSz = requiredParamSz;
            // 继承的不执行，其他ClassValue FunValue 都是 final, 不需要 guard
            if (this.getClass() == StdFun.class) {
                init(true);
            }
        }
        Scope bindScope(Scope env, Location loc, ObjectValue self) {
            Value superValue;
            if (self instanceof ScopeObject) {
                superValue = ScopeObject.superObject((ScopeObject) self);
            } else {
                if (self.type(loc) == ClassObject) {
                    // 处理 new Object().getSuper() == null
                    superValue = Null;
                } else {
                    // 这里藏了一个坑... 要求非ScopeObject都没有 非extends object的继承关系
                    superValue = StdObject.Immutable;
                }
            }
            env.define(loc, TokenType.THIS.name, self, false);
            env.define(loc, TokenType.SUPER.name, superValue, false);
            return env;
        }
        @Override public Value id() { return String("stdFun#" + System.identityHashCode(this)); }
        @Override public FunMeta meta() { return primitiveFun(name, className, params, defaults, requiredParamSz); }
        @Override public @NotNull ObjectValue doBind(Location loc, @NotNull ObjectValue self) {
            Scope env1 = bindScope(env.inherit(), loc, self);
            return new StdFun(className, name, env1, apply, params, defaults, requiredParamSz);
        }
        public Value apply(Location loc, Value... args) { return apply.apply(loc, env, args); }
        @Override public Value apply(Location loc, @NotNull Scope s, Value... args) { return apply.apply(loc, env, args); }
        @Override public String toString() {
            if (className == null) {
                return "Fun " + name;
            } else {
                return "Fun " + className + "." + name;
            }
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StdFun stdFun = (StdFun) o;
            return Objects.equals(className, stdFun.className) &&
                    name.equals(stdFun.name) &&
                    apply.equals(stdFun.apply);
        }
        @Override public int hashCode() { return apply.hashCode(); }
    }
    final class UserDefFun extends StdFun {
        public final FunMeta funMeta;
        private UserDefFun(@NotNull FunMeta funMeta, @NotNull Scope env, @NotNull Applicative apply) {
            super(funMeta.className, funMeta.name, env, apply, funMeta.params, funMeta.defaults, funMeta.requiredParamSz);
            this.funMeta = funMeta;
            init(false);
        }
        // 因为 userDefClass 唯一，但非扩展对象方法的 env 是每次实例化的对象，缓存起来逻辑不对
        // 不同对象实例的，方法的环境不能共享!!! 所以干掉 env, 每次取出来临时把对象实例绑定方法的env
        UserDefFun cacheable() {
            return isPlainMethod() ? new UserDefFun(funMeta, Scope.Immutable, apply) : this;
        }
        boolean isMethod() { return className != null; }
        boolean isPlainMethod() { return isMethod() && !funMeta.extend; }
        boolean isUserDefPlainMethod(ObjectValue self) { return isPlainMethod() && self instanceof ScopeObject; }
        @Override public FunMeta meta() { return funMeta; }
        @Override public @NotNull ObjectValue doBind(Location loc, @NotNull ObjectValue self) {
            Scope env1;
            if (isUserDefPlainMethod(self)) {
                env1 = ((ScopeObject) self).scope.inherit();
            } else {
                env1 = env.inherit();
            }
            env1 = bindScope(env1, loc, self);
            return new UserDefFun(funMeta, env1, apply);
        }
        @Override public int hashCode() { return Objects.hash(funMeta); }
        @Override public boolean equals(Object obj) {
            return this == obj || obj instanceof UserDefFun && funMeta.equals(((UserDefFun) obj).funMeta);
        }
    }
    final class UserDefClass implements ClassValue {
        public final ClassMeta classMeta;
        public final Scope env;
        public Applicative apply;
        private UserDefClass(ClassMeta classMeta, @NotNull Scope env, Function<UserDefClass, Applicative> maker) {
            this.classMeta = classMeta;
            this.env = env;
            this.apply = maker.apply(this);
            init(false);
        }
//        public @Nullable Map<Ast.FunDef, Scope> extendsScope = null;
//        public void addExtendsMethod(@NotNull Ast.FunDef funDef, @NotNull Scope s) {
//            if (extendsScope == null) {
//                extendsScope = new HashMap<>();
//            }
//            cls.methods.add(funDef);
//            extendsScope.put(funDef, s);
//        }
        @Override public String name() { return classMeta.name; }
        @Override public FunMeta construct() { return classMeta.ctor; }
        @Override public ClassMeta meta() { return classMeta; }
        @Override public @NotNull ClassValue parent() {
            // 处理所有用户定义对象，默认继承 Object
            if (classMeta.parent == null) {
                return ClassObject;
            }
            // ClassValue parent = env.lookupClass(None, classMeta.parent.name);
            // if (parent == null) throw Error.bug(None);
            return classMeta.parent;
        }
        @Override public ObjectValue newInstance(Location loc, Value... args) {
            return apply.apply(loc, env, args).asObject(loc);
        }
        @Override public String toString() { return "Class " + classMeta.name; }
    }
    final class JavaValue implements ObjectValue {
        public final Object value;
        private JavaValue(Object value) { this.value = value; }
        @Override public Value id() { return String("javaValue#" + System.identityHashCode(this)); }
        @Override public ClassValue type(Location loc) { return ClassJavaValue; }
        @Override public JavaValue asJavaValue(Location loc) { return this; }
        @Override public String toString() { return "JavaValue " + value; }
        static Object unbox(Location loc, Value arg) {
            return arg.asJavaValue(loc).value;
        }
        static Object[] unbox(Location loc, Value ...args) {
            Object[] unbox = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                unbox[i] = unbox(loc, args[i]);
            }
            return unbox;
        }
        @Override public boolean equals(Object o) {
            return this == o || o instanceof JavaValue && Objects.equals(value, ((JavaValue) o).value);
        }
        @Override public int hashCode() { return Objects.hash(value); }
    }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    class VarMeta {
        @NotNull public final String id;
        public final boolean mut;
        public final @NotNull Consumer<Scope> defVar;
        VarMeta(@NotNull String id, boolean mut, @NotNull Consumer<Scope> defVar) {
            this.id = id;
            this.mut = mut;
            this.defVar = defVar;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VarMeta varMeta = (VarMeta) o;
            return mut == varMeta.mut && id.equals(varMeta.id);
        }
        @Override public int hashCode() { return Objects.hash(id, mut); }
    }
    class FunMeta {
        @NotNull public final String name; // 注意：`sym`
        @Nullable public final String className;
        // @Nullable public /*final ClassMeta*/ClassValue classValue; todo 代替 className
        @NotNull public final List<String> params;
        @NotNull public final List</*@Nullable*/Value> defaults;
        public final boolean ctor;
        public final boolean symbol;
        public final boolean extend;
        public final boolean arrow;
        public final boolean generatedSuperCall;
        public final int requiredParamSz;
        public final @NotNull Consumer<Scope> defFun;
        FunMeta(@NotNull String name,
                @Nullable String className,
                @NotNull List<String> params,
                @NotNull List</*@Nullable*/Value> defaults,
                boolean ctor,
                boolean symbol,
                boolean extend,
                boolean arrow,
                boolean generatedSuperCall,
                int requiredParamSz,
                @NotNull Consumer<Scope> defFun
        ) {
            this.className = className;
            this.name = name;
            this.params = params;
            this.defaults = defaults;
            this.ctor = ctor;
            this.symbol = symbol;
            this.extend = extend;
            this.arrow = arrow;
            this.generatedSuperCall = generatedSuperCall;
            this.requiredParamSz = requiredParamSz;
            this.defFun = defFun;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FunMeta funMeta = (FunMeta) o;
            return ctor == funMeta.ctor &&
                    symbol == funMeta.symbol &&
                    extend == funMeta.extend &&
                    arrow == funMeta.arrow &&
                    generatedSuperCall == funMeta.generatedSuperCall &&
                    requiredParamSz == funMeta.requiredParamSz &&
                    Objects.equals(className, funMeta.className) &&
                    name.equals(funMeta.name) &&
                    params.equals(funMeta.params) &&
                    defaults.equals(funMeta.defaults);
        }
        @Override public int hashCode() { return Objects.hash(className, name, params, defaults, ctor, symbol, extend, arrow, generatedSuperCall, requiredParamSz); }
        @Override public String toString() {
            return "fun → " + name;
        }
    }
    class ClassMeta {
        @NotNull public final String name;
        @Nullable public final ClassValue parent;
        @NotNull public final List<VarMeta> props;
        @NotNull public final List<FunMeta> methods;
        @NotNull public final FunMeta ctor;
        public final boolean sealed;
        public final boolean tagged; // discriminated;
        public final boolean throwable;
        ClassMeta(@NotNull String name, @Nullable ClassValue parent, @NotNull List<VarMeta> props,
                  @NotNull List<FunMeta> methods, @NotNull FunMeta ctor,
                  boolean sealed, boolean tagged, boolean throwable) {
            this.name = name;
            this.parent = parent;
            this.props = props;
            this.methods = methods;
            this.ctor = ctor;
            this.sealed = sealed;
            this.tagged = tagged;
            this.throwable = throwable;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassMeta classMeta = (ClassMeta) o;
            return sealed == classMeta.sealed &&
                    tagged == classMeta.tagged &&
                    throwable == classMeta.throwable &&
                    name.equals(classMeta.name) &&
                    Objects.equals(parent, classMeta.parent) &&
                    props.equals(classMeta.props) &&
                    methods.equals(classMeta.methods) &&
                    ctor.equals(classMeta.ctor);
        }
        @Override public int hashCode() { return Objects.hash(name, parent, props, methods, ctor, sealed, tagged, throwable); }
        @Override public String toString() { return "class → " + name; }
    }

    final class Meta {
        private static Consumer<Scope> placeholder = s -> {};
        private static ObjectValue var(Location loc, VarMeta var) {
            ObjectValue ov = Value.Object();
            ov.setProperty(loc, "name", String(var.id));
            ov.setProperty(loc, "mutable", Bool(var.mut));
            return ov;
        }
        private static ObjectValue fun(Location loc, FunMeta fun) {
            ObjectValue ov = Value.Object();
            ov.setProperty(loc, "isPrimitive", False);
            ov.setProperty(loc, "name", String(fun.name));
            ov.setProperty(loc, "className", fun.className == null ? Null : String(fun.className)); // todo 把 name 换成 对象
            ov.setProperty(loc, "parameters", Value.List(fun.params.stream().map(Value::String).toArray(Value[]::new)));
            ov.setProperty(loc, "defaults", Value.List(fun.defaults.stream().map(it -> it == null ? Null : it).toArray(Value[]::new)));
            ov.setProperty(loc, "isConstruct", Bool(fun.ctor));
            ov.setProperty(loc, "isSymbol", Bool(fun.symbol));
            // ov.setProperty(loc, "isExtend", Bool(fun.extend));
            ov.setProperty(loc, "isArrow", Bool(fun.arrow));
            ov.setProperty(loc, "requiredParametersSize", Int(fun.requiredParamSz));
            return ov;
        }
        static ClassMeta primitiveClass(ClassValue cv, @Nullable ClassValue parent, FunMeta ctor) {
            return classMeta(cv.name(), parent, lists(), lists(), ctor, cv != ClassObject, false, false);
        }
        static FunMeta primitiveCtor(ClassValue cv, List<String> params, List</*@Nullable*/Value> defaults,
                                     @SuppressWarnings("SameParameterValue") int requiredParamSz) {
            return funMeta("construct", cv.name(), params, defaults,
                    true, false, false, false, false, requiredParamSz,  placeholder);
        }
        static FunMeta primitiveFun(String name, String className, List<String> params, List</*@Nullable*/Value> defaults, int requiredParamSz) {
            return funMeta(name, className,  params, defaults,
                    false, false, false, false, false, requiredParamSz, placeholder);
        }
        static void initMetaTable(FunValue fv, boolean isPrimitive) {
            if (MetaTable.cache.containsKey(fv)) {
                // todo Class 的 method 每次 new 时候都会重新 def 一遍, 不合理... 这里临时 hack 一下
                // FunValue 没有 BuildIn.initMetaTable 过程，如果缓存存在一定往里头塞过 meta 信息
                return;
            }
            FunMeta fm = fv.meta();
            MetaTable tbl = fv.metaTable();
            Location loc = None;
            tbl.set(loc, "isPrimitive", isPrimitive ? True : False, false);
            tbl.set(loc, "name", String(fm.name), false);
            tbl.set(loc, "className", fm.className == null ? Null : String(fm.className), false); // todo 把 name 换成 对象
            tbl.set(loc, "parameters", Value.List(fm.params.stream().map(Value::String).toArray(Value[]::new)), false);
            tbl.set(loc, "defaults", Value.List(fm.defaults.stream().map(it -> it == null ? Null : it).toArray(Value[]::new)), false);
            tbl.set(loc, "isConstruct", Bool(fm.ctor), false);
            tbl.set(loc, "isSymbol", Bool(fm.symbol), false);
            // tbl.set(loc, "isExtend", Bool(fm.extend), false);
            tbl.set(loc, "isArrow", Bool(fm.arrow), false);
            tbl.set(loc, "requiredParametersSize", Int(fm.requiredParamSz), false);
        }
        static void initMetaTable(ClassValue cv, boolean isPrimitive) {
            ClassMeta cm = cv.meta();
            MetaTable tbl = cv.metaTable();
            Location loc = None;
            tbl.set(loc, "isPrimitive", isPrimitive ? True : False, false);
            tbl.set(loc, "name", String(cm.name), false);
            tbl.set(loc, "parent", cm.parent == null ? Null : cm.parent, false);
            tbl.set(loc, "properties", Value.List(cm.props.stream().map(it -> var(loc, it)).toArray(Value[]::new)), false);
            tbl.set(loc, "methods", Value.List(cm.methods.stream().map(it -> fun(loc, it)).toArray(Value[]::new)), false);
            tbl.set(loc, "construct", fun(loc, cm.ctor), false);
            tbl.set(loc, "isSealed", Bool(cm.sealed), false);
            tbl.set(loc, "isTagged", Bool(cm.tagged), false);
        }
    }
    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\
    /*
    final class JavaMethodHandle implements FunValue {
        // final static MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        final static MethodHandles.Lookup lookup = MethodHandles.lookup();
        public final MethodType type;
        public final MethodHandle mh;
        static final byte
                findSpecial           = 1,
                findStatic            = 2,
                findVirtual           = 3;
        private JavaMethodHandle(Location loc, int findType, Class<?> refc, String name, String descriptor, Class<?> specialCaller) {
            type = MethodType.fromMethodDescriptorString(descriptor, null);
            try {
                switch (findType) {
                    // 调用实例构造方法，私有方法，父类方法。
                    // e.g. ctor: lookup.findSpecial(refc, "<init>", methodType(void.class), refc);
                    case findSpecial: mh = lookup.findSpecial(refc, name, type, specialCaller); break;
                    // 调用静态方法
                    case findStatic: mh = lookup.findStatic(refc, name, type); break;
                    // 调用所有的虚方法 调用接口方法，
                    case findVirtual: mh = lookup.findVirtual(refc, name, type); break;
                    default: throw Error.runtime(loc, "不支持该的 findType: " + findType);
                }
            } catch (NoSuchMethodException e) {
                throw Error.runtime(loc, "方法没找到: " + refc.getName() + "." + name);
            } catch (IllegalAccessException e) {
                throw Error.runtime(loc, "方法不能访问: " + refc.getName() + "." + name);
            }
        }
        @Override
        public Value apply(Location loc, @NotNull Scope s, Value... args) {
            Expect.exactArgs(loc,1 + type.parameterCount(), args.length);
            Object[] unbox = JavaValue.unbox(loc, args);
            Object[] args1 = Arrays.copyOfRange(unbox, 1, args.length);
            try {
                return JavaValue(mh.invoke(unbox[0], (Object[]) args1));
            } catch (Throwable t) {
                throw Error.runtime(loc, "Java方法调用失败:" + t.getMessage());
            }
        }
        @Override public String toString() { return "JavaMethodHandle"; }
    }
    */
    /*
    final class JavaMethod implements FunValue {
        final String name;
        final Class<?> clazz;
        final Class<?>[] argClasses;
        final List<String> params;
        final Object method; // Method | Constructor
        final boolean isStatic;
        final int requiredParamSz;
        private JavaMethod(Location loc, Class<?> clazz, String methodName, Class<?>[] argClasses) {
            try {
                this.clazz = clazz;
                this.argClasses = argClasses;
                name = clazz.getCanonicalName() + "." + methodName;
                if ("<init>".equals(methodName)) {
                    Constructor<?> ctor = clazz.getConstructor(argClasses);
                    params = Arrays.stream(ctor.getParameters()).map(Parameter::getName).collect(toList());
                    requiredParamSz = ctor.getParameterCount();
                    isStatic = false;
                    this.method = ctor;
                } else {
                    Method method = clazz.getMethod(methodName, argClasses);
                    params = Arrays.stream(method.getParameters()).map(Parameter::getName).collect(toList());
                    isStatic = Modifier.isStatic(method.getModifiers());
                    this.method = method;
                    requiredParamSz = method.getParameterCount();
                }
            } catch (NoSuchMethodException e) {
                throw Error.runtime(loc, "方法没找到: " + clazz.getName() + "." + methodName);
            }
            init(true);
        }
        @Override public FunMeta meta() { return primitiveFunMeta(name, null, params, lists(), requiredParamSz); }
        @Override public @NotNull ObjectValue bind(Location loc, @NotNull ObjectValue self) { throw Error.runtime(loc, this + " 不支持绑定"); }
        @Override public Value apply(Location loc, @NotNull Scope s, Value... args) {
            try {
                if (method instanceof Method) {
                    Expect.exactArgs(loc,1 + argClasses.length, args.length);
                    Object[] unbox = JavaValue.unbox(loc, args);
                    Object[] args1 = Arrays.copyOfRange(unbox, 1, args.length);
                    return JavaValue(((Method) method).invoke(isStatic ? null : unbox[0], args1));
                } else {
                    Expect.exactArgs(loc,argClasses.length, args.length);
                    Object[] unbox = JavaValue.unbox(loc, args);
                    return JavaValue(((Constructor<?>) method).newInstance(unbox));
                }
            } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
                throw Error.runtime(loc, "Java方法调用失败:" + e.getMessage());
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof Interp.ControlFlow) {
                    throw ((Interp.ControlFlow) t);
                } else {
                    throw Error.runtime(loc, "Java方法调用失败:" + t.toString());
                }
            }
        }
        @Override public String toString() { return "JavaMethod " + name; }
    }
    */
    /*
    // private!!
    final class MapPatternValue implements Ast.Pattern {
        public final Location loc;
        public final Map<Value, Ast.Pattern> props;
        MapPatternValue(Location loc, Map<Value, Ast.Pattern> props) {
            this.loc = loc;
            this.props = props;
        }
    }
    // private!!
    final class UnApplyPatternValue implements Ast.Pattern {
        public final Location loc;
        public final Value.ClassValue classValue;
        public final List<Ast.Pattern> props;
        UnApplyPatternValue(Location loc, ClassValue classValue, List<Ast.Pattern> props) {
            this.loc = loc;
            this.classValue = classValue;
            this.props = props;
        }
    }
    */
}
