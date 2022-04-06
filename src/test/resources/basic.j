// assert语法  assert assert-expr [: string-errMsg-expr]
try {
    assert false : 'hello'
} catch (e) {
    case AssertError~(msg,_,_) -> assert msg == 'hello'
    case _ -> assert false
}

try {
    val hello = 'hello'
    assert false : hello
} catch (e) {
    case AssertError~(msg,_,_) -> assert msg == 'hello'
    case _ -> assert false
}

try {
    class MyInt extends Int
    assert false
} catch(e) {
    // sealed 类 Int 不能被继承
    assert e is RuntimeError
}

{
    val a = 42
    try {
        // 🚀 Bool 扩展了 orThrows 方法
        (a > 100).orThrows(Error('hello'))
        assert false
    } catch(e) {
        assert e is Error
        assert e.msg == 'hello'
    }

    try {
        (a > 100).orThrows(Error('hello'))
        assert false
    } catch(e) {
        case RuntimeError~(msg, _) -> assert false
        case Error~(msg, _) -> assert msg == 'hello'
    }
}



val init = 0

assert Symbol(1 + '') + '' == '`1`'
assert Int(1) + '' == '1'
assert Float(1) + '' == '1.0'
assert Bool(true) + '' == 'true'
assert String('') == ''

// println(List(1,2,3) + '' == '')
// println(Map() + '' == '')
// println(Range(1,9) + '')


fun f(a,b,c) = 42
assert f.apply([1,2,3]) == 42


{
    class User(val id)
    val user = User(42)
    user.name = "xiaofeng"
    val a = user match {
        case [id: 42, name: 'xiaofeng'] -> 1
    }
    assert a == 1
}


for(var i=(1+1);i<10;i++) {}

null
true
false
1
3.14
// debugger

~1

assert 1.1 <= 1.2
assert 1.1 <= 2
assert 2 <= 3.1
assert 1.1 < 1.2
assert 1.1 < 2
assert 2 < 3.1
assert 1.1 + 1.1 == 2.2
assert 1.1 + 1 == 2.1
assert 1 + 1.1 == 2.1
assert '' + true == 'true'
assert '' + false == 'false'
assert '' + 1 == '1'
assert '' + 1.1 == '1.1'
assert [1] + [2] == [1,2]
assert 1.1 - 1.1 == 0
val precise = 0.001
assert 1.1 - 1 - 0.1 <= precise
assert 1 - 1.1 + 0.1 <= precise
assert 1.1 * 1.1 - 1.21 <= precise
assert 1.1 * 1 - 1.1 <= precise
assert 1 * 1.1 - 1.1 <= precise


assert (false && false) == false
assert (true && false) == false
assert (true && true) == true
assert (false || false) == false
assert (false || true) == true
assert  (true || false) == true

{
    val a = [1,2,3]
    a[0] = 42
    a[1] = 42
    a[2] = 42
    assert a[0] == 42 && a[1] == 42 && a[2] == 42
}


{
    // todo
    // val a = [name: 'xiaofeng', age: 42]
    // a['name'] = 'chu'
    // a['age'] = 18
    // assert a['name'] == 'chu' && a['age'] == 18 && a.name == 'chu' && a.age == 18

    // todo
    // a = 1
    // a['name'] = 'chu'
    // a['age'] = 18
    // assert a['name'] == 'chu' && a['age'] == 18 && a.name == 'chu' && a.age == 18
}



{
    val a = 42
    val r = if (a == 1) {
        1
    } else if (a == 2) {
        2
    } else {
        42
    }
    assert a == r
}


// val obj = [set: { -> it}]
val obj = [set: a => a]
assert obj['set'](42) == 42



// 块作用域
{
    val a = 1
    {
        val a = 2
        assert a == 2
    }
    assert a == 1
}


// 声明
{
    val x = 1, y = 2
    assert x + y == 3

    var a,b,c = 42
    assert a == null
    assert b == null
    assert c == 42
}

assert Float.NaN != Float.NaN