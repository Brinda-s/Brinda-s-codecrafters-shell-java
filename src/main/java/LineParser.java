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
            } else {
                token.append(currentChar);  // Add non-whitespace character to the current token
            }

            index++;
        }

        // Add last token if any
        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        // Now handle the concatenation of quoted tokens
        List<String> finalTokens = new ArrayList<>();
        StringBuilder concatenatedToken = new StringBuilder();

        for (String currentToken : tokens) {
            if (concatenatedToken.length() > 0 && (currentToken.startsWith("\"") || currentToken.startsWith("\'"))) {
                // Concatenate quoted tokens directly, no space in between
                concatenatedToken.append(currentToken);
            } else {
                // Add current token to final tokens list
                if (concatenatedToken.length() > 0) {
                    finalTokens.add(concatenatedToken.toString());
                }
                concatenatedToken.setLength(0);  // Reset for next token
                concatenatedToken.append(currentToken);  // Start new token
            }
        }

        // Add the last concatenated token
        if (concatenatedToken.length() > 0) {
            finalTokens.add(concatenatedToken.toString());
        }

        // **Fix**: Ensure spaces between non-quoted tokens and avoid concatenating too closely
        List<String> finalResult = new ArrayList<>();
        StringBuilder resultToken = new StringBuilder(); // Renamed to `resultToken`
        boolean lastWasQuoted = false;

        for (String currentToken : finalTokens) {
            if (currentToken.startsWith("\"") || currentToken.startsWith("\'")) {
                // If it's a quoted token, append it directly to resultToken
                if (lastWasQuoted) {
                    resultToken.append(currentToken);  // Concatenate without space
                } else {
                    if (resultToken.length() > 0) {
                        finalResult.add(resultToken.toString());  // Add previous token
                    }
                    resultToken.setLength(0);  // Reset for next token
                    resultToken.append(currentToken);
                }
                lastWasQuoted = true;
            } else {
                if (lastWasQuoted && resultToken.length() > 0) {
                    finalResult.add(resultToken.toString());  // Add quoted token
                    resultToken.setLength(0);  // Reset
                }
                lastWasQuoted = false;
                // Add space between non-quoted tokens if they are separate
                if (resultToken.length() > 0) {
                    resultToken.append(" ");  // Add a space between separate non-quoted tokens
                }
                resultToken.append(currentToken);  // For non-quoted tokens, keep adding
            }
        }

        if (resultToken.length() > 0) {
            finalResult.add(resultToken.toString());  // Add any remaining token
        }

        return finalResult;
    }
}
