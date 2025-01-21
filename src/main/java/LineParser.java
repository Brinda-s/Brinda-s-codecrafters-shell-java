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

            // Handle escape sequence
            if (escaped) {
                if (c == 'n') {
                    currentToken.append('\n'); // Handle newline escape sequence
                } else {
                    currentToken.append(c); // Handle other escape sequences (\\, \", etc.)
                }
                escaped = false;
            } else if (c == ESCAPE) {
                // Set escape flag to process escape sequences
                escaped = true;
            } else if (c == SINGLE && !inDoubleQuotes) {
                // Toggle single quotes only when not inside double quotes
                inSingleQuotes = !inSingleQuotes;
            } else if (c == DOUBLE && !inSingleQuotes) {
                // Toggle double quotes only when not inside single quotes
                inDoubleQuotes = !inDoubleQuotes;
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                // Add token if not in quotes and whitespace encountered
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                // If inside single quotes, treat backslashes literally
                if (inSingleQuotes) {
                    currentToken.append(c);
                } else {
                    currentToken.append(c);
                }
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

