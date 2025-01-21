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

            // Handle escape sequences
            if (escaped) {
                if (c == 'n') {
                    currentToken.append('\n'); // Handle newline escape
                } else {
                    currentToken.append(c); // Append the escaped character
                }
                escaped = false; // Reset escape flag
            } else if (c == ESCAPE) {
                escaped = true; // Set the escape flag
            } else if (c == SINGLE && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes; // Toggle single quotes
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes; // Toggle double quotes
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                // Handle whitespace outside quotes (split token)
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                // Inside quotes (single or double), append characters normally
                if (inSingleQuotes) {
                    currentToken.append(c); // Inside single quotes, treat everything literally
                } else if (inDoubleQuotes) {
                    currentToken.append(c); // Inside double quotes, append normally
                } else {
                    currentToken.append(c); // Outside quotes, just append
                }
            }

            index++; // Move to the next character
        }

        // Add the last token if any
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }

        return result;
    }
}
