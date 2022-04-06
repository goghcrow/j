{
    assert 1 + 2 === 3
    assert 1 - 2 === -1
    assert -1 === -1
    assert +2 === 2
    assert 2 * 3 === 6
    assert 4 / 2 === 2
    assert 4 % 3 === 1
    assert 4 << 2 === 16
    assert 1 << 0 === 1
    assert (1 << 63) < 0 === true
    assert 1 << 63 === -0x7fffffffffffffff - 1
    assert 1 << 64 === 1
    assert -4 >> 1 === -2
    assert -4 >>> 1 === 0x7fffffffffffffff - 1
    assert 1 & 1 === 1
    assert 0 | 1 === 1
    assert 1 ^ 1 === 0
    assert ~1 === -2
    // assert !1 === false
    assert 1 < 2 === true
    assert 2 > 1 === true
    assert 2 ** 8 === 256.0
}

{
    // TODO 强类型还是弱类型
    // 布尔 and or 操作数不返回布尔值
    // assert (0 || 'hello') === 'hello'
    // assert (42 || 'hello') === 42

    // assert (0 && 'hello') === 0
    // assert (42 && 'hello') === 'hello'

    // val f = (a) => { a = a || 42 }
    // assert f(0) == 42 // 可以用作默认值
}

{
    // ====== Updater ======

    // 不能写成1--1， -(-1) 会和 -- 冲突
    assert 1 - -1 == 2

    var i = 0
    assert ++i == 1
    assert i == 1
    assert --i == 0
    assert i == 0
    assert i++ == 0
    assert i == 1
    assert i-- == 1
    assert i == 0
}


{
    // POWER 右结合性
    val foo = 3
    assert foo * 2 == 6
    assert foo* 2 == 6
    assert foo** 2 == 9

    assert (2 ** 3 ** 2) == (2 ** (3 ** 2))

    // 三元运算
    assert (true ? 1 : 2) == 1
    assert (false ? 1 : 2) == 2
}

{
    assert +1 == 1
    assert +1.1 == 1.1

    assert -1 == -1
    assert -1.1 == -1.1

    var x = 1
    x = +x
    assert x == 1

    x = -x
    assert x == -1

    x = 1
    x = -(-x)
    assert x == 1

    assert ++x == 2
    assert x == 2

    assert x++ == 2
    assert x == 3

    assert --x == 2
    assert x == 2

    assert x-- == 2
    assert x == 1
}