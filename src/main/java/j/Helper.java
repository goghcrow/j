package j;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * 💐 ❌ ✅ 🍺 🚀 👻 🤢 😓 😱
 * @author chuxiaofeng
 */
public interface Helper {

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    static @NotNull String read(@NotNull Path path) {
        try {
            return UTF_8.decode(ByteBuffer.wrap(Files.readAllBytes(path))).toString();
        } catch (IOException e) {
            Helper.sneakyThrows(e);
            return null;
        }
    }

    static @NotNull Path resource2path(String name) {
        try {
            URL r = Helper.class.getResource(name);
            if (r == null) {
                throw Error.runtime(null,"木有找到文件 → " + name);
            }
            return Paths.get(r.toURI());
        } catch (URISyntaxException e) {
            Helper.sneakyThrows(e);
            return null;
        }
    }

    static boolean fileExists(String src) {
        try {
            return Files.exists(Paths.get(src));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    static void err(String msg) {
        System.err.println(msg);
    }

    static void log(String msg) {
        System.out.println(msg);
    }


    static <T> String join(@NotNull Collection<T> c, String sep) {
        return c.stream().map(Object::toString).collect(Collectors.joining(sep));
    }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

//    class CircularReferenceProtector<T> {
//        boolean flag = false;
//        public T protectOrDefault(Supplier<T> f, T defaultValue) {
//            if (flag) {
//                return defaultValue;
//            }
//            try {
//                flag = true;
//                return f.get();
//            } finally {
//                flag = false;
//            }
//        }
//        public T protectOrThrow(Supplier<T> f, Exception e) {
//            if (flag) {
//                sneakyThrows(e);
//            }
//            try {
//                flag = true;
//                return f.get();
//            } finally {
//                flag = false;
//            }
//        }
//    }

    // -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ \\

    @SafeVarargs
    static <T> List<T> lists(T...els) {
        List<T> lst = new ArrayList<>(els.length);
        lst.addAll(Arrays.asList(els));
        return lst;
    }

    static <T extends Throwable> void sneakyThrows(@NotNull Throwable e) throws T {
        //noinspection unchecked
        throw ((T) e);
    }

    @NotNull
    static <T extends Value> T field(Field f, Object obj) {
        try {
            //noinspection unchecked
            return ((T) f.get(obj));
        } catch (IllegalAccessException e) {
            sneakyThrows(e);
            return null;
        }
    }

    @NotNull
    static Method method(Class<?> c, String name, Class<?>... paramTypes) {
        try {
            return c.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            sneakyThrows(e);
            return null;
        }
    }


}
