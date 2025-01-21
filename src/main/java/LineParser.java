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
                // Handle escaped characters
                if (inDoubleQuotes && (c == DOUBLE || c == ESCAPE || c == '$' || c == '\n')) {
                    currentToken.append(c); // Add the escaped character
                } else if (!inDoubleQuotes) {
                    currentToken.append(ESCAPE).append(c); // Add literal backslash + character
                } else {
                    currentToken.append(c); // Add as is
                }
                escaped = false; // Reset escaped flag
            } else if (c == ESCAPE) {
                escaped = true; // Start escape sequence
            } else if (c == SINGLE && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes; // Toggle single quotes
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes; // Toggle double quotes
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0); // Clear the token
                }
            } else {
                currentToken.append(c); // Add regular character
            }

            index++;
        }

        // Add the last token if present
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }

        return result;
    }
}
