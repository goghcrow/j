// 🚀 for-in 由迭代器与解构赋值两部分构成  for (解构赋值 in 迭代器), 会被解语法糖成 for-i


// 🚀 for-i
{
    var c = 0
    for(var i = 0; i < 4; i++) {
        c++
    }
    assert c == 4

    c = 0
    var i = 0
    for(; i < 4; i++) {
        c++
    }
    assert c == 4

    c = 0
    for(i = 0; i < 4; i++) {
        c++
    }
    assert c == 4
    assert i == 4


    c = 0
    for(var i = 0; ; i++) {
        if (i < 4) {
            c++
        } else {
            break
        }
    }
    assert c == 4

    c = 0
    for(var i = 0; i < 4;) {
        c++
        i++
    }
    assert c == 4

    c = 0
    i = 0
    for(;;) {
        if (i < 4) {
            c++
        } else {
            break
        }
        i++
    }
    assert c == 4

    // for(;;) {} // 死循环
}

// 🚀 for-i
{
    for(var i = 0; i < 4; i++) {
        // 每次都新开启 scope
        val x = 1
    }
}

// 🚀 for-in-Iterator
{
    // 对象 -> Iterator
    val iter1 = object {
        fun hasNext() = true
        fun next() = 1
    }

    var i = 0
    for(val a in iter1) {
        assert a == 1
        if (i++ > 3) {
            break
        }
    }

    // Map -> Iterator
    val iter2 = [
        hasNext: () => true,
        next: () => 1
    ]

    i = 0
    for(val a in iter1) {
        assert a == 1
        if (i++ > 3) {
            break
        }
    }
}


// 🚀 for-in-Range
{
    var sum = 0
    for (val i in 1..3) {
        sum += i
    }
    assert sum == 6
}

// 🚀 for-in-List
{
    var sum = 0
    for (val v in [1,2,3]) {
        sum += v
    }
    assert sum == 6
}

// 🚀 for-in-Map, 实际是解构赋值
{
    val m = [id:1, name:'xiaofeng']
    for (val [k,v] in m) {
        if (k == 'id') {
            assert v == 1
            continue
        }
        if (k == 'name') {
            assert v == 'xiaofeng'
            continue
        }
        assert false
    }

    for (val entry in m) {
        val k = entry[0]
        val v = entry[1]
        if (k == 'id') {
            assert v == 1
            continue
        }
        if (k == 'name') {
            assert v == 'xiaofeng'
            continue
        }
        assert false
    }
}

// 🚀 for-in 解构赋值
{
    val m = [ [id:0, name:'a'], [id:1, name:'b'], [id:2, name:'c'] ]
    for (val [id:k, name:v] in m) {
        assert m[k]['name'] == v
    }
}