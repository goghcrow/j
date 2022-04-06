// 🚀 map 字面量格式 [expr: expr]， 其中有一个特殊的处理
// 因为 expr 大部分情况是字符串, 所以字符串默认不用加引号
// 如果 key 想表达变量，需要将 name/identifier 转换成 expr,   e.g. (id)
// 🚀 map 键值对只能用 [] 访问, .用来访问 map 作为对象的属性
// 这里有个问题 obj[expr] 可以用来动态访问对象属性, 但是没有办法动态访问 map作为对象的属性, 只能动态访问 map 的 key

{
    assert [str: 1] == ['str':1]
    val str = 'hello'
    assert [(str): 1] == ['hello': 1]
    assert [str + '': 1] == ['hello': 1]

    // pattern 一样
    {
        val [name: x] = [name: 'xiaofeng']
        assert x == 'xiaofeng'
    }
    {
        val key = 'name'
        val [(key): x] = [name: 'xiaofeng']
        assert x == 'xiaofeng'
    }
}

{
    class A
    assert [class: A].class == Map
    assert [class: A]['class'] == A
}

{
    [:]
    ['class': 1]
}
{
    assert [true ? 1 : 2] == [1]
    assert [(true ? 1 : 2): 2] == [1: 2]
}


{
    assert [(1+1): 1][2] == 1
}

{
    assert [1:1][1] == 1
    assert [(1):1][1] == 1
    assert ['1':1]['1'] == 1
    assert ['1':1][1] == null

    assert [1.1: 1][1.1] == 1
}

{
    assert ['foo': 1]['foo'] == 1
    assert ["foo": 1]['foo'] == 1
    assert [foo: 1]['foo'] == 1
}

{
    val k = 'foobar'
    assert [('foo'+'bar'): 1][k] == 1
    assert [('foo'+'bar'): 1]['foobar'] == 1
}

{
    val map = [
        (null): 1,
        (true): 1,
        (1): 1,
        (3.14): 1,
        ([1,2]): 1,
        ([foo:'bar']): 1,
    ]

    assert map.null == null
    assert map[null] == 1

    assert map['1'] == null
    assert map[1] == 1

    assert map['3.14'] == null
    assert map[3.14] == 1

    assert map[[1,2]] == 1

    assert map[[foo:'bar']] == 1
}

{
    // symbol 属性不能用来迭代或者遍历
    val m = [a:1, b:2, `sym`:3]
    assert m.size() == 2
    var i = 0
    for (val [k,_] in m) {
        assert k == 'a' || k == 'b'
    }
}