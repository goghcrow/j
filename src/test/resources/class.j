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