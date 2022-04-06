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