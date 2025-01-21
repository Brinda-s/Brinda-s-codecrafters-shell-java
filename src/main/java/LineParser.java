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
        boolean wasQuoted = false;

        while (index < input.length()) {
            char currentChar = input.charAt(index);

            if (currentChar == SINGLE) {
                insideSingleQuotes = !insideSingleQuotes;
                if (!insideSingleQuotes && token.length() > 0) {
                    // Only add token if we're exiting quotes and have content
                    tokens.add(token.toString());
                    token.setLength(0);
                    wasQuoted = true;
                }
            } else if (currentChar == DOUBLE) {
                insideDoubleQuotes = !insideDoubleQuotes;
                if (!insideDoubleQuotes && token.length() > 0) {
                    // Only add token if we're exiting quotes and have content
                    tokens.add(token.toString());
                    token.setLength(0);
                    wasQuoted = true;
                }
            } else if (Character.isWhitespace(currentChar)) {
                if (insideSingleQuotes || insideDoubleQuotes) {
                    token.append(currentChar);
                } else if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                    wasQuoted = false;
                }
            } else {
                token.append(currentChar);
            }
            index++;
        }

        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        // Process tokens to handle concatenation
        List<String> result = new ArrayList<>();
        StringBuilder concatenated = new StringBuilder();
        boolean previousWasQuoted = false;

        for (String t : tokens) {
            boolean currentIsQuoted = t.startsWith("\"") || t.startsWith("\'");
            
            if (currentIsQuoted && previousWasQuoted) {
                // If current token is quoted and previous was quoted, concatenate without space
                concatenated.append(t);
            } else {
                if (concatenated.length() > 0) {
                    // Add previous concatenated content if any
                    result.add(concatenated.toString());
                }
                concatenated = new StringBuilder(t);
            }
            
            previousWasQuoted = currentIsQuoted;
        }

        if (concatenated.length() > 0) {
            result.add(concatenated.toString());
        }

        // Final step: remove quotes from output
        List<String> finalResult = new ArrayList<>();
        for (String t : result) {
            if ((t.startsWith("\"") && t.endsWith("\"")) || 
                (t.startsWith("\'") && t.endsWith("\'"))) {
                finalResult.add(t.substring(1, t.length() - 1));
            } else {
                finalResult.add(t);
            }
        }

        return finalResult;
    }
}