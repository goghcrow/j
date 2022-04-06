package j;

import j.parser.Location;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.lang.Math.*;

/**
 * ʕ•ᴥ•ʔ
 * ʅʕ•ᴥ•ʔʃ
 * ᵔᴥᵔ
 * ʕ•͡•ʔ
 * ʕ∙ჲ∙ʔ
 * ʕ·ᴥ·ʔ
 * ʕ •ᴥ•ʔ
 * ʕ •ᴥ• ʔ
 * ʕ´•ᴥ•`ʔ
 * ʕ´• ᴥ •`ʔ
 * ʕ´•ᴥ•`ʔ
 * ʕ◕ ͜ʖ◕ʔ
 * ʕ •́؈•̀ ₎
 * http://japaneseemoticons.me/all-japanese-emoticons/3/
 * ❌✅
 * 🍺💐🎂👏
 * ✔️✔✘❌✖✕☓✗☒☑✅❎💯
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class Error extends RuntimeException {

    public static Error mixed(List<Error> errors) {
        return new Mixed(errors);
    }

    public static Error lexer(Location loc, String msg) {
        return new Lexer(loc, msg);
    }

    public static Error syntax(Location loc, String msg) {
        return new Syntax(loc, msg);
    }

    public static Error type(Location loc, String msg) {
        return new Type(loc, msg);
    }

    public static RuntimeException runtime(Location loc, String msg) {
        Interp interp = J.interp();
        if (interp.Class_RuntimeError == null) {
            return new Runtime(loc, msg);
        } else {
            return interp.error(loc, interp.Class_RuntimeError, msg);
        }
    }

    public static RuntimeException nativeError(Location loc, String ex, String msg) {
        throw J.interp().nativeError(loc, ex, msg);
    }

    public static RuntimeException nullPointerError(Location loc, String msg) {
        throw J.interp().nullPointerError(loc, msg);
    }

    public static RuntimeException nullPointerError(Location loc, Scope s, String msg) {
        throw J.interp(s).nullPointerError(loc, msg);
    }

    public static Error bug(Location loc) {
        return new Error(loc, "🐞");
    }

    public static Error bug(Location loc, String msg) {
        return new Error(loc, "🐞 " + msg);
    }

    public static Error todo(Location loc, String msg) {
        return new Error(loc, msg + " " + loc);
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    public static class Mixed extends Error {
        List<Error> errors;
        public Mixed(List<Error> errors) {
            super(errors.isEmpty() ? null : errors.get(0).loc, errors.isEmpty() ? "" : errors.get(0).msg);
            this.errors = errors;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Error e : errors) {
                sb.append(e.toString());
                sb.append("\n\n");
            }
            return sb.toString();
        }
    }

    public static class Lexer extends Error {
        public Lexer(Location loc, String msg) {
            super(loc, msg);
        }
    }

    public static class Syntax extends Error {
        public Syntax(Location loc, String msg) {
            super(loc, msg);
        }
    }

    public static class Type extends Error {
        public Type(Location loc, String msg) {
            super(loc, msg);
        }
    }

    public static class Runtime extends Error {
        public Runtime(Location loc, String msg) {
            super(loc, msg);
        }
    }

    // =+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

    @Nullable
    public final Location loc;
    public final String msg;
    public Error(@Nullable Location loc, String msg) {
        this.loc = loc;
        this.msg = msg;
    }

    @Override
    public String toString() {
        if (loc == null || loc == Location.None) {
            return msg;
        } else {
            String codeSpan = loc.si.input.substring(
                    max(0, loc.idxBegin),
                    min(loc.si.input.length(), loc.idxEnd)
            );
            // todo: 做一个能看的错误提示 e.g.
            /*
            ... a = 1
            ... -----^
            这里缺了一个分号
             */
            // String code = loc.si.input.substring(loc.idxBegin, loc.idxEnd);
            return msg + " [" + loc + "] " + "\n" + codeSpan + "\n";
        }
    }
}
