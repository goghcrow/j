// tips
// 选 -> 语法初衷：是因为 lambda 使用了 =>，后来发现 java23 switch 语法
/*
int numLetters = switch (day) {
    case MONDAY, FRIDAY, SUNDAY -> 6;
    case TUESDAY                -> 7;
    case THURSDAY, SATURDAY     -> 8;
    case WEDNESDAY              -> 9;
};
JEP 305 Pattern Matching
    switch(n) {
        case IntNode(int i): return i;
        case NegNode(Node n): return -eval(n);
        case AddNode(Node left, Node right): return eval(left) + eval(right);
        case MulNode(Node left, Node right): return eval(left) * eval(right);
        default: throw new IllegalStateException(n);
    };
*/



// 🚀 is 操作符支持模式匹配
{
    class A(a)

    assert A(42) is A
    assert A(42) is A~(42)

    assert A(A([1, true, [id:42]])) is A~(_)
    assert A(A([1, true, [id:42]])) is A~(A~(_))
    assert A(A([1, true, [id:42]])) is A~(A~([_, _, _]))
    assert !(A(A([1, true, [id:42]])) is A~(A~([0, _, _])))
    assert A(A([1, true, [id:42]])) is A~(A~([_, _, [id: _]]))
    assert !(A(A([1, true, [id:42]])) is A~(A~([_, _, [id: 0]])))
    assert A(A([1, true, [id:42]])) is A~(A~([1, true, [id: 42]]))

    class B(a, b)
    assert B(1, 2) is B
    assert B(1, 2) is B~(1, _)
    assert !(B(1, 2) is B~(2, _))
}

// 🚀 注意 [:] mapPattern 用来匹配 map 时候, 匹配 map 键值对, 用来匹配对象时候, 用来匹配对象属性名和值

{
    class A
    class B(v)
    val test = it => it match {
        case A~() -> 42
        case B~(v) -> v
        case _ -> 0
    }
    assert test(null) == 0
    assert test(A()) == 42
    assert test(B(100)) == 100
}


{
    val is12 = it => it == 12

    val test = it =>
        // or it match {}
        match (it) {
          case [id: 1]              -> it   // if (it.id == 1) { it }
          case [1,2]                -> it   // val iter = i.`iterable`()
                                            // if (iter != null && iter.size() == 2 && iter[0] == 1 && iter[1] == 2)

          case User~(0, '')          -> assert false
          case User~(42, name)       -> '42' + name  // if (it is User && it.id == 42)
          case User~(12, name)       -> '12' + name  // if (it is User && it.id == 12)
          case User~(id, 'a')        -> id + 'a' // if (it is User && it.name == 'a')
          case User~(-1, 'x')        -> '-1x' // if (it is User && it.id == -1 && it.name == 'x')
          case User~(1+1, 'x'+'y')   -> '2xy'  // if (it is User && it.id == 1+1 && it.name == 'x'+'y')
          case User~(_, _)           -> `*` // if (it is User)

          case 1                -> it   // if (it == 1)     { it }
          case 'foo'            -> it   // if (it == 'foo') { it }
          case false            -> it   // if (it == true)  { it }
          case null             -> it   // if (it == null)  { it }
          case x if (x == 42)   -> it   // x = it if (x == 42)  { it }
          case x if (is12(x))   -> it   // x = it if (x == 42)  { it }
          case x                -> ``   // if (true) { x = it  x }
        }

    class IntRange(val from, val to) {
        fun `iterable`() {
            var cur = from
            object {
                fun hasNext() = cur <= to
                fun next() = hasNext() ? cur++ : null
            }
        }
        fun `hash`() = sys.hash(from, to)
        fun `==`(that) = that is IntRange && from == that.from && to == that.to
    }

    // println(IntRange(1, 2))
    // for(val it in IntRange(1, 2)) { println(it) }

    class A(id, name) {
        fun `hash`() = sys.hash(id, name)
        fun `==`(that) = that is A && id == that.id && name == that.name
    }
    class User(id, name)

    class Proxy(v) {
        fun `destruct`() = v
    }

    assert test([id:1, name:'xiaofeng']) == [id:1, name:'xiaofeng'] // case [id: 1]
    assert test(A(1, 'xiaofeng')) == A(1, 'xiaofeng') // case [id: 1]

    assert test([1,2]) == [1,2] // case [1,2]
    assert test([1,2,3]) == `` // 不能匹配 case [1,2], 数量不相等


    assert test(User(42, 'hello')) == '42hello' // case is User(42, name)
    assert test(User(42, 'world')) == '42world' // case is User(42, name)
    assert test(User(12, 'hello')) == '12hello' // case is User(12, name)
    assert test(User(12, 'world')) == '12world' // case is User(12, name)
    assert test(User(100, 'a')) == '100a' // case is User(id, 'a')
    assert test(User(-1, 'x')) == '-1x' // case is User(id, 'a')case User(-1, 'x')
    assert test(User(2, 'xy')) == '2xy' // case is User(1+1, 'x'+'y')
    assert test(User(-2, '~')) == `*` // case is User(_, _)

    assert test(Proxy(User(42, 'hello'))) == '42hello' // case is User(42, name)
    assert test(Proxy(User(42, 'world'))) == '42world' // case is User(42, name)
    assert test(Proxy(User(12, 'hello'))) == '12hello' // case is User(12, name)
    assert test(Proxy(User(12, 'world'))) == '12world' // case is User(12, name)
    assert test(Proxy(User(100, 'a'))) == '100a' // case is User(id, 'a')
    assert test(Proxy(User(-1, 'x'))) == '-1x' // case is User(id, 'a')case User(-1, 'x')
    assert test(Proxy(User(2, 'xy'))) == '2xy' // case is User(1+1, 'x'+'y')
    assert test(Proxy(User(-2, '~'))) == `*` // case is User(_, _)

    // array-pattern 可以 match 迭代器
    assert test(IntRange(1, 2)) == IntRange(1, 2)
    assert test(IntRange(1, 3)) == ``

    assert test(1) == 1 // case 1
    assert test('foo') == 'foo' // case 'foo'
    assert test(false) == false // case false
    assert test(null) == null // case null
    assert test(42) == 42 // case x if (x == 42)
    assert test(12) == 12 // case x if (x == 42)
    assert test(100) == `` // case x -> ``
}

// pattern 支持嵌套
{
    class A(b)
    class B(c)
    class C(val map)
    val test = it => it match {
        case A~(B~(C~([k: [a,b,c]]))) if (c == 42) -> a + b + c
        case _ -> 0
    }

    assert test(A(B(C([k: [1,2,42]])))) == 45
    assert test(A(B(C([k: [1,2, 0]])))) == 0
}


{
    class MyList(list)

    val test = it =>
        match(it) {
            case MyList~([1,2,3])    -> 1
            case MyList~([2,_])      -> 2
            case MyList~(_)          -> 3
            case _                  -> 0
        }

    assert test(MyList([1,2,3])) == 1
    assert test(MyList([2,3])) == 2
    assert test(MyList([2,4])) == 2
    assert test(MyList([2,3,1])) == 3
}

// 🚀 [ ... ] 可以解构任何迭代器
{
    // range
    val [_,a,b] = 1..10
    assert a == 2
    assert b == 3

    // 对象 -> Iterator
    val [a1, a2] = object {
        fun hasNext() = true
        fun next() = 1
    }
    assert a1 == 1
    assert a2 == 1


    // Symbol-Iterator -> Iterator
    object iter {
        fun `iterable`() {
            var i = 0
            object {
                fun hasNext() = i < 10
                fun next() = [id : i, val: 100 + i++]
            }
        }
    }
    val [ [id:k1,val:v1], _,[id:k3,val:v3] ] = iter
    assert k1 == 0
    assert v1 == 100
    assert k3 == 2
    assert v3 == 102
}


// 交换变量
{
    var a = 1
    var b = 2

    [b, a] = [a, b]

    assert a == 2
    assert b == 1
}

// 忽略值, 多余值
{
    // declare 支持忽略列表值
    val x = [1, 2, 3, 4, 5]
    val [y, z] = x
    assert y == 1
    assert z == 2

    val [,second,,,fifth, sixth] = x
    assert second == 2
    assert fifth == 5
    assert sixth == null


    // assign 支持 忽略列表值
    var a
    var b
    var c
    [,a,,,b, c] = x
    assert a == 2
    assert b == 5
    assert c == null
}


// 从函数返回
{
    val f = () => [1, 2]
    val a val b
    [a, b] = f()
    assert a == 1
    assert b == 2
}


// 基本赋值
{
    val o = [p: 42, q: true]

    // 与 key 同名变量需要啰嗦的写一遍好了, 否则 MapPattern 与 ListPattern 语法冲突
    val [p:p, q:q] = o
    assert p == 42
    assert q == true

    // 给新的变量名赋值
    val [p:a, q:b] = o
    assert a == 42
    assert b == true

    // 对象 pattern 与 map 字面量 key 规则一致, map key 支持表达式计算
    val [key_p, key_q] = ['p', 'q']
    val [(key_p):x, (key_q):y] = o
    assert x == 42
    assert y == true

    // 对象 pattern 与 map 字面量 key 规则一致, map key 支持表达式计算
    val o1 = [(42): 'answer']
    val [(42): answer] = o1
    assert answer == 'answer'

    // 对象 pattern 与 map 字面量 key 规则一致, map key 支持表达式计算
    val [('hello' + '-' + 'world'): hello_world] = ['hello-world': 42]
    assert hello_world == 42
}


// 无声明赋值
{
    var a
    var b
    [a, b] = [1, 2]
    assert a == 1
    assert b == 2

    ([a, b] = [1, 2])
    assert a == 1
    assert b == 2
}


// 嵌套
{
    val [a, [b, c], d] = [1, [2, 3], 4]
    assert a == 1
    assert b == 2
    assert c == 3
    assert d == 4
}


// 嵌套
{
    val metadata = [
        title: 'Hello World!',
        translations: [
            [
                locale: 'ch',
                localization_tags: [],
                url: '/ch/docs',
                title: '你好，世界！'
            ]
        ],
        url: '/en-US/docs'
    ]

    val [
        title: englishTitle,
        translations: [
            [
                title: localeTitle
            ]
        ]
    ] = metadata

    assert englishTitle == 'Hello World!'
    assert localeTitle == '你好，世界！'
}


// 从对象结构
{
    object user {
        val id = 42
        val name = 'xiaofeng'
        val metadata = [
                title: 'Hello World!',
                translations: [
                    [
                        locale: 'ch',
                        localization_tags: [],
                        url: '/ch/docs',
                        title: '你好，世界！'
                    ]
                ],
                url: '/en-US/docs'
            ]
    }

    val [
            id: uid,
            name: uname,
            metadata: [
                title: englishTitle,
                translations: [
                    [
                        title: localeTitle
                    ]
                ]
            ]
    ] = user

    assert uid == 42
    assert uname == 'xiaofeng'
    assert englishTitle == 'Hello World!'
    assert localeTitle == '你好，世界！'
}


{
    val test = a => match(a) {
        case [_, a] -> a
        // 嵌套
        case [id:x] -> match(x) { case _ -> x }
        case _      -> 42
    }

    assert test([1, 2]) == 2
    assert test([id:100]) == 100
    assert test('hello') == 42
}


// 木有匹配到报错
{
    val test = a => match(a) { }
    try {
        test(1)
    } catch (e) {
        assert e is MatchError
    }

    try {
        match(1) { case 2 -> null }
    } catch (e) {
        assert e is MatchError
    }
}

{
    val x = s => match(s) {
        // case _ if (s is String) -> s
        case String~(_) -> s
    }
    assert x('hello') == 'hello'
}