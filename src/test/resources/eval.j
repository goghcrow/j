{
    global.id = 42
    // eval 在 global scope 中执行
    eval('assert id == 42')
}