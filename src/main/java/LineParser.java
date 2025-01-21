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
        StringBuilder token = new StringBuilder();  // Only one token variable
        boolean insideSingleQuotes = false;
        boolean insideDoubleQuotes = false;

        // Parsing loop
        while (index < input.length()) {
            char currentChar = input.charAt(index);

            if (currentChar == SINGLE) {
                if (insideSingleQuotes) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                insideSingleQuotes = !insideSingleQuotes;
            } else if (currentChar == DOUBLE) {
                if (insideDoubleQuotes) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                insideDoubleQuotes = !insideDoubleQuotes;
            } else if (Character.isWhitespace(currentChar)) {
                if (insideSingleQuotes || insideDoubleQuotes) {
                    token.append(currentChar);  // Space inside quotes is part of the token
                } else if (token.length() > 0) {
                    tokens.add(token.toString());  // Space outside quotes separates tokens
                    token.setLength(0);
                }
            } else {
                token.append(currentChar);
            }

            index++;
        }

        // Add any remaining token
        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        // Now handle the concatenation of quoted tokens
        List<String> finalResult = new ArrayList<>();
        StringBuilder concatenatedToken = new StringBuilder();
        boolean lastWasQuoted = false;

        // Process final tokens and concatenate quoted tokens
        for (int i = 0; i < tokens.size(); i++) {
            String currentToken = tokens.get(i);
            boolean isQuoted = currentToken.startsWith("\"") || currentToken.startsWith("\'");

            if (isQuoted && lastWasQuoted) {
                // If both current and last tokens are quoted, concatenate without space
                concatenatedToken.append(currentToken);
            } else {
                // If we have a pending token, add it to final result
                if (concatenatedToken.length() > 0) {
                    finalResult.add(concatenatedToken.toString());
                }
                concatenatedToken.setLength(0);  // Reset
                concatenatedToken.append(currentToken);
            }

            lastWasQuoted = isQuoted;
        }

        // Add any remaining token to finalResult
        if (concatenatedToken.length() > 0) {
            finalResult.add(concatenatedToken.toString());
        }

        return finalResult;
    }

    public static void main(String[] args) {
        // Example usage
        String input = "echo \"hello  world\" \"test\"";
        LineParser parser = new LineParser(input);
        List<String> result = parser.parse();

        // Print parsed tokens
        for (String token : result) {
            System.out.println(token);
        }
    }
}
