package j;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

/**
 * @author chuxiaofeng
 */
public class ClassScopeTest {

    @Test
    public void test0() {
        int x = 1;

        class A {
            int a = x;
        }

        {
            int a = 42;
            int y = 100;
            class B extends A {
                int b = a;
                int c = y;
            }

            assertEquals(1, new B().a);
            assertEquals(1, new B().b);
            assertEquals(100, new B().c);
        }
    }

    @Test
    public void test1() {
        int x = 1;
        class A {  int a = x; }

        {
            int a = 42;
            class B extends A {
                int a1;
                B(int a) { this.a1 = a; }
            }

            assertEquals(1, new B(a).a);
            assertEquals(42, new B(a).a1);

            new B(a){
                void m() {
                    assertEquals(1, a);
                    assertEquals(42, a1);
                }
            }.m();
        }
    }

    @Test
    public void test2() {
        AtomicLong i = new AtomicLong(1);
        class B { AtomicLong a = i; }

        B b1 = new B();
        B b2 = new B();
        assertEquals(1, b1.a.get());
        assertEquals(1, b2.a.get());

        i.set(42);
        assertEquals(42, b1.a.get());
        assertEquals(42, b2.a.get());
    }

    @Test
    public void test3() {
        abstract class A {
            int id;
            A(int id) { this.id = id; }
        }

        new A(1) {};
    }
}
