// 这特么好像有歧义...
{
    class B(id = 42)
    // 空出一行否则 block 会变成 class 的body
    {
        class A extends B
        class B(id = 100)
        // 42 !!!
        assert new A().id == 42
    }
}


{
    val v = 1
    class A {
        val a
        fun construct(a) = this.a = a
        fun getValue() = v
    }
    {
        val v = 42
        val a = 200
        fun getValue1 extends A() = v
        fun getA1 extends A() = a
        fun getA2 extends A() = this.a
    }
    val a = new A(100)
    assert a.getValue() == 1
    // assert a.getValue1() == 42
    println(a.getA1())
    println(a.getA2())
}

assert [1,2] + [3,4] == [1, 2, 3, 4]
// todo println([1, 2] + 1)



val [code, out, err] = sys.exec("curl www.baidu.com")
println(code)
println(out)



val io = require("io.j")
println(io.write("/tmp/test.txt", "hello\n", ["CREATE", "APPEND"]))
println(io.read("/tmp/test.txt"))
println(io.delete("/tmp/test.txt"))
io.mkdir("/tmp/a/b/c/d")


fun thunk(f /*, args*/) {
    val args = arguments.cdr()
    () => f.apply(args)
}
fun trampoline(f /*, args */) {
    var r = f.apply(arguments.cdr())
    while (r is Fun) { r = r() }
    r
}
fun evenSteven(n) {
  if (n == 0) {
    true
  } else {
    thunk(oddJohn, n - 1)
  }
}
fun oddJohn(n) {
  if (n == 0) {
    false
  } else {
    thunk(evenSteven, n - 1)
  }
}

// println(trampoline(oddJohn, 2000))




/*
{

class A(id) {
    fun self() = 'a'
}
class B(id, name) extends A(id) {
    fun self() = 'b'
    fun test() = this
}

B.`this_id` = fun() = this.id
assert B(42, 'xiao').`this_id`() == 42

B.`this_name` = fun() = this.name
assert B(42, 'xiao').`this_name`() == 'xiao'

// B.`super_name` = fun() = super_name.name
// assert B(42, 'xiao').`super_name`() == 'xiao'


B.`this_self` = fun() = this.self()
assert B(42, 'xiao').`this_self`() == 'b'

B.`super_self` = fun() = super.self()
assert B(42, 'xiao').`super_self`() == 'a'

println(B(42, 'xiao').test())

}
*/