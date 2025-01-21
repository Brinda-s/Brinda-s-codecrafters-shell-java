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

            // Handle entering and exiting quotes (single or double)
            if (currentChar == SINGLE) {
                if (insideSingleQuotes) {
                    tokens.add(token.toString());  // Add current token (content inside single quotes)
                    token.setLength(0);  // Reset for next token
                }
                insideSingleQuotes = !insideSingleQuotes;  // Toggle single quote state
            } else if (currentChar == DOUBLE) {
                if (insideDoubleQuotes) {
                    tokens.add(token.toString());  // Add current token (content inside double quotes)
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
            } else {
                token.append(currentChar);  // Add non-whitespace character to the current token
            }

            index++;
        }

        // Add last token if any
        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        // Now handle concatenating quoted tokens together without spaces
        List<String> finalTokens = new ArrayList<>();
        StringBuilder concatenatedToken = new StringBuilder();
        boolean lastWasQuoted = false;

        for (String currentToken : tokens) {
            if (currentToken.startsWith("\"") || currentToken.startsWith("\'")) {
                // Concatenate quoted tokens directly, no space in between
                if (lastWasQuoted) {
                    concatenatedToken.append(currentToken);
                } else {
                    if (concatenatedToken.length() > 0) {
                        finalTokens.add(concatenatedToken.toString());  // Add non-quoted token
                    }
                    concatenatedToken.setLength(0);  // Reset
                    concatenatedToken.append(currentToken);
                }
                lastWasQuoted = true;
            } else {
                // If it's a non-quoted token, add the space between it and any previous token
                if (lastWasQuoted) {
                    if (concatenatedToken.length() > 0) {
                        finalTokens.add(concatenatedToken.toString());
                    }
                    concatenatedToken.setLength(0);  // Reset after quoted token
                }

                if (concatenatedToken.length() > 0) {
                    finalTokens.add(concatenatedToken.toString());
                }

                concatenatedToken.setLength(0);
                concatenatedToken.append(currentToken);  // Start a new token

                lastWasQuoted = false;
            }
        }

        if (concatenatedToken.length() > 0) {
            finalTokens.add(concatenatedToken.toString());  // Add any remaining token
        }

        return finalTokens;
    }
}
