// 根据 http://code.activestate.com/recipes/474088/ 这货原理改的...
// https://www.zhihu.com/question/29717057
/*
1. 通过stack introspection查看调用链上的调用者之中有没有自己
2. 有的话，通过抛异常来迫使栈回退（stack unwind）到之前的一个自己的frame
3. 在回退到的frame接住异常，拿出后来调用的参数，用新参数再次调用自己
*/

class TcoControl(args) extends Throwable(null, null)

fun tco(f, args, dArgs) {
    assert f is Fun
    assert args is List
    assert dArgs is List

    val recursive = sys.backtrace().filter(it => it.callee == f).size() > 1
    if (recursive) {
        throw new TcoControl(args)
    } else {
        while(true) {
            try {
                assert args is List
                return f.apply(args)
            } catch(e) {
                case TcoControl~(args1) -> args = args1
            }
        }
    }
}

// export
tco