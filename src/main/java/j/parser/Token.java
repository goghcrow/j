package j.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * token 是个next惰性的双向链表
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public class Token<T extends Enum<T>> {
    Lexer<T> lexer;
    Token<T> next;
    Token<T> prev;

    public final T type;
    public final String lexeme;
    public final Location loc;
    public final boolean keep;

    public Token(@NotNull Lexer<T> lexer, @Nullable Token<T> prev,
                 @NotNull T type, @NotNull String lexeme, @NotNull Location loc, boolean keep) {
        this.lexer = lexer;
        this.prev = prev;
        this.type = type;
        this.lexeme = lexeme;
        this.loc = loc;
        this.keep = keep;
    }

    public static <T extends Enum<T>> Token<T> copy(@NotNull Token<T> from, T type) {
        return new Token<>(from.lexer, from.prev, type, from.lexeme, from.loc, from.keep);
    }

    public boolean is(@NotNull T type) {
        return Objects.equals(this.type, type);
    }

    @NotNull
    public Token<T> getNext() throws NoSuchElementException {
        if (is(lexer.EOF)) {
            throw new NoSuchElementException();
        }

        if (next == null) {
            next = lexer.nextKeepToken(this);
        }
        return next;
    }

    public Token<T> getPrev() throws NoSuchElementException {
        if (prev == null) {
            throw new NoSuchElementException();
        }
        return prev;
    }

    @Override
    public int hashCode() {
        return 59 + Objects.hashCode(type) + Objects.hashCode(lexeme) + Objects.hashCode(loc);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Token) {
            Token<?> other = (Token<?>) obj;
            return type == other.type && lexeme.equals(other.lexeme) && loc.equals(other.loc);
        }
        return false;
    }

    @Override
    public String toString() {
        return lexeme;
    }
}
