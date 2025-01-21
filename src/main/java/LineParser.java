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
                // Handle escape sequences inside quotes
                if (c == ESCAPE || c == '"' || c == '\\' || c == 'n') {
                    if (c == 'n') {
                        currentToken.append('\n'); // Handle newline escape
                    } else {
                        currentToken.append(c); // Handle other escape sequences
                    }
                } else {
                    // Invalid escape sequence
                    currentToken.append(ESCAPE).append(c);
                }
                escaped = false;
            } else if (c == ESCAPE) {
                // Handle the start of escape sequence
                escaped = true;
            } else if (c == SINGLE && !inDoubleQuotes) {
                // Toggle single quote only when not inside double quotes
                inSingleQuotes = !inSingleQuotes;
            } else if (c == DOUBLE && !inSingleQuotes) {
                // Toggle double quote only when not inside single quotes
                inDoubleQuotes = !inDoubleQuotes;
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                // Add token if not in quotes and whitespace encountered
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                // Append character to the current token
                currentToken.append(c);
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
