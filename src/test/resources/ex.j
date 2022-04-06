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