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
