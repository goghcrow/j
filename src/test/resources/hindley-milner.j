// hindley-milner
// https://zhuanlan.zhihu.com/p/24181997

// 单态类型的合一

// A monomorphic type
class Monomorphic {
    // Apply a substitution m
    fun applySub(m) = null
    // 在前一节的 unify 中，我们还需要保证：当变量 a 合一到类型 b 的时候，b 中不能包含 a。我们用一个 freeFrom 方法实现：
    fun getFreeSlots(m, a) = null // Put free type variables, from mapping m, to set a
    fun freeFrom(s) = null // type t is free freom slot s
}

// 自由变量 Slots for free variables
sealed class Slot(name) extends Monomorphic {
    fun applySub(m) {
    		val r = m[this]
    		// 不存在或者指向自己...
    		if (r == null || r == this) {
    		    this
    		} else {
        		r.applySub(m)
    		}
    }
    fun getFreeSlots(m, a) = if (!m.has(this) && !a.has(this)) a << this
	fun freeFrom(s) = return this != s
    fun `toString`() = "#" + name
    fun `hash`() = sys.hash(name)
    fun `==`(t) = t is Slot && name == t.name
}

// 基础类型 Primitive types
sealed class Primitive(name, kind) extends Monomorphic {
	fun applySub(m) = this
    fun freeFrom(s) = true
    fun `toString`() = name
    fun `hash`() = sys.hash(name)
	fun `==`(t) = t is Primitive && name == t.name
}

// 复合类型 Composite types, like [(->) a b] or [List a]
// 由一个构造器（ctor）和一个参数（argument）复合得到。对于 (->)、(*) 之类的二元构造器，使用 Curryize 将他变换为嵌套的 Composite。
sealed class Composite(ctor, arg) extends Monomorphic {
	fun applySub(m) = Composite(ctor.applySub(m), arg.applySub(m))
    fun getFreeSlots(m, a) {
		ctor.getFreeSlots(m, a)
		arg.getFreeSlots(m, a)
	}
	fun freeFrom(s) = ctor.freeFrom(s) && arg.freeFrom(s)
    fun `toString`() = ctor + (arg is Composite ? " (" + arg + ")" : " " + arg + "")
	fun `hash`() = sys.hash(ctor, arg)
	fun `==`(t) = t is Composite && ctor == t.ctor && arg == t.arg
}

/*
所谓的合一，指的是：给出两个类型 A 和 B，找到一组变量替换，使得两者的自由变量经过替换之后可以得到一个相同的类型 C。考虑

A = ((α -> β) X [γ]) -> [β]
B = ((γ - δ) X [γ]) -> ε
这两个类型可以合一，对应的替换是
<α -> α, β -> β, γ -> α, δ -> β, ε -> [β]>

实现合一的算法基本思路就是维护一个 slot 的映射。对于任意的类型 a 和 b，以及「当前状态」的映射 m：

1. 如果 a 和 b 都是 slot 并且 m[a] == m[b]，那么 a b 可以合一，m 不变。
2. 如果 a 和 b 都是 primitive 并且相同，那么 a b 可以合一，m 不变。
3. 如果 a 是 slot，可以合一，并且需要 m[a] 设置为 b；反之亦然。
4. 如果 a 和 b 都是 composite，检查两者的构造器和参数是否都能合一，m 会最多被设置两次。
5. 对于其他一切情况，a 和 b 不能合一。
*/

/*
https://www.zhihu.com/question/22256149

Unification 的概念见的最多的是在Type system中。
比较白话的说法就是，对于一个没有type notation 的代码，如果想要知道各个term 的type是通过type inference的，这里就用到了unification。
在进行type inference的时候，我们会对各个term产生constraint，通过不断产生这样的constraints，最终unify出我们想要的值。

比如我们知道map :: (a -> b) -> [a] -> [b]，现在不知道一个数组A的类型，定其为x，
就有constraint1 : typ(A) = x，又已知函数f :: (int -> bool) 那么又有constraint2 : typ(f) = int -> bool，
根据map f A则知道a -> b = int -> bool ,A = [a] ，则可以unify出A = [int]，
最后通过constraints得到我们想要的结果，就是unification。



简单的说unification 就是一个用来解constraint problem 的算法。这个算法的输入是一个constraint set, 输出是一组substitution 。

有些问题咋看来无法直接求解，但有一些数据之间必需满足的关系（constraints ）事先给出了，合理的情况都要满足这种关系（比如著名的图着色问题）。

以类型推导为例，我们看一个ML 程序时有一个函数f 接受参数x，但程序没有写出函数的参数和返回值类型，所以我们先假设参数类型是a，返回类型是b。
这样我们就带着这个假设去看函数定义。
比如这个函数的函数体只有一个表达式x + 1 而且我们知道（事先给出）只有int 类型的值可以做“+”运算而且结果还是int，
假设这个表达式的类型是c，那我们就有了constraint set {a = int, c = int}，
我们还知道函数体的类型就是函数的返回类型，所以constraint set 更新为{a = int, c = int, b = c},
经过unify之后得出的substitution 是{a -> int, b -> int}。

这个例子太简单了，所以unification 其实没有做多少计算（固定电话打字，请谅解…）。
你可以看看Artificial Intelligence A Modern Approach 这本书上有很多constraint programming 的例子。
引申一点，这种基础的unification 是有限制的，它是based on something you know，即a和b这些临时变量最终会被约束到一个基础值。
与之对应的是semi-unification，它可以将变量递归地约束到变量本身，所以完全的类型推导在System F 上是undecidable 的。
*/

// Unify two monomorphic types, p and q with slot mapping m.
fun unify1(m, s, t) {
	if (s is Slot && t is Slot && s.applySub(m) == t.applySub(m)) {
		true
	} else if (s is Primitive && t is Primitive && s == t) {
		true
	} else if (s is Composite && t is Composite) {
		unify1(m, s.ctor, t.ctor) && unify1(m, s.arg, t.arg)
	} else if (s is Slot) {
		val t1 = t.applySub(m)
        if (t1.freeFrom(s)) {
            m[s] = t1
            true
        } else false
	} else if (t is Slot) {
	    val s1 = s.applySub(m)
	    if (s1.freeFrom(t)) {
    		m[t] = s
	    	true
	    } else false
	} else {
		false
	}
}
fun unify(m, s, t) {
    if (unify1(m, s, t)) {
        for (val [k,v] in m) {
            m[k] = v.applySub(m)
        }
        true
    } else {
        false
    }
}


// Slot symbol table
val st = [:]
fun slot(name) {
	if (st[name] == null) {
	    st[name] = Slot(name)
	}
    st[name]
}

// Primitive symbol table
val pt = [:]
fun pm(name, kind = null) {
    if (pt[name] == null) {
        pt[name] = Primitive(name, kind)
    }
    pt[name]
}

// Composite types
fun ct(ctor, arg) = Composite(ctor, arg)
fun arrow(p, q) = ct(ct(pm("[->]"), p), q)
fun product(p, q) = ct(ct(pm("[*]"), p), q)


// (-> (* (-> a1 a2) (List a3)) (List a2))
val type1 = arrow(
	product(
		arrow(slot("a1"), slot("a2")),
		ct(pm("list"), slot("a3"))
	),
	ct(pm("list"), slot("a2"))
)

// (-> (* (-> a3 a4) (List a3)) a5)
val type2 = arrow(
	product(
		arrow(slot("a3"), slot("a4")),
		ct(pm("list"), slot("a3"))
	),
	slot("a5")
)

fun inspect_map(m) {
    var buf = '['
    var flag = true
    for (val [k,v] in m) {
        if (flag) flag = false else buf += ', '
        buf += k + ' : ' + v
    }
    if (flag) buf += ':'
    buf += ']'
    println(buf)
}

val m = [:]
println("Type 1     -> ", type1)
println("Type 2     -> ", type2)

assert type1 + '' == '[->] ([*] ([->] #a1 #a2) (list #a3)) (list #a2)'
assert type2 + '' == '[->] ([*] ([->] #a3 #a4) (list #a3)) #a5'

assert unify(m, type1, type2)
inspect_map(m) // [#a1 : #a3, #a2 : #a4, #a5 : list #a4]

assert m[slot('a1')] == slot('a3')
assert m[slot('a2')] == slot('a4')
assert m[slot('a5')] == ct(pm('list'), slot('a4'))

println('===================================')
// [#a1 : #a3, #a2 : #a4, #a5 : list #a2]

// [->] ([*] ([->] #a1 #a2) (list #a3)) (list #a2)
println("applySub 1 -> ", type1.applySub(m))
// [->] ([*] ([->] #a3 #a4) (list #a3)) #a5
println("applySub 2 -> ", type2.applySub(m))

assert type1.applySub(m) + '' == '[->] ([*] ([->] #a3 #a4) (list #a3)) (list #a4)'
assert type2.applySub(m) + '' == '[->] ([*] ([->] #a3 #a4) (list #a3)) (list #a4)'

// ====================================================================================

// todo
// 多态类型的合一
// https://zhuanlan.zhihu.com/p/24195357

fun convertToNumberingScheme(number) {
	val baseChar = "a".ord()
	var letters = ""

    number -= 1
    letters = (baseChar + (number % 26)).chr() + letters
    number = (number / 26) >> 0
    while (number > 0) {
        number -= 1
        letters = (baseChar + (number % 26)).chr() + letters
        number = (number / 26) >> 0
    }
	letters
}

class Set {
    val map = [:]
    fun `<<`(elem) = map[elem] = true
    fun `iterable`() {
        val iter = map.`iterable`()
        object {
            fun hasNext() = iter.hasNext()
            fun next() = hasNext() ? iter.next()[0] : throw new IterError
        }
    }
}

{
    val set = Set()
    set << 1
    set << 1
    set << 2
    set << 2
    for(val v in set) {
        println(v)
    }
}


class Polymorphic {
    val quantifier
    val base
    fun construct(quantifier, base) {
		// Rename quantified slots
		var N = 1
		val s = Set()
		val m = [:]
		for (val key in quantifier) {
			val param = slot(convertToNumberingScheme(N))
			m[key] = param
			s << param
			N++
		}
		this.quantifier = s
		this.base = base.applySub(m)
    }
    // 将多态类型的量化部分进行替换，生成一个单态的类型
    fun instance(gen) {
		val m = [:]
		for (val key in this.quantifier) {
			m[key] = gen()
		}
		this.base.applySub(m)
	}
    fun `toString`() = {
        var buf = "forall"
        for (val item in this.quantifier) {
			buf += " " + item
		}
		buf + ". " + this.base
		buf
    }
    fun `hash`() = sys.hash(quantifier, base)
    fun `==`(t) = t is Polymorphic && quantifier == t.quantifier && base == t.base
}


sealed class Environment {
	val parent
	val variables
	val typeSlots
	fun construct(parent = null) {
		this.parent = parent
		this.variables = [:]
		this.typeSlots = parent = null ? [:] : parent.typeSlots
	}
	fun lookup(name) {
		if (variables.has(name)) variables[name]
		else if (parent == null) null
		else parent.lookup(name)
	}
}

val newType = {
	var N = 0
	(name = "") => Slot(name + (N++))
}

val newVar = {
	val N = 0
	(name = "t") => name + (N++)
}



class VariableNotFoundError(msg) extends Error(msg, null)
class Form
class Id(name) extends Form {
    fun inference(env) {
		val r = env.lookup(name)
		if (r == null) throw new VariableNotFoundError(name)
		if (r is Polymorphic) {
			r.instance(newType)
		} else {
			r
		}
	}
	fun `toString`() = name
}
class Apply extends Form
class FDef extends Form
class Assign extends Form