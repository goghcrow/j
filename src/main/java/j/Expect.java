package j;

import j.parser.Location;

/**
 * @author chuxiaofeng
 */
public interface Expect {
    static <T> T notNull(Location loc, T o, String msg) {
        if (o == null) {
            throw Error.runtime(loc, msg);
        }
        return o;
    }

    static <T> T notNull(T o, String msg) {
        if (o == null) {
            throw new RuntimeException(msg);
        }
        return o;
    }

    static void notEmpty(Object[] a, String msg) {
        if (a.length == 0) {
            throw new RuntimeException(msg);
        }
    }

    static void notEmpty(String str, String msg) {
        if (str == null || str.isEmpty()) {
            throw new RuntimeException(msg);
        }
    }

    static void expect(Location loc, boolean bool, String msg) {
        if (!bool) {
            throw Error.runtime(loc, msg);
        }
    }

    static void expect(boolean bool, String msg) {
        if (!bool) {
            throw new RuntimeException(msg);
        }
    }

    static void exactArgs(Location loc, int expect, int actual) {
        if (actual != expect) {
            throw Error.runtime(loc, String.format("需要 %d 个参数, 实际只有 %d 个", expect, actual));
        }
    }

    static void atLeastArgs(Location loc, int expect, int actual) {
        if (actual < expect) {
            throw Error.runtime(loc, String.format("至少需要 %d 个参数, 实际只有 %d 个", expect, actual));
        }
    }

    static <T> T as(Location loc, Object obj, Class<T> type) {
        if (obj != null && type.isAssignableFrom(obj.getClass())) {
            return type.cast(obj);
        }
        throw Error.type(loc, "类型错误, 期望 " + type + " ,实际是" + obj);
    }
}
