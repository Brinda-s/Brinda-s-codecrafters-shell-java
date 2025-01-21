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

        boolean firstToken = true; // Flag to handle first token's leading space

        while (index < input.length()) {
            char currentChar = input.charAt(index);

            // Handle entering and exiting quotes (single or double)
            if (currentChar == SINGLE) {
                if (insideSingleQuotes) {
                    tokens.add(token.toString());  // Add current token
                    token.setLength(0);  // Reset for next token
                }
                insideSingleQuotes = !insideSingleQuotes;  // Toggle single quote state
            } else if (currentChar == DOUBLE) {
                if (insideDoubleQuotes) {
                    tokens.add(token.toString());  // Add current token
                    token.setLength(0);  // Reset for next token
                }
                insideDoubleQuotes = !insideDoubleQuotes;  // Toggle double quote state
            } else if (Character.isWhitespace(currentChar)) {
                if (insideSingleQuotes || insideDoubleQuotes) {
                    token.append(currentChar);  // Inside quotes, space is part of the token
                } else {
                    // If outside quotes, treat space as a separator
                    if (token.length() > 0) {
                        tokens.add(token.toString());  // Add token
                        token.setLength(0);  // Reset for next token
                    }
                }
                firstToken = false;  // After first space, no longer treat it as leading space
            } else {
                token.append(currentChar);  // Add non-whitespace character to the current token
            }

            index++;
        }

        // Add last token if any
        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        // Now, ensure no extra spaces between quoted strings
        List<String> finalTokens = new ArrayList<>();
        StringBuilder concatenatedToken = new StringBuilder();

        for (String currentToken : tokens) {
            if (concatenatedToken.length() > 0 && !currentToken.startsWith("\"") && !currentToken.startsWith("\'")) {
                // Concatenate directly without adding spaces
                concatenatedToken.append(currentToken);
            } else {
                if (concatenatedToken.length() > 0) {
                    finalTokens.add(concatenatedToken.toString());
                }
                concatenatedToken.setLength(0);  // Reset for next token
                concatenatedToken.append(currentToken);
            }
        }

        // Add the last concatenated token
        if (concatenatedToken.length() > 0) {
            finalTokens.add(concatenatedToken.toString());
        }

        return finalTokens;
    }
}
