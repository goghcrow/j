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

