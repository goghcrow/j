package j;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chuxiaofeng
 */
public class Dumper implements Visitor<Object, Void> {
    final String Prefix = "  ";
    int i = 0;
    StringBuilder buf = new StringBuilder();
    StringBuilder prefix(String s) {
        for (int j = 0; j < i; j++) {
            buf.append(Prefix);
        }
        buf.append(s);
        return buf;
    }
    StringBuilder append(String s) { return buf.append(s); }
    void params(List<Ast.Expr> exprs, @NotNull Object ctx) {
        boolean isFirst = true;
        for (Ast.Expr expr : exprs) {
            if (isFirst) {
                isFirst = false;
                visit(expr, ctx);
            } else {
                deleteLastLFs();
                append(", ");
                visit(expr, ctx);
            }
        }
    }

    @Override
    public Void visit(Ast.@NotNull Group group, @NotNull Object ctx) {
        append("(");
        visit(group.expr, ctx);
        deleteLastLFs();
        append(")");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Id id, @NotNull Object ctx) {
        append(id.name);
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Literal literal, @NotNull Object ctx) {
        literal(literal);
        return null;
    }
    void literal(Ast.Literal literal) {
        if (literal.type == Ast.LiteralType.Map) {
            Map<Ast.Literal, Ast.Literal> map = (Map<Ast.Literal, Ast.Literal>) literal.content;
            append("[");
            if (map.isEmpty()) {
                append(":");
            } else {
                boolean isFirst = true;
                for (Map.Entry<Ast.Literal, Ast.Literal> it : map.entrySet()) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        append(", ");
                    }
                    literal(it.getKey());
                    append(": ");
                    literal(it.getValue());
                }
            }
            append("]");
        } else if (literal.type == Ast.LiteralType.List) {
            List<Ast.Literal> lst = (List<Ast.Literal>) literal.content;
            append("[");
            boolean isFirst = true;
            for (Ast.Literal vv : lst) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    append(", ");
                }
                literal(vv);
            }
            append("]");
        } else {
            append(((String) literal.content));
        }
    }

    boolean isDesugarPattern(Ast.Block block) {
        boolean allVarDef = !block.scope;
        if (allVarDef) {
            for (Ast.Expr stmt : block.stmts) {
                if (!(stmt instanceof Ast.VarDef) &&
                        !(stmt instanceof Ast.Assign) &&
                        !(stmt instanceof Ast.While) &&
                        !(stmt instanceof Ast.FunDef)
                ) {
                    allVarDef = false;
                    break;
                }
            }
        }
        return allVarDef;
    }

    @Override
    public Void visit(Ast.@NotNull Block block, @NotNull Object ctx) {
        boolean allDefBlock = isDesugarPattern(block);
        if (block.scope) {
            append("{\n");
            i++;
        }
        boolean isFirst = true;
        for (Ast.Expr stmt : block.stmts) {
            if (isFirst) {
                isFirst = false;
                if (block.scope) prefix("");
                visit(stmt, ctx);
            } else {
                append("\n");
                // if (block.scope || allDefBlock) // todo...
                    prefix("");
                visit(stmt, ctx);
            }
        }
        if (block.scope) {
            i--;
            if (buf.charAt(buf.length() - 1) != '\n') {
                append("\n");
            }
            prefix("}\n");
        }
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull VarDef var, @NotNull Object ctx) {
        append(var.mut ? "var " : "val ");
        visit(var.id, ctx);
        if (Ast.isUndefined(var.init)) {
            append("/* = unbounded*/");
        } else {
            append(" = ");
            visit(var.init, ctx);
        }
        return null;
    }

    void funParams(LinkedHashMap<Ast.Id, /*@Nullable*/Ast.Literal> params, Object ctx) {
        boolean isFirst = true;
        for (Map.Entry<Ast.Id, Ast.Literal> it : params.entrySet()) {
            if (isFirst) {
                isFirst = false;
                visit(it.getKey(), ctx);
            } else {
                deleteLastLFs();
                append(", ");
                visit(it.getKey(), ctx);
            }
            Ast.Literal defVal = it.getValue();
            if (defVal != null) {
                append(" = ");
                visit(defVal, ctx);
            }
        }
    }
    @Override
    public Void visit(Ast.@NotNull FunDef fun, @NotNull Object ctx) {
        if (fun.arrow) {
            append("(");
            funParams(fun.params, ctx);
            append(") => ");
            visit(fun.body, ctx);
            return null;
        } else {
//            if (fun.decorators == null || fun.decorators.isEmpty()) {
                append("fun ");
//            } else {
//                boolean isFirst = true;
//                for (Ast.Call decorator : fun.decorators) {
//                    if (isFirst) {
//                        isFirst = false;
//                        append(AT);
//                    } else {
//                        prefix(AT);
//                    }
//                    visit(decorator, ctx);
//                    append("\n");
//                }
//                prefix(FUN).append(" ");
//            }

            // if (fun.symbol) append("`");
            visit(fun.name, ctx);
            // if (fun.symbol) append("`");

            if (fun.extend) {
                append(" extends ");
                assert fun.className != null;
                visit(fun.className, ctx);
            }

            append("(");
            funParams(fun.params, ctx);
            append(") ");
            visit(fun.body, ctx);
            return null;
        }
    }

    @Override
    public Void visit(Ast.@NotNull ClassDef cls, @NotNull Object ctx) {
        if (cls.sealed) {
            append("sealed ");
        }
        append("class ");
        visit(cls.name, ctx);
        if (cls.parent != null) {
            append(" extends ");
            visit(cls.parent, ctx);
        }
        append(" {\n");
        i++;
        boolean hasProp = !cls.props.isEmpty();
        boolean isFirst = true;
        for (Ast.VarDef prop : cls.props) {
            if (isFirst) {
                isFirst = false;
            } else {
                append("\n");
            }
            prefix("");
            visit(prop, ctx);
        }
        if (hasProp) append("\n");
        isFirst = true;
        for (Ast.FunDef method : cls.methods) {
            if (isFirst) {
                isFirst = false;
            } else {
                append("\n");
            }
            prefix("");
            visit(method, ctx);
        }
        i--;
        prefix("}\n"); // rm \n
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Assign assign, @NotNull Object ctx) {
        visit(assign.lhs, ctx);
        append(" = ");
        visit(assign.rhs, ctx);
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Subscript subscript, @NotNull Object ctx) {
        visit(subscript.object, ctx);
        deleteLastLFs();
        append("[");
        visit(subscript.idxKey, ctx);
        append("]");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull ObjectMember objectMember, @NotNull Object ctx) {
        visit(objectMember.object, ctx);
        deleteLastLFs();
        append(".");
        visit(objectMember.prop, ctx);
        return null;
    }

    // deleteLastLFs 调这个方法是因为如果不清除通常是 block 留下的\n, 将生成有歧义的语法
    void deleteLastLFs() {
        while (buf.charAt(buf.length() - 1) == '\n') {
            buf.deleteCharAt(buf.length() - 1);
        }
    }

    @Override
    public Void visit(Ast.@NotNull Call call, @NotNull Object ctx) {
        visit(call.callee, ctx);
        deleteLastLFs();
        append("(");
        params(call.args, ctx);
        deleteLastLFs();
        append(")");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull New new1, @NotNull Object ctx) {
        append("new ");
        visit(new1.name, ctx);
        append("(");
        params(new1.args, ctx);
        deleteLastLFs();
        append(")");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Unary unary, @NotNull Object ctx) {
        switch (unary.op) {
            case UNARY_PLUS:
            case UNARY_MINUS:
            case BIT_NOT:
            case LOGIC_NOT:
            case PREFIX_INCR:
            case PREFIX_DECR:
                append(unary.op.name); visit(unary.arg, ctx); break;
            case POSTFIX_INCR:
            case POSTFIX_DECR:
                visit(unary.arg, ctx); append(unary.op.name); break;
            default:
                // todo metatable..
                throw Error.runtime(unary.loc, "不支持的 unary 操作符");
        }
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Binary binary, @NotNull Object ctx) {
        switch (binary.op) {
            case EQ:
            case STRICT_EQ:
            case NE:
            case STRICT_NE:

            case GT:
            case LT:
            case LE:
            case GE:

            case MUL:
            case DIV:
            case MOD:
            case PLUS:
            case MINUS:
            case POWER:

            case LOGIC_AND:
            case LOGIC_OR:
            case IS:

            case LT_LT:
            case GT_GT:
            case GT_GT_GT:

            case BIT_OR:
            case BIT_XOR:
            case BIT_AND:

                visit(binary.lhs, ctx);
                deleteLastLFs();
                append(" ");
                append(binary.op.name);
                append(" ");
                visit(binary.rhs, ctx);
                break;

            // case IN:
            // case LT_LT_LT:
            // case EXCLUSIVE_RANGE:
            default:
                visit(binary.lhs, ctx);
                append(".`").append(binary.op.name).append("`(");
                visit(binary.rhs, ctx);
                append(")");
                break;
        }
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Ternary ternary, @NotNull Object ctx) {
        switch (ternary.op) {
            case COND:
                visit(ternary.left, ctx);
                append(" ? ");
                visit(ternary.mid, ctx);
                append(" : ");
                visit(ternary.right, ctx);
                break;
            default:
                throw Error.bug(ternary.loc);
        }
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull If if1, @NotNull Object ctx) {
        append("if (");
        visit(if1.test, ctx);
        deleteLastLFs();
        append(") ");
        visit(if1.then, ctx);
        if (if1.orelse != null) {
            deleteLastLFs();
            append(" else ");
            visit(if1.orelse, ctx);
        }
        return null;
    }

    /*
    @Override
    public Void visit(Ast.@NotNull Match match, @NotNull Object ctx) {
        match("match", match, ctx);
        return null;
    }

    void match(String s, Ast.Match match, Object ctx){
        append(s).append(" (");
        visit(match.value, ctx);
        append(") {\n");
        i++;
        for (Ast.MatchCase c : match.cases) {
            prefix("case ");
            patternDumper.match(c.pattern, ctx);
            // todo 取出 literal 中的 value 之后 这里要改
            if (c.guard != null) {
                append(" if (");
                visit(c.guard, ctx);
                deleteLastLFs();
                append(")");
            }
            append(" -> ");
            visit(c.body, ctx);
        }
        i--;
        prefix("}\n");
    }

    PatternDumper patternDumper = new PatternDumper();
    class PatternDumper implements Matcher<Object, Void> {
        @Override
        public Void match(Ast.@NotNull Id ptn, @NotNull Object ctx) {
            visit(ptn, ctx);
            return null;
        }

        @Override
        public Void match(Ast.@NotNull Literal ptn, @NotNull Object ctx) {
            visit(ptn, ctx);
            return null;
        }

        @Override
        public Void match(Ast.@NotNull ListPattern ptn, @NotNull Object ctx) {
            append("[");
            listPattern(ptn.elems, ctx);
            append("]");
            return null;
        }

        void listPattern(List<Ast.Pattern> elems, Object ctx) {
            boolean isFirst = true;
            for (Ast.Pattern ptn1 : elems) {
                if (isFirst) {
                    isFirst = false;
                    match(ptn1, ctx);
                } else {
                    append(", ");
                    match(ptn1, ctx);
                }
            }
        }

        @Override
        public Void match(Ast.@NotNull MapPattern ptn, @NotNull Object ctx) {
            append("[");
            if (ptn.props.isEmpty()) {
                append(":");
            } else {
                boolean isFirst = true;
                for (Map.Entry<Ast.Expr, Ast.Pattern> it : ptn.props.entrySet()) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        append(", ");
                    }
                    visit(it.getKey(), ctx);
                    append(": ");
                    match(it.getValue(), ctx);
                }
            }
            append("]");
            return null;
        }

        @Override
        public Void match(Ast.@NotNull ExprPattern ptn, @NotNull Object ctx) {
            visit(ptn.expr, ctx);
            return null;
        }

        @Override
        public Void match(Ast.@NotNull UnApplyPattern ptn, @NotNull Object ctx) {
            visit(ptn.name, ctx);
            append("(");
            listPattern(ptn.props, ctx);
            append(")");
            return null;
        }
    }
    */

    @Override
    public Void visit(Ast.@NotNull While while1, @NotNull Object ctx) {
        append("while (");
        visit(while1.test, ctx);
        append(") ");
        visit(while1.body, ctx);
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Continue cont, @NotNull Object ctx) {
        append("continue\n");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Break brk, @NotNull Object ctx) {
        append("break\n");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Return ret, @NotNull Object ctx) {
        append("return ");
        visit(ret.arg, ctx);
        append("\n");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Throw throw1, @NotNull Object ctx) {
        append("throw ");
        visit(throw1.expr, ctx);
        append("\n");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Try try1, @NotNull Object ctx) {
        append("try ");
        visit(try1.try1, ctx);
        if (try1.catch1 != null) {
            deleteLastLFs();
            // match 版本
            // match(" catch", try1.catch1, ctx);

            // 解糖版本
            append(" catch (");
            assert try1.error != null;
            visit(try1.error, ctx);
            append(") ");
            visit(try1.catch1, ctx);
        }
        if (!try1.final1.stmts.isEmpty()) {
            deleteLastLFs();
            append(" finally ");
            visit(try1.final1, ctx);
        }
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Debugger debugger, @NotNull Object ctx) {
        append("debugger");
        return null;
    }

    @Override
    public Void visit(Ast.@NotNull Assert a, @NotNull Object ctx) {
        Ast.Id id = Ast.anon(a.loc);
        Ast.VarDef varDef = Ast.varDef(a.loc, id, a.expr, false, true);
        visit(varDef, ctx);
        append("\n");
        prefix("assert ");
        visit(id, ctx);
        if (a.msg != null) {
            append(" : ");
            visit(a.msg, ctx);
        }
        return null;
    }
}
