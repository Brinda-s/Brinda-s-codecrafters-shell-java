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
                if (inDoubleQuotes || inSingleQuotes) {
                    // Handle valid escapes within quotes
                    if (c == ESCAPE || c == DOUBLE || c == SINGLE || c == 'n') {
                        currentToken.append(c == 'n' ? '\n' : c);
                    } else {
                        currentToken.append(ESCAPE).append(c); // Invalid escape
                    }
                } else {
                    currentToken.append(ESCAPE).append(c); // Handle escapes outside quotes
                }
                escaped = false;
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
                currentToken.append(c); // Add character to current token
            }

            index++;
        }

        // Add the remaining token to the result
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }

        return result;
    }

    public static void main(String[] args) {
        // Example test cases
        String input1 = "echo \"script'world'\\n'test\"";
        String input2 = "cat '/tmp/quz/\"f 24\"' '/tmp/quz/\"f\\41\"' '/tmp/quz/f27'";
        
        LineParser parser1 = new LineParser(input1);
        LineParser parser2 = new LineParser(input2);

        System.out.println(parser1.parse());
        System.out.println(parser2.parse());
    }
}
