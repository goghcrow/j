val Prototype = require("proto.j")

{
    // 原型继承
    class A(val id) {
        fun m() = 1
    }
    val proto = new A(42)
    val p1 = Prototype.copy(proto)
    val p2 = Prototype.copy(proto)

    p1.name = 'x'
    p2.name = 'y'

    assert p1.m() == 1
    assert p2.m() == 1
    assert p1.name == 'x'
    assert p2.name == 'y'

    proto.m = () => 2
    assert p1.m() == 2
    assert p2.m() == 2

    p1.m = () => 3
    assert p1.m() == 3
    assert p2.m() == 2
}

{
    class A(val id)
    val a = A(42)
    assert Prototype.copy(a).id == 42
}
