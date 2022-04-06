{
    fun f() {
        while(true) {
            if (true) {
                return 42
            }
        }
    }

    assert f() == 42
}


{
    var x = 0
    val y = while(true) {
        while(true) {
            break
        }
        x = 1
        break
    }

    assert x == 1
    assert y == null
}

{
    val y = while(true) {
        val b = while(true) {
            1
            break
        }
        assert b == null
        break
    }
    assert y == null
}

{
    /*
    val x = { ->
        var a = 1
        while(true) {
            a = a + 1
            if (a == 3) {
                return a
            }
        }
    } ()
    */
    val x = () => {
        var a = 1
        while(true) {
            a = a + 1
            if (a == 3) {
                return a
            }
        }
    } ()
    assert x == 3
}


{
    var a = 1
    val r = while(a < 3) {
        a = a + 1 // 赋值语句返回 null
        a
    }
    assert a == 3
    // while 返回 迭代 block 最后一次执行的结果
    // block 返回最后一条表达式的结果
    assert r == 3
}

{
    var a = 1
    val r = while(true) {
        a = a + 1
        if (a >= 3) {
            break
        }
    }
    assert a == 3
    assert r == null
}

{
    var a = 1
    while(true) {
        a = a + 1
        if (a == 3) {
            continue
        }
        if (a >= 3) {
            break
        }
    }
    assert a == 4
}
