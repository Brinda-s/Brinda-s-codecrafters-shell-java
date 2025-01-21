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
                if (inDoubleQuotes && (c == ESCAPE || c == DOUBLE || c == 'n')) {
                    // Handle valid escape sequences inside double quotes
                    if (c == 'n') {
                        currentToken.append('\n'); // Replace \n with newline
                    } else {
                        currentToken.append(c); // Keep the escaped character
                    }
                } else if (!inDoubleQuotes) {
                    // Add literal backslash followed by the character if outside quotes
                    currentToken.append(ESCAPE).append(c);
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
                currentToken.append(c); // Add the character to the current token
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
