package j;

import org.jetbrains.annotations.NotNull;

import static j.Ast.*;

/**
 * PatternMatcher
 *
 * visitor 模式是因为 java 不支持双分派，且 java 也木有模式匹配
 * visitor 是个递归的模式匹配(王垠)，是OO语言用来表达模式匹配的工具(虚方法性能捉急...)
 * 所以用宿主语言的 visitor 来实现脚本的模式匹配比较符合直觉...
 * 不喜欢在一堆数据上挖坑，宁愿用 switch-case(需要Sealed穷举) 来分派方法
 * Ast visitor 道理相同
 *
 * scala apply - unapply 原来特么是追求形式一致性...
 * 模式匹配可以用来解构(de-construct)数据
 *
 * @author chuxiaofeng
 */
public interface Matcher<C, R> {
    default R match(@NotNull Pattern ptn, @NotNull C ctx) {
        if (ptn instanceof Id)                  return match(((Id) ptn), ctx);
        if (ptn instanceof Literal)             return match(((Literal) ptn), ctx);
        if (ptn instanceof ListPattern)         return match(((ListPattern) ptn), ctx);
        if (ptn instanceof MapPattern)          return match(((MapPattern) ptn), ctx);

        // 模式匹配未解语法糖前解释过程中间状态，配合 PatternInterpreter 使用
        // if (ptn instanceof MapPatternValue)     return match(((MapPatternValue) ptn), ctx);

        if (ptn instanceof UnApplyPattern)      return match(((UnApplyPattern) ptn), ctx);

        // // 模式匹配未解语法糖前解释过程中间状态，配合 PatternInterpreter 使用
        // if (ptn instanceof UnApplyPatternValue) return match(((UnApplyPatternValue) ptn), ctx);

        // for MapPattern-Key & UnApplyPattern-Parameter
        if (ptn instanceof ExprPattern)         return match(((ExprPattern) ptn), ctx);

        throw Error.runtime(((Expr) ptn).loc, "不支持的 pattern: " + ptn);
    }

    R match(@NotNull Id ptn,            @NotNull C ctx);
    R match(@NotNull Literal ptn,       @NotNull C ctx);
    R match(@NotNull ListPattern ptn,   @NotNull C ctx);
    R match(@NotNull MapPattern ptn,    @NotNull C ctx);

    // default R match(@NotNull MapPatternValue val, @NotNull C ctx) { throw Error.runtime(val.loc, "不支持的 pattern"); }
    default R match(@NotNull UnApplyPattern ptn, @NotNull C ctx) { throw Error.runtime(ptn.loc, "不支持的 pattern"); }
    // default R match(@NotNull UnApplyPatternValue val, @NotNull C ctx) { throw Error.runtime(val.loc, "不支持的 pattern"); }
    default R match(@NotNull ExprPattern ptn, @NotNull C ctx) { throw Error.runtime(ptn.loc, "不支持的 pattern"); };

    // 字面量不能作为声明赋值左值放到运行时检查了... parse 过程忽略错误
    /*static boolean supportDefineAssign(Expr expr) {
        return expr instanceof Id || expr instanceof ListPattern || expr instanceof MapPattern;
    }*/
    String Wildcards = "_";
    static boolean isWildcards(Id ptn) { return ptn.name.equals(Wildcards); }
}