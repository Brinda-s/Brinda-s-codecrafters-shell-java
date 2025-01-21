
import java.util.ArrayList;
import java.util.List;

public class LineParser {
    public static final char SINGLE = '\'';
    public static final char DOUBLE = '"';
    public static final char ESCAPE = '\\';

    private final String input;
    private int index;

    public LineParser(String input) {
        this.input = input;
        this.index = 0;
    }

    public List<String> parse() {
        List<String> result = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean escaped = false;

        while (index < input.length()) {
            char c = input.charAt(index);

            if (inSingleQuotes) {
                // Inside single quotes, everything is literal - no escaping
                if (c == SINGLE) {
                    inSingleQuotes = false;
                } else {
                    currentToken.append(c);
                }
            } else if (escaped) {
                // Handle escape sequences (only outside single quotes)
                if (inDoubleQuotes) {
                    // Inside double quotes, only certain characters are escaped
                    if (c == ESCAPE || c == DOUBLE || c == '\\' || c == 'n') {
                        if (c == 'n') {
                            currentToken.append('\n');
                        } else {
                            currentToken.append(c);
                        }
                    } else {
                        currentToken.append(ESCAPE).append(c);
                    }
                } else {
                    // Outside quotes, everything can be escaped
                    currentToken.append(c);
                }
                escaped = false;
            } else if (c == ESCAPE && !inSingleQuotes) {
                // Start escape sequence (only outside single quotes)
                escaped = true;
            } else if (c == SINGLE) {
                inSingleQuotes = true;
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);
            }

            index++;
        }

        // Add remaining token if exists
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }

        return result;
    }
}