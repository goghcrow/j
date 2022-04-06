// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

// fun `apply` extends Object() = copy(this) // 把所有对象调用操作符 () 默认为原型继承, 不大合理

// 所有对象扩展 each 方法, 如果有迭代器则迭代
fun each extends Object(f) = {
    val iter = this.`iterable`
    if (iter is Fun) iter().each(f) else null
}

fun getSuper extends Object() = super
// todo 等完成 ?. 之后把这个方法干掉
fun getSuperClass extends Object() = super == null ? null : super.class

fun `in` extends Object(v) {
    val isCase = v.`isCase`
    if (isCase is Fun) isCase(this) else throw new OperatorError(v + " 未实现 `isCase`")
}

// todo
// fun `is` extends Object()

fun `inspect` extends Object() {
    val toString = this.`toString`
    if (toString is Fun) toString() else throw new OperatorError(this + " 未实现 `toString`或`inspect`")
}

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

// class A  Class 直接调用行为等同于 new Class代表对象的实例
// 通过 Class上的扩展方法实现，而不是解释器支持
// 原来解释器实现 call
//      Value callee = interp(call.callee, s); Value[] args = interpArgs(call.args, s);
//      return callee.asApply(call.loc).apply(call.loc, s, args);
//      所以 ClassValue 配合实现 Applicative 接口, apply 内部调用 newInstance
//      1. interface ClassValue 实现 Applicative 实现
//      2. @Override default Applicative asApply(Location loc) { return this;}
//      3. @Override default Value apply(Location loc, @NotNull Scope s, Value ...args) { return newInstance(loc, args); }
// todo: __.locationNone 把报错信息吞了...
fun `apply` extends Class() = __.Unbox(__.newInstance(__.Box(this), __.locationNone, __.list2javaArr(arguments, __.j_Value)))

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

fun orThrows extends Bool(throws) = if (!this) throw throws

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

fun length extends String() = __.toVInt(__.stringLength(Java.raw(this)))
fun size extends String() = this.length()
// todo 处理 index out exception
fun subString extends String(beginIdx, endIdx = null) = {
    assert beginIdx is Int
    assert endIdx == null || endIdx is Int

    val sz = this.length()
    if (endIdx == null || endIdx > sz) endIdx = sz
    if (beginIdx < 0) beginIdx = 0

    endIdx < beginIdx ? '' : __.toVStr(__.substring(Java.raw(this), __.toJInt(beginIdx), __.toJInt(endIdx)))
}
fun subStr extends String(beginIdx, endIdx = null) = this.subString(beginIdx, endIdx)
// 转换字符串第一个字节为 0-255 之间的值
fun ord extends String() = {
    val ch = this[0]
    assert ch != null
    __.toVIntChar(__.stringCharAt(__.toJStr(ch), __.toJInt(0)))
}

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

// 返回指定的字符
fun chr extends Int() = __.toVStr(__.stringValueOfChar(__.javaNull, __.intCharValue(__.Box(this))))

fun times extends Int(f) {
    assert f is Fun
    for(var i = 0; i < this; i++) {
        f(i)
    }
}

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

Float.NaN = 0.0 / 0.0
Float.POSITIVE_INFINITY = 1.0 / 0.0
Float.NEGATIVE_INFINITY = -1.0 / 0.0

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

fun add extends List(elem) = {
    __.listAdd(Java.raw(this), Java.box(elem))
    this
}
fun `<<` extends List(elem) = this.add(elem)
fun `+` extends List(lst) = {
    if (lst is List) {
        val r = []
        this.each(it => r << it)
        lst.each(it => r << it)
        r
    } else {
        // todo .... super.`+`(this, lst)
        assert false
    }
}

fun contains extends List(elem) = __.toVBool(__.listContains(Java.raw(this), Java.box(elem)))
fun `isCase` extends List(elem) = this.contains(elem)
fun has extends List(elem) = this.contains(elem)

fun subList extends List(fromIdx, toIdx) = {
    assert fromIdx is Int
    assert toIdx is Int
    __.toVList(__.listSubList(Java.raw(this), __.toJInt(fromIdx), __.toJInt(toIdx)))
}
fun size extends List() =  __.toVInt(__.listSize(Java.raw(this)))
fun car extends List() = this[0]
fun cdr extends List() = this.subList(1, this.size())
fun each extends List(f) = {
    assert f is Fun
    for (val it in this) {
        f(it)
    }
}
fun reduce extends List(cb/*(acc, cur, idx, lst)*/, init) {
    assert cb is Fun
    var acc = init, i = 0, sz = this.size()
    while(i++ < sz) {
        acc = cb(acc, this[i-1], i-1, this)
    }
    acc
}
fun map extends List(mapper) = {
    assert mapper is Fun
    val lst = []
    this.each( it => lst.add(mapper(it)) )
    lst
}
fun filter extends List(test) = {
    assert test is Fun
    val lst = []
    this.each( it => if(test(it)) { lst.add(it) } )
    lst
}

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

fun each extends Iterator(f) = {
    assert f is Fun
    for (val it in this) {
        f(it)
    }
}


// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

fun size extends Map() = __.toVInt(__.mapSize(Java.raw(this)))
fun each extends Map(f) = {
    assert f is Fun
    for (val [k,v] in this) {
        f(k, v)
    }
}
fun contains extends Map(v) = __.toVBool(__.mapContainsKey(Java.raw(this), Java.box(v)))
fun `isCase` extends Map(v) = this.contains(v)
fun has extends Map(v) = this.contains(v)