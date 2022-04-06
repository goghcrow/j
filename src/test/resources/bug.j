// stackoverflow
/*
{
    class A
    val a1 = A()
    val a2 = A()
    a1.x = a2
    a2.x = a1
    assert a1 == a2
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


// class B(a, b = 1) 生成的requiredParametersSize 不对
{
    class A { fun construct(a, b = 1) = null }
    assert A.construct.requiredParametersSize == 1

    class B(a, b = 1)
    assert B.construct.requiredParametersSize == 1

    class C(var a, b = 1)
    assert C.construct.requiredParametersSize == 1

    class D(val a, b = 1)
    assert D.construct.requiredParametersSize == 1
}

{
    assert class A {} is Class
    assert class B is Class
}

// literal 不能 存 value, 特别是可变的 value, !!! 解释过程要生成新的 value
// 否则就会出现下面的情况, a = [] 复用了 ast literal 里头的 value 了
{
    fun a() {
        val a = []
        a.add(1)
        a
    }
    assert a() == [1]
    assert a() == [1] // bug !!![1,1]
}

{
    // todo: match 歧义，无法分清是 null 还是不存在
    val test = it => it match {
        case [id: null] -> 42
        case _ -> null
    }
    class A(id)
    assert test([id: null]) == 42
    assert test([:]) == 42
    assert test(A(null)) == 42
    assert test(object {}) == 42

    assert test(1) == null
    assert test(3.14) == null
    assert test(true) == null
    assert test(`a`) == null
    assert test([]) == null
    assert test(() => null) == null
}