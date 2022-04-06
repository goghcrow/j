fun hash_equals_test(a, b) {
    val map = [:]
    val ref_eq = sys.objectId(a) == sys.objectId(b)
    if (ref_eq || a == b) {
        assert a == b
        assert a.`hash`() == b.`hash`()
        map[a] = 1
        assert map[b] == 1
    } else {
        map[a] == 1
        assert map[b] == null
    }
}


fun value_literal_object_eq(a, b) {
    assert sys.objectId(a) == sys.objectId(b)
    hash_equals_test(a, b)
}
{
    // Null Bool Int Float String Symbol 都可以看成值类型
    // == 相当于 eqv? 值相等
    assert null == null
    assert true == true
    assert false == false
    assert true != false
    assert 42 == 42
    assert 3.14 == 3.14
    assert `a` == `a`
    assert 'a' == 'a'
    assert 'str' == 'str'

    value_literal_object_eq(null, null)
    value_literal_object_eq(true, true)
    value_literal_object_eq(42, 42)
    value_literal_object_eq(3.14, 3.14)
    value_literal_object_eq('str', 'str')
    value_literal_object_eq(`sym`, `sym`)

    assert true.class == false.class
    assert 42.class == 1.class
    assert 3.14.class == 0.0.class
    assert 'a'.class == 'b'.class
    assert `sym1`.class == `sym2`.class

    // class 这里都是 singleton
    value_literal_object_eq(true.class, false.class)
    value_literal_object_eq(42.class, 1.class)
    value_literal_object_eq(3.14.class, 0.0.class)
    value_literal_object_eq('a'.class, 'b'.class)
    value_literal_object_eq(`sym1`.class, `sym2`.class)

    assert 1 != '1'
    assert 3.14 != '3.14'
    assert -0 == +0
    assert -0.0 == +0.0

    assert false != null
    assert false != 0
    assert false != 0.0
    assert false != ''
    assert true != 1
    assert "" != 0
    assert "123" != 123
}

// Iterator ref_eq
{
    val iter1 = Iterator(object { fun hasNext() = true fun next() = 1 })
    val iter2 = Iterator(object { fun hasNext() = true fun next() = 1 })
    assert iter1 != iter2
    hash_equals_test(iter1, iter2)
}

// Range 重写了 `hash` `==` , val_eq
{
    assert Range(1, 42) == Range(1, 42)
    assert 1..42 == 1..42
    hash_equals_test(1..42, 1..42)
}


// list, val_eq
{
    val a = [1, 2]
    val b = [1, 2]
    assert sys.objectId(a) != sys.objectId(b)
    assert a == b
    hash_equals_test(a, b)
}
// map, val_eq
{
    val a = [1: 2]
    val b = [1: 2]
    assert sys.objectId(a) != sys.objectId(b)
    assert a == b
    hash_equals_test(a, b)
}
// 顺序无关
{
    val a = [1: 2, 2: 1]
    val b = [2: 1, 1: 2]
    assert sys.objectId(a) != sys.objectId(b)
    assert a == b
    hash_equals_test(a, b)
}

{
    fun a() = null
    fun b() = null
    assert sys.objectId(a) != sys.objectId(b)
    assert a != b
}

// 用户自定义对象的 `==` 默认是 object_id 相等
{
    assert global is Scope

    try {
        assert sys.objectId(global) == 0
    } catch (e) {
        case _ if (e is AssertError) -> null // 等价 case AssertError~(_, _, _) -> null
        case _ -> assert false
    }
    assert sys.objectId(global) == 1

    var i = sys.objectId(object {})
    assert sys.objectId(object {}) == ++i
    assert sys.objectId(object {}) == ++i
}

{
    class A(a)
    class B(a, b) extends A(a)

    val a = A(1)
    val b = B(1, 2)

    assert b.getSuper() != a
    assert b.getSuper().a == 1
    // !!! 这俩对象 id 相等，但是 scope 不同
    assert sys.objectId(b.getSuper()) == sys.objectId(b)
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
    // 注意：这里相等的原因是 B 没有 `==` 方法, 所以使用了 A 的方法判断
    assert B(1, '1') == B(1, '2')
}


// hash ==
{
    val map = [:]
    class A(a)
    val a1 = A(1)
    val a2 = A(1)
    assert a1 != a2
    assert sys.objectId(a1) != sys.objectId(a2)
    assert a1.`hash`() != a2.`hash`()
    map[a1] = 1
    assert map[a2] == null
}
{
    val map = [:]
    class A(a) {
        fun `hash`() = a.`hash`()
        fun `==`(that) = that is A && this.a == that.a
    }
    val a1 = A(1)
    val a2 = A(1)
    assert a1 == a2
    assert sys.objectId(a1) != sys.objectId(a2)
    hash_equals_test(a1, a2)
}