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

            if (escaped) {
                // Handle escape sequences
                if (inDoubleQuotes) {
                    if (c == DOUBLE || c == ESCAPE || c == 'n') {
                        if (c == 'n') {
                            currentToken.append('\n'); // Translate \n to newline
                        } else {
                            currentToken.append(c); // Add escaped double quote or backslash
                        }
                    } else {
                        currentToken.append(ESCAPE).append(c); // Invalid escape, treat as literal
                    }
                } else {
                    currentToken.append(c); // Append literal character after escape
                }
                escaped = false;
            } else if (c == ESCAPE) {
                escaped = true; // Start escape sequence
            } else if (c == SINGLE && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes; // Toggle single quotes
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes; // Toggle double quotes
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                // Add token and reset if not in quotes
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c); // Add regular character
            }

            index++;
        }

        // Add remaining token to the result
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }

        return result;
    }
}