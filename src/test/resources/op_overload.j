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

// todo 穷举其他 操作符