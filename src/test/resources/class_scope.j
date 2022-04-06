{
    // ⭐️⭐️⭐️
    // scope 与 object 统一
    // 函数闭包 = 类闭包 = 作用域 = 键值对链

    // 函数声明的解释求值，即将函数代码+求值环境打包生成闭包对象
    // 对象声明的解释求值，即将类代码+求值环境打包生成闭包对象(与函数闭包相同结构)

    // 对象构造时涉及3个Scope
    // 1. 类构造时(ctor)参数求值的 Scope，即 new 或者 对象名()表达式 所在的词法作用域
    // 2. 类声明的词法 Scope，当类 A 有父类 B 时，会在 A 声明的作用域查找 B 的声明
    // 3. 类实例作用域，当类 A 与父类 B 时, A 实例作用域的父作用域会指向 B 实例的作用域
    // 当类没有父类时，类实例作用域的父作用域指向类声明时的词法作用域（同函数调用一致）

    // -> 表示 parent
    // B实例Scope -> A实例Scope -> B声明时Scope(B的词法作用域，类比与函数声明时的词法作用域)
    // ❗️❗️❗️注意这里 A-Scope 的 parent 指向 B 的词法作用域

    // 对象作用域即对象自身!!!

    // 函数调用时候，创造一个新作用域指向函数声明时的词法作用域，在函数调用发生的词法作用域对实参求值，
    // 将形参-实参定义在新作用域，对函数体求值

    // 语义基本与 java 一致，参见 ClassScopeTest


    {
        val v = 'a-up-val'
        class A {
            val v = 'a-prop'
        }

        {
            val v = 'b-up-val'
            class B extends A {
                fun get_v() {
                    v
                }
            }
            val b = new B()
            assert b.v == 'a-prop'
            assert b.get_v() == 'a-prop'
        }
    }

    // bug!  objectMember scope查找时候不应该查到环境中
    {
        val v = 1
        class A
        assert new A().v != v
        assert new A().v == null
    }

    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/

    {
        val v = 'a-up-val'
        class A {
            // fun v = 'a-prop'
        }

        {
            val v = 'b-up-val'
            class B extends A {
                fun get_v() {
                    v
                }
            }
            assert B().v == 'b-up-val'
            assert B().get_v() == 'b-up-val'
        }
    }
}


// ⭐️⭐️⭐️ 注意声明顺序无所谓，执行时 lookup
{
    class A {
        fun getValue() = v
    }
    val v = 42
    assert new A.getValue() == 42
}

// ⭐️⭐️⭐️ 自己臆想出来的作用域链
{
    // scope 遵循词法作用域

    // scope-1 (A定义所在环境, 类比闭包环境)
    val v = 1
    class A {
        // scope-2 (getValue 定义所在环境)
        fun getValue() = v // scope-3 (A.getValue 方法体 block 环境)
    }

    // scope-1
    val class_b = {
        // scope-4 (B定义所在环境, 类比闭包环境)
        val v = 42
        class B extends A {
            // scope-5
            fun getValue() = v // scope-6 (B.getValue 方法体 block 环境)
            fun getSuperValue() = super.getValue()
        }
    }

    val b = class_b() // 这里通过 ClassValue 的 `apply` 来 new 对象

    // b 的 scope 链条 5 -> 2 -> 4 -> 1
    //      [5(b) -> 2(a)] -> [4(B定义环境) -> 1]
    // super 即 a 的 scope 链条 2 -> 1

    assert b.getValue() == 42
    // B.getValue() v 的 lookup 过程 6 -> 5 -> 2 -> 4
    // B.getSuperValue() super lookupLocal 命中 a
    //      A.getValue() v 的 lookup 过程 2 -> 1

    assert b.getSuperValue() == 1
}

{
    class A {
        fun getValue() = v
    }
    val v = 1

    val class_b = {
        class B extends A {
            fun getValue() = v
            fun getSuperValue() = super.getValue()
        }
        val v = 42
        B
    }

    // 这里通过 ClassValue 的 apply 来 new 对象
    val b = class_b()
    assert b.getValue() == 42
    assert b.getSuperValue() == 1
    assert b.getSuper().getValue() == 1
}



{
    val x = 1
    class A {
        val a = x
    }

    {
        val x = 2
        val a = 3
        class B extends A {
           val b = x // 优先文法最近的 upValue 2
           val c = a // 优先父类属性 1
           fun ma() {
               a // 优先父类属性 1
           }
           fun mx() {
               x // 优先文法最近的 upValue 2
           }
        }
        assert B().a == 1
        assert B().b == 2
        assert B().c == 1
        assert B().ma() == 1
        assert B().mx() == 2
    }
}



// 扩展方法的 scope 比较特殊
//      需要      ext-method-scope -> object-scope -> ...parent-object-scope -> ext-method-def-scope
//      普通方法作用域 method-scope -> object-scope -> ...parent-object-scope -> class-def-scope
//      或者直接做成 ext-method-scope -> ext-method-def-scope, object-scope 内容使用 this super 来索引
{
    // 扩展内置类型作用域测试
    {
        val up_val = 100
        {
            val up_val = 42
            fun test_scope extends Int() = this + up_val
        }
        assert 1.test_scope() == 43
    }

    // 扩展自定义类型作用域测试
    {
        val up_val = 100
        class A(v)  // 注意与下面 block 之间多一个空行，否则下方 block 会被识别为 class-block

        {
            val up_val = 42
            fun test_scope extends A() = this.v + up_val
            assert new A(1).test_scope() == 43
        }
    }
}