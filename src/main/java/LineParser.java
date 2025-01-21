import java.util.ArrayList;
import java.util.List;

public class LineParser {
    public static final char SINGLE = '\'';
    public static final char DOUBLE = '"';

    private final String input;
    private int index;

    public LineParser(String input) {
        this.input = input;
        this.index = 0;
    }

    public List<String> parse() {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean insideSingleQuotes = false;
        boolean insideDoubleQuotes = false;

        while (index < input.length()) {
            char currentChar = input.charAt(index);

            if (currentChar == SINGLE && !insideDoubleQuotes) {
                if (insideSingleQuotes) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                insideSingleQuotes = !insideSingleQuotes;
            } else if (currentChar == DOUBLE && !insideSingleQuotes) {
                if (insideDoubleQuotes) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                insideDoubleQuotes = !insideDoubleQuotes;
            } else if (Character.isWhitespace(currentChar) && !insideSingleQuotes && !insideDoubleQuotes) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(currentChar);
            }

            index++;
        }

        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        // Now handle concatenation
        StringBuilder output = new StringBuilder();
        List<String> result = new ArrayList<>();
        
        for (int i = 0; i < tokens.size(); i++) {
            String current = tokens.get(i);
            
            if (output.length() > 0) {
                // If the current token starts with a quote, concatenate without space
                if (current.startsWith("\"") || current.startsWith("\'")) {
                    output.append(current);
                } else {
                    // If not quoted, add the previous output and start new
                    result.add(output.toString());
                    output = new StringBuilder(current);
                }
            } else {
                output.append(current);
            }
            
            // Handle the last token
            if (i == tokens.size() - 1 && output.length() > 0) {
                result.add(output.toString());
            }
        }

        return result;
    }
}