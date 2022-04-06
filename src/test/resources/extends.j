assert Java.box(1) is JavaValue
assert Java.unbox(Java.box(1)) is Int

assert Java.raw([]) is JavaValue // Java.raw([]) == new JavaValue(new java.util.List())
assert Java.raw([:]) is JavaValue // Java.raw([:]) == new JavaValue(new java.util.LinkedHashMap())
assert Java.raw("") is JavaValue // Java.raw("") == new JavaValue(new java.lang.String(""))
assert Java.raw(1) is JavaValue // Java.raw(1) == new JavaValue(1 long)
assert Java.raw(3.14) is JavaValue // Java.raw(3.14) == new JavaValue(3.14 double)
assert Java.raw(true) is JavaValue // Java.raw(true) == new JavaValue(true boolean)
assert Java.raw(null) is JavaValue // Java.raw(null) == new JavaValue(null java)

assert Java.type("int") is JavaValue
assert Java.type("java.lang.String") is JavaValue
assert Java.field("java.lang.System", "in") is Fun
assert Java.field("java.lang.System", "in")(Java.box(null)) is JavaValue
assert Java.new("java.lang.String") is JavaValue
assert Java.method(Java.type("java.lang.Math"), "toIntExact", "long") is Fun


{
    assert 'a'.ord() == 97
    assert 97.chr() == 'a'
}

{
    var sum = 0
    10.times( i => sum += i )
    if (sum != 45) { throw new Error('boot fail', null) }
}

{
    val lst = []
    lst.add(1)
    assert lst == [1]
    lst.add(2)
    assert lst == [1, 2]
    lst.add(3)
    assert lst == [1, 2, 3]
}

{
    var sum = 0
    // Iterator 扩展了 each 方法
    Iterator(object {
        var i = 0
        fun hasNext() = i < 10
        fun next() = i++
    }).each(it => sum += it)
    assert sum == 45
}

{
/*
class Range(val from, val to) {
    fun `iterable`() {
        var cur = from
        object {
            fun hasNext() = cur <= to
            fun next() = hasNext() ? cur++ : throw new IterError
        }
    }
}
*/
    var sum = 0
    (0..9).each(it => sum += it)
    assert sum == 45
}

{
    val lst = []
    lst << 1
    lst << 2
    assert lst == [1, 2]
}





// todo
// list each map filter reduce
// string substring ...



/*
val Frame = Java.type("java.awt.Frame")
val showFrame = Java.method(Frame, "show")
showFrame(Java.new(Frame))
*/

// val showFrame = Java.method("java.awt.Frame", "show")
// showFrame(Java.new("java.awt.Frame"))