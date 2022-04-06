package j.parser;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static j.parser.Lexer.SourceInput;

/**
 * Position
 * Source File Location
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
public final class Location {
    public final static Location None = new Location(null, -1, -1, -1, -1, -1, -1);

    public final SourceInput si;
    public final int idxBegin;  // inclusive
    public final int idxEnd;    // exclusive
    public final int rowBegin;
    public final int colBegin;
    public final int rowEnd;
    public final int colEnd;

    public Location(SourceInput si, int idxBegin, int idxEnd, int rowBegin, int colBegin, int rowEnd, int colEnd) {
        this.si = si;
        this.idxBegin = idxBegin;
        this.idxEnd = idxEnd;
        this.rowBegin = rowBegin;
        this.colBegin = colBegin;
        this.rowEnd = rowEnd;
        this.colEnd = colEnd;
    }

    public String codeSpan() {
        if (this == Location.None || si == null) {
            return "";
        } else {
            return si.input.substring(idxBegin, idxEnd);
        }
    }

    public boolean contains(@NotNull Location other) {
        if (this == None || other == None) {
            return false;
        }
        return idxBegin <= other.idxBegin && idxEnd >= other.idxEnd;
    }

    /**
     * CodeSpan
     */
    public static Location range(@NotNull Token<?> from, @NotNull Token<?> to) {
        return range(from.loc, to.loc);
    }

    public static Location range(@NotNull Location from, @NotNull Location to) {
        return new Location(from.si, from.idxBegin, to.idxEnd, from.rowBegin, from.colBegin, to.rowEnd, to.colEnd);
    }

    @Override
    public int hashCode() {
        return idxBegin + idxEnd + rowBegin + colBegin + rowEnd + colEnd;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Location) {
            Location other = (Location) obj;
            return idxBegin == other.idxBegin && idxEnd == other.idxEnd
                    && rowBegin == other.rowBegin && colBegin == other.colBegin
                    && rowEnd == other.rowEnd && colEnd == other.colEnd;
        }
        return false;
    }


    static Pattern NON_SPACE = Pattern.compile("\\S");
    public String inspect(String msg) {
        if (this == None) {
            return msg;
        }
        String[] lines = si.input.split("\\r?\\n");
        StringBuilder buf = new StringBuilder();
        int max = 3;

        int lineNoLen = (Math.max(rowEnd + max, lines.length) + "").length();
        for (int i = max; i > 0; i--) {
            if (rowBegin > i) {
                buf.append(inspectLine(lines, rowBegin - i, lineNoLen));
            }
        }
        if (rowBegin == rowEnd) {
            buf.append(highlightLine(msg, lines, rowBegin, colBegin, colEnd, lineNoLen));
        } else {
            for (int i = rowBegin; i < rowEnd + 1; i++) {
                Matcher m = NON_SPACE.matcher(lines[i - 1]);
                buf.append(highlightLine(i == rowEnd ? msg : null, lines, i, m.find() ? m.start() + 1 : 1, lines[i - 1].length() + 1, lineNoLen));
            }
        }
        for (int i = 1; i < max + 1; i++) {
            if (rowEnd + i <= lines.length) {
                buf.append(inspectLine(lines, rowEnd + i, lineNoLen));
            }
        }
        return buf.toString();
    }
    private String repeatSP(int n) { return new String(new char[n]).replace("\0", " "); }
    String inspectLine(String[] lines, int rowBegin, int lineNoLen) {
        String prefixSp = repeatSP(lineNoLen - (rowBegin + "").length());
        return "\n" + prefixSp + rowBegin + " | " + lines[rowBegin - 1];
    }
    String highlightLine(String msg, String[] lines, int rowBegin, int colBegin, int colEnd, int lineNoLen) {
        String prefixSp = repeatSP(lineNoLen - (rowBegin + "").length());
        String fstPrefix = prefixSp + rowBegin + " | ";
        String prefix = repeatSP((prefixSp + rowBegin).length()) + " | ";
        String nSpaces = repeatSP(colBegin - 1);
        prefix += nSpaces;

        StringBuilder buf = new StringBuilder("\n");
        buf.append(fstPrefix);

        String line = lines[rowBegin - 1];
        buf.append(line);
        if (buf.charAt(buf.length() - 1) != '\n') {
            buf.append("\n");
        }

        buf.append(prefix);
        for (int i = 0; i < colEnd - colBegin; i++) {
            buf.append("^");
        }
        // buf.append("\n");

        // buf.append(prefix);
        if (msg != null) {
            buf.append(" ");
            buf.append(msg);
        }

        return buf.toString();
    }

    @Override
    public String toString() {
        if (this == None) {
            return "None";
        }
        if (si.src.isEmpty()) {
            return "第" + rowBegin + "行第" + colBegin + "列";
        } else {
            return "第" + rowBegin + "行第" + colBegin + "列 @ " + si.src;
        }
        // return si.src + ":" + rowBegin + ":" + colBegin /*+ " → " + si.input.substring(idxBegin, idxEnd)*/;
    }
}
