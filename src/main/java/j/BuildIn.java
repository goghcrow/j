package j;

import j.parser.Literals;
import j.parser.Location;
import org.jetbrains.annotations.Nullable;
import sun.invoke.util.BytecodeDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.String;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static j.Helper.*;
import static j.Value.*;
import static j.Value.Constant.*;
import static java.util.stream.Collectors.toList;

/**
 * Primitive / BuildIn / Intrinsic
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class BuildIn {

    public static void buildIn(Scope s, String name, Value v) {
        s.define(Location.None, name, v, false);
    }

    static Scope globalScope() {
        Scope scope = Scope.create();
        buildIn(scope, Class_Class,     ClassClass);
        buildIn(scope, Class_Null,      ClassNull);
        buildIn(scope, Class_Object,    ClassObject);
        buildIn(scope, Class_Bool,      ClassBool);
        buildIn(scope, Class_Int,       ClassInt);
        buildIn(scope, Class_Float,     ClassFloat);
        buildIn(scope, Class_Symbol,    ClassSymbol);
        buildIn(scope, Class_String,    ClassString);
        buildIn(scope, Class_Iterator,  ClassIterator);
        buildIn(scope, Class_List,      ClassList);
        buildIn(scope, Class_Map,       ClassMap);
        buildIn(scope, Class_Fun,       ClassFun);
        buildIn(scope, Class_JavaValue, ClassJavaValue);
        buildIn(scope, Java.class);
        buildIn(scope, Kit.class);
        // buildIn(scope, Fun_Exec, Fun(null, Fun_Exec, lists("cmd"), lists(), 1, Sys::exec));
        return scope;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Intrinsic {
        String value();
        int requiredParamSz() default 0;
        String[] params() default {};
        String[] defaults() default {};
    }

    static <T extends Value.ClassValue> void initMetaTable(T cv) {
        Location none = Location.None;
        MetaTable tbl = cv.metaTable();
        String kName = cv.name();
        for (Method m : cv.getClass().getDeclaredMethods()) {
            boolean isApplicative = isPublicStatic(m) && isApplicative(m) && m.isAnnotationPresent(Intrinsic.class);
            if (isApplicative) {
                Intrinsic anno = m.getAnnotation(Intrinsic.class);
                String name = anno.value();
                int requiredParamSz = anno.requiredParamSz();
                boolean isSymbol = name.startsWith("`");

                List<Value> defaults = Arrays.stream(anno.defaults()).map(Literals::parse).collect(toList());
                if (isSymbol) {
                    SymbolValue sv = ((SymbolValue) Literals.parse(Ast.litSymbol(none, name)));
                    name = sv.val;
                    tbl.set(none, Symbol(name), method2fun(kName, name, lists(anno.params()), defaults, requiredParamSz, m), false);
                } else {
                    tbl.set(none, name, method2fun(kName, name, lists(anno.params()), defaults, requiredParamSz, m), false);
                }
            }
        }
    }

    public static void buildIn(Scope s, Class<?> k) {
        // assert k.isAnnotationPresent(Intrinsic.class);
        // String kName = k.getAnnotation(Intrinsic.class).value();
        String kName = k.getSimpleName();
        ObjectValue ov = Value.Object();
        for (Method m : k.getDeclaredMethods()) {
            if (isPublicStatic(m) && isApplicative(m) && m.isAnnotationPresent(Intrinsic.class)) {
                Intrinsic anno = m.getAnnotation(Intrinsic.class);
                String name = anno.value();
                List<Value> defaults = Arrays.stream(anno.defaults()).map(Literals::parse).collect(toList());
                FunValue f = method2fun(kName, name, lists(anno.params()), defaults, anno.requiredParamSz(), m);
                ov.setProperty(Location.None, name, f);
            }
        }
        s.define(Location.None, kName, ov, false);
    }

    public static class Kit {
        @Intrinsic(value = Fun_Exec, requiredParamSz = 1, params = { "cmd" })
        public static Value exec(Location loc, Scope s, Value ...args) {
            Expect.exactArgs(loc, 1, args.length);
            Process p;
            try {
                p = new ProcessBuilder().command("/bin/bash", "-c", args[0].asString(loc).val).start();
            } catch (IOException e) {
                throw Error.runtime(loc, e.getMessage());
            }
            String sep = System.getProperty("line.separator");
            String out = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().collect(Collectors.joining(sep));
            String err = new BufferedReader(new InputStreamReader(p.getErrorStream())).lines().collect(Collectors.joining(sep));
            return Value.List(Int(p.exitValue()), Value.String(out), Value.String(err));
        }
        @Intrinsic(value = Fun_ObjectId, requiredParamSz = 1, params = { "object" })
        public static Value objectId(Location loc, Value ...args) {
            Expect.exactArgs(loc, 1, args.length);
            return args[0].asObject(loc).id();
        }
    }

    public static class Java {
        @Intrinsic(value = "type", requiredParamSz = 1, params = {"className"})
        public static Value javaClass(Location loc, Value ...args) {
            Expect.exactArgs(loc, 1, args.length);
            String name = args[0].asString(loc).val;
            try {
                return JavaValue(toClass(loc, name));
            } catch (ClassNotFoundException e) {
                throw Error.type(loc, "Java.class " + name + " 不存在");
            }
        }
        @Intrinsic(value = "field", requiredParamSz = 2, params = {"class", "fieldName"})
        public static Value javaField(Location loc, Value ...args) {
            Expect.exactArgs(loc, 2, args.length);
            try {
                Class<?> clazz = toClass(loc, args[0]);
                String fieldName = args[1].asString(loc).val;
                Field field = clazz.getDeclaredField(fieldName);
                return Value.Fun("Java", "getField", lists("object"), lists(), 1, (loc1, s, args1) -> {
                    Expect.exactArgs(loc, 1, args1.length);
                    Object obj = JavaValue.unbox(loc, args1[0]);
                    try {
                        return JavaValue(field.get(obj));
                    } catch (IllegalAccessException e) {
                        sneakyThrows(e);
                        return Null;
                    }
                });
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                throw Error.runtime(loc, "字段没找到: " + args[0]);
            }
        }
        @Intrinsic(value = "method", requiredParamSz = 2, params = {"class", "methodName"})
        public static FunValue javaMethod(Location loc, Value ...args) {
            Expect.atLeastArgs(loc, 2, args.length);
            try {
                Class<?> clazz = toClass(loc, args[0]);
                String methodName = args[1].asString(loc).val;

                Class<?>[] argClasses;
                boolean isMethodDesc = args.length >= 3 && args[2] instanceof StringValue && isMethodDescriptor(((StringValue) args[2]).val);
                if (isMethodDesc) {
                    argClasses = fromMethodDescriptor(loc, ((StringValue) args[2]).val);
                } else {
                    Object[] argClassNames = Arrays.copyOfRange(args, 2, args.length);
                    argClasses = toArrayClass(loc, argClassNames);
                }
                return method2Fun(loc, clazz, methodName, argClasses);
            } catch (ClassNotFoundException e) {
                throw Error.runtime(loc, "类没找到: " + args[0]);
            }
        }
        @Intrinsic(value = "new", requiredParamSz = 1, params = {"class"})
        public static Value javaNew(Location loc, Value ...args) {
            Expect.exactArgs(loc, 1, args.length);
            try {
                return JavaValue(toClass(loc, args[0]).newInstance());
            } catch (Exception e) {
                throw Error.runtime(loc, "Java.new错误: new " + args[0] + ", " + e.getMessage());
            }
        }
        // Value -> JavaValue
        @Intrinsic(value = "box", requiredParamSz = 1, params = {"value"})
        public static Value javaBox(Location loc, Value ...args) {
            Expect.exactArgs(loc, 1, args.length);
            return JavaValue(args[0]);
        }
        // JavaValue -> Value
        @Intrinsic(value = "unbox", requiredParamSz = 1, params = {"javaValue"})
        public static Value javaUnbox(Location loc, Value ...args) {
            Expect.exactArgs(loc, 1, args.length);
            Object unboxed = Expect.as(loc, args[0], JavaValue.class).value;
            return Expect.as(loc, unboxed, Value.class);
        }
        @Intrinsic(value = "raw", requiredParamSz = 1, params = {"boxedValue"})
        public static Value javaRaw(Location loc, Value ...args) {
            Expect.exactArgs(loc, 1, args.length);
            return Expect.as(loc, args[0], Boxed.class).unbox();
        }

        static Class<?> toClass(Location loc, Object name) throws ClassNotFoundException {
            if (name instanceof Class<?>)   return ((Class<?>) name);
            if (name instanceof JavaValue)  return toClass(loc, ((JavaValue) name).value);
            if (name instanceof StringValue)return toClass(loc, ((StringValue) name).val);
            if (name.equals("void"))        return Void.TYPE;
            else if (name.equals("boolean"))return Boolean.TYPE;
            else if (name.equals("byte"))   return Byte.TYPE;
            else if (name.equals("char"))   return Character.TYPE;
            else if (name.equals("short"))  return Short.TYPE;
            else if (name.equals("int"))    return Integer.TYPE;
            else if (name.equals("long"))   return Long.TYPE;
            else if (name.equals("float"))  return Float.TYPE;
            else if (name.equals("double")) return Double.TYPE;
            else return java.lang.Class.forName(Expect.as(loc, name, String.class));
        }

        static Class<?>[] toArrayClass(Location loc, Object[] names) throws ClassNotFoundException {
            Class<?>[] array = new Class[names.length];
            for(int i = 0; i < names.length; i++) {
                array[i] = toClass(loc, names[i]);
            }
            return array;
        }

        static boolean isMethodDescriptor(String descriptor) {
            return descriptor.startsWith("(") && descriptor.indexOf(')') >= 0 && descriptor.indexOf('.') < 0;
        }

        static Class<?>[] fromMethodDescriptor(Location loc, String descriptor) {
            if (!isMethodDescriptor(descriptor)) {
                throw Error.runtime(loc, "方法签名错误: " + descriptor);
            }
            List<Class<?>> types = BytecodeDescriptor.parseMethod(descriptor, null);
            /*Class<?> rtype = */types.remove(types.size() - 1);
            int count = types.size();
            if ((count & 255) != count) {
                throw Error.runtime(loc, "参数个数错误 " + count);
            }
            return types.toArray(new Class<?>[0]);
        }
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    static FunValue method2Fun(Location loc, Class<?> clazz, String methodName, Class<?>[] argClasses) {
        String name = clazz.getCanonicalName() + "." + methodName;
        List<String> params;
        int requiredParamSz;
        boolean isStatic;
        Object method; // Method | Constructor
        try {
            if ("<init>".equals(methodName)) {
                Constructor<?> ctor = clazz.getConstructor(argClasses);
                params = Arrays.stream(ctor.getParameters()).map(Parameter::getName).collect(toList());
                requiredParamSz = ctor.getParameterCount();
                isStatic = false;
                method = ctor;
            } else {
                Method m = clazz.getMethod(methodName, argClasses);
                params = Arrays.stream(m.getParameters()).map(Parameter::getName).collect(toList());
                isStatic = Modifier.isStatic(m.getModifiers());
                method = m;
                requiredParamSz = m.getParameterCount();
            }
        } catch (NoSuchMethodException e) {
            throw Error.runtime(loc, "方法没找到: " + clazz.getName() + "." + methodName);
        }
        return Value.Fun(null, name, params, lists(), requiredParamSz, (loc1, s, args) -> {
            try {
                // todo 参数个数检查
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

                throw Error.nativeError(loc, e.getClass().getName(), "Java方法调用失败:" + e.getMessage());
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof Interp.ControlFlow) {
                    throw ((Interp.ControlFlow) t);
                } else {
                    throw Error.nativeError(loc, t.getClass().getName(), "Java方法调用失败:" + t.toString());
                }
            }
        });
    }

    static FunValue method2fun(@Nullable String cName, String fName,
                               List<String> params, List</*@Nullable*/Value> defaults,
                               int requiredParamSz, Method m) {
        // 3 :: Location loc, Scope s, Value ...args
        // 2 :: Location loc, Value ...args
        boolean withScope = m.getParameterCount() != 2;
        // 注意：这里生成StdFun 环境为空 !!!
        return Value.Fun(cName, fName, params, defaults, requiredParamSz, (loc, s, args) -> {
            try {
                // todo 参数个数检查
                if (withScope) {
                    return (Value) m.invoke(null, loc, s, args);
                } else {
                    return (Value) m.invoke(null, loc, args);
                }
            } catch (IllegalAccessException e) {
                sneakyThrows(e);
                return Null;
            } catch (InvocationTargetException e) {
                sneakyThrows(e.getTargetException());
                return Null;
            }
        });
    }

    static boolean isPublicStatic(Method m) {
        return Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers());
    }

    static boolean isApplicative(Method m) {
        boolean isApplicative = false;
        Class<?>[] types = m.getParameterTypes();
        int pCnt = m.getParameterCount();
        if (pCnt == 2) {
            isApplicative = Location.class.isAssignableFrom(types[0]) &&
                    Value[].class.isAssignableFrom(types[1]);
        } else if (pCnt == 3) {
            isApplicative = Location.class.isAssignableFrom(types[0]) &&
                    Scope.class.isAssignableFrom(types[1]) &&
                    Value[].class.isAssignableFrom(types[2]);
        }
        if (isApplicative) {
            isApplicative = Value.class.isAssignableFrom(m.getReturnType());
        }
        return isApplicative;
    }
}
