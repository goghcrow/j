// https://web.archive.org/web/20161029144711/http://typeof.net/2014/m/an-example-about-control-flow-abstraction-parser-expression-grammar.html

class MatchFail(val str) extends Error(str, null)

enum Rule {
    Str(val ptn)
    Choose(var superior, var inferior)
    Seq(var front, var rear)
}

fun match1(rule, str, onMatch, onFail) {
    rule match {
        case Str~(ptn) -> {
            if (str.subStr(0, ptn.size()) == ptn) {
                onMatch(str.subStr(ptn.size()), ptn)
            } else {
                onFail(str, MatchFail("Excepted " + ptn))
            }
        }
        case Choose~(superior, inferior) -> {
            match1(superior, str, onMatch,
                (str1, ret1) => match1(inferior, str1, onMatch, onFail)
            )
        }
        case Seq~(front, rear) -> {
            match1(front, str,
                (str1, ret1) => match1(rear, str1,
                    (str2, ret2) => onMatch(str2, ret1 + ret2), onFail),
            onFail)
        }
    }
}



// todo return error(a == null ? None : a.loc, Class_AssertError, String(msg), String(reason), Null);
/*
assert ['', 'a'] == match1(Str('a'), "a", (s, ret) => [s, ret], (s, ret) => [s, ret])
assert ['b', 'a'] == match1(Str('a'), "ab", (s, ret) => [s, ret], (s, ret) => [s, ret])

assert ['', 'a'] == match1(Choose(Str('a'),Str('b')), "a", (s, ret) => [s, ret], (s, ret) => [s, ret])
assert ['', 'b'] == match1(Choose(Str('a'),Str('b')), "b", (s, ret) => [s, ret], (s, ret) => [s, ret])
assert ['c', 'b'] == match1(Choose(Str('a'),Str('b')), "bc", (s, ret) => [s, ret], (s, ret) => [s, ret])

assert ['', 'ab'] == match1(Seq(Str('a'),Str('b')), "ab", (s, ret) => [s, ret], (s, ret) => [s, ret])
assert ['c', 'ab'] == match1(Seq(Str('a'),Str('b')), "abc", (s, ret) => [s, ret], (s, ret) => [s, ret])
*/

val brackets = Choose(
    Seq(
        Str('('),
        Seq(null, Str(')'))
    ),
    Str('')
)
// todo 递归数据结构导致 Stack Overflow
brackets.superior.rear.front = brackets // 递归
println('-------------------------')
match1(brackets, "(((()))---", (s, ret) => println(ret), println)
println('-------------------------~~~~~~~~~~~~~~~~~~')

assert false