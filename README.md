## 弱类型脚本语言解释器

- 支持 OO / 反射 / 模式匹配 / 注解 / 操作符重载 / 模式匹配 / Power Assert / Java 互操作等


## Demo

### pattern matching
```
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
```

### class
```
/*
java record
record Point(int x, int y);

scala
class(val x: int, val y: int)

kotlin
data class User(val name: String, val age: Int)
*/


// Class 扩展了 `apply` 可以直接当 new
{
    class A(a, b = 1)
    assert A(42).a == 42
    assert A(42).b == 1
    assert new A(42).a == 42
    assert new A(42).b == 1
}

// Class 扩展了 `apply` 可以直接当 new
{
    // class 声明 返回 class
    val x = class World {
        val id = 42
        fun getId() { id }
    }

    //  new: 对 class 进行调用产生对象实例
    assert x == World
    assert x().getId() == World().getId()
}


{
    class A
    class B extends A

    assert A() is A
    assert B() is B
    assert B() is A
    assert A().class == A
    assert B().class == B

    assert A.class == Class
    assert B.class == Class
}

{
    class A(a)
    class B(a, b) extends A(a)

    val a = A(1)
    val b = B(1, 2)

    assert new Object.getSuper() == null

    assert a.getSuperClass() == Object
    assert a.getSuper() is Object
    assert b.getSuper().getSuper() == a.getSuper()
    assert a.getSuper().getSuper() == null

    assert b.getSuperClass() == A
    assert b.getSuper() is A
    // assert b.getSuper() == a // equals 逻辑改成 id 之后这里不成立
    assert sys.objectId(b.getSuper()) == sys.objectId(b) // 这俩对象 id 相等

    assert b.getSuper().a == 1

    // objectMember scope查找时候不应该查到环境中
    assert b.getSuper().b != b
    assert b.getSuper().b == null
}


assert true.class == Bool
assert false.class == Bool
assert true.class.class == Class
assert true.class.class.class == Class
assert 1.class == Int
assert 1.1.class == Float
assert ''.class == String
assert [].class == List
assert [:].class == Map
assert (() => null).class == Fun
assert ``.class == Symbol
assert new Object().class == Object
// assert == IteratorValue
// assert == JavaValueValue

assert Class is Class
assert Bool is Class
assert Int is Class
assert Float is Class
assert Symbol is Class
assert String is Class
assert Iterator is Class
assert List is Class
assert Map is Class
assert Fun is Class
assert Range is Class

assert Bool.class == Class
assert Int.class == Class
assert Float.class == Class
assert String.class == Class
assert List.class == Class
assert Map.class == Class
assert Fun.class == Class
assert Symbol.class == Class
assert Object.class == Class
assert Class.class == Class


assert class A1 is Class
assert true is Bool
assert 1 is Int
assert 1.1 is Float
assert Symbol('') is Symbol
assert '' is String
assert [] is List
assert [][`iterable`]() is Iterator
assert [].`iterable`() is Iterator
assert [:] is Map
assert fun f42() {} is Fun
assert 1..2 is Range

assert class A2 is Object
assert true is Object
assert 1 is Object
assert 1.1 is Object
assert Symbol('') is Object
assert '' is Object
assert [] is Object
assert [][`iterable`]() is Object
assert [].`iterable`() is Object
assert [:] is Object
// assert {-> } is Object
assert (a => a) is Object
assert 1..2 is Object


// == 默认行为属性递归相等
assert Object() == Object()
assert Bool() == false
assert Bool(false) == false
assert Bool(true) == true
assert Int() == 0
assert Int(42) == 42
assert Float() == 0.0
assert Float(42.42) == 42.42
assert String() == ''
assert String("Hi") == 'Hi'
assert List(1,2,3) == [1,2,3]
assert Map() == [:]
assert Range(1, 42) == 1..42
{
    val r1 = Range(1, 42)
    val r2 = 1..42
}


class A
class B extends A
assert A() is A
assert B() is B
assert B() is A
assert A() is Object
assert B() is Object
assert A is Class
assert B is Class
assert A is Object
assert B is Object
assert Class is Class
assert Class is Object


class MyList(val list)
assert MyList is Class


// 修改成必须先定义再使用，所以构造不出来循环定义了...
/*
{
    class A extends C {}
    class B extends A {}
    class C extends B {}
    try {
        new C()
        assert false
    } catch (e) {
        case _ -> assert e.msg == '类循环定义 → A, B, C'
    }
}
*/

{
    try {
        class A extends NotExist
    } catch(e) {
        case ClassNotFoundError~(msg) -> assert true
        case _ -> assert false
    }
}

{
    try {
        class A extends NotExist
    } catch(e) {
        assert e is ClassNotFoundError
    }
}

{
    try {
        sealed class A
        class B extends A
    } catch (e) {
        assert e.msg == 'sealed A 不能被继承'
    }
}

{
    val isIter = Iterator(object {
        fun hasNext() = true
        fun next() = 1
    }) is Iterator
    assert isIter
}

// 静态属性，可以挂到 class 上
{
    class A
    A.static_name = 'xiaofeng'
    A.static_id = 42

    assert A.static_name == 'xiaofeng'
    assert A.static_id == 42

    val a1 = A()
    val a2 = A()

    // class 属性可以通过实例来读
    assert a1.static_name == 'xiaofeng'
    assert a1.static_id == 42

    assert a2.static_name == 'xiaofeng'
    assert a2.static_id == 42

    // 但是不能通过实例来写, 实例写了自己的属性
    a1.static_id = 100
    assert a1.static_id == 100
    assert A.static_id == 42
    assert a2.static_id == 42
}

{
    class A {
        var id = 100
    }
    A.id = 42

    assert A.id == 42
    assert A().id == 100

    A().id == 0
    assert A.id == 42
}


{
    class A
    assert new A is A
    assert A() is A
}


{
    class Color(val r, val g, val b)
    class Red(val r) extends Color(r, 0, 0)
    class Green(val g) extends Color(0, g, 0)
    class Blue(val b) extends Color(0, 0, b)

    val [r:r1,g:g1,b:b1] = Red(255)
    assert r1 == 255 && g1 == 0 && b1 == 0

    val [r:r2,g:g2,b:b2] = Green(255)
    assert r2 == 0 && g2 == 255 && b2 == 0

    val [r:r3,g:g3,b:b3] = Blue(255)
    assert r3 == 0 && g3 == 0 && b3 == 255

    val test = color => color match {
        case Red~(r) -> r
        case Green~(g) -> g
        case Blue~(b) -> b
    }
    assert test(Red(42)) == 42
    assert test(Green(125)) == 125
    assert test(Blue(100)) == 100

    // 测试优先级
    Red(0) is Red match { case _ -> null }
}

{
    class Color(val r, val g, val b)
    object Red extends Color(255, 0, 0)
    object Green extends Color(0, 255, 0)
    object Blue extends Color(0, 0, 255)

    val [r:r1,g:g1,b:b1] = Red
    assert r1 == 255 && g1 == 0 && b1 == 0

    val [r:r2,g:g2,b:b2] = Green
    assert r2 == 0 && g2 == 255 && b2 == 0

    val [r:r3,g:g3,b:b3] = Blue
    assert r3 == 0 && g3 == 0 && b3 == 255
}

// 🚀 object 直接声明单例类
{
    object user {
        val id = 42
        val name = 'xiaofeng'
    }

    assert user.id == 42
    assert user.name == 'xiaofeng'
}

// 🚀 匿名单例
{
    val user = object {
        val id = 42
        val name = 'xiaofeng'
    }

    assert user.id == 42
    assert user.name == 'xiaofeng'
}

// 🚀 类方法声明 可选等号, body 如果只有一个表达式，可以省略 block
// 🚀 object 声明可以直接继承 class
{
    class A(id) {
        fun m1(a) = a
        fun m2(a) = { a }
        fun m3(a) { a }
        fun m4(a) = {
            val b = a + 1
            b
        }
        fun m5(a) {
            val b = a + 1
            b
        }
    }

    object subAIns extends A(42)

    assert subAIns.id == 42
    assert subAIns.m1(42) == 42
    assert subAIns.m2(42) == 42
    assert subAIns.m3(42) == 42
    assert subAIns.m4(42) == 43
    assert subAIns.m5(42) == 43
}

// 🚀 类签名构造函数，参数会被提升为类同名属性，可以直接用子类签名构造函数参数调用父类构造函数
{
    class A(x)
    class B(a,b) extends A(a)
    class C(a,b) extends A(b)

    val b = B(1, 2)
    assert b.a == 1 && b.b == 2 && b.x == 1

    val c = C(1, 2)
    assert c.a == 1 && c.b == 2 && c.x == 2
}


// 🚀 当采用签名构造函数时，类 body 由属性、方法列表变成 block = list of stmt
// 所以变量声明被提升为类属性，所以方法声明（不包括匿名函数）被提升为类方法
{
    class User(name, age) {
        val addr = "hz"
        fun getAddr() = addr

        class Person(name) // 可以嵌套

        assert this.addr == 'hz' && this.name == 'x' && this.age == 42
    }
    var u = User('x', 42)
    assert u.Person("xf").name == 'xf'
    assert u.addr == 'hz'
    assert u.getAddr() == 'hz'
}

// 🚀 构造函数也可以解构赋值，解构出来的标识符都会被自动转换成类属性
{
    class User(val id, val name) {
        // val var 声明变量会被提取成类属性

        // array-pattern嵌套, 可以略过不需要的值
        val [a, [b, c], , d] = [1, [2, 3], 4, 5]

        // array-pattern 与 object-pattern 嵌套
        val [
            title: englishTitle,
            translations: [
                [
                    title: localeTitle
                ]
            ]
        ] = [
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

        // fun 声明会被提取成类方法
        fun getTitle() = localeTitle
    }

    val user = new User(42, 'xiaofeng')

    assert user.name == 'xiaofeng'
    assert user.id == 42
    assert user.a == 1
    assert user.b == 2
    assert user.c == 3
    assert user.d == 5
    assert user.englishTitle == 'Hello World!'
    assert user.localeTitle == '你好，世界！'
    assert user.getTitle() == '你好，世界！'
}


// 🚀 类签名声明构造函数后，body 不能再重复声明
{
    class A() {
        // fun A = 1    // 与 ctor 重名 error
        // fun A() {}   // 与 ctor 重名 error
        // fun construct = 1    // 与 ctor 重名 error
        // fun construct() {}   // 与 ctor 重名 error
    }
}


// 🚀 类签名构造函数参数可以选择声明 val/var，默认 val
{
    class Point(x, y)
    var p = Point(1, 2)
    assert p.x == 1
    assert p.y == 2
    // p.x = 2 // error
}


// 🚀 类签名构造函数参数可以选择声明 val/var，默认 val
{
    class Point(val x, val y)
    var p = Point(1, 2)
    assert p.x == 1
    assert p.y == 2
    // p.x = 2 // error

    assert Point(3,4).x == 3
    assert Point(1,2).y == 2
}


// 🚀 类签名构造函数参数可以选择声明 val/var，默认 val
{
    class Point(var x, y)

    var p = Point(1, 2)
    assert p.x == 1
    assert p.y == 2

    p.x = 2
    assert p.x == 2
}


// 🚀 类声明可以省略 body
{
    class User
    assert User() is User
}


// 🚀 因为继承只会在解释执行时候查找，所以声明顺序无所谓，只要子类声明作用域能找到父类即可
// 下一个 case 有歧义，取消这种玩意
/*
{
    class A extends B
    class B
}
*/

// 消除歧义，先定义再使用
{
    // 注意：需要空出一行否则 block 会变成 class 的body
    class B(id = 42)

    {
        class A extends B
        class B(id = 100)
        assert new A().id == 42
    }
}


// 🚀 类名可选，支持匿名类
{
    class A {
        var id
        fun construct(id) { this.id = id }
    }
    assert (class extends A { fun construct(id) = super(id) } (42)).id == 42
    assert (class extends A { fun construct(id) = super(id) } )(42) is A
}


// 🚀 类名可选，支持匿名类，类声明是表达式
{
    val x = class {
        var id
        fun setId(id) {
            this.id = id
            this
        }
        fun getId() { id }
    }
    assert x is Class
    assert x().setId(42).getId() == 42

    val y = class {
        var id
        fun setId(id) {
            this.id = id
            this
        }
        fun getId() { id }
    } ()
    assert y.setId(42).getId() == 42
}



// 🚀 继承的类每个类的实例都有自己独立的类作用域，子类实例可以通过 super 访问到父类实例
{
    class X {
        val x = 1
        fun m() { 1 }
    }
    class Y extends X {
        val x = 42
        fun m() { super.m() }
        fun sx() { super.x }
        fun tx() { this.x }
    }
    val y = Y()
    assert y.sx() == 1
    assert y.tx() == 42
    assert y.m() == 1
}


// 🚀 实例构造顺序 & override & super 引用
{
    class X {
        val x1 = 'x1'
        var x2
        var SEQ = ''
        var RESULT
        fun construct(id, x2) {
            RESULT = id
            SEQ += "🦉 X ctor"
            this.x2 = x2
        }
        fun override() = 'x'
    }
    class Y extends X {
        val y1 = 'y1'
        var y2
        fun construct(id, y2) {
            super(id, 'x2')
            SEQ += "\n🦉 Y ctor"
            this.y2 = y2
        }
        fun override() = super.override() + 'y'
    }
    class Z extends Y {
        val z1 = 'z1'
        var z2
        fun construct(id, z2) {
            super(id, 'y2')
            SEQ += "\n🦉 Z ctor"
            this.z2 = z2
        }
        fun override() = super.override() + 'z'
    }


    val z = Z(42, 'z2')

    assert z.RESULT == 42
    assert z.x1 == 'x1'
    assert z.x2 == 'x2'
    assert z.y1 == 'y1'
    assert z.y2 == 'y2'
    assert z.z1 == 'z1'
    assert z.z2 == 'z2'

    assert z.override() == 'xyz'

    assert z.SEQ == "🦉 X ctor\n🦉 Y ctor\n🦉 Z ctor"

    /*
    🦉 X ctor
    🦉 Y ctor
    🦉 Z ctor
    x-override
    y-override
    z-override
    */
}


// 🚀 文法作用域
{
    var i = 1
    class A {
        val a = i + 1
        val b = a + 1
    }

    assert A().a == 2
    assert A().b == 3

    i = 2
    assert A().a == 3
    assert A().b == 4

    var c = 0
    class Test {
        fun m() { ++c }
    }
    assert Test().m() == 1
    assert Test().m() == 2
}

{
    class A(var id) {
        fun `hash`() = sys.hash(id)
        fun `==`(that) = that is A && id == that.id
    }
    class B(id, name) extends A(id)
    class C(id, name) extends A(id) {
        fun `hash`() = sys.hash(id, name)
        fun `==`(that) = that is B && super.`==`(that) && name == that.name
    }

    assert A(1) == A(1)
    assert B(1, '') != B(2, '')
    // todo 这里相等的原因是 B 没有 `==` 方法, 所以使用了 A 的方法判断
    assert B(1, '1') == B(1, '2')
}

{
    class A(var id) {
        fun `hash`() = sys.hash(id)
        fun `==`(that) = that is A && id == that.id
    }
    assert A(1) == A(1)

    class B extends A {
        val name
        fun construct(id, name) {
            // 父类有参数需要显示 call 父类构造函数
            super(id)
            this.name = name
        }
        fun getId() = id
        fun getThisId() = this.id
        fun getSuperId() = super.id
        fun `hash`() = sys.hash(id, name)
        fun `==`(that) = that is B && super.`==`(that) && name == that.name
    }

    assert B(1, '') != B(2, '')
    assert B(1, '1') != B(1, '2')
    assert B(1, '1') == B(1, '1')

    val b = new B(42, 'xiao')
    assert b.id == 42
    assert b.getId() == b.getThisId()
    assert b.getId() == b.getSuperId()

    assert new B(1, 'xiao') == new B(1, 'xiao')
    assert new B(1, 'xiao') != new B(2, 'xiao')
}

// 一个初始化问题bug, so, 必须把属性初始化放到 ctor 里头, 爆炸...
// todo.. 可以优化下, 常量不需要改写到 ctor
{
    object a {
        val f1 = 42
        val f2 = m()
        fun m() = f1
    }
    assert a.f2 == 42
}
```
### class_scope
```
{
    // ⭐️⭐️⭐️
    // scope 与 object 统一
    // 函数闭包 = 类闭包 = 作用域 = 键值对链

    // 函数声明的解释求值，即将函数代码+求值环境打包生成闭包对象
    // 对象声明的解释求值，即将类代码+求值环境打包生成闭包对象(与函数闭包相同结构)

    // 对象构造时涉及3个Scope
    // 1. 类构造时(ctor)参数求值的 Scope，即 new 或者 对象名()表达式 所在的词法作用域
    // 2. 类声明的词法 Scope，当类 A 有父类 B 时，会在 A 声明的作用域查找 B 的声明
    // 3. 类实例作用域，当类 A 与父类 B 时, A 实例作用域的父作用域会指向 B 实例的作用域
    // 当类没有父类时，类实例作用域的父作用域指向类声明时的词法作用域（同函数调用一致）

    // -> 表示 parent
    // B实例Scope -> A实例Scope -> B声明时Scope(B的词法作用域，类比与函数声明时的词法作用域)
    // ❗️❗️❗️注意这里 A-Scope 的 parent 指向 B 的词法作用域

    // 对象作用域即对象自身!!!

    // 函数调用时候，创造一个新作用域指向函数声明时的词法作用域，在函数调用发生的词法作用域对实参求值，
    // 将形参-实参定义在新作用域，对函数体求值

    // 语义基本与 java 一致，参见 ClassScopeTest


    {
        val v = 'a-up-val'
        class A {
            val v = 'a-prop'
        }

        {
            val v = 'b-up-val'
            class B extends A {
                fun get_v() {
                    v
                }
            }
            val b = new B()
            assert b.v == 'a-prop'
            assert b.get_v() == 'a-prop'
        }
    }

    // bug!  objectMember scope查找时候不应该查到环境中
    {
        val v = 1
        class A
        assert new A().v != v
        assert new A().v == null
    }

    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/

    {
        val v = 'a-up-val'
        class A {
            // fun v = 'a-prop'
        }

        {
            val v = 'b-up-val'
            class B extends A {
                fun get_v() {
                    v
                }
            }
            assert B().v == 'b-up-val'
            assert B().get_v() == 'b-up-val'
        }
    }
}


// ⭐️⭐️⭐️ 注意声明顺序无所谓，执行时 lookup
{
    class A {
        fun getValue() = v
    }
    val v = 42
    assert new A.getValue() == 42
}

// ⭐️⭐️⭐️ 自己臆想出来的作用域链
{
    // scope 遵循词法作用域

    // scope-1 (A定义所在环境, 类比闭包环境)
    val v = 1
    class A {
        // scope-2 (getValue 定义所在环境)
        fun getValue() = v // scope-3 (A.getValue 方法体 block 环境)
    }

    // scope-1
    val class_b = {
        // scope-4 (B定义所在环境, 类比闭包环境)
        val v = 42
        class B extends A {
            // scope-5
            fun getValue() = v // scope-6 (B.getValue 方法体 block 环境)
            fun getSuperValue() = super.getValue()
        }
    }

    val b = class_b() // 这里通过 ClassValue 的 `apply` 来 new 对象

    // b 的 scope 链条 5 -> 2 -> 4 -> 1
    //      [5(b) -> 2(a)] -> [4(B定义环境) -> 1]
    // super 即 a 的 scope 链条 2 -> 1

    assert b.getValue() == 42
    // B.getValue() v 的 lookup 过程 6 -> 5 -> 2 -> 4
    // B.getSuperValue() super lookupLocal 命中 a
    //      A.getValue() v 的 lookup 过程 2 -> 1

    assert b.getSuperValue() == 1
}

{
    class A {
        fun getValue() = v
    }
    val v = 1

    val class_b = {
        class B extends A {
            fun getValue() = v
            fun getSuperValue() = super.getValue()
        }
        val v = 42
        B
    }

    // 这里通过 ClassValue 的 apply 来 new 对象
    val b = class_b()
    assert b.getValue() == 42
    assert b.getSuperValue() == 1
    assert b.getSuper().getValue() == 1
}



{
    val x = 1
    class A {
        val a = x
    }

    {
        val x = 2
        val a = 3
        class B extends A {
           val b = x // 优先文法最近的 upValue 2
           val c = a // 优先父类属性 1
           fun ma() {
               a // 优先父类属性 1
           }
           fun mx() {
               x // 优先文法最近的 upValue 2
           }
        }
        assert B().a == 1
        assert B().b == 2
        assert B().c == 1
        assert B().ma() == 1
        assert B().mx() == 2
    }
}



// 扩展方法的 scope 比较特殊
//      需要      ext-method-scope -> object-scope -> ...parent-object-scope -> ext-method-def-scope
//      普通方法作用域 method-scope -> object-scope -> ...parent-object-scope -> class-def-scope
//      或者直接做成 ext-method-scope -> ext-method-def-scope, object-scope 内容使用 this super 来索引
{
    // 扩展内置类型作用域测试
    {
        val up_val = 100
        {
            val up_val = 42
            fun test_scope extends Int() = this + up_val
        }
        assert 1.test_scope() == 43
    }

    // 扩展自定义类型作用域测试
    {
        val up_val = 100
        class A(v)  // 注意与下面 block 之间多一个空行，否则下方 block 会被识别为 class-block

        {
            val up_val = 42
            fun test_scope extends A() = this.v + up_val
            assert new A(1).test_scope() == 43
        }
    }
}
```

### reflect
```
{
    assert Bool.name == 'Bool'
    assert true.class.name == 'Bool'

    assert Int.name == 'Int'
    assert 42.class.name == "Int"
    assert Int.class.name == 'Class'

    assert Float.name == 'Float'
    assert 3.14.class.name == "Float"

    assert Symbol.name == 'Symbol'
    assert `42`.class.name == 'Symbol'

    assert String.name == 'String'
    assert ''.class.name == 'String'

    assert List.name == 'List'
    assert [].class.name == 'List'
    assert new List().class.name == 'List'

    assert Map.name == 'Map'
    assert [:].class.name == 'Map'
    assert new Map().class.name == 'Map'

    assert [][`iterable`]() is Iterator
    assert [][`iterable`]().class.name == 'Iterator'

    assert Java.raw(1) is JavaValue
    assert Java.box(1) is JavaValue
    assert Java.raw(1).class.name == 'JavaValue'
    assert Java.box(1).class.name == 'JavaValue'

    fun hello() = null
    assert hello.name == 'hello'
    assert hello.class.name == 'Fun'
    assert (() => null).class.name == "Fun"

    assert println.name == 'println'
    val my_println = println
    assert my_println.name == 'println'

    val arrow_fn = it => null
    assert arrow_fn.isArrow

    class A
    fun `sym` extends A() = null
    // todo 这里鬼畜
    // assert A.methods[1].name == '`sym`'
    // assert A.methods[1].isSymbol
    // assert A.methods[1].isExtend
}

{
    assert Int.parent == Object
    class A
    class B extends A
    assert A.parent == Object
    assert B.parent == A
}

{
    class A(var a, val b = 1)

    assert A.name == 'A'
    assert A.parent == Object
    assert A.properties is List
    assert A.properties.size() == 2
    assert A.properties[0].name == 'a'
    assert A.properties[0].mutable == true
    assert A.properties[1].name == 'b'
    assert A.properties[1].mutable == false
    assert A.methods is List
    assert A.methods.size() == 1
    assert A.construct == A.methods[0]

    val ctor = A.construct
    assert ctor.className == 'A'
    assert ctor.name == 'construct'
    assert ctor.parameters is List
    assert ctor.parameters == ['a', 'b']
    assert ctor.defaults is List
    assert ctor.defaults == [null, 1]
    assert ctor.isSymbol == false
    // assert ctor.isExtend == false
    assert ctor.isArrow == false

    class B extends A(0)
    assert new B().a == 0
    assert new B().b == 1
    assert new B().class == B
    assert B.parent == A
    assert B.parent.name == 'A'

    assert new B().getSuper().class == A
}

// lst map 字面量
{
    fun f(lst = [1,2,3], map = [id:[1,2,3]]) = null
    val defaults = f.defaults
    assert defaults[0] == [1,2,3]
    assert defaults[1] == [id:[1,2,3]]
}

{
    fun a() = null
    assert a.requiredParametersSize == 0

    fun ab(a, b = null) = null
    assert ab.requiredParametersSize == 1

    fun abc(a = null, b = null) = null
    assert abc.requiredParametersSize == 0
}
```
### enum
```
// 其实是 class 的语法糖，用来进行模式匹配语义上更合理

{
    enum A
}

{
    enum A {
        A1
        A2
    }
    try {
        class x extends A
        // todo 不能继承
        // assert false
    } catch(e) {}

    try {
        class x extends A1
        assert false
    } catch(e) {
        assert e.msg == 'sealed A1 不能被继承'
    }
}

{
    enum A {
        /*class*/A1(val x) {
            fun get_x() = x
        }
        /*class*/A2(val y, val z) {
            fun y_plus_z() = y + z
        }
    }
    assert A1(42).get_x() == 42
    assert A2(1, 2).y_plus_z() == 3
    assert A.values() == [A1, A2]
}


enum Shape {
    Retangle(width, height)
    Triangle(bottom, height)
    Circle(radius)
    Nothing
}

assert Shape.values() == [Retangle, Triangle, Circle, Nothing]

{
    enum Option {
        Some(value)
        None // todo 没有参数直接构造成def object，别构造成 class ???
    }

    val x = Some(5)
    val y = None() // 这样这里就不用构造一个新的 None 了

    fun foo(option) {
        option match {
            case Some~(x) -> x
            case None~() -> 0
        }
    }

    assert foo(x) == 5
    assert foo(y) == 0
}
```

### exception
```
// 🚀 try-catch-finally 三部分构成, catch-finally 二选一,
// catch 部分可以写成传统形式， 也可以写成 match 结构，
// match 不到任何 case, catch 到的 throwable 会继续往上抛出

{
    try {
        throw 42
        assert false
    } catch (i) {
        assert i == 42
    }
}


{
    var i = 0
    val x = try {
        throw 42
        assert false
    } catch (e) {
        case a -> {
            assert a == 42
            12
        }
    } finally {
        i++
        // return
    }

    assert x == 12
    assert i == 1
}

// try-finally
{
    var a = 1
    try {

    } finally {
        a++
    }
    assert a == 2
}

// 为空代表吃掉异常
{
    val v = try {
        try {
            throw 42
        } catch(v) {}
    } catch(v) {
        case _ -> v
    }
    assert v == null
}

// match 不到异常继续往上抛
{
    val test = it =>
        try {
            try {
                throw it
            } catch(v) {
                case [1, 3] -> 2
                case [1, 2, 3] -> 4
            }
        } catch(v) {
            case [1, 2] -> 1
            case [2, 3] -> 3
            case _ -> 42
        }

    assert test([1,2]) == 1
    assert test([1,3]) == 2
    assert test([2,3]) == 3
    assert test([1,2,3]) == 4
    assert test(null) == 42
    assert test([:]) == 42
}

{
    try {
        while(true) {
            throw 1
        }
    } catch (e) {
        case _ -> null
    }
}

{
    class C {
        fun a() { b() }
        fun b() { c() }
        fun c() { throw 1 }
    }

    val v = try {
        C().a()
        assert false
    } catch (v) {
        case 1 -> 42
    }
    assert v == 42
}


{
    fun a() { b() }
    fun b() { c() }
    fun c() { throw 1 }

    try {
        a()
        assert false
    } catch (v) {
        case _ -> null
    }
}

{
    fun test_apply() {
        val t1 = Throwable('hello')
        assert t1.msg == 'hello'
        // assert t1.stackTrace.size() == 1

        val t2 = Throwable('world', t1)
        assert t2.msg == 'world'
        // assert t2.stackTrace.size() == 1
        assert t2.cause == t1
    }
    test_apply()

    fun test_new() {
        val t1 = new Throwable('hello')
        assert t1.msg == 'hello'
        // assert t1.stackTrace.size() == 1

        val t2 = new Throwable('world', t1)
        assert t2.msg == 'world'
        // assert t2.stackTrace.size() == 1
        assert t2.cause == t1
    }
    test_new()

    fun test_ext() {
        class Error(msg, cause) extends Throwable(msg, cause)

        val t1 = new Error('hello', null)
        assert t1.msg == 'hello'
        // assert t1.stackTrace.size() == 1

        val t2 = new Error('world', t1)
        assert t2.msg == 'world'
        // assert t2.stackTrace.size() == 1
        assert t2.cause == t1
    }
    test_ext()
}

{
    class A_Error(msg, cause = null) extends Error(msg, cause)
    class B_Error(msg, cause = null) extends Error(msg, cause)

    val test = it => try {
        throw it
    } catch (e) {
        case RuntimeError~(msg, _) -> [RuntimeError, msg]
        case A_Error~(msg, _) -> [A_Error, msg]
        case B_Error~(msg, _) -> [B_Error, msg]
        // case _ -> throw e 木有必要，默认行为
    }

    var [e, msg] = test(new RuntimeError('hello'))
    assert e == RuntimeError
    assert msg == 'hello'

    [e, msg] = test(new A_Error('a'))
    assert e == A_Error
    assert msg == 'a'

    [e, msg] = test(new B_Error('b'))
    assert e == B_Error
    assert msg == 'b'

    try {
        test(new Error)
        assert false
    } catch (e) {
        case Error~(_,_) -> assert true
        case _ -> assert false
    }
}

{
    fun a(x,y) = b(x+1,y+1)
    fun b(x,y) = c(x+1,y+1)
    fun c(x,y) = d(x+1,y+1)
    fun d(x,y) = throw new RuntimeError(x + y)

    try {
        a(1, 2)
    } catch (e) {
        case RuntimeError~(msg, _) -> {
            // val [d, c, b, a] = e.stackTrace
            val [
                [callee:fun_d, args:args_d],
                [callee:fun_c, args:args_c],
                [callee:fun_b, args:args_b],
                [callee:fun_a, args:args_a]
            ] = e.stackTrace

            assert fun_d == d
            assert fun_c == c
            assert fun_b == b
            assert fun_a == a
            assert args_d == [4,5]
            assert args_c == [3,4]
            assert args_b == [2,3]
            assert args_a == [1,2]
        }
    }
}


{
    fun a(x,y) = b(x+1,y+1)
    fun b(x,y) = c(x+1,y+1)
    fun c(x,y) = d(x+1,y+1)
    fun d(x,y) = sys.backtrace()

            val [
                [callee:fun_d, args:args_d],
                [callee:fun_c, args:args_c],
                [callee:fun_b, args:args_b],
                [callee:fun_a, args:args_a]
            ] = a(1, 2)

            assert fun_d == d
            assert fun_c == c
            assert fun_b == b
            assert fun_a == a
            assert args_d == [4,5]
            assert args_c == [3,4]
            assert args_b == [2,3]
            assert args_a == [1,2]
}


// case 木有匹配 继续往外抛e
{
    val r  = try {
        try {
            throw new Error(1)
        } catch (e) {
            // case 木有匹配 继续往外抛e
            case Error~(2, _) -> assert false
        }
    } catch(e1) {
        case Error~(1, _) -> 1
    }
    assert r == 1
}
```

### operator
```
{
    assert 1 + 2 === 3
    assert 1 - 2 === -1
    assert -1 === -1
    assert +2 === 2
    assert 2 * 3 === 6
    assert 4 / 2 === 2
    assert 4 % 3 === 1
    assert 4 << 2 === 16
    assert 1 << 0 === 1
    assert (1 << 63) < 0 === true
    assert 1 << 63 === -0x7fffffffffffffff - 1
    assert 1 << 64 === 1
    assert -4 >> 1 === -2
    assert -4 >>> 1 === 0x7fffffffffffffff - 1
    assert 1 & 1 === 1
    assert 0 | 1 === 1
    assert 1 ^ 1 === 0
    assert ~1 === -2
    // assert !1 === false
    assert 1 < 2 === true
    assert 2 > 1 === true
    assert 2 ** 8 === 256.0
}

{
    // ====== Updater ======

    // 不能写成1--1， -(-1) 会和 -- 冲突
    assert 1 - -1 == 2

    var i = 0
    assert ++i == 1
    assert i == 1
    assert --i == 0
    assert i == 0
    assert i++ == 0
    assert i == 1
    assert i-- == 1
    assert i == 0
}


{
    // POWER 右结合性
    val foo = 3
    assert foo * 2 == 6
    assert foo* 2 == 6
    assert foo** 2 == 9

    assert (2 ** 3 ** 2) == (2 ** (3 ** 2))

    // 三元运算
    assert (true ? 1 : 2) == 1
    assert (false ? 1 : 2) == 2
}

{
    assert +1 == 1
    assert +1.1 == 1.1

    assert -1 == -1
    assert -1.1 == -1.1

    var x = 1
    x = +x
    assert x == 1

    x = -x
    assert x == -1

    x = 1
    x = -(-x)
    assert x == 1

    assert ++x == 2
    assert x == 2

    assert x++ == 2
    assert x == 3

    assert --x == 2
    assert x == 2

    assert x-- == 2
    assert x == 1
}
```

### operator overload
```
// `in` `isCase`
{
    {
        assert 'a' in [a:1]
        assert ('b' in [a:1]) == false

        assert 1 in [1,2,3]
        assert (0 in [1,2,3]) == false

        val v = try {
            1 in 1
            assert false
        } catch(e) {
            case OperatorError~(err, _) -> 42
            case _ -> assert false
        }
        assert v == 42
    }

    // a in b  b.`isCase`(a)
    {
        class A {
            // `isCase` 参数为 in 操作符的左操作数
            fun `isCase`(lhs) = true
        }
        val a = A()

        assert null in [null]
        assert null in a
        assert 1 in a
        assert '' in a
        assert [] in a
        assert [:] in a
        assert 1..9 in a
        assert false in a
    }

    // `in` `isCase`
    {
        class A {
            // `isCase` 参数为 in 操作符的左操作数
            fun `isCase`(lhs) = lhs == 42
        }
        val a = A()

        assert 42 in a
        assert ([] in a) == false
        assert (3.14 in a) == false
    }

    {
        class A {
            fun `in`(rhs) = true
        }
        val a = A()

        assert a in null
        assert a in 1
        assert a in ''
        assert a in []
        assert a in [:]
        assert a in 1..9
        assert a in false
    }
}

{
    class A(a = 42)
    A.`toString` = () => this.a
    assert new A() + '' == "42"

// todo
/*
    class B(a = 42)
    B.`inspect` = () => this.a
    assert new B() + '' == "42"
*/

    val null_toString = Null.`toString`

    // ‼️ 这种扩展是全局的
    Null.`toString` = () => 'nil'
    assert null + '' == 'nil'

    fun `toString` extends Null() = 'hello'
    assert null + '' == 'hello'

    // 测试完了还原回去
    Null.`toString` = null_toString
}

// 两种方式等价
{
    val int_toString = Int.`toString`

    Int.`toString` = () => this + 1
    assert 42 + '' == '43'

    fun `toString` extends Int() = this + 2
    assert 42 + '' == '44'

    Int.`toString` = int_toString
}


{
    class Range(val from, val to) {
        fun `iterable`() {
            var cur = from
            object {
                fun hasNext() = cur <= to
                fun next() = hasNext() ? cur++ : throw new IterError
            }
        }
    }

    var sum = 0
    for(val it in Range(0, 9)) {
        sum += it
    }
    assert sum == 45
}

{
    class A {
        fun `apply`() {
            println(this)
        }
    }

   //A()
}
```
### map & list & symbol
```
// 🚀 map 字面量格式 [expr: expr]， 其中有一个特殊的处理
// 因为 expr 大部分情况是字符串, 所以字符串默认不用加引号
// 如果 key 想表达变量，需要将 name/identifier 转换成 expr,   e.g. (id)
// 🚀 map 键值对只能用 [] 访问, .用来访问 map 作为对象的属性
// 这里有个问题 obj[expr] 可以用来动态访问对象属性, 但是没有办法动态访问 map作为对象的属性, 只能动态访问 map 的 key

{
    assert [str: 1] == ['str':1]
    val str = 'hello'
    assert [(str): 1] == ['hello': 1]
    assert [str + '': 1] == ['hello': 1]

    // pattern 一样
    {
        val [name: x] = [name: 'xiaofeng']
        assert x == 'xiaofeng'
    }
    {
        val key = 'name'
        val [(key): x] = [name: 'xiaofeng']
        assert x == 'xiaofeng'
    }
}

{
    class A
    assert [class: A].class == Map
    assert [class: A]['class'] == A
}

{
    [:]
    ['class': 1]
}
{
    assert [true ? 1 : 2] == [1]
    assert [(true ? 1 : 2): 2] == [1: 2]
}


{
    assert [(1+1): 1][2] == 1
}

{
    assert [1:1][1] == 1
    assert [(1):1][1] == 1
    assert ['1':1]['1'] == 1
    assert ['1':1][1] == null

    assert [1.1: 1][1.1] == 1
}

{
    assert ['foo': 1]['foo'] == 1
    assert ["foo": 1]['foo'] == 1
    assert [foo: 1]['foo'] == 1
}

{
    val k = 'foobar'
    assert [('foo'+'bar'): 1][k] == 1
    assert [('foo'+'bar'): 1]['foobar'] == 1
}

{
    val map = [
        (null): 1,
        (true): 1,
        (1): 1,
        (3.14): 1,
        ([1,2]): 1,
        ([foo:'bar']): 1,
    ]

    assert map.null == null
    assert map[null] == 1

    assert map['1'] == null
    assert map[1] == 1

    assert map['3.14'] == null
    assert map[3.14] == 1

    assert map[[1,2]] == 1

    assert map[[foo:'bar']] == 1
}

{
    // symbol 属性不能用来迭代或者遍历
    val m = [a:1, b:2, `sym`:3]
    assert m.size() == 2
    var i = 0
    for (val [k,_] in m) {
        assert k == 'a' || k == 'b'
    }
}

assert [1, ] == [1]

// 💐 负数数组下标
{
    val arr = [1,2,3,]
    assert arr[-1] == 3
    assert arr[-2] == 2
    assert arr[-3] == 1
}

assert [1,2] + [3,4] == [1, 2, 3, 4]

{
    val lst = []
    lst << 1
    lst << 2
    assert lst == [1, 2]
}

assert [].`iterable`() is Iterator
assert [].`iterable`() is Object

{
    assert `+` == Symbol('+')
}

{
    class A {
        var i = 42
    }

    A.`+` = (rhs) => this.i + rhs
    A.`toString` = () => this.i

    val a = new A()
    assert a + 1 == 43
    assert a + '' == '42'
}

{
    class A {
        var i = 42
        fun `+`(rhs) = i + rhs
        fun `toString`() = i
    }

    val a = new A()
    assert a + 1 == 43
    assert a + '' == '42'
}

{
    object list {
        var i = 0
        val v = []
        fun `<<`(a) = v[i++] = a
    }

    list << 'hello'
    list << 'world'

    assert list.v[0] == 'hello'
    assert list.v[1] == 'world'
}

{
    class A(id)
    class B extends A {
        fun construct() = super(42)
        fun `+`(v) = id + v
    }
    assert new B() + 1 == 43
}

{
    class A(name)
    class B(id,name) extends A(name)

    // 💕 在类外头声明的可以自动绑定 this super
    B.`+` = (rhs) => {
        assert this.id == 42
        assert super.name == 'xiao'
        rhs
    }
    assert B(42, 'xiao') + 100 == 100
}

{
    class A(name)
    class B(id,name) extends A(name)

    // 💕 扩展方法可以自动绑定 this super
    fun `+` extends B(rhs) {
        assert this.id == 42
        assert super.name == 'xiao'
        rhs
    }
    assert B(42, 'xiao') + 100 == 100
}

{
    class A(name)
    class B(id,name) extends A(name) {
        // this super 可选
        fun `+`(rhs) {
            assert this.id == 42
            assert super.name == 'xiao'
            rhs
        }
    }
    assert B(42, 'xiao') + 100 == 100
}

{
    class A(val id)
    // 指定 call 行为
    A.`apply` = () => this.id
    val a = A(42)
    assert a() == 42
}


// 🚀 迭代器
{
    object obj {
        var i = 0
        fun `iterable`() {
            object {
                fun hasNext() = i < 100
                fun next() = i++
            }
        }
    }

    var i_ = 0
    for (val i in obj) {
        assert i_ == i
        i_++
    }
    assert i_ == 100
}

// 🚀 迭代器
{
    object obj {
        val start = 100
        val end = 110
        var cur = start

        fun `iterable`() {
            object {
                fun hasNext() = cur < end
                fun next() = [cur - 100, cur++]
            }
        }
    }

    var i = 0
    for (val [k,v] in obj) {
        assert k == i
        assert v == i + 100
        i++
    }
    assert i == 10
}


{
    var i = 0
    for (val [k,v] in object {
                            val start = 100
                            val end = 110
                            var cur = start

                            fun `iterable`() {
                                object {
                                    fun hasNext() = cur < end
                                    fun next() = [cur - 100, cur++]
                                }
                            }
                        }) {
        assert k == i
        assert v == i + 100
        i++
    }
    assert i == 10
}



{
    var flag = 1
    class A {
        // 不是ctor
        fun `construct`() = flag = 0
    }
    A()
    assert flag == 1
}

{
    class A {
        // 不算同名
        fun `f`() = null
        fun f() = null
    }
}


{
    // 方法不能匿名
    // class B { fun() {} }
    // 函数可以匿名
    assert fun() { 42 } () == 42
    // symbol 只能声明方法名, 或者扩展方法，不能声明函数名
    // fun `hello`() = null
    class A { fun `hello`() = null }
}


```
