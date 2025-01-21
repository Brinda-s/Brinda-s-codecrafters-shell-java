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
                // Preserve the backslash and append the next character
                currentToken.append(ESCAPE);
                currentToken.append(c);
                escaped = false;
            } else if (c == ESCAPE) {
                // Handle escape sequence
                escaped = true;  // Escape the next character
            } else if (c == SINGLE && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;  // Toggle single quotes state
                if (!inSingleQuotes && currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else if (c == DOUBLE && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;  // Toggle double quotes state
                if (!inDoubleQuotes && currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (currentToken.length() > 0) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);  // Add non-whitespace character to the current token
            }
            
            index++;
        }
        
        // Handle any remaining token
        if (currentToken.length() > 0) {
            result.add(currentToken.toString());
        }
        
        return result;
    }
}
