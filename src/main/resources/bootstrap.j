class Throwable(msg = null, cause = null) // val stackTrace:List 🍺 构造过程会自动填入 stackTrace属性
class Error(msg, cause = null) extends Throwable(msg, cause)
class NativeError(ex, msg, cause = null) extends Error(msg, cause)
class AssertError(msg, reason, cause = null) extends Error(msg, cause)
class NullPointerError(msg) extends Error(msg, null)
class ClassNotFoundError(msg) extends Error(msg, null)
class CallError(msg, cause = null) extends Error(msg, cause)
class RuntimeError(msg, cause = null) extends Error(msg, cause)
class OperatorError(msg, cause = null) extends Error(msg, cause)
class MatchError(msg) extends Error(msg, null)
class IterError extends Error(null, null)
class Enum
// 这个对象有点特殊，scope 中没有 class 属性, 否则 scope 中的 class 会影响 scope 链条向上回溯到对象 root 的判断
sealed class Scope
// from..to  .. range操作符会解糖成 Range 对象
sealed class Range(val from, val to) {
    fun `iterable`() {
        var cur = from
        object {
            fun hasNext() = cur <= to
            fun next() = hasNext() ? cur++ : throw new IterError
        }
    }
    fun `hash`() = sys.hash(from, to)
    fun `==`(that) = that is Range && from == that.from && to == that.to
}

object sys {
    fun backtrace() = new Throwable().stackTrace.cdr()
    fun now() = _.toVInt(_.systemNow(_.javaNull))
    fun objectId(obj) = Kit.objectId(obj)
    fun exec(cmd) = Kit.exec(cmd)
    fun exit(code = 0) = _.systemExit(_.javaNull, _.toJInt(code))
    fun hash() {
        var code = 1
        for (val it in arguments) {
            code = 31 * code + (it == null ? 0 : it.`hash`())
        }
        return code == 1 ? 0 : code
    }

    // todo doc
    fun loadString(str) = Java.unbox(_.evalExpr(_.interp(_.javaNull), _.loadString(_.javaNull, _.toJStr("<inline>"), _.toJStr(str))))
    // todo doc
    fun loadFile(path) = {
        // 这里最好加一个查找路径... 目前要么绝对路径, 要么与 bootstrap 同路径
        var p = _.path(path)
        if (!_.toVBool(_.fileExist(p))) {
            p = _.path(__DIR__ + "/" + path)
        }
        Java.unbox(_.evalExpr(_.interp(_.javaNull), _.loadFile(_.javaNull, p)))
    }

    val loadedModules = [:]
    // todo doc
    fun doFile(path) = loadFile(path)()
    // todo doc
    fun doString(str) = loadString(str)()
    // todo doc
    // requireOnce
    fun require(path) = {
        val k = _.toVStr(_.toString(_.path(path)))
        if (!_.toVBool(_.mapContainsKey(_.Raw(loadedModules), _.Box(k)))) {
            val v = doFile(path)
            loadedModules[k] = v
        }
        loadedModules[k]
    }
}

object _ {

    val Class   = Java.type
    val Method  = Java.method
    val Field   = Java.field
    val New     = Java.new
    val Box     = Java.box
    val Unbox   = Java.unbox
    val Raw     = Java.raw


    val javaNull = Box(null)

    val java_char               = Class("char")
    val java_int                = Class("int")
    val java_long               = Class("long")
    val java_bool               = Class("boolean")
    val java_byte_Arr           = Class("[B")

    val java_lang_Enum          = Class("java.lang.Enum")
    val java_lang_System        = Class("java.lang.System")
    val java_lang_Object        = Class("java.lang.Object")
    val java_util_Map           = Class("java.util.Map")
    val java_util_List          = Class("java.util.List")
    val java_util_ArrayList     = Class("java.util.ArrayList")
    val java_lang_Character     = Class("java.lang.Character")
    val java_lang_String        = Class("java.lang.String")
    val java_lang_String_Arr    = Class("[Ljava.lang.String;")
    val java_lang_Math          = Class("java.lang.Math")
    val java_lang_Class         = Class("java.lang.Class")
    val java_lang_reflect_Array = Class("java.lang.reflect.Array")

    val java_io_PrintStream     = Class("java.io.PrintStream")
    val java_nio_file_Path      = Class("java.nio.file.Path")
    val java_nio_file_Paths     = Class("java.nio.file.Paths")
    val java_nio_file_Files     = Class("java.nio.file.Files")

    val java_nio_file_LinkOption                    = Class("java.nio.file.LinkOption")
    val java_nio_file_OpenOption                    = Class("java.nio.file.OpenOption")
    val java_nio_file_StandardOpenOption            = Class("java.nio.file.StandardOpenOption")
    val java_nio_file_Attribute_FileAttribute       = Class("java.nio.file.attribute.FileAttribute")
    val java_nio_file_LinkOption_Arr                = Class("[Ljava.nio.file.LinkOption;")
    val java_nio_file_OpenOption_Arr                = Class("[Ljava.nio.file.OpenOption;")
    val java_nio_file_Attribute_FileAttribute_Arr   = Class("[Ljava.nio.file.attribute.FileAttribute;")


    val j_Value                 = Class("j.Value")
    val j_Value_ClassValue      = Class("j.Value$ClassValue")
    val j_Value_IntValue        = Class("j.Value$IntValue")
    val j_Value_ObjectValue     = Class("j.Value$ObjectValue")
    val j_Value_ListValue       = Class("j.Value$ListValue")
    val j_Parser_Location       = Class("j.parser.Location")
    val j_Value_Arr             = Class("[Lj.Value;")

    val j_Interp                = Class("j.Interp")
    val j_Ast_Expr              = Class("j.Ast$Expr")
    val j_J                     = Class("j.J")


    val stdIn                   = Field(java_lang_System, "in")(javaNull)
    val stdOut                  = Field(java_lang_System, "out")(javaNull)
    val stdErr                  = Field(java_lang_System, "err")(javaNull)
    val locationNone            = Field(j_Parser_Location, "None")(javaNull)

    val classCast               = Method(java_lang_Class, "cast", java_lang_Object)
    val toString                = Method(java_lang_Object, "toString")

    val println                 = Method(java_io_PrintStream, "println", java_lang_Object)
    fun println_stdOut(v)       = println(stdOut, Java.box(v))

    val enumValueOf             = Method(java_lang_Enum, "valueOf", java_lang_Class, java_lang_String)

    val stringCharAt            = Method(java_lang_String, "charAt", java_int)
    val stringValueOfChar       = Method(java_lang_String, "valueOf", java_char)
    val stringNew               = Method(java_lang_String, "<init>", java_byte_Arr)
    val substring               = Method(java_lang_String, "substring", java_int, java_int)
    val stringLength            = Method(java_lang_String, "length")
    val stringGetBytes          = Method(java_lang_String, "getBytes")

    val listAdd                 = Method(java_util_List, "add", java_lang_Object)
    val listContains            = Method(java_util_List, "contains", java_lang_Object)
    val listSize                = Method(java_util_List, "size")
    val listSubList             = Method(java_util_List, "subList", java_int, java_int)

    val mapSize                 = Method(java_util_Map, "size")
    val mapContainsKey          = Method(java_util_Map, "containsKey", java_lang_Object)

    val mathToIntExact          = Method(java_lang_Math, "toIntExact", java_long)
    val systemNow               = Method(java_lang_System, "currentTimeMillis")
    val systemExit              = Method(java_lang_System, "exit", java_int)

    val arrayNew                = Method(java_lang_reflect_Array, "newInstance", java_lang_Class, java_int)
    val arraySet                = Method(java_lang_reflect_Array, "set", java_lang_Object, java_int, java_lang_Object)
    fun newArray(cls, len)      = arrayNew(javaNull, cls, toJInt(len))
    fun list2javaArr(lst, javaClass, mapper = null) {
        mapper = mapper == null ? Box : mapper
        val sz = lst.size()
        val arr = newArray(javaClass, sz)
        for (var i = 0; i < sz; i++) {
            arraySet(javaNull, arr, toJInt(i), mapper(lst[i]))
        }
        arr
    }

    val emptyStringArr          = newArray(java_lang_String, 0)
    val emptyLinkOptionArr      = newArray(java_nio_file_LinkOption, 0)
    val emptyOpenOptionArr      = newArray(java_nio_file_OpenOption, 0)
    val emptyFileAttribute      = newArray(java_nio_file_Attribute_FileAttribute, 0)
    val emptyJValueArr          = newArray(j_Value, 0)

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // val pathsGet                = Method(java_nio_file_Paths, "get", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;")
    val pathsGet                = Method(java_nio_file_Paths, "get", java_lang_String, java_lang_String_Arr)
    val pathNormalize           = Method(java_nio_file_Path, "normalize")
    val filesExists             = Method(java_nio_file_Files, "exists", java_nio_file_Path, java_nio_file_LinkOption_Arr)
    val filesReadAllBytes       = Method(java_nio_file_Files, "readAllBytes", java_nio_file_Path)
    val filesWrite              = Method(java_nio_file_Files, "write", java_nio_file_Path, java_byte_Arr, java_nio_file_OpenOption_Arr)
    val filesDeleteIfExists     = Method(java_nio_file_Files, "deleteIfExists", java_nio_file_Path)
    val filesCreateDirectories  = Method(java_nio_file_Files, "createDirectories", java_nio_file_Path, java_nio_file_Attribute_FileAttribute_Arr)

    fun path(VStr)              = pathNormalize(pathsGet(javaNull, toJStr(VStr), emptyStringArr))
    fun fileExist(p)            = filesExists(javaNull, p, emptyLinkOptionArr)
    fun fileDelete(p)           = filesDeleteIfExists(javaNull, p)
    fun fileRead(p)             = stringNew(filesReadAllBytes(javaNull, p))
    fun fileWrite(p, VStr, opts = null)  = {
        opts = opts == null ? [] : opts
        val JOpts = list2javaArr(opts, java_nio_file_OpenOption, it => enumValueOf(javaNull, java_nio_file_StandardOpenOption, toJStr(it)))
        filesWrite(javaNull, p, stringGetBytes(Raw(VStr)), JOpts)
        null
    }
    fun fileCreateDir(dir)      = {
        filesCreateDirectories(javaNull, dir, emptyFileAttribute)
        null
    }

    val interp                  = Method(j_J, "interp")
    val loadFile                = Method(j_J, "loadFile", java_nio_file_Path)
    val loadString              = Method(j_J, "loadString", java_lang_String, java_lang_String)

    val evalPath                = Method(j_Interp, "eval", java_nio_file_Path)
    val evalString              = Method(j_Interp, "eval", java_lang_String)
    val evalExpr                = Method(j_Interp, "eval", j_Ast_Expr)

    val newInstance             = Method(j_Value_ClassValue, "newInstance", j_Parser_Location, j_Value_Arr)
    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=


    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    val intCharValue            = Method(j_Value_IntValue, "charValue")

    val valueCopy               = Method(j_Value, "Copy", j_Value_ObjectValue)
    val newVIntFromJInt         = Method(j_Value, "Int", java_long)
    val newVIntFromJChar        = Method(j_Value, "Int", java_char)
    val newVBool                = Method(j_Value, "Bool", java_bool)
    val newVStr                 = Method(j_Value, "String", java_lang_String)
    val newVList                = Method(j_Value, "List", java_util_List)

    fun toVInt(JInt)            = Unbox(newVIntFromJInt(javaNull, JInt))
    fun toVIntChar(JChar)       = Unbox(newVIntFromJChar(javaNull, JChar))
    fun toVStr(JStr)            = Unbox(newVStr(javaNull, JStr))
    fun toVBool(JBool)          = Unbox(newVBool(javaNull, JBool))
    fun toVList(JList)          = Unbox(newVList(javaNull, JList))
    fun toJInt(VInt)            = mathToIntExact(javaNull, Box(Raw(VInt)))
    fun toJStr(VStr)            = Raw(VStr)
}

// export
global.Throwable = Throwable
global.Error = Error
global.NativeError = NativeError
global.AssertError = AssertError
global.NullPointerError = NullPointerError
global.ClassNotFoundError = ClassNotFoundError
global.CallError = CallError
global.RuntimeError = RuntimeError
global.OperatorError = OperatorError
global.MatchError = MatchError
global.IterError = IterError
global.Enum = Enum
global.Scope = Scope
global.Range = Range
global.sys = sys
global.__ = _

sys.require("buildin.j")
sys.require("extends.j")

null