package j.parser;

import j.Ast;
import j.Error;
import j.Helper;
import j.Value;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static j.Helper.lists;
import static j.Value.*;
import static java.util.stream.Collectors.toList;

import java.lang.ClassValue;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class Literals {

    @SuppressWarnings("unchecked")
    public static Value parse(Ast.Literal literal) {
        Location loc = literal.loc;
        Object v = literal.content;
        switch (literal.type) {
            case Bool: return parseBool(loc, ((String) v));
            case Int: return parseInt(loc, ((String) v));
            case Float: return parseFloat(loc, ((String) v));
            case String: return parseStr(loc, ((String) v));
            case Symbol: return parseSymbol(loc, ((String) v));
            case List: return parseList(loc, ((List<Ast.Literal>)v));
            case Map: return parseMap(loc, ((Map<Ast.Literal, Ast.Literal>) v));
            case Class: return parseClass(loc, ((String) v));
            case Null: return Null;
            case Undefined: return Undefined;
        }
        throw Error.bug(loc);
    }

    static ListValue parseList(Location loc, @NotNull List<Ast.Literal> elems) {
        return Value.List(elems.stream().map(Literals::parse).collect(toList()));
    }

    static MapValue parseMap(Location loc, @NotNull Map<Ast.Literal, Ast.Literal> props) {
        MapValue map = Value.Map();
        for (Map.Entry<Ast.Literal, Ast.Literal> it : props.entrySet()) {
            map.put(parse(it.getKey()), parse(it.getValue()));
        }
        return map;
    }

    static Value.ClassValue parseClass(Location loc, @NotNull String str) {
        switch (str) {
            case Constant.Class_Class          : return ClassClass;
            case Constant.Class_Object         : return ClassObject;
            case Constant.Class_Bool           : return ClassBool;
            case Constant.Class_Int            : return ClassInt;
            case Constant.Class_Float          : return ClassFloat;
            case Constant.Class_Symbol         : return ClassSymbol;
            case Constant.Class_String         : return ClassString;
            case Constant.Class_Iterator       : return ClassIterator;
            case Constant.Class_List           : return ClassList;
            case Constant.Class_Map            : return ClassMap;
            case Constant.Class_Fun            : return ClassFun;
        }
        throw Error.bug(loc);
    }

    static BoolValue parseBool(Location loc, @NotNull String str) {
        if ("true".equals(str)) {
            return True;
        } else if ("false".equals(str)) {
            return False;
        }
        throw Error.bug(loc);
    }

    static IntValue parseInt(Location loc, @NotNull String str) {
        try {
            return Literals.parseInt(str);
        } catch (NumberFormatException ignored) {
            throw Error.type(loc, "😓整数格式不对: " + str);
        }
    }
    static FloatValue parseFloat(Location loc, @NotNull String str) {
        try {
            return Literals.parseFloat(str);
        } catch (NumberFormatException ignored) {
            throw Error.type(loc, "😓浮点数格式不对: " + str);
        }
    }

    static IntValue parseInt(@NotNull String str) {
        int base, sign;
        if (str.startsWith("+")) {
            sign = 1;
            str = str.substring(1);
        } else if (str.startsWith("-")) {
            sign = -1;
            str = str.substring(1);
        } else {
            sign = 1;
        }

        if (str.startsWith("0b")) {
            base = 2;
            str = str.substring(2);
        } else if (str.startsWith("0x")) {
            base = 16;
            str = str.substring(2);
        } else if (str.startsWith("0o")) {
            base = 8;
            str = str.substring(2);
        } else {
            base = 10;
        }

        long val = Long.parseLong(str, base);
        if (sign == -1) {
            val = -val;
        }
        return Value.Int(val, base);
    }

    static FloatValue parseFloat(@NotNull String str) {
        return Value.Float(Double.parseDouble(str));
    }

    static SymbolValue parseSymbol(Location loc, @NotNull String s) {
        return Value.Symbol(s.substring(1, s.length() - 1));
    }

    static StringValue parseStr(Location loc, @NotNull String s) {
        return Value.String(unescapeStr(loc, s, s.charAt(0)));
    }

    // TODO 这里要不优化下，一团浆糊
    static String unescapeStr(@NotNull Location loc, String s, char quote) {
        // char quote = s.charAt(0);
        s = s.substring(1, s.length() - 1);

        char[] a = s.toCharArray(), ss = new char[a.length];
        int l = a.length, cnt = 0;

        for (int i = 0; i < l; i++) {
            char c = a[i];
            if (c == quote && i + 1 < l) {
                // """"   ''''
                char n = a[i + 1];
                if (n == quote) {
                    i++;
                    ss[cnt++] = quote;
                } else {
                    ss[cnt++] = c;
                }
            } else if (c == '\\' && i + 1 < l) {
                // \' \" \\ \/ \t \r \n \b \f
                char n = a[i + 1];
                i++;
                if (n == quote) {
                    ss[cnt++] = quote;
                } else {
                    switch (n) {
                        // case quote: ss[cnt++] = quote ;break;
                        case '\\': ss[cnt++] = '\\';break;
                        // case '/': ss[cnt++] = '/';break;
                        case 't': ss[cnt++] = '\t';break;
                        case 'r': ss[cnt++] = '\r';break;
                        case 'n': ss[cnt++] = '\n';break;
                        case 'b': ss[cnt++] = '\b';break;
                        case 'f': ss[cnt++] = '\f';break;
                        case 'u':
                            ss[cnt++] = parseUnicodeEscape(loc, a[i + 1], a[i + 2], a[i + 3], a[i + 4]);
                            i += 4;
                            break;
                        default:
                            i--;
                            ss[cnt++] = c;
                    }
                }
            } else {
                ss[cnt++] = c;
            }
        }
        return new String(ss, 0, cnt);
    }

    static char parseUnicodeEscape(Location loc, char c1, char c2, char c3, char c4) {
        // return (char) Integer.parseInt(String.valueOf(c1) + c2 + c3 + c4, 16);
        int i = parseHexDigit(loc, c1) << 12 | parseHexDigit(loc, c2) << 8 | parseHexDigit(loc, c3) << 4 | parseHexDigit(loc, c4);
        if (Double.isInfinite(i) || Double.isNaN(i)) {
            throw Error.syntax(loc, "有问题的\\u Unicode");
        }
        return (char) i;
    }

    static int parseHexDigit(Location loc, char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'F') {
            return c + 10 - 'A';
        } else if (c >= 'a' && c <= 'f') {
            return c + 10 - 'a';
        }
        throw Error.syntax(loc, "有问题的\\u Unicode");
    }

    // ==========================================================

    // 玩具，木有做任何错误检查
    public static Value parse(String literalJSON) {
        return parse1((Map) jsonParse(literalJSON));
    }
    private static Value parse1(Map<String, Object> map) {
        Location loc = Location.None;
        Object v = map.get("content");
        Object type = map.get("type");
        if ("Bool".equals(type)) {
            return parseBool(loc, ((String) v));
        } else if ("Int".equals(type)) {
            return parseInt(loc, ((String) v));
        } else if ("Float".equals(type)) {
            return parseFloat(loc, ((String) v));
        } else if ("String".equals(type)) {
            return parseStr(loc, ((String) v));
        } else if ("Symbol".equals(type)) {
            return parseSymbol(loc, ((String) v));
        } else if ("List".equals(type)) {
            return parseList1(((List) v));
        } else if ("Map".equals(type)) {
            return parseMap1(((List) v));
        } else if ("Class".equals(type)) {
            return parseClass(loc, ((String) v));
        } else if (Null.equals(type)) {
            return Null;
        } else if (Undefined.equals(type)) {
            return Undefined;
        }
        throw Error.bug(loc);
    }
    private static ListValue parseList1(List elems) {
        List<Value> lst = new ArrayList<>();
        for (Object elem : elems) {
            lst.add(parse1(((Map) elem)));
        }
        return Value.List(lst);
    }
    private static MapValue parseMap1(List props) {
        MapValue map = Value.Map();
        for (Object prop : props) {
            Map key = (Map) ((Map) prop).get("key");
            Map value = (Map) ((Map) prop).get("value");
            map.put(parse1(key), parse1(value));
        }
        return map;
    }
    // 不想搞个 json 库进来...  把 json 当成 js 对象字面量（想下兼容 json），利用 nashorn parse 出来
    private final static ScriptEngine JS = new ScriptEngineManager().getEngineByName("javascript");
    private static Object jsonParse(String json) {
        try {
            return JS.eval("Java.asJSONCompatible(" + json + ")");
        } catch (ScriptException e) {
            Helper.sneakyThrows(e);
            return null;
        }
    }

    public static void main(String[] args) {
        Value parsed = parse("{\"type\": \"Map\", \"content\": [\n" +
                "  {\n" +
                "    \"key\": {\"type\": \"String\", \"content\": \"'Id'\"},\n" +
                "    \"value\": {\"type\": \"Int\", \"content\": \"42\"}\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": {\"type\": \"String\", \"content\": \"'Colors'\"},\n" +
                "    \"value\": {\"type\": \"List\", \"content\": [\n" +
                "      {\"type\": \"Bool\", \"content\": \"true\"},\n" +
                "      {\"type\": \"Float\", \"content\": \"3.14\"},\n" +
                "      {\"type\": \"Symbol\", \"content\": \"`Hello`\"},\n" +
                "      {\"type\": \"Class\", \"content\": \"Bool\"},\n" +
                "      {\"type\": \"Map\", \"content\": [\n" +
                "        {\n" +
                "          \"key\": {\"type\": \"Class\", \"content\": \"Bool\"},\n" +
                "          \"value\": {\"type\": \"Symbol\", \"content\": \"`Hello`\"},\n" +
                "        }\n" +
                "      ]}\n" +
                "    ]}\n" +
                "  },\n" +
                "] }");
        // Map {Id=42, Colors=List [true, 3.14, `Hello`, Class Bool, Map {Class Bool=`Hello`}]}
        System.out.println(parsed);
    }
}
