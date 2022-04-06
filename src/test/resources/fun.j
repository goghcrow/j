/*
{
    // ⭐️ 最后一个参数是 fuc，可以省略括号，传入 block ⭐️

    fun f(f) { f() }
    fun f1(a, f) { f(a) }
    fun f2(a, b, f) { f(a,b) }

    assert f { } == null
    assert f { 42 } == 42
    assert f { -> 42 } == 42
    assert f1(42) { it -> it } == 42
    assert f2(1,2) { a,b -> [a, b] } == [1, 2]
}
*/

{
    // val f1 = () => { a -> a }
    val f2 = () => a => a

    // assert f1() is Fun
    assert f2()(42) == 42

    // assert (() => { a -> a }) () is Fun
    assert (() => a => a)()(42) == 42
}

/*
{
    val a = {->
        { ->
            { ->  1 }() + 1
        }() + 1
    }()
    assert a == 3

    assert {-> { -> { ->  1 }() + 1 }() + 1 }() == 3
}
*/

{
    val a = (
        () => (
            () => (
                () => 1
            )() + 1
        )() + 1
    )()
    assert a == 3
}


/*
{
    assert {a -> true ? 2 : 3}() == 2
    assert ({a -> true ? 2 : 3}()) == 2
}
*/

{
    assert (a => true ? 2 : 3)(null) == 2
}

// 🍺 默认参数
{
    fun add(a = 1, b = 2) = a + b
    val add1 = (a = 1, b = 2) => a + b
    object adder {
        fun add(a = 1, b = 2) = a + b
    }
    class Add(a = 1, b = 2)

    assert add() == 3
    assert add(2) == 4
    assert add(2, 3) == 5

    assert add1() == 3
    assert add1(2) == 4
    assert add1(2, 3) == 5

    assert adder.add() == 3
    assert adder.add(2) == 4
    assert adder.add(2, 3) == 5

    assert new Add().a == 1
    assert new Add().b == 2

    assert new Add(2).a == 2
    assert new Add().b == 2

    assert new Add(2, 3).a == 2
    assert new Add(2, 3).b == 3

    // 位置错误
    // fun add(a = 1,b) = a + b

    // 关于字面量
    // 注意字面量不包括 symbol
    // 字符串,整数,浮点,布尔,null, 所有元素都是字面量的 list, key+value 都是字面量的 map
    // 默认参数支持 列表和字段 字面量
    class Literal(val lst = [1,2,3], val map = [id:42])
    assert new Literal().lst == [1,2,3]
    assert new Literal().map == [id:42]
}

// lst map 字面量
{
    fun f(lst = [1,2,3], map = [id:[1,2,3]]) = null
    val defaults = f.defaults
    assert defaults[0] == [1,2,3]
    assert defaults[1] == [id:[1,2,3]]
}

// apply 默认参数
{
    fun test_apply_def_args(a, b = 41) = a + b
    assert test_apply_def_args.apply([1]) == 42
}

{
    // 单参数默认参数, 必须有括号
    val id = (a = 42) => a
    assert id(1) == 1
    assert id() == 42

    val add = (a, b = 1) => a + b
    assert add(1) == 2
}

// 参数个数检查
{
    fun f(a, b = 1) = a

    try {
        f()
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert f(1) == 1

    try {
        f.apply([])
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert f.apply([1]) == 1

}
// 参数个数检查
{
    class A {
        val a
        fun construct(a, b = 1) = this.a = a
        fun f(a, b = 1) = a
    }
    try {
        new A
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert new A(1).a == 1
    try {
        A(null).f()
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert A(null).f(1) == 1

    class B(a, b = 1) {
        fun f(a, b = 1) = a
    }
    try {
        new B
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert new B(1).a == 1
    try {
        B(null).f()
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert B(null).f(1) == 1

    class C(var a, b = 1) {
        fun f(a, b = 1) = a
    }
    try {
        new C
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert new C(1).a == 1
    try {
        C(null).f()
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert C(null).f(1) == 1

    class D(val a, b = 1) {
        fun f(a, b = 1) = a
    }
    try {
        new D
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert new D(1).a == 1
    try {
        D(null).f()
        assert false
    } catch (e) {
        assert e is CallError
    }
    assert D(null).f(1) == 1
}



// 🐈 decorator 默认参数
{
    fun log(f, args, dArgs = ['hello', 'world']) {
        f.apply(args)
        dArgs
    }
    fun log1(f, args, dArgs) {
        // f = log
        f.apply(args)
    }

    // log1 -> log
    @log1
    @log
    fun f1() = null
    assert f1() == ['hello', 'world']

    @log('hi')
    fun f2() = null
    assert f2() == ['hi', 'world']

    @log('hi', 'universe')
    fun f3() = null
    assert f3() == ['hi', 'universe']
}


// 🐈🐈🐈🐈🐈🐈 函数默认参数 + decorator 默认参数
{
    // decorator 默认第一个参数 f 和 第二个参数 args 的默认参数都没有用
    // 只有 第三个参数默认值有用
    fun log(f = null, args = [100, 200], dArgs = ['hello', 'world']) {
        dArgs.add(f.apply(args))
        dArgs
    }
    fun log1(f, args, dArgs) {
        // f = log
        f.apply(args)
    }

    // log1 -> log
    @log1
    @log
    fun f1(a, b = 1) = a + b
    assert f1(1) == ['hello', 'world', 2]
    assert f1(1, 41) == ['hello', 'world', 42]

    @log('hi')
    fun f2(a, b = 1) = a + b
    assert f2(1) == ['hi', 'world', 2]
    assert f2(1, 41) == ['hi', 'world', 42]

    @log('hi', 'universe')
    fun f3(a, b = 1) = a + b
    assert f3(1) == ['hi', 'universe', 2]
    assert f3(1, 41) == ['hi', 'universe', 42]
}




{
    fun f() = 1
    assert f() == 1

    val id = a => a
    assert id(42) == 42

    // 右结合
    val id_curry = a => a => a
    assert id_curry(1)(2) == 2

    // var block_id = { -> it }
    var block_id = it => it
    assert block_id(42) == 42
    // block_id = { a -> a }
    block_id = a => a
    assert block_id(42) == 42

    // assert (a => { -> it })(1)(2) == 2
    assert (a => it => it)(1)(2) == 2

    {
        fun add(a,b) = a + b
        assert add(1,2) == 3

        var arrow_add = (a, b) => a + b
        assert arrow_add(1,2) == 3

        arrow_add = (a, b) => { a + b }
        assert arrow_add(1,2) == 3

        // val block_add = {a,b -> a + b }
        // assert block_add(1,2) == 3
    }

    {
		class A {
			val c = 1
			val add_arrow1 = (a, b) => a + b + c
			val add_arrow2 = (a, b) => { a + b + c }
			// val add_block = {a, b -> a + b + c }
			fun add_method1(a, b) { a + b +c }
			fun add_method2(a, b) = a + b + c
			fun add_method3(a, b) = { a + b + c }
		}
		assert A().add_arrow1(1,2) == 4
		assert A().add_arrow2(1,2) == 4
		// assert A().add_block(1,2) == 4
		assert A().add_method1(1,2) == 4
		assert A().add_method2(1,2) == 4
		assert A().add_method3(1,2) == 4
    }
}

/*
{
    var log_str = ""
    val log = {str ->  log_str += str + ","}

    val f = { a, b, c ->
        val x = 10
        log("a=" + a)
        val g = { d ->
            val h = { ->
                log("d=" + d)
                log("x=" + x)
            }
            log("b=" + b)
            log("c=" + c)
            h()
        }
        g(4)
        return g
    }

    f(1, 2, 3)(5)
    assert log_str == "a=1,b=2,c=3,d=4,x=10,b=2,c=3,d=5,x=10,"
}
*/


{
    var log_str = ""
    val log = str => log_str += str + ","

    val f =  (a, b, c) => {
        val x = 10
        log("a=" + a)
        val g = d => {
            val h = () => {
                log("d=" + d)
                log("x=" + x)
            }
            log("b=" + b)
            log("c=" + c)
            h()
        }
        g(4)
        return g
    }
    f(1, 2, 3)(5)
    assert log_str == "a=1,b=2,c=3,d=4,x=10,b=2,c=3,d=5,x=10,"
}


/*
{
    val f = { ->
        var v = 1
        val set = { -> v = it }
        val get = { -> v }
        return [set: set, get: get]
    }
    val obj = f()
    obj.set(10)
    assert obj.get() == 10
}
*/

{
    val f = () => {
        var v = 1
        val set = it => v = it
        val get = () => v
        return [set: set, get: get]
    }
    val obj = f()
    obj['set'](10)
    assert obj['get']() == 10
}


/*
{
    val myfunc1 =  { ->
        val myfunc2 = { -> myfunc1(it - 1) }
        if (it == 0) {
            0
        } else {
            myfunc2(it)
        }
    }
    assert myfunc1(1) == 0
}
*/

{
    val myfunc1 = it => {
        val myfunc2 = it => myfunc1(it - 1)
        if (it == 0) {
            0
        } else {
            myfunc2(it)
        }
    }
    assert myfunc1(1) == 0
}



{
    /*
    val fib =  { ->
        if (it <= 0) {
            0
        } else if (it == 1) {
            1
        } else {
            fib(it - 1) + fib(it - 2)
        }
    }
    */
    val fib =  it =>
        if (it <= 0) {
            0
        } else if (it == 1) {
            1
        } else {
            fib(it - 1) + fib(it - 2)
        }
    assert fib(6) == 8
}

{
    fun fib(n) {
        if (n <= 2) {
            1
        } else {
            fib(n-1) + fib(n-2)
        }
    }
    assert [fib(1),fib(2),fib(3),fib(4),fib(5)] == [1,1,2,3,5]
}

{
    // val fib = {-> it <= 2 ? 1 : fib(it-1) + fib(it-2) }
    val fib = it => it <= 2 ? 1 : fib(it-1) + fib(it-2)
    assert [fib(1),fib(2),fib(3),fib(4),fib(5)] == [1,1,2,3,5]
}

{

    // stack overflow
    // fun f() { f() }
    // f()
}


// 💥 decorator 规则允许常量标记，具体参加 ParserJ.checkDecorator
// 💥 decorator(f/*:Fun*/, args/*:List*/, dArgs/*:List*/)
{
    fun decorator1(f, args, dArgs) {
        assert f is Fun
        assert args is List
        assert dArgs is List
        f.apply(args) + 1
    }

    object obj {
        fun decorator2() = decorator1.apply(arguments)
        val decorator3 = decorator1
        val prop = object {
            val decorator4 = decorator1
        }
    }
    val lst = [decorator1]
    val map = [
        decorator4: decorator1
    ]

    @decorator1
    @decorator1(42)
    @obj.decorator2
    @obj.decorator2(42)
    @obj.decorator3
    @obj.decorator3(42)
    @obj["decorator2"]
    @obj["decorator2"](42)
    @obj["decorator3"]
    @obj["decorator3"](42)
    @obj.prop.decorator4
    @obj.prop.decorator4(42)
    @obj["prop"]["decorator4"]
    @obj["prop"]["decorator4"](42)
    @lst[0]
    @lst[0](42)
    @map['decorator4']
    @map['decorator4'](42)
    fun f() = 0
    assert f() == 18
}

// 💥 decorator(f/*:Fun*/, args/*:List*/, dArgs/*:List*/)
{
    fun repeat(f, args, dArgs) = dArgs[0].times(() => f.apply(args))

    var i = 0

    @repeat(3)
    fun add1() = i += 1
    add1()
    assert i == 3

    i = 0
    @repeat(2)
    @repeat(2)
    fun add2() = i += 2
    add2()
    assert i == 8
}


// 💥 decorator(f/*:Fun*/, args/*:List*/, dArgs/*:List*/)
{
    var i = 0
    fun log(f, args, dArgs) {
        assert dArgs[0] == 'error'
        assert dArgs[1] == 3.14

        println("start invoke " + f)
        val r = f.apply(args)
        println("end invoke " + f)
        i++
        r
    }


    fun time(f, args, dArgs) {
        val s = sys.now()
        val r = f.apply(args)
        println('cost: ' + (sys.now() - s))
        i++
        r
    }

    @time
    @log('error', 3.14)
    fun biz(a, b) = a + b

    assert biz(1, 2) == 3
    assert i == 2


    class C {
        @time
        @log('error', 3.14)
        fun biz(a, b) = a + b
    }
    assert new C().biz(1, 2) == 3
    assert i == 4
}


// 💥 decorator(f/*:Fun*/, args/*:List*/, dArgs/*:List*/)
{
    object decorator {
        var i = 0
        fun test(f, args, dArgs) {
            dArgs.size() > 0 && dArgs[0] == 'x' ? i++ : (i += 2)
            f.apply(args)
        }
    }

    @decorator.test
    fun f1() = 42

    @decorator.test('x')
    fun f2() = 1

    assert f1() == 42
    assert decorator.i == 2
    f2() == 1
    assert decorator.i == 3
}


// 尾递归优化, 保证栈不会溢出
{
    val tco = require("tco.j")
    @tco
    fun f(a) {
        assert sys.backtrace().size() < 10
        if (a == 0) true else f(a - 1)
    }
    f(100)
}

// 尾递保证栈不会溢出
{
    val tco = require("tco.j")

    @tco
    fun f(a) = if (a == 0) true else f(a - 1)

    f(100)
}

{
    fun fib_iter(i) {
        var cur = 0, next = 1
        while(i-- > 0) {
            val tmp = cur
            cur = next
            next = tmp + next
        }
        cur
    }

    fun fib(i) = fib1(i, 0, 1)

    val tco = require("tco.j")

    @tco
    fun fib1(i, cur, next) = if (i == 0) cur else fib1(i - 1, next, cur + next)

    // 忽略溢出
    // assert fib(1000) == fib_iter(1000) // 执行太慢了...
    assert fib(100) == fib_iter(100)
}

{
    val tco = require("tco.j")

    @tco
    fun foo(a) = if (a == 0) false else bar(a - 1)
    fun bar(a) = if (a == 0) true else foo(a - 1)

    // assert foo(1000) == false  // 执行太慢了...
    assert foo(100) == false
}

{
    val tco = require("tco.j")
    @tco
    fun fib1(i, cur, next) = if (i == 0) cur else fib1(i - 1, next, cur + next)
    fib1(100, 0, 1)
}