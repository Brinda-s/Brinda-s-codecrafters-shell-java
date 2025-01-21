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
                // Handle escape sequences for both single and double quotes
                if (inSingleQuotes) {
                    // In single quotes, escape sequences should be treated literally
                    currentToken.append(c);
                } else if (inDoubleQuotes) {
                    // In double quotes, handle escape sequences like \n or \\
                    if (c == 'n') {
                        currentToken.append('\n'); // Translate \n to newline
                    } else {
                        currentToken.append(c); // Handle escape like \ or "
                    }
                } else {
                    currentToken.append(c); // Append any other character after escape
                }
                escaped = false;
            } else if (c == ESCAPE) {
                escaped = true; // Start of an escape sequence
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
                currentToken.append(c); // Regular character handling
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
