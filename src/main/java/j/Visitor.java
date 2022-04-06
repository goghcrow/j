package j;

import j.parser.Location;
import org.jetbrains.annotations.NotNull;

import static j.Ast.*;

/**
 * @author chuxiaofeng
 */
public interface Visitor<C, R> {
    R visit(@NotNull Group group, @NotNull C ctx);
    R visit(@NotNull Id id, @NotNull C ctx);

    // todo
    R visit(@NotNull Literal literal, @NotNull C ctx);

    R visit(@NotNull Block block, @NotNull C ctx);

    R visit(@NotNull VarDef var, @NotNull C ctx);
    R visit(@NotNull FunDef fun, @NotNull C ctx);
    R visit(@NotNull ClassDef cls, @NotNull C ctx);
    R visit(@NotNull Assign assign, @NotNull C ctx);

    R visit(@NotNull Subscript subscript, @NotNull C ctx);
    R visit(@NotNull ObjectMember objectMember, @NotNull C ctx);
    R visit(@NotNull Call call, @NotNull C ctx);
    R visit(@NotNull New new1, @NotNull C ctx);

    R visit(@NotNull Unary unary, @NotNull C ctx);
    R visit(@NotNull Binary binary, @NotNull C ctx);
    R visit(@NotNull Ternary ternary, @NotNull C ctx);

    R visit(@NotNull If if1, @NotNull C ctx);
    // R visit(@NotNull Match match, @NotNull C ctx);

    R visit(@NotNull While while1, @NotNull C ctx);
    R visit(@NotNull Continue cont, @NotNull C ctx);
    R visit(@NotNull Break brk, @NotNull C ctx);
    R visit(@NotNull Return ret, @NotNull C ctx);
    R visit(@NotNull Throw throw1, @NotNull C ctx);
    R visit(@NotNull Try try1, @NotNull C ctx);

    R visit(@NotNull Debugger debugger, @NotNull C ctx);
    R visit(@NotNull Assert assert1, @NotNull C ctx);

    // 不喜欢在一堆 node 上挖个洞实现 visit 方法... so 宁愿写 switch-case
    default R visit(@NotNull Node node, @NotNull C ctx) {
        if (node instanceof Group)      return visit(((Group) node), ctx);

        if (node instanceof Id)         return visit(((Id) node), ctx);
        if (node instanceof Literal)    return visit(((Literal) node), ctx);
        if (node instanceof Block)      return visit(((Block) node), ctx);
        if (node instanceof VarDef)     return visit(((VarDef) node), ctx);
        if (node instanceof FunDef)     return visit(((FunDef) node), ctx);
        if (node instanceof ClassDef)   return visit(((ClassDef) node), ctx);

        // if (node instanceof Match)      return visit(((Match) node), ctx);

        if (node instanceof Continue)   return visit(((Continue) node), ctx);
        if (node instanceof Break)      return visit(((Break) node), ctx);
        if (node instanceof Return)     return visit(((Return) node), ctx);
        if (node instanceof Throw)      return visit(((Throw) node), ctx);
        if (node instanceof Try)        return visit(((Try) node), ctx);
        if (node instanceof If)         return visit(((If) node), ctx);
        if (node instanceof While)      return visit(((While) node), ctx);


        if (node instanceof New)        return visit(((New) node), ctx);
        if (node instanceof Call)       return visit(((Call) node), ctx);
        if (node instanceof Assign)     return visit(((Assign) node), ctx);
        if (node instanceof Subscript)  return visit(((Subscript) node), ctx);
        if (node instanceof ObjectMember) return visit(((ObjectMember) node), ctx);

        if (node instanceof Unary)      return visit(((Unary) node), ctx);
        if (node instanceof Binary)     return visit(((Binary) node), ctx);
        if (node instanceof Ternary)    return visit(((Ternary) node), ctx);

        if (node instanceof Debugger)   return visit(((Debugger) node), ctx);
        if (node instanceof Assert)     return visit(((Assert) node), ctx);

        throw Error.bug(Location.None);
    }

}
